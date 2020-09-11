package com.playmonumenta.plugins.abilities.warlock;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Exorcism  extends Ability {

	private static final int RANGE = 12;
	private static final int DURATION = 20 * 15;
	private static final int COOLDOWN_1 = 20 * 25;
	private static final int COOLDOWN_2 = 20 * 15;

	public Exorcism(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Exorcism");
		mInfo.mLinkedSpell = Spells.EXORCISM;
		mInfo.mScoreboardId = "Exorcism";
		mInfo.mShorthandName = "Ex";
		mInfo.mDescriptions.add("Sneak left clicking while looking up removes all your debuffs and applies them to enemies within 12 blocks of you. Level of debuffs is preserved. (Cooldown: 25s)");
		mInfo.mDescriptions.add("Also apply the corresponding debuff to enemies for every buff you have. Cooldown is reduced to 15s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
	}

	private final List<PotionEffect> mDebuffs = new ArrayList<>();

	@Override
	public void cast(Action action) {
		//	needs better sound
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.7f, -3f);

		putOnCooldown();

		boolean onFire = false;
		for (PotionEffect effect : mPlayer.getActivePotionEffects()) {
			if (PotionUtils.hasNegativeEffects(effect.getType())) {
				mDebuffs.add(effect.getType().createEffect(DURATION, effect.getAmplifier()));
			} else if (getAbilityScore() > 1) {
				if (effect.getType().equals(PotionEffectType.SPEED)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.SLOW, DURATION, effect.getAmplifier()));
				} else if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.WEAKNESS, DURATION, effect.getAmplifier()));
				} else if (effect.getType().equals(PotionEffectType.REGENERATION)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.WITHER, DURATION, effect.getAmplifier()));
				} else if (effect.getType().equals(PotionEffectType.FAST_DIGGING)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.SLOW_DIGGING, DURATION, effect.getAmplifier()));
				} else if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.UNLUCK, DURATION, (effect.getAmplifier()*2) + 1));
				} else if (effect.getType().equals(PotionEffectType.FIRE_RESISTANCE)) {
					onFire = true;
				}
			}
		}
		PotionUtils.clearNegatives(mPlugin, mPlayer);
		if (mPlayer.getFireTicks() > 1) {
			onFire = true;
			mPlayer.setFireTicks(1);
		}

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RANGE)) {
			for (PotionEffect debuff : mDebuffs) {
				PotionUtils.applyPotion(mPlayer, mob, debuff);
			}
			if (onFire) {
				EntityUtils.applyFire(mPlugin, DURATION, mob, mPlayer);
			}
			mWorld.spawnParticle(Particle.SQUID_INK, mob.getLocation(), 40, 0.1, 0.2, 0.1, 0.15);
		}

		// a cool particle effect on the player would be nice too
		mDebuffs.clear();

	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50
				&& InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())
				&& (!mPlayer.getActivePotionEffects().isEmpty() || (mPlayer.getFireTicks() > 1));
	}

}
