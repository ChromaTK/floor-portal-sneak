package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author G3m1n1Boy
 * @deprecated use boss_laser instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_laser
 * /bos var Tags add boss_laser[damage=19,cooldown=160,singletarget=true,effects=[(fire,80)]]
 * /bos var Tags add boss_laser[soundTicks=[(UI_TOAST_IN,0.5)],soundEnd=[(ENTITY_DRAGON_FIREBALL_EXPLODE,1,1.5)]]
 * /bos var Tags add boss_laser[ParticleLaser=[(CLOUD,1,0.02,0.02,0.02,0),(FLAME,1,0.04,0.04,0.04,1)],ParticleEnd=[(FIREWORKS_SPARK,300,0.8,0.8,0.8,0)]]
 * </pre></blockquote>
 */
@Deprecated
public class FlameLaserBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flamelaser";

	public static class Parameters extends BossParameters {
		public int DAMAGE = 20;
		public int DELAY = 100;
		public int DETECTION = 30;
		public int COOLDOWN = 8 * 20;
		public int FUSE_TIME = 5 * 20;
		public int FIRE_DURATION = 4 * 20;
		public boolean SINGLE_TARGET = true;
	}

	public FlameLaserBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellBaseLaser(plugin, boss, p.DETECTION, p.FUSE_TIME, false, p.SINGLE_TARGET, p.COOLDOWN,
			// Tick action per player
			(LivingEntity target, int ticks, boolean blocked) -> {
				target.getWorld().playSound(target.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 0.5f, 0.5f + (ticks / 80f) * 1.5f);
				boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 1f, 0.5f + (ticks / 80f) * 1.5f);
				if (ticks == 0) {
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
						new BaseMovementSpeedModifyEffect(110, -0.75));
				}
			},
			// Particles generated by the laser
			(Location loc) -> {
				new PartialParticle(Particle.CLOUD, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
				new PartialParticle(Particle.FLAME, loc, 1, 0.04, 0.04, 0.04, 1).spawnAsEntityActive(boss);
			},
			// Damage generated at the end of the attack
			(LivingEntity target, Location loc, boolean blocked) -> {
				loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 1f, 1.5f);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 300, 0.8, 0.8, 0.8, 0).spawnAsEntityActive(boss);
				if (!blocked) {
					BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE);
					// Shields don't stop fire!
					EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), p.FIRE_DURATION, target, boss);
				}
			});

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
