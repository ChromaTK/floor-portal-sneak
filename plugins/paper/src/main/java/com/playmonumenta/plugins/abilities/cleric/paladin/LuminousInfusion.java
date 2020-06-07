package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
* Level 1: Shift+RClick+LookDown to charge you weapon with light. Your next
* attack (melee, ranged, magic, etc.) on an undead mob triggers an explosion
* with a 4 block radius, knocking enemies away. Undead take 20 damage
* from the explosion and all other mobs take 10 (Cooldown: 15 seconds).
* Level 2: Additionally, deal 5 extra damage with melee attacks on undead.
*/

public class LuminousInfusion extends Ability {

	private static final String LUMINOUS_INFUSION_EXPIRATION_MESSAGE = "The light from your hands fades...";
	private static final double LUMINOUS_INFUSION_RADIUS = 4;
	private static final int LUMINOUS_INFUSION_NORMIE_DAMAGE = 10;
	private static final int LUMINOUS_INFUSION_UNDEAD_DAMAGE = 20;
	private static final int LUMINOUS_INFUSION_PASSIVE_DAMAGE = 5;
	private static final int LUMINOUS_INFUSION_COOLDOWN = 20 * 15;
	private static final float LUMINOUS_INFUSION_KNOCKBACK_SPEED = 0.7f;

	private boolean mActive = false;

	public LuminousInfusion(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Luminous Infusion");
		mInfo.mLinkedSpell = Spells.LUMINOUS_INFUSION;
		mInfo.mScoreboardId = "LuminousInfusion";
		mInfo.mShorthandName = "LI";
		mInfo.mDescriptions.add("Sneak and right-click while looking at the ground to charge your weapon with holy light. Your next attack (melee, ranged, magic, etc.) on an undead mob triggers an explosion with a 4 block radius, knocking enemies away. Undead take 20 damage and all other mobs take 10 damage (Cooldown: 15 seconds).");
		mInfo.mDescriptions.add("Additionally, melee attacks against undead passively deal +5 damage.");
		mInfo.mCooldown = LUMINOUS_INFUSION_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public void cast(Action action) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}

		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (!mPlayer.isSneaking() || InventoryUtils.isBowItem(mainHand)
		    || mPlayer.getLocation().getPitch() < 50) {
			return;
		}

		// Cast conditions met
		mActive = true;
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Holy energy radiates from your hands...");
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1.65f);
		mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.75f, 0.25f, 0.75f, 1);
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				Location rightHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.45).subtract(0, .8, 0);
				Location leftHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.45).subtract(0, .8, 0);
				mWorld.spawnParticle(Particle.SPELL_INSTANT, leftHand, 1, 0.05f, 0.05f, 0.05f, 0);
				mWorld.spawnParticle(Particle.SPELL_INSTANT, rightHand, 1, 0.05f, 0.05f, 0.05f, 0);
				if (mT >= LUMINOUS_INFUSION_COOLDOWN || !mActive) {
					if (mT >= LUMINOUS_INFUSION_COOLDOWN) {
						MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, LUMINOUS_INFUSION_EXPIRATION_MESSAGE);
					}
					mActive = false;
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 1, 1);

		putOnCooldown();
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		DamageCause cause = event.getCause();
		if (cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.CUSTOM) {
			LivingEntity le = (LivingEntity) event.getEntity();

			// Passive damage to undead from every melee hit, regardless of active
			if (cause == DamageCause.ENTITY_ATTACK && getAbilityScore() > 1 && EntityUtils.isUndead(le)) {
				event.setDamage(event.getDamage() + LUMINOUS_INFUSION_PASSIVE_DAMAGE);
			}

			if (mActive && EntityUtils.isUndead(le)) {
				execute(le, event);
			}
		}

		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (mActive && EntityUtils.isUndead(damagee)) {
			execute(damagee, event);
		}

		return true;
	}

	public void execute(LivingEntity damagee, EntityDamageByEntityEvent event) {
		mActive = false;
		Location loc = damagee.getLocation();
		// Active damage to undead
		event.setDamage(event.getDamage() + LUMINOUS_INFUSION_UNDEAD_DAMAGE);
		mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 100, 0.05f, 0.05f, 0.05f, 0.3);
		mWorld.spawnParticle(Particle.FLAME, loc, 75, 0.05f, 0.05f, 0.05f, 0.3);
		mWorld.playSound(loc, Sound.ITEM_TOTEM_USE, 0.8f, 1.1f);
		List<LivingEntity> affected = EntityUtils.getNearbyMobs(loc, LUMINOUS_INFUSION_RADIUS, damagee);
		for (LivingEntity e : affected) {
			// Reduce overall volume of noise the more mobs there are, but still make it louder for more mobs
			double volume = 0.6 / Math.sqrt(affected.size());
			mWorld.playSound(loc, Sound.ITEM_TOTEM_USE, (float) volume, 1.1f);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 10, 0.05f, 0.05f, 0.05f, 0.1);
			mWorld.spawnParticle(Particle.FLAME, loc, 7, 0.05f, 0.05f, 0.05f, 0.1);
			if (EntityUtils.isUndead(e)) {
				EntityUtils.damageEntity(mPlugin, e, LUMINOUS_INFUSION_UNDEAD_DAMAGE, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
			} else {
				EntityUtils.damageEntity(mPlugin, e, LUMINOUS_INFUSION_NORMIE_DAMAGE, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
			}
			MovementUtils.knockAway(loc, e, LUMINOUS_INFUSION_KNOCKBACK_SPEED, LUMINOUS_INFUSION_KNOCKBACK_SPEED / 2);
		}
	}

}
