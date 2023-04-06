package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class HallowedLanceCS extends ManaLanceCS {
	// Test only. What happens if two skins for one spell?

	public static final String NAME = "Hallowed Lance";

	private static final Particle.DustOptions HALLOWED_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 80), 1.0f);

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE_DUST;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void lanceHitBlock(Player mPlayer, Location bLoc, World world) {
		new PartialParticle(Particle.CLOUD, bLoc, 20, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.65f);
	}

	@Override
	public void lanceParticle(Player player, Location startLoc, Location endLoc) {
		new PPLine(Particle.END_ROD, startLoc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.35).data(HALLOWED_LANCE_COLOR).spawnAsPlayerActive(player);
	}

	@Override
	public void lanceSound(World world, Player player) {
		world.playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.75f);
	}
}
