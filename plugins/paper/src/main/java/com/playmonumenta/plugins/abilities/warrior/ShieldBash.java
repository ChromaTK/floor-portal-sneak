package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public class ShieldBash extends Ability {

	private static final int SHIELD_BASH_DAMAGE = 8;
	private static final int SHIELD_BASH_STUN = 20 * 1;
	private static final int SHIELD_BASH_COOLDOWN = 20 * 8;
	private static final int SHIELD_BASH_2_RADIUS = 3;
	private static final int SHIELD_BASH_RANGE = 4;
	private static final int ENHANCEMENT_BLOCKING_DURATION = 20;

	public static final String CHARM_DAMAGE = "Shield Bash Damage";
	public static final String CHARM_DURATION = "Shield Bash Duration";
	public static final String CHARM_COOLDOWN = "Shield Bash Cooldown";
	public static final String CHARM_RADIUS = "Shield Bash Radius";
	public static final String CHARM_RANGE = "Shield Bash Range";

	public static final AbilityInfo<ShieldBash> INFO =
		new AbilityInfo<>(ShieldBash.class, "Shield Bash", ShieldBash::new)
			.linkedSpell(ClassAbility.SHIELD_BASH)
			.scoreboardId("ShieldBash")
			.shorthandName("SB")
			.descriptions(
				"Block while looking at an enemy within 4 blocks and not sneaking to deal 8 melee damage, stun for 1 second, and taunt the targeted enemy. " +
					"Elites and bosses are rooted instead of stunned. Cooldown: 8s.",
				"Additionally, apply damage, stun, and taunt to all enemies in a 3 block radius from the enemy you are looking at.",
				"Blocking damage with a shield within 1s of beginning to block refreshes 50% of this skill's cooldown.")
			.simpleDescription("Bash mobs with your shield, stunning and taunting them.")
			.cooldown(SHIELD_BASH_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.IRON_DOOR);

	private boolean mIsEnhancementUsed = true;

	public ShieldBash(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void blockWithShieldEvent() {
		if (isOnCooldown() || mPlayer.isSneaking()) {
			return;
		}

		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, SHIELD_BASH_RANGE);
		LivingEntity mob = EntityUtils.getHostileEntityAtCursor(mPlayer, range);

		if (mob == null) {
			return;
		}

		Location mobLoc = mob.getEyeLocation();
		Location eyeLoc = mPlayer.getEyeLocation();
		World world = eyeLoc.getWorld();
		new PartialParticle(Particle.CRIT, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, mobLoc, 5, 0.15, 0.5, 0.15, 0).spawnAsPlayerActive(mPlayer);
		world.playSound(eyeLoc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 2.0f, 1);
		world.playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2.0f, 0.5f);

		bash(mob, ClassAbility.SHIELD_BASH);
		if (isLevelTwo()) {
			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mob), CharmManager.getRadius(mPlayer, CHARM_RADIUS, SHIELD_BASH_2_RADIUS));
			for (LivingEntity le : hitbox.getHitMobs(mob)) {
				bash(le, ClassAbility.SHIELD_BASH_AOE);
			}
		}

		if (isEnhanced()) {
			mIsEnhancementUsed = false;
		}
		putOnCooldown();
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// If:
		// isEnhanced,
		// Player has blocked within the parry (0.5 seconds)
		// Event has been successfully blockedbyshield
		// And ShieldBash is on CD...
		if (isEnhanced() && mPlayer.getHandRaisedTime() < ENHANCEMENT_BLOCKING_DURATION && event.isBlockedByShield() && isOnCooldown() && !mIsEnhancementUsed) {
			// Reduce cooldown by half of shield bash's CD.
			mPlugin.mTimers.updateCooldown(mPlayer, ClassAbility.SHIELD_BASH, getModifiedCooldown() / 2);
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1, 2);

			mIsEnhancementUsed = true;
			// mPlayer.sendMessage("Shield bash CD reduced!");
		}
	}

	private void bash(LivingEntity le, ClassAbility ca) {
		DamageUtils.damage(mPlayer, le, DamageType.MELEE_SKILL, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, SHIELD_BASH_DAMAGE), ca, true, true);
		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, SHIELD_BASH_STUN);
		if (EntityUtils.isBoss(le) || EntityUtils.isElite(le)) {
			EntityUtils.applySlow(mPlugin, duration, .99, le);
		} else {
			EntityUtils.applyStun(mPlugin, duration, le);
		}
		if (le instanceof Mob mob) {
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					if (mob.isDead() || !mob.isValid() || mT > duration * 2) {
						this.cancel();
					} else if (!EntityUtils.isStunned(mob)) {
						EntityUtils.applyTaunt(mob, mPlayer, false);
						this.cancel();
					}

					mT++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

}
