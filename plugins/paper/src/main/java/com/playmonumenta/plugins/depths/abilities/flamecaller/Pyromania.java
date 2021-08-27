package com.playmonumenta.plugins.depths.abilities.flamecaller;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;

import net.md_5.bungee.api.ChatColor;

public class Pyromania extends DepthsAbility {

	public static final String ABILITY_NAME = "Pyromania";
	public static final double[] FIRE_BONUS_DAMAGE = {1, 1.25, 1.5, 1.75, 2};
	public static final int RADIUS = 8;

	public static String tree;

	public Pyromania(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.CAMPFIRE;
		mTree = DepthsTree.FLAMECALLER;
	}

	@Override
	public String getDescription(int rarity) {
		return "All fire tick damage on enemies within " + RADIUS + " blocks of you is increased by " + DepthsUtils.getRarityColor(rarity) + FIRE_BONUS_DAMAGE[rarity - 1] + ChatColor.WHITE + ", and fire damage goes through invulnerability frames. Stacks if multiple players have the skill.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}
}
