package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCloseEvent.Reason;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

public class LootChestsInInventory implements Listener {
	private final Map<UUID, Integer> mLootMenu = new HashMap<>();

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!event.getClick().equals(ClickType.RIGHT)) {
			return;
		}

		ItemStack item = event.getCurrentItem();
		//Make sure it is at the very least a chest
		if (item == null) {
			return;
		}

		if (!item.getType().equals(Material.CHEST)) {
			return;
		}

		Player player = (Player)event.getWhoClicked();

		//This is needed for it to work
		NBTItem nbti = new NBTItem(item);
		NBTCompound tag = nbti.getCompound("BlockEntityTag");
		if (tag == null) {
			return;
		}
		tag.setString("id", "minecraft:chest");
		ItemStack item2 = nbti.getItem();

		//Classic turning an item into a blockstate
		BlockStateMeta meta = (BlockStateMeta)item2.getItemMeta();
		BlockState state = meta.getBlockState();
		Chest chest = (Chest)state;
		//Loot tables are fun. Make sure the loot table exists
		LootTable table = chest.getLootTable();
		if (table == null) {
			return;
		}
		if (item.isSimilar(event.getCursor())) {
			return;
		}
		if (!event.getCursor().getType().equals(Material.AIR)) {
			player.sendMessage(Component.text("You must have an empty cursor to open loot chests!", NamedTextColor.DARK_RED));
			event.setCancelled(true);
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return;
		}
		//Make an inventory and do some good ol roundabout population of the loot
		Inventory inventory = Bukkit.createInventory(null, 27, item.getItemMeta().displayName());
		LootContext.Builder builder = new LootContext.Builder(player.getLocation());
		Collection<ItemStack> loot = table.populateLoot(FastUtils.RANDOM, builder.build());
		item.subtract();
		ChestUtils.generateLootInventory(loot, inventory, player, true);

		addOrInitializePlayer(player);
		player.closeInventory(Reason.OPEN_NEW);
		player.openInventory(inventory);
		ItemStack emptyChest = new ItemStack(Material.CHEST);
		ItemMeta emptyChestMeta = emptyChest.getItemMeta();
		if (item2.hasItemMeta() && item2.getItemMeta().hasDisplayName()) {
			emptyChestMeta.displayName(item2.getItemMeta().displayName());
		}
		emptyChest.setItemMeta(emptyChestMeta);
		ItemUtils.setPlainTag(emptyChest);
		InventoryUtils.giveItem(player, emptyChest);
	}

	//Drop the items upon closing the inventory
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (event.getInventory().getHolder() == null && event.getView().getTopInventory().getType().equals(InventoryType.CHEST) && event.getView().getTopInventory().getSize() == 27
			    && event.getPlayer() instanceof Player player) {
			/* Right type of inventory - check if the player is in the map */

			/* Check if the player had a loot table chest open, and if so, decrement the count by 1. If it decrements to 0, remove from the map */
			boolean hadLootInventoryOpen = decrementOrClearPlayer(player);
			if (hadLootInventoryOpen) {
				/* Player did have a virtual loot inventory open - give remaining items to the player */
				ItemStack[] items = event.getView().getTopInventory().getContents();
				for (ItemStack item : items) {
					if (item != null && !item.getType().isAir()) {
						// dropped instead of given directly to allow /pickup to filter out filler items
						InventoryUtils.dropTempOwnedItem(item, player.getLocation(), player);
					}
				}
				/* Make sure the source container is cleared, since it won't be reachable anymore anyway */
				event.getView().getTopInventory().clear();
			}
		}
	}

	//Failsafes
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		mLootMenu.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mLootMenu.remove(event.getPlayer().getUniqueId());
	}

	private void addOrInitializePlayer(HumanEntity player) {
		Integer value = mLootMenu.get(player.getUniqueId());
		if (value == null) {
			value = 1;
		} else {
			value += 1;
		}
		mLootMenu.put(player.getUniqueId(), value);
	}

	/* Returns whether or not the player was in the map to begin with */
	private boolean decrementOrClearPlayer(HumanEntity player) {
		Integer value = mLootMenu.get(player.getUniqueId());
		if (value == null) {
			return false;
		} else {
			if (value > 1) {
				value--;
				mLootMenu.put(player.getUniqueId(), value);
			} else {
				mLootMenu.remove(player.getUniqueId());
			}
			return true;
		}
	}
}
