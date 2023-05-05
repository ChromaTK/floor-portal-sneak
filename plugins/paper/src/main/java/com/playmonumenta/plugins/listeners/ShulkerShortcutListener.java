package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.inventories.ShulkerInventory;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.itemstats.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

/**
 * These listeners work together with ShulkerInventoryManager and ShulkerInventory to
 * Allow players to access Shulker Boxes without being placed first.
 *
 * @see com.playmonumenta.plugins.inventories.ShulkerInventoryManager
 * @see com.playmonumenta.plugins.inventories.ShulkerInventory
 */
public class ShulkerShortcutListener implements Listener {
	private static final Permission PERMISSION = new Permission("monumenta.feature.shulkershortcut");
	private final Plugin mPlugin;

	public ShulkerShortcutListener(Plugin plugin) {
		mPlugin = plugin;
	}

	/**
	 * Event Handler for when a player performs any click inside any inventory.
	 * Used to prevent open Shulker Boxes from being removed from an inventory,
	 * as well as to open Shulker Boxes or deposit items when right-clicked.
	 *
	 * @see InventoryClickEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		ClickType click = event.getClick();
		InventoryAction action = event.getAction();
		ItemStack itemHeld = event.getCursor();
		ItemStack itemClicked = event.getCurrentItem();
		int slotClicked = event.getSlot();
		Inventory topInventory = event.getView().getTopInventory();
		Inventory clickedInventory = event.getClickedInventory();
		if (!(event.getWhoClicked() instanceof Player player) || clickedInventory == null) {
			return;
		}
		// A player clicked inside an inventory window

		boolean portableStorageAllowed = !ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_PORTABLE_STORAGE);

		// check if a shulker inventory is open
		ShulkerInventory shulkerInventory = ShulkerInventoryManager.getOpenShulkerInventory(player, topInventory);
		if (shulkerInventory != null) { // Shulker was opened via shortcut
			if (!mPlugin.mShulkerInventoryManager.updateShulker(player) || !portableStorageAllowed) { // Try to update Shulker if it still exists.
				// The currently open shulker no longer exists, cancel the click and close the inventory.
				event.setCancelled(true);
				player.sendMessage(ChatColor.RED + "Shulker no longer available");
				Bukkit.getScheduler().runTask(mPlugin, () -> player.closeInventory(InventoryCloseEvent.Reason.CANT_USE));
				return;
			}

			if (event.getClickedInventory() == shulkerInventory.getInventory()
					&& event.getSlot() >= shulkerInventory.getSlots()) {
				// clicked on a reserved slot of a reduced-size shulker box
				event.setCancelled(true);
				return;
			}

			if (shulkerInventory.getInventory().getType() != InventoryType.SHULKER_BOX) {
				// Disallow sorting partial inventories to prevent it duping the filler items and moving items into slots where they shouldn't be
				if (shulkerInventory.getSlots() % 9 != 0
					    && event.getClick().isRightClick()
					    && ItemUtils.isNullOrAir(event.getCursor())
					    && ItemUtils.isNullOrAir(event.getCurrentItem())) {
					event.setCancelled(true);
					return;
				}
				// prevent picking up the filler items if the player for some reason has some
				if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR
						&& ShulkerInventory.FILLER.isSimilar(event.getCursor())) {
					event.setCancelled(true);
					return;
				}
				// Quiver or modified shulker inventory is involved
				// For quivers, make sure only arrows can be put in it
				// For modified shulker inventories, prevent putting shulkers in
				ItemStack deposited = null;
				switch (event.getAction()) {
					case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> {
						if (event.getClickedInventory() == shulkerInventory.getInventory()) {
							deposited = event.getCursor();
						}
					}
					case MOVE_TO_OTHER_INVENTORY -> {
						if (event.getClickedInventory() != shulkerInventory.getInventory()) {
							deposited = event.getCurrentItem();
						}
					}
					case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
						if (event.getClickedInventory() == shulkerInventory.getInventory()) {
							deposited = event.getClick() == ClickType.SWAP_OFFHAND ? player.getInventory().getItemInOffHand() : player.getInventory().getItem(event.getHotbarButton());
						}
					}
					case UNKNOWN -> { // unknown click (modded?): disallow
						event.setCancelled(true);
						return;
					}
					default -> {
						// nothing deposited for other actions
					}
				}
				if (deposited != null && deposited.getType() != Material.AIR) {
					if (ItemUtils.isShulkerBox(deposited.getType())) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}
		if (itemClicked != null
			    && (click == ClickType.RIGHT || click == ClickType.SWAP_OFFHAND)
			    && ItemUtils.isShulkerBox(itemClicked.getType())
			    && !portableStorageAllowed) {
			// Cancel all right clicks on Shulkers if portable storage is not allowed
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			player.sendMessage(ChatColor.RED + "You can't use this here");
			event.setCancelled(true);
		} else if ((click == ClickType.RIGHT || click == ClickType.SWAP_OFFHAND)
			           && isEnderExpansion(itemClicked)
			           && !clickedInventory.getType().equals(InventoryType.ENDER_CHEST)) {
			// Right clicked an Ender Chest Expansion shulker outside an ender chest
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			player.sendMessage(ChatColor.RED + "This item only works in an ender chest");
			event.setCancelled(true);
		} else if ((click == ClickType.RIGHT || click == ClickType.SWAP_OFFHAND)
			           && isPurpleTesseractContainer(itemClicked)) {
			// Right-clicked a purple tesseract shulker that can't be opened
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			player.sendMessage(ChatColor.RED + "This container must be placed to access its items");
			event.setCancelled(true);
		} else if ((click == ClickType.RIGHT || click == ClickType.SWAP_OFFHAND)
			           && ChestUtils.isLootBox(itemClicked)) {
			if (click == ClickType.SWAP_OFFHAND) {
				return;
			}
			// Right-clicked a lootbox - dump contents into player's inventory
			List<ItemStack> items = ChestUtils.removeOneLootshareFromLootbox(itemClicked);
			if (items == null) {
				// Lootbox empty
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			} else {
				// Non-empty, got some items, drop them on the player
				player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.PLAYERS, 0.6f, 1f);
				for (ItemStack item : items) {
					if (item != null && !item.getType().isAir()) {
						InventoryUtils.dropTempOwnedItem(item, player.getLocation(), player);
					}
				}
			}
			event.setCancelled(true);
		} else if (itemClicked != null
			           && ItemUtils.isShulkerBox(itemClicked.getType())
			           && !ShulkerEquipmentListener.isAnyEquipmentBox(itemClicked)
			           && !PortableEnderListener.isPortableEnder(itemClicked)) {
			// Player clicked a non-equipment shulker box in an inventory.
			if (clickedInventory == topInventory && shulkerInventory != null && (click == ClickType.RIGHT || click == ClickType.SWAP_OFFHAND)) {
				// A shulker inside another shulker was right-clicked, cancel.
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.sendMessage(ChatColor.RED + "Cannot open nested shulkers");
				event.setCancelled(true);
			} else if (ShulkerInventoryManager.isShulkerInUse(itemClicked)) {
				// A currently open shulker box was clicked, cancel.
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.sendMessage(ChatColor.RED + "That shulker is open");
				event.setCancelled(true);
			} else {
				// A shulker box that isn't currently open was clicked.
				if (player.hasPermission(PERMISSION)) {
					if (click == ClickType.RIGHT && action == InventoryAction.SWAP_WITH_CURSOR &&
						    itemHeld != null && !ItemUtils.isShulkerBox(itemHeld.getType()) &&
						    !CurseOfEphemerality.isEphemeral(itemHeld)) {

						// Player right-clicked shulker while holding an item on their cursor.
						event.setCancelled(true);
						int starting = itemHeld.getAmount();
						int remaining = mPlugin.mShulkerInventoryManager.addItemToShulker(player, clickedInventory, slotClicked, itemHeld);
						if (remaining < 0) {
							// An error occurred (error message already sent to the player)
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (remaining == 0) {
							// All items were inserted successfully.
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
							player.sendMessage(ChatColor.GOLD + "Item deposited into shulker.");
							event.getView().setCursor(null);
						} else if (remaining == starting) {
							// No items were placed, shulker is full.
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
							player.sendMessage(ChatColor.RED + "That shulker is full.");
						} else {
							// Items were inserted, but not all
							player.sendMessage(ChatColor.RED + "That shulker was too full to accept the full stack.");
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						}
					} else if (click == ClickType.RIGHT && action == InventoryAction.PICKUP_HALF) {
						// Player right-clicked shulker with an empty cursor.
						if (mPlugin.mShulkerInventoryManager.openShulker(player, clickedInventory, slotClicked)) {
							// Shulker was successfully opened
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else {
							// Shulker couldn't be opened
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						}
						event.setCancelled(true);
					} else if (click == ClickType.SWAP_OFFHAND && ItemUtils.isNullOrAir(itemHeld)) {
						// Pressed swap on a shulker with empty cursor: Deposit matching items from the inventory
						event.setCancelled(true);
						depositAllMatching(player, itemClicked);
					} else if (ShulkerInventoryManager.playerIsShulkerRateLimited(player)) {
						player.sendMessage(ChatColor.RED + "Too fast! Please try again");
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						event.setCancelled(true);
					}
				}
			}
		}
	}

	/**
	 * Event Handler for when the player drags an ItemStack across one or more slots in an inventory.
	 * Used to update Shulker Boxes when items are dragged in their inventory.
	 *
	 * @see InventoryDragEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			ShulkerInventory shulkerInventory = ShulkerInventoryManager.getOpenShulkerInventory(player, event.getInventory());
			if (shulkerInventory != null) { // Shulker was opened via shortcut
				if (!acceptsItem(shulkerInventory, event.getOldCursor())
					&& event.getRawSlots().stream().anyMatch(slot -> event.getView().getInventory(slot) == shulkerInventory.getInventory())) {
					player.sendMessage(ChatColor.RED + "Only arrows can be put into a quiver");
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					event.setCancelled(true);
					return;
				}
				if (!mPlugin.mShulkerInventoryManager.updateShulker(player)) { // Try to update Shulker if it still exists.
					// The currently open shulker no longer exists, cancel the click and close the inventory.
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "Shulker no longer available");
					new BukkitRunnable() {
						@Override
						public void run() {
							player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
						}
					}.runTask(mPlugin);
				}
			}
		}
	}

	/**
	 * Event Handler for whenever an inventory is closed.
	 * Used to update and unlock Shulker Boxes when closed.
	 *
	 * @see InventoryCloseEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player player) {
			mPlugin.mShulkerInventoryManager.closeShulker(player);
		}
	}

	/**
	 * Event Handler for whenever a dispenser is activated.
	 * Used to prevent placing open shulkers, and unlock shulkers with an invalid lock.
	 *
	 * @see BlockDispenseEvent
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		ItemStack item = event.getItem();
		if (ItemUtils.isShulkerBox(item.getType())
			&& (ShulkerInventoryManager.isShulkerInUse(item)
			|| isPurpleTesseractContainer(item)
			|| isEnderExpansion(item)
			|| ItemStatUtils.getTier(item) != ItemStatUtils.Tier.SHULKER_BOX)) {
			event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			event.setCancelled(true);
		}
	}

	/**
	 * Event Handler for whenever a block is placed.
	 * Used to prevent placing open shulkers, and unlock shulkers with an invalid lock.
	 *
	 * @see BlockPlaceEvent
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlockPlaced();
		if (ItemUtils.isShulkerBox(block.getType())) {
			if (ShulkerInventoryManager.isShulkerInUse(block)) {
				event.setCancelled(true);
				event.setBuild(false);
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.sendMessage(ChatColor.RED + "That shulker is open");
			} else if (isPurpleTesseractContainer(event.getItemInHand())) {
				event.setCancelled(true);
				event.setBuild(false);

				ItemStack item = event.getItemInHand();
				ShulkerBox shulkerBox = (ShulkerBox) ((BlockStateMeta) item.getItemMeta()).getBlockState();
				@Nullable ItemStack[] contents = shulkerBox.getInventory().getContents();
				final String lockStr;
				if (shulkerBox.isLocked()) {
					lockStr = shulkerBox.getLock();
				} else {
					lockStr = null;
				}

				// Get the new chest and update that
				Bukkit.getScheduler().runTask(mPlugin, () -> {
					// Clears contents
					block.setType(Material.CHEST);

					if (block.getState() instanceof Chest chest) {
						if (lockStr != null) {
							chest.setLock(null);
							chest.customName(GsonComponentSerializer.gson().deserialize(lockStr));
						}
						chest.update();

						chest = (Chest) block.getState();
						chest.getInventory().setContents(contents);

						// Log CoreProtect data for chest placement
						CoreProtectIntegration.logPlacement(player, chest.getLocation(), chest.getBlockData().getMaterial(), chest.getBlockData());
					}
				});

				item.subtract();
			} else if (ChestUtils.isLootBox(event.getItemInHand())) {
				event.setCancelled(true);
				event.setBuild(false);

				ItemStack item = event.getItemInHand();
				@Nullable List<ItemStack> contents = ChestUtils.removeOneLootshareFromLootbox(item);
				if (contents == null) {
					// LootBox is empty
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					return;
				}

				// Get the new chest and update that
				Bukkit.getScheduler().runTask(mPlugin, () -> {
					// Clears contents
					block.setType(Material.CHEST);

					if (block.getState() instanceof Chest chest) {
						chest.update();

						chest = (Chest) block.getState();
						ChestUtils.generateLootInventory(contents, chest.getInventory(), player, true);
					}
				});
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mPlugin.mShulkerInventoryManager.closeDepositShulker(event.getPlayer());
		mPlugin.mShulkerInventoryManager.closeShulker(event.getPlayer(), true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		mPlugin.mShulkerInventoryManager.closeShulker(event.getEntity(), true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		if (isEnderExpansion(event.getItemDrop().getItemStack())) {
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			if (event.getPlayer().getInventory().firstEmpty() >= 0) {
				event.setCancelled(true);
			}
		}
	}

	public static boolean isPurpleTesseractContainer(ItemStack item) {
		return item != null
			       && ItemUtils.isShulkerBox(item.getType())
			       && (InventoryUtils.testForItemWithName(item, "Carrier of Emotion", true)
				           || InventoryUtils.testForItemWithName(item, "Carrier of Festivity", true));
	}

	public static boolean isPurpleTesseract(ItemStack item) {
		return item != null
			       && ((item.getType() == Material.PURPLE_STAINED_GLASS && InventoryUtils.testForItemWithName(item, "Tesseract of Emotions", true))
				           || isFestivePurpleTesseract(item));
	}

	public static boolean isFestivePurpleTesseract(ItemStack item) {
		return item != null
			       && item.getType() == Material.RED_DYE && InventoryUtils.testForItemWithName(item, "The Gift Wrapper", true);
	}

	public static boolean isEnderExpansion(ItemStack item) {
		return item != null &&
			       ItemUtils.isShulkerBox(item.getType()) &&
			       InventoryUtils.testForItemWithName(item, "Ender Chest Expansion", true);
	}

	/**
	 * Check if a given item is a restricted shulker, i.e. a shulker where a player cannot deposit items directly or take out items.
	 * These have some special means of accessing their contents, e.g. require being placed or used to swap equipment.
	 */
	public static boolean isRestrictedShulker(ItemStack item) {
		return isPurpleTesseractContainer(item)
			       || isEnderExpansion(item)
			       || ChestUtils.isLootBox(item)
			       || ShulkerEquipmentListener.isAnyEquipmentBox(item)
			       || PortableEnderListener.isPortableEnder(item);
	}

