package com.playmonumenta.plugins.abilities.mage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * LEVEL 1: Hitting an enemy with a spell inflicts “static” for 6 seconds. If an enemy
 * with static is hit by another spell, a spellshock centered on the enemy deals 3
 * damage to all mobs in a 3 block radius. Spellshock can cause a chain reaction on
 * enemies with static. An enemy can only be hit by a spellshock once per tick.
 *
 * LEVEL 2: Damage is increased to 5.
 * Additionally, gain speed 1 for 6 seconds whenever a spellshock is triggered.
 */
public class Spellshock extends Ability {

	public static class SpellShockedMob {
		public boolean mTriggered = false;
		public Player mTriggeredBy;
		public LivingEntity mMob;
		public int mTicksLeft = SPELL_SHOCK_DURATION;

		public SpellShockedMob(LivingEntity mob) {
			this.mMob = mob;
		}
	}

	private static final int SPELL_SHOCK_DURATION = 6 * 20;
	private static final int SPELL_SHOCK_RADIUS = 3;
	private static final int SPELL_SHOCK_1_DAMAGE = 3;
	private static final int SPELL_SHOCK_2_DAMAGE = 5;
	private static final Particle.DustOptions SPELL_SHOCK_COLOR = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);

	private static final Map<UUID, SpellShockedMob> mSpellShockedMobs = new HashMap<>();
	private static final Set<LivingEntity> mPendingStaticMobs = new HashSet<>();
	private static BukkitRunnable mRunnable = null;

	public Spellshock(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Spellshock");
		mInfo.mLinkedSpell = Spells.SPELLSHOCK;
		mInfo.mScoreboardId = "SpellShock";
		mInfo.mShorthandName = "SS";
		mInfo.mDescriptions.add("Hitting an enemy with a wand or spell inflicts “static” for 6 seconds. If an enemy with static is hit by another spell, a spellshock centered on the enemy deals 3 damage to all mobs in a 3 block radius. Spellshock can cause a chain reaction on enemies with static. An enemy can only be hit by a spellshock once per tick.");
		mInfo.mDescriptions.add("Damage is increased to 5. Additionally, gain Speed 1 for 6 seconds whenever a spellshock is triggered.");
		/*
		 * Only one runnable ever exists for spellshock - it is a global list, not tied to any individual players
		 * At least one player must be a mage for this to start running. Once started, it runs forever.
		 */
		if (mRunnable == null || mRunnable.isCancelled()) {
			mRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					// Do at most 10 loops to get all the mobs caught in the spellshock chain
					Map<LivingEntity, Player> pendingDamageMobs = new HashMap<>();
					boolean continueLooping;
					for (int i = 0; i < 10; i++) {
						Set<UUID> triggeredMobs = new HashSet<>();
						continueLooping = false;
						for (Map.Entry<UUID, SpellShockedMob> entry : mSpellShockedMobs.entrySet()) {
							SpellShockedMob e = entry.getValue();
							if (e.mTriggered) {
								triggeredMobs.add(entry.getKey());
								runTriggerParticles(e.mMob.getLocation().add(0, 1, 0));
								// Apply speed I for 6 seconds if player has level 2 spellshock
								if (ScoreboardUtils.getScoreboardValue(e.mTriggeredBy, "SpellShock") > 1) {
									plugin.mPotionManager.addPotion(e.mTriggeredBy, PotionID.ABILITY_SELF,
									                                new PotionEffect(PotionEffectType.SPEED, SPELL_SHOCK_DURATION, 0));
								}
								for (LivingEntity le : EntityUtils.getNearbyMobs(e.mMob.getLocation(), SPELL_SHOCK_RADIUS)) {
									// Add nearby mobs to the damage queue
									pendingDamageMobs.put(le, e.mTriggeredBy);
									// If the mob has static and hasn't been triggered, trigger it and do another loop later
									UUID leUniqueId = le.getUniqueId();
									if (mSpellShockedMobs.containsKey(leUniqueId) && !triggeredMobs.contains(leUniqueId)) {
										mSpellShockedMobs.get(leUniqueId).mTriggered = true;
										mSpellShockedMobs.get(leUniqueId).mTriggeredBy = e.mTriggeredBy;
										continueLooping = true;
									}
								}
							}
						}
						mSpellShockedMobs.keySet().removeAll(triggeredMobs);
						if (!continueLooping) {
							break;
						}
					}

					// Damage the mobs all at once to prevent repeat damage applications
					for (Map.Entry<LivingEntity, Player> entry : pendingDamageMobs.entrySet()) {
						LivingEntity damagee = entry.getKey();
						Player damager = entry.getValue();
						int abilityScore = ScoreboardUtils.getScoreboardValue(damager, "SpellShock");
						double damage = abilityScore == 1 ? SPELL_SHOCK_1_DAMAGE : SPELL_SHOCK_2_DAMAGE;
						// Since spellshock damage is applied on the tick after initial spell damage, EntityUtils.damageEntity()
						// will not see it as intentional damage stacking, so iFrames need to be set manually
						damagee.setNoDamageTicks(0);
						Vector velocity = damagee.getVelocity();
						// This won't proc Perspicacity unless we rework how that enchantment works
						// This is because it doesn't call the CustomDamageEvent
						EntityUtils.damageEntity(plugin, damagee, damage, damager, MagicType.ARCANE, false /* do not register CustomDamageEvent */, mInfo.mLinkedSpell);
						damagee.setVelocity(velocity);
					}

					// Only put pending static mobs into the actual map if they weren't damaged by spellshock - this
					// prevents you from doing something like static'ing half the mobs in a group at any given time,
					// which would allow you to trigger spellshock on every spellcast
					for (LivingEntity mob : mPendingStaticMobs) {
						if (!pendingDamageMobs.containsKey(mob)) {
							mSpellShockedMobs.put(mob.getUniqueId(), new SpellShockedMob(mob));
						}
					}
					mPendingStaticMobs.clear();


					// Particles and time tracking on static duration, at the end so that a mob with static that gets
					// killed by a spell gets its static triggered first
					Set<UUID> expiredMobs = new HashSet<>();
					for (Map.Entry<UUID, SpellShockedMob> entry : mSpellShockedMobs.entrySet()) {
						SpellShockedMob e = entry.getValue();
						Location loc = e.mMob.getLocation();
						loc.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 1, 0.2, 0.6, 0.2, 1);
						loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 1, 0.3, 0.6, 0.3, SPELL_SHOCK_COLOR);
						e.mTicksLeft--;
						if (e.mTicksLeft <= 0 || e.mMob.isDead()) {
							expiredMobs.add(entry.getKey());
						}
					}
					mSpellShockedMobs.keySet().removeAll(expiredMobs);
				}
			};
			mRunnable.runTaskTimer(plugin, 0, 1);
		}
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		LivingEntity mob = event.getDamaged();

		// If the mob has static, trigger it, otherwise, apply it
		if (event.triggersSpellshock() && mSpellShockedMobs.containsKey(mob.getUniqueId())) {
			SpellShockedMob e = mSpellShockedMobs.get(mob.getUniqueId());
			e.mTriggeredBy = mPlayer;
			e.mTriggered = true;
		} else if (event.appliesSpellshock()) {
			mPendingStaticMobs.add(mob);
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity mob = (LivingEntity) event.getEntity();
		if (!mPendingStaticMobs.contains(mob) && event.getCause() == DamageCause.ENTITY_ATTACK) {
			mPendingStaticMobs.add(mob);
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.isWandItem(mPlayer.getInventory().getItemInMainHand());
	}

	public static void runTriggerParticles(Location loc) {
		World world = loc.getWorld();
		world.spawnParticle(Particle.SPELL_WITCH, loc, 60, 1, 1, 1, 0.001);
		world.spawnParticle(Particle.CRIT_MAGIC, loc, 45, 1, 1, 1, 0.25);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 2.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 2.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 1.5f);
	}

}
