package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class BucketOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.SURVIVAL && ZoneUtils.isInPlot(player)) {
			return true;
		}

		return false;
	}

	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity,
	                                           ItemStack itemInHand) {
		if (clickedEntity == null) {
			return true;
		} else if (clickedEntity instanceof Cow) {
			return false;
		} else if (clickedEntity instanceof Fish && ZoneUtils.hasZoneProperty(clickedEntity, ZoneProperty.ADVENTURE_MODE)) {
			return false;
		}

		return true;
	}

	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		if (blockType.equals(Material.AIR) || dispensed == null) {
			return false;
		} else if (blockType.equals(Material.DISPENSER)) {
			return ZoneUtils.isInPlot(block.getLocation());
		} else if (blockType.equals(Material.DROPPER)) {
			return true;
		}

		return false;
	}
}
