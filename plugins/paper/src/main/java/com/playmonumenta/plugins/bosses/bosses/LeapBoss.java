package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLeapAttack;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class LeapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_leap";
	public static final int detectionRange = 32;

	private static final int COOLDOWN = 20 * 4;
	private static final int MIN_RANGE = 6;
	private static final int RUN_DISTANCE = 3;
	private static final double VELOCITY_MULTIPLIER = 1.3;
	private static final double DAMAGE_RADIUS = 3;
	private static final int DAMAGE = 30;

	public LeapBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new SpellBaseLeapAttack(plugin, boss, detectionRange, MIN_RANGE, RUN_DISTANCE, COOLDOWN, VELOCITY_MULTIPLIER,
			// Initiate Aesthetic
			(World world, Location loc) -> {
				new PartialParticle(Particle.VILLAGER_ANGRY, loc, 15, 0.5, 0.5, 0.5, 0).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1f, 0.5f);
			},
			// Leap Aesthetic
			(World world, Location loc) -> {
				new PartialParticle(Particle.CLOUD, loc, 30, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.5f, 0.5f);
			},
			// Leaping Aesthetic
			(World world, Location loc) -> {
				new PartialParticle(Particle.CLOUD, loc, 1, 0.3, 0.3, 0.3, 0.1).spawnAsEntityActive(boss);
			},
			// Hit Action
			(World world, @Nullable Player player, Location loc, Vector dir) -> {
				ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
					List.of(
						new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
							new PartialParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(boss);
							new PartialParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(boss);
						})
					)
				);
				new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).minimumCount(1).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 0.5f);
				for (Player p : PlayerUtils.playersInRange(loc, DAMAGE_RADIUS, true)) {
					BossUtils.blockableDamage(boss, p, DamageType.MELEE, DAMAGE);
				}
			}, null, null);

		super.constructBoss(spell, detectionRange);
	}

}
