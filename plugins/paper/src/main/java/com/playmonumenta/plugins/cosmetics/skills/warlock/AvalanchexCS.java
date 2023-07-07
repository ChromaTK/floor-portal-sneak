package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.cosmetics.skills.DepthsCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AvalanchexCS extends AmplifyingHexCS implements DepthsCS {
	//Icy amplifying hex. Depth set: frost

	public static final String NAME = "Avalanchex";

	private static final Particle.DustOptions ICE_PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(207, 242, 255), 1.0f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Let everything under your avalanche",
			"be not above innocence.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.POWDER_SNOW_BUCKET;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public String getToken() {
		return TALISMAN_FROST;
	}

	@Override
	public void amplifyingParticle(Player player, Location l) {
		new PartialParticle(Particle.REDSTONE, l, 10, 0.25, 0.25, 0.25, 0.1, ICE_PARTICLE_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, l, 1, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SNOWFLAKE, l, 3, 0.25, 0.25, 0.25, 0.05).spawnAsPlayerActive(player);
	}

	@Override
	public void amplifyingEffects(Player player, World world, Location soundLoc) {
		world.playSound(soundLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.25f, 0.7f);
		world.playSound(soundLoc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 1.25f, 0.6f);
		world.playSound(soundLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1.25f, 0.65f);
		world.playSound(soundLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.25f, 0.65f);

		ParticleUtils.drawParticleCircleExplosion(player, soundLoc.clone().add(0, 0.15, 0), 0, 1, 0, 0, 75, 0.5f,
			true, 0, 0, Particle.EXPLOSION_NORMAL);
	}

	@Override
	public double amplifyingAngle(double angle, double radius) {
		return -(angle - 90) + ((angle - 90) * (radius * 0.07));
	}

	@Override
	public double amplifyingHeight(double radius, double max) {
		radius -= 1.75;
		max -= 1.75;
		return FastUtils.sin((radius / max) * Math.PI) * 1.5;
	}
}
