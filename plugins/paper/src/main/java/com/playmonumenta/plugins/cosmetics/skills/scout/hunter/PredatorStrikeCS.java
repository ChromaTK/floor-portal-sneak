package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class PredatorStrikeCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PREDATOR_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SPECTRAL_ARROW;
	}

	public void strikeTick(Player mPlayer, int tick) {
		new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation().add(0, 0.75, 0), 1, 0.25, 0, 0.25, 0).spawnAsPlayerActive(mPlayer);
	}

	public void strikeParticleProjectile(Player mPlayer, Location bLoc) {
		new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 10, 0.15, 0.15, 0.15, 0.075).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, bLoc, 2, 0.2, 0.2, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
	}

	public void strikeSoundReady(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, SoundCategory.PLAYERS, 1, 1.0f);
	}

	public void strikeLaunch(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1, 0.8f);
	}

	public void strikeExplode(World world, Player mPlayer, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.7f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.7f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 45, radius, radius, radius, 0.125).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, loc, 12, radius, radius, radius, 0.1).spawnAsPlayerActive(mPlayer);
	}

	public void strikeImpact(Runnable runnable, Location l, Player mPlayer) {
		runnable.run();
	}
}
