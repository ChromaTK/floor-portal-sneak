package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.utils.BossUtils;

/**
 * @deprecated use boss_laser instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_laser
 * /bos var Tags add boss_laser[damage=18]
 * /bos var Tags add boss_laser[soundTicks=[(UI_TOAST_IN,0.8)],soundEnd=[(ENTITY_DRAGON_FIREBALL_EXPLODE,0.6,1.5)]]
 * /bos var Tags add boss_laser[ParticleLaser=[(REDSTONE,3,0.04,0.04,0.04,0,#ffffff,0.75)],ParticleEnd=[(EXPLOSION_NORMAL,35,0,0,0,0.25)]]
 * </pre></blockquote>
 * @author G3m1n1Boy
 */
public class PulseLaserBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_pulselaser";

	public static class Parameters {
		public int DAMAGE = 18;
		public int DELAY = 100;
		public int DETECTION = 30;
		public int DURATION = 5 * 20;
		public int COOLDOWN = 12 * 20;
		public boolean SINGLE_TARGET = false;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PulseLaserBoss(plugin, boss);
	}

	public PulseLaserBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseLaser(plugin, boss, p.DETECTION, p.DURATION, false, p.SINGLE_TARGET, p.COOLDOWN,
					// Tick action per player
					(Player player, int ticks, boolean blocked) -> {
						player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.8f, 0.5f + (ticks / 80f) * 1.5f);
						boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, 0.8f, 0.5f + (ticks / 80f) * 1.5f);
						if (ticks == 0) {
							boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 110, 4));
						}
					},
					// Particles generated by the laser
					(Location loc) -> {
						loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 3, 0.04, 0.04, 0.04, 0, new Particle.DustOptions(Color.WHITE, 0.75f));
					},
					// Particle frequency (always, no skipped segments)
					1,
					// Particle chance (1/3)
					3,
					// Damage generated at the end of the attack
					(Player player, Location loc, boolean blocked) -> {
						loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.6f, 1.5f);
						loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 35, 0, 0, 0, 0.25);
						if (!blocked) {
							BossUtils.bossDamage(boss, player, p.DAMAGE);
						}
					})
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
