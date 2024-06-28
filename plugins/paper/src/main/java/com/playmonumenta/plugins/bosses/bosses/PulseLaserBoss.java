package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/**
 * @author G3m1n1Boy
 * @deprecated use boss_laser instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_laser
 * /bos var Tags add boss_laser[damage=18]
 * /bos var Tags add boss_laser[soundTicks=[(UI_TOAST_IN,0.8)],soundEnd=[(ENTITY_DRAGON_FIREBALL_EXPLODE,0.6,1.5)]]
 * /bos var Tags add boss_laser[ParticleLaser=[(REDSTONE,3,0.04,0.04,0.04,0,#ffffff,0.75)],ParticleEnd=[(EXPLOSION_NORMAL,35,0,0,0,0.25)]]
 * </pre></blockquote>
 */
@Deprecated
public class PulseLaserBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_pulselaser";

	public static class Parameters extends BossParameters {
		public int DAMAGE = 18;
		public int DELAY = 100;
		public int DETECTION = 30;
		public int DURATION = 5 * 20;
		public int COOLDOWN = 12 * 20;
		public boolean SINGLE_TARGET = false;
	}

	public PulseLaserBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellBaseLaser(plugin, boss, p.DETECTION, p.DURATION, false, p.SINGLE_TARGET, p.COOLDOWN,
			// Tick action per player
			(LivingEntity player, int ticks, boolean blocked) -> {
				player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 0.8f, 0.5f + (ticks / 80f) * 1.5f);
				boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 0.8f, 0.5f + (ticks / 80f) * 1.5f);
				if (ticks == 0) {
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
						new BaseMovementSpeedModifyEffect(110, -0.75));
				}
			},
			// Particles generated by the laser
			(Location loc) -> new PartialParticle(Particle.REDSTONE, loc, 3, 0.04, 0.04, 0.04, 0, new Particle.DustOptions(Color.WHITE, 0.75f)).spawnAsEntityActive(boss),
			// Particle frequency (always, no skipped segments)
			1,
			// Particle chance (1/3)
			3,
			// Damage generated at the end of the attack
			(LivingEntity player, Location loc, boolean blocked) -> {
				loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 0.6f, 1.5f);
				new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 35, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
				if (!blocked) {
					BossUtils.blockableDamage(boss, player, DamageType.MAGIC, p.DAMAGE);
				}
			});

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
