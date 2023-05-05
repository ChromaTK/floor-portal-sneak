package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellAxtalWitherAoe extends SpellBaseAoE {

	private final float mMinDamage;
	private final float mMaxDamage;

	public SpellAxtalWitherAoe(Plugin plugin, LivingEntity launcher, int radius, float minDamage, float maxDamage) {
		super(plugin, launcher, radius, 80, 160, true, Sound.ENTITY_CAT_HISS);
		mMinDamage = minDamage;
		mMaxDamage = maxDamage;
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		new PartialParticle(Particle.SPELL_WITCH, loc, 25, 6, 3, 6).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void chargeCircleAction(Location loc, double radius) {
		new PPCircle(Particle.SMOKE_LARGE, loc, radius).count(12).delta(0.25).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void outburstAction(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.6f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.6f, 1f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.6f, 1.5f);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 125, 0, 0, 0, 0.5).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void circleOutburstAction(Location loc, double radius) {
		new PPCircle(Particle.SMOKE_NORMAL, loc, radius).count(96).delta(0.25).extra(0.35).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void dealDamageAction(Location loc) {
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius, true)) {
			double distance = player.getLocation().distance(mLauncher.getLocation());
			BossUtils.blockableDamage(mLauncher, player, DamageType.MAGIC, ((mMaxDamage - mMinDamage) * ((mRadius - distance) / mRadius)) + mMinDamage);
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
		}
	}

}
