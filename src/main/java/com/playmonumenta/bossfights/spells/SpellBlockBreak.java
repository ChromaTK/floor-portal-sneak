package com.playmonumenta.bossfights.spells;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityExplodeEvent;

public class SpellBlockBreak extends Spell {
	private Entity mLauncher;
	private List<Material> mNoBreak;

	public SpellBlockBreak(Entity launcher) {
		mLauncher = launcher;
		mNoBreak = new ArrayList<Material>();
	}

	public SpellBlockBreak(Entity launcher, Material... noBreak) {
		mLauncher = launcher;
		mNoBreak = Arrays.asList(noBreak);
	}

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
	            Material.AIR,
	            Material.COMMAND_BLOCK,
	            Material.CHAIN_COMMAND_BLOCK,
	            Material.REPEATING_COMMAND_BLOCK,
	            Material.BEDROCK,
				Material.BARRIER,
	            Material.SPAWNER
	        );

	@Override
	public void run() {
		Location loc = mLauncher.getLocation();

		/* Get a list of all blocks that impede the boss's movement */
		List<Block> badBlockList = new ArrayList<Block>();
		Location testloc = new Location(loc.getWorld(), 0, 0, 0);
		for (int x = -1; x <= 1; x++) {
			testloc.setX(loc.getX() + (double) x);
			for (int y = 1; y <= 3; y++) {
				testloc.setY(loc.getY() + (double) y);
				for (int z = -1; z <= 1; z++) {
					testloc.setZ(loc.getZ() + (double) z);
					Material material = testloc.getBlock().getType();
					if ((!mIgnoredMats.contains(material)) && !mNoBreak.contains(material) &&
						(material.isSolid() || material.equals(Material.COBWEB))) {
						badBlockList.add(testloc.getBlock());
					}
				}
			}
		}

		/* If more than two blocks, destroy all blocking blocks */
		if (badBlockList.size() > 2) {
			/* Call an event with these exploding blocks to give plugins a chance to modify it */
			EntityExplodeEvent event = new EntityExplodeEvent(mLauncher, mLauncher.getLocation(), badBlockList, 0f);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}

			/* Remove any remaining blocks, which might have been modified by the event */
			for (Block block : badBlockList) {
				block.setType(Material.AIR);
			}
			if (badBlockList.size() > 0) {
				loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 3f, 0.6f);
				loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 3f, 0.6f);
				loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.8f);
				Location particleLoc = loc.add(new Location(loc.getWorld(), -0.5f, 0f, 0.5f));
				particleLoc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, particleLoc, 10, 1, 1, 1, 0.03);
			}
		}
	}

	@Override
	public int duration() {
		return 1;
	}
}
