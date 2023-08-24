package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

public class PortableEnderListener implements Listener {
	private static final String LOCK_STRING = "PortableEnder";

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.getClick() == ClickType.RIGHT &&
				event.getAction() == InventoryAction.PICKUP_HALF &&
				event.getWhoClicked() instanceof Player player) {
			// An item was right-clicked
			ItemStack item = event.getCurrentItem();
			if (isPortableEnder(item)) {
				// The clicked item is a portable ender chest, and is not shattered
				event.setCancelled(true);
				if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_PORTABLE_STORAGE)) {
					player.sendMessage(Component.text("The void here is too thick to part", NamedTextColor.RED));
					player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 0.6f);
				} else if (ScoreboardUtils.getScoreboardValue(player, "RushDown").orElse(0) < 40 &&
					           ScoreboardUtils.getScoreboardValue(player, "RushDuo").orElse(0) < 80) {
					player.sendMessage(Component.text("You must conquer Wave 40 of Rush of Dissonance solo or Wave 80 as a duo before you can part the void.", NamedTextColor.RED));
					player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 0.6f);
				} else {
					player.closeInventory(InventoryCloseEvent.Reason.OPEN_NEW);
					player.openInventory(player.getEnderChest());
					player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		if (isPortableEnder(event.getItem())) {
			event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			event.setCancelled(true);
		}
	}

	public static boolean isPortableEnder(@Nullable ItemStack item) {
		if (item != null && ItemUtils.isShulkerBox(item.getType()) && item.hasItemMeta()) {
			if (item.getItemMeta() instanceof BlockStateMeta) {
				BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
				if (blockStateMeta.getBlockState() instanceof ShulkerBox) {
					ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
					return shulkerBox.isLocked() && shulkerBox.getLock().equals(LOCK_STRING);
				}
			}
		}
		return false;
	}

}