	private static boolean acceptsItem(ShulkerInventory shulkerInventory, ItemStack item) {
		return !ItemStatUtils.isQuiver(shulkerInventory.getShulkerItem()) || ItemUtils.isArrow(item);
	}

	/**
	 * Deposits all items in the player's inventory into the shulker if there's a matching item inside already.
	 */
	private static void depositAllMatching(Player player, ItemStack shulker) {
		if (shulker.getItemMeta() instanceof BlockStateMeta blockStateMeta
			    && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
			Inventory shulkerInventory = shulkerBox.getInventory();
			PlayerInventory playerInventory = player.getInventory();

			Map<String, Integer> depositedItems = new TreeMap<>();
			int totalDeposited = 0;

			// first clean items in the shulker in case that hasn't been done yet
			for (ItemStack item : shulkerInventory) {
				ItemStatUtils.cleanIfNecessary(item);
			}

			for (ItemStack playerItem : playerInventory) {
				if (playerItem == null
					    || playerItem.getType() == Material.AIR
					    || ItemUtils.isShulkerBox(playerItem.getType())
					    || !shulkerInventory.containsAtLeast(playerItem, 1)) {
					continue;
				}
				int originalAmount = playerItem.getAmount();
				String name = ItemUtils.getPlainNameOrDefault(playerItem);
				HashMap<Integer, ItemStack> remaining = shulkerInventory.addItem(playerItem);
				if (!remaining.containsKey(0)) {
					playerItem.setAmount(0);
					totalDeposited += originalAmount;
				} else {
					int remainingAmount = remaining.get(0).getAmount();
					playerItem.setAmount(remainingAmount);
					totalDeposited += originalAmount - remainingAmount;
				}
				depositedItems.merge(name, originalAmount - playerItem.getAmount(), Integer::sum);
			}

			blockStateMeta.setBlockState(shulkerBox);
			shulker.setItemMeta(blockStateMeta);

			if (totalDeposited > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.sendMessage(Component.text(totalDeposited + " item" + (totalDeposited == 1 ? "" : "s") + " deposited into shulker.", NamedTextColor.GOLD)
					                   .hoverEvent(HoverEvent.showText(Component.text(
						                   depositedItems.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
							                   .collect(Collectors.joining("\n")), NamedTextColor.GRAY))));
			}
		}
	}

}
