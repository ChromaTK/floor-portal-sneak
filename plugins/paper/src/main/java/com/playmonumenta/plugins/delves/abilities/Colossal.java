package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

public class Colossal {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.05;
	private static final LoSPool COLOSSAL_LAND_POOL = new LoSPool.LibraryPool("~DelveColossalLand");
	private static final LoSPool COLOSSAL_WATER_POOL = new LoSPool.LibraryPool("~DelveColossalWater");

	public static final String DESCRIPTION = "Broken spawners unleash enemies.";

	public static String[] rankDescription(int level) {
		return new String[]{
			"Broken Spawners have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance",
			"to spawn Colossi."
		};
	}

	public static void applyModifiers(Location loc, int level) {
		if (level == 0 || FastUtils.RANDOM.nextDouble() > SPAWN_CHANCE_PER_LEVEL * level) {
			return;
		}

		int airBlocks = 0;
		int waterBlocks = 0;

		for (int x = -2; x <= 2; x++) {
			for (int y = -2; y <= 2; y++) {
				for (int z = -2; z <= 2; z++) {
					Location locClone = loc.clone().add(x, y, z);
					if (locClone.getBlock().getType() == Material.WATER) {
						waterBlocks++;
					}

					if (locClone.getBlock().getType() == Material.AIR) {
						airBlocks++;
					}
				}
			}
		}

		new PartialParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0).spawnAsEnemy();
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 100, 0.2, 0.2, 0.2, 0.2).spawnAsEnemy();

		final int mAir = airBlocks;
		final int mWater = waterBlocks;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mAir >= mWater) {
					COLOSSAL_LAND_POOL.spawn(loc);
				} else {
					COLOSSAL_WATER_POOL.spawn(loc);
				}
			}
		}.runTaskLater(Plugin.getInstance(), 20);
	}

}
