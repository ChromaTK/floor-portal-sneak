package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.abilities.Entropy;
import com.playmonumenta.plugins.delves.abilities.StatMultiplier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DelveCustomInventory extends CustomInventory {

	private static final ItemStack WHITE_ITEM = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
	private static final ItemStack SELECT_ALL_MOD_ITEM = getSelectAllModifiers();
	private static final ItemStack REMOVE_ALL_MOD_ITEM = getResetModifiers();
	private static final ItemStack STARTING_ITEM = new ItemStack(Material.OBSERVER);

	private static final Map<String, String> DUNGEON_FUNCTION_MAPPINGS = new HashMap<>();

	static {
		ItemMeta meta = WHITE_ITEM.getItemMeta();
		meta.displayName(Component.empty());
		WHITE_ITEM.setItemMeta(meta);

		meta = STARTING_ITEM.getItemMeta();
		meta.displayName(Component.text("Start delve!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		List<Component> lore = new ArrayList<>();
		lore.add(Component.text("- Requires " + DelvesUtils.MINIMUM_DEPTH_POINTS + " Depth Points to begin", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);
		STARTING_ITEM.setItemMeta(meta);

		meta = SELECT_ALL_MOD_ITEM.getItemMeta();
		meta.displayName(Component.text("Select all modifiers!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		SELECT_ALL_MOD_ITEM.setItemMeta(meta);

		meta = REMOVE_ALL_MOD_ITEM.getItemMeta();
		meta.displayName(Component.text("Remove all modifiers!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		REMOVE_ALL_MOD_ITEM.setItemMeta(meta);


		DUNGEON_FUNCTION_MAPPINGS.put("white", "function monumenta:lobbies/d1/new");
		DUNGEON_FUNCTION_MAPPINGS.put("orange", "function monumenta:lobbies/d2/new");
		DUNGEON_FUNCTION_MAPPINGS.put("magenta", "function monumenta:lobbies/d3/new");
		DUNGEON_FUNCTION_MAPPINGS.put("lightblue", "function monumenta:lobbies/d4/new");
		DUNGEON_FUNCTION_MAPPINGS.put("yellow", "function monumenta:lobbies/d5/new");
		DUNGEON_FUNCTION_MAPPINGS.put("willows", "function monumenta:lobbies/db1/new");
		DUNGEON_FUNCTION_MAPPINGS.put("reverie", "function monumenta:lobbies/dc/new");
		DUNGEON_FUNCTION_MAPPINGS.put("lime", "function monumenta:lobbies/d6/new");
		DUNGEON_FUNCTION_MAPPINGS.put("pink", "function monumenta:lobbies/d7/new");
		DUNGEON_FUNCTION_MAPPINGS.put("gray", "function monumenta:lobbies/d8/new");
		DUNGEON_FUNCTION_MAPPINGS.put("lightgray", "function monumenta:lobbies/d9/new");
		DUNGEON_FUNCTION_MAPPINGS.put("cyan", "function monumenta:lobbies/d10/new");
		DUNGEON_FUNCTION_MAPPINGS.put("purple", "function monumenta:lobbies/d11/new");
		DUNGEON_FUNCTION_MAPPINGS.put("teal", "function monumenta:lobbies/dtl/new");
		DUNGEON_FUNCTION_MAPPINGS.put("forum", "function monumenta:lobbies/dff/new");
		DUNGEON_FUNCTION_MAPPINGS.put("shiftingcity", "function monumenta:lobbies/drl2/new");
	}

	private static final int[] DELVE_MOD_ITEM_SLOTS = {
		///* 0,*/   1,  2,  3,  4,  5,  6,  7, // 8,
		///* 9,*/  10, 11, 12, 13, 14, 15, 16, //17,
		///*18,*/  19, 20, 21, 22, 23, 24, 25, //26,
		///*27,*/  28, 29, 30, 31, 32, 33, 34, //35,
		///*36,*/  37, 38, 39, 40, 41, 42, 43, //44,
		/*45,*/    46, 47, 48, 49, 50, 51, 52, //53
	};

	private static final int[] COLUMN_INDEX_SLOT = {
		/*0,*/  1, 2, 3, 4, 5, 6, 7, //53
	};

	private static final int TOTAL_POINT_SLOT = 0;
	private static final int START_SLOT = 8;
	private static final int PAGE_LEFT_SLOT = 45;
	private static final int PAGE_RIGHT_SLOT = 53;

	private static final int TOTAL_DELVE_MOD = DelvesModifier.values().length;

	private final boolean mEditableDelvePoint;
	private final String mDungeonName;
	private int mPage = 0;
	private int mTotalPoint = 0;

	private final Map<DelvesModifier, Integer> mPointSelected = new HashMap<>();

	public DelveCustomInventory(Player owner, String dungeon, boolean editable) {
		super(owner, 54, "Delve Modifiers " + (editable ? "Selection" : "Selected"));
		mEditableDelvePoint = editable;
		mDungeonName = dungeon;

		for (DelvesModifier mod : DelvesModifier.values()) {
			mPointSelected.put(mod, DelvesUtils.getDelveModLevel(owner, dungeon, mod));
		}

		loadInv();
	}

	private void loadInv() {
		mTotalPoint = 0;
		mInventory.clear();
		List<DelvesModifier> mods = DelvesModifier.valuesList();
		mods.remove(DelvesModifier.ENTROPY);
		mods.remove(DelvesModifier.TWISTED);

		for (DelvesModifier mod : mods) {
			mTotalPoint += mPointSelected.getOrDefault(mod, 0);
		}

		mTotalPoint += Entropy.getDepthPointsAssigned(mPointSelected.getOrDefault(DelvesModifier.ENTROPY, 0));
		mTotalPoint += (mPointSelected.getOrDefault(DelvesModifier.TWISTED, 0) * 5);

		mTotalPoint = Math.min(DelvesUtils.MAX_DEPTH_POINTS, mTotalPoint);


		mInventory.setItem(TOTAL_POINT_SLOT, getSummary());
		if (mEditableDelvePoint) {
			mInventory.setItem(START_SLOT, STARTING_ITEM);
		}

		mods = DelvesModifier.valuesList();
		for (int i = 0; i < 7; i++) {
			DelvesModifier mod = mods.get((mPage * 7) + i);
			if (mod != null) {
				mInventory.setItem(DELVE_MOD_ITEM_SLOTS[i], mod.getIcon());
				int level = mPointSelected.getOrDefault(mod, 0);

				for (int j = 0; j < 5; j++) {
					ItemStack stack = DelvesUtils.getRankItem(mod, j + 1, level);
					int slot = (DELVE_MOD_ITEM_SLOTS[i] - (9 * (j + 1)));
					mInventory.setItem(slot, stack);
				}
			}
		}

		if (mPage > 0) {
			mInventory.setItem(PAGE_LEFT_SLOT, getPreviousPage());
		} else {
			mInventory.setItem(PAGE_LEFT_SLOT, REMOVE_ALL_MOD_ITEM);
		}

		if (TOTAL_DELVE_MOD - ((mPage + 1) * 7) > 0) {
			mInventory.setItem(PAGE_RIGHT_SLOT, getNextPage());
		} else {
			mInventory.setItem(PAGE_RIGHT_SLOT, SELECT_ALL_MOD_ITEM);
		}


		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, WHITE_ITEM);
			}
		}
	}

	public ItemStack getSummary() {
		int depthPoints = mTotalPoint;

		ItemStack item = new ItemStack(Material.SOUL_LANTERN, Math.max(1, depthPoints));

		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Delve Summary", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

		List<Component> lore = new ArrayList<>();

		lore.add(Component.text(String.format("%d Depth Points Assigned", depthPoints), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));

		lore.add(Component.text(""));

		lore.add(Component.text("Stat Multipliers from Depth Points:", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		double damageMultiplier = StatMultiplier.getDamageMultiplier(depthPoints);
		double healthMultiplier = StatMultiplier.getHealthMultiplier(depthPoints);
		double speedMultiplier = StatMultiplier.getSpeedMultiplier(depthPoints);
		NamedTextColor damageMultiplierColor;
		if (damageMultiplier >= 1.75) {
			damageMultiplierColor = NamedTextColor.DARK_RED;
		} else if (damageMultiplier >= 1.45) {
			damageMultiplierColor = NamedTextColor.RED;
		} else {
			damageMultiplierColor = NamedTextColor.GRAY;
		}
		lore.add(Component.text(String.format("- Damage Multiplier: x%.3f", damageMultiplier), damageMultiplierColor).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Health Multiplier: x%.3f", healthMultiplier), damageMultiplierColor).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Speed Multiplier: x%.3f", speedMultiplier), damageMultiplierColor).decoration(TextDecoration.ITALIC, false));

		lore.add(Component.text(""));

		double dungeonMultiplier = StatMultiplier.getStatCompensation(mDungeonName);
		lore.add(Component.text("Stat Multipliers from Base Dungeon:", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Damage Multiplier: x%.3f", dungeonMultiplier), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Health Multiplier: x%.3f", dungeonMultiplier), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		lore.add(Component.text(""));


		double baseAmount = DelveLootTableGroup.getDelveMaterialTableChance(DelvesUtils.MINIMUM_DEPTH_POINTS, 9001);
		double delveMaterialMultiplierSolo = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 1) / baseAmount;
		double delveMaterialMultiplierDuo = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 2) / baseAmount;
		double delveMaterialMultiplierTrio = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 3) / baseAmount;
		double delveMaterialMultiplier = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 9001) / baseAmount;

		if (delveMaterialMultiplier > 0 && !ServerProperties.getShardName().contains("depths")) {
			lore.add(Component.text("Delve Material Multipliers (Not Counting Loot Scaling):", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			if (delveMaterialMultiplierSolo == DelveLootTableGroup.getDelveMaterialTableChance(9001, 1) / baseAmount) {
				lore.add(Component.text(String.format("- 1 Player: x%.2f (Capped)", delveMaterialMultiplierSolo), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			} else {
				lore.add(Component.text(String.format("- 1 Player: x%.2f", delveMaterialMultiplierSolo), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			}

			if (delveMaterialMultiplierDuo == DelveLootTableGroup.getDelveMaterialTableChance(9001, 2) / baseAmount) {
				lore.add(Component.text(String.format("- 2 Players: x%.2f (Capped)", delveMaterialMultiplierDuo), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			} else {
				lore.add(Component.text(String.format("- 2 Players: x%.2f", delveMaterialMultiplierDuo), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			}

			if (delveMaterialMultiplierTrio == DelveLootTableGroup.getDelveMaterialTableChance(9001, 3) / baseAmount) {
				lore.add(Component.text(String.format("- 3 Players: x%.2f (Capped)", delveMaterialMultiplierTrio), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			} else {
				lore.add(Component.text(String.format("- 3 Players: x%.2f", delveMaterialMultiplierTrio), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			}

			if (delveMaterialMultiplier == DelveLootTableGroup.getDelveMaterialTableChance(9001, 9001) / baseAmount) {
				lore.add(Component.text(String.format("- 4+ Players: x%.2f (Capped)", delveMaterialMultiplier), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			} else {
				lore.add(Component.text(String.format("- 4+ Players: x%.2f", delveMaterialMultiplier), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			}
		} else if (!ServerProperties.getShardName().contains("depths")) {
			lore.add(Component.text("Delve Material Multipliers (Not Counting Loot Scaling):", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text(String.format("  - 1 Player: x%.2f", delveMaterialMultiplierSolo), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text(String.format("  - 2 Players: x%.2f", delveMaterialMultiplierDuo), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text(String.format("  - 3 Players: x%.2f", delveMaterialMultiplierTrio), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text(String.format("  - 4+ Players: x%.2f", delveMaterialMultiplier), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		}

		lore.add(Component.text(""));

		if (depthPoints == DelvesUtils.MAX_DEPTH_POINTS) {
			lore.add(Component.text("- All DelvesModifiers Advancement Granted upon Completion", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("- All DelvesModifiers Advancement Granted upon Completion", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		}

		meta.lore(lore);

		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);

		return item;
	}

	private static ItemStack getResetModifiers() {
		ItemStack item = new ItemStack(Material.BARRIER, 1);

		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Reset Modifiers", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		item.setItemMeta(meta);
		ItemUtils.setPlainName(item);

		return item;
	}

	private static ItemStack getSelectAllModifiers() {
		ItemStack item = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);

		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Select All Modifiers", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		item.setItemMeta(meta);
		ItemUtils.setPlainName(item);

		return item;
	}

	private ItemStack getNextPage() {
		ItemStack item = new ItemStack(Material.ARROW, 1);

		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Next Page", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		item.setItemMeta(meta);
		ItemUtils.setPlainName(item);

		return item;
	}

	private ItemStack getPreviousPage() {
		ItemStack item = new ItemStack(Material.ARROW, 1);

		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Previous Page", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		item.setItemMeta(meta);
		ItemUtils.setPlainName(item);

		return item;
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		if (!mInventory.equals(event.getClickedInventory())) {
			return;
		}
		event.setCancelled(true);

		Player player = (Player) event.getWhoClicked();
		int slot = event.getSlot();

		int column = slot % 9;
		int row = slot / 9;
		if (mEditableDelvePoint) {
			for (int i : COLUMN_INDEX_SLOT) {
				if (i == column) {
					DelvesModifier mod = DelvesModifier.values()[column - 1 + (mPage * 7)];
					if (row == 5) {
						//last row -> clearing this delve mod point
						mPointSelected.put(mod, 0);
					} else {
						int finaPoint = DelvesUtils.getMaxPointAssignable(mod, 5 - row);
						mPointSelected.put(mod, finaPoint);
					}
				}
			}

		}

		if (slot == PAGE_RIGHT_SLOT) {
			if (TOTAL_DELVE_MOD - ((mPage + 1) * 7) > 0) {
				mPage++;
				player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
			} else if (mEditableDelvePoint) {
				player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
				for (DelvesModifier mod : DelvesModifier.values()) {
					mPointSelected.put(mod, DelvesUtils.getMaxPointAssignable(mod, 1000));
				}
			}

		}

		if (slot == PAGE_LEFT_SLOT) {
			if (mPage > 0) {
				mPage--;
				player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
			} else if (mEditableDelvePoint) {
				player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1f, 0.5f);
				mPointSelected.forEach((mod, value) -> mPointSelected.put(mod, 0));
			}
		}

		if (slot == START_SLOT && mEditableDelvePoint) {
			if (mTotalPoint >= 5) {
				if (mPointSelected.containsKey(DelvesModifier.ENTROPY)) {
					//give random point for each point of entropy

					int entropyPoint = Entropy.getDepthPointsAssigned(mPointSelected.get(DelvesModifier.ENTROPY));

					List<DelvesModifier> mods = DelvesModifier.valuesList();
					mods.remove(DelvesModifier.ENTROPY);
					mods.remove(DelvesModifier.TWISTED);

					while (entropyPoint > 0) {
						if (mods.isEmpty()) {
							break;
						}
						Collections.shuffle(mods);
						DelvesModifier mod = mods.get(0);
						int oldValue = mPointSelected.getOrDefault(mod, 0);
						if (oldValue == DelvesUtils.getMaxPointAssignable(mod, oldValue + 1)) {
							mods.remove(mod);
							continue;
						}
						mPointSelected.put(mod, oldValue + 1);
						entropyPoint--;

					}
				}

				DelvesManager.savePlayerData(player, mDungeonName, mPointSelected);
				mInventory.clear();
				player.closeInventory();
				String dungeonFunc = DUNGEON_FUNCTION_MAPPINGS.get(mDungeonName);
				if (dungeonFunc == null) {
					player.sendMessage(Component.text("There have been some problem, please contact the dev team, reason: DUNGEON_FUNCTION_MAPPINGS.get() is null", NamedTextColor.RED));
				} else {
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
						Bukkit.getConsoleSender().getServer().dispatchCommand(Bukkit.getConsoleSender(),
							"execute as " + player.getName() + " at @s run " + dungeonFunc);

					}, 0);
				}
			} else {
				player.sendMessage(Component.text("You need at least 5 delve point to start a delve", NamedTextColor.RED));
			}
		}
		loadInv();

	}
}