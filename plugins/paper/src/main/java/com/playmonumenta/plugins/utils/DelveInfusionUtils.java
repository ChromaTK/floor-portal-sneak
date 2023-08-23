package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.custominventories.ClassSelectionCustomInventory;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.listeners.AuditListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class DelveInfusionUtils {

	public static final int MAX_LEVEL = 4;
	public static final int[] MAT_DEPTHS_COST_PER_INFUSION = {2, 4, 8, 16};
	public static final int[] MAT_COST_PER_INFUSION = {3, 6, 12, 24};
	public static final int[] XP_COST_PER_LEVEL = {ExperienceUtils.LEVEL_40, ExperienceUtils.LEVEL_50, ExperienceUtils.LEVEL_60, ExperienceUtils.LEVEL_70};

	/**When set to true the refund function will return all the XP used for the infusion, when false only the 75% */
	public static final boolean FULL_REFUND = false;
	public static final double REFUND_PERCENT = 0.75;

	public static final NamespacedKey DEPTHS_MAT_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/depths/loot/voidstained_geode");

	public enum DelveInfusionSelection {
		PENNATE("pennate", InfusionType.PENNATE, Location.WHITE, Material.WHITE_WOOL, "Soul Essences", NamespacedKeyUtils.fromString("epic:r1/delves/white/auxiliary/delve_material"), "White"),
		CARAPACE("carapace", InfusionType.CARAPACE, Location.ORANGE, Material.ORANGE_WOOL, "Beastly Broods", NamespacedKeyUtils.fromString("epic:r1/delves/orange/auxiliary/delve_material"), "Orange"),
		AURA("aura", InfusionType.AURA, Location.MAGENTA, Material.MAGENTA_WOOL, "Plagueroot Saps", NamespacedKeyUtils.fromString("epic:r1/delves/magenta/auxiliary/delve_material"), "Magenta"),
		EXPEDITE("expedite", InfusionType.EXPEDITE, Location.LIGHTBLUE, Material.LIGHT_BLUE_WOOL, "Arcane Crystals", NamespacedKeyUtils.fromString("epic:r1/delves/lightblue/auxiliary/delve_material"), "LightBlue"),
		CHOLER("choler", InfusionType.CHOLER, Location.YELLOW, Material.YELLOW_WOOL, "Season's Wraths", NamespacedKeyUtils.fromString("epic:r1/delves/yellow/auxiliary/delve_material"), "Yellow"),
		UNYIELDING("unyielding", InfusionType.UNYIELDING, Location.WILLOWS, Material.MOSSY_COBBLESTONE, "Echoes of the Veil", NamespacedKeyUtils.fromString("epic:r1/delves/willows/auxiliary/echoes_of_the_veil"), "R1Bonus"),
		USURPER("usurper", InfusionType.USURPER, Location.REVERIE, Material.NETHER_WART_BLOCK, "Nightmare Fuels", NamespacedKeyUtils.fromString("epic:r1/delves/reverie/auxiliary/delve_material"), "Corrupted"),
		VENGEFUL("vengeful", InfusionType.VENGEFUL, Location.EPHEMERAL, Material.MAGMA_BLOCK, "Persistent Parchments", NamespacedKeyUtils.fromString("epic:r1/delves/rogue/persistent_parchment"), "RogFinished", "RogFinishedN", "RogFinishedC", "RogFinishedD"),

		EMPOWERED("empowered", InfusionType.EMPOWERED, Location.LIME, Material.LIME_WOOL, "Refound Knowledge", NamespacedKeyUtils.fromString("epic:r2/delves/lime/auxiliary/delve_material"), "Lime"),
		NUTRIMENT("nutriment", InfusionType.NUTRIMENT, Location.PINK, Material.PINK_WOOL, "Roots of Balance", NamespacedKeyUtils.fromString("epic:r2/delves/pink/auxiliary/delve_material"), "Pink"),
		EXECUTION("execution", InfusionType.EXECUTION, Location.GRAY, Material.GRAY_WOOL, "Forgotten Ashes", NamespacedKeyUtils.fromString("epic:r2/delves/gray/auxiliary/delve_material"), "Gray"),
		REFLECTION("reflection", InfusionType.REFLECTION, Location.LIGHTGRAY, Material.LIGHT_GRAY_WOOL, "Aurora Shards", NamespacedKeyUtils.fromString("epic:r2/delves/lightgray/auxiliary/delve_material"), "LightGray"),
		MITOSIS("mitosis", InfusionType.MITOSIS, Location.CYAN, Material.CYAN_WOOL, "Feverish Flesh", NamespacedKeyUtils.fromString("epic:r2/delves/cyan/auxiliary/delve_material"), "Cyan"),
		ARDOR("ardor", InfusionType.ARDOR, Location.PURPLE, Material.PURPLE_WOOL, "Despondent Doubloons", NamespacedKeyUtils.fromString("epic:r2/delves/purple/auxiliary/delve_material"), "Purple"),
		EPOCH("epoch", InfusionType.EPOCH, Location.TEAL, Material.CYAN_CONCRETE_POWDER, "Weathered Runes", NamespacedKeyUtils.fromString("epic:r2/delves/teal/auxiliary/delve_material"), "Teal"),
		NATANT("natant", InfusionType.NATANT, Location.SHIFTING, Material.BLUE_CONCRETE, "Primordial Clay", NamespacedKeyUtils.fromString("epic:r2/delves/shiftingcity/auxiliary/delve_material"), "Fred"),
		UNDERSTANDING("understanding", InfusionType.UNDERSTANDING, Location.FORUM, Material.BOOKSHELF, "Binah Leaves", NamespacedKeyUtils.fromString("epic:r2/delves/forum/auxiliary/delve_material"), "Forum"),

		SOOTHING("soothing", InfusionType.SOOTHING, Location.BLUE, Material.BLUE_WOOL, "Sorceress' Staves", NamespacedKeyUtils.fromString("epic:r3/items/currency/sorceress_stave"), "Blue"),
		FUELED("fueled", InfusionType.FUELED, Location.BROWN, Material.BROWN_WOOL, "Broken God Gearframes", NamespacedKeyUtils.fromString("epic:r3/items/currency/broken_god_gearframe"), "Brown"),
		REFRESH("refresh", InfusionType.REFRESH, Location.SILVER, Material.POLISHED_DEEPSLATE, "Silver Remnants", NamespacedKeyUtils.fromString("epic:r3/items/currency/silver_remnant"), "SKT", "SKTH"),
		QUENCH("quench", InfusionType.QUENCH, Location.FOREST, Material.DARK_OAK_WOOD, "Fenian Flowers", NamespacedKeyUtils.fromString("epic:r3/items/currency/fenian_flower"), ClassSelectionCustomInventory.R3_UNLOCK_SCOREBOARD),
		GRACE("grace", InfusionType.GRACE, Location.KEEP, Material.CRACKED_STONE_BRICKS, "Iridium Catalysts", NamespacedKeyUtils.fromString("epic:r3/items/currency/iridium_catalyst"), ClassSelectionCustomInventory.R3_UNLOCK_SCOREBOARD),
		GALVANIC("galvanic", InfusionType.GALVANIC, Location.SCIENCE, Material.IRON_BLOCK, "Corrupted Circuits", NamespacedKeyUtils.fromString("epic:r3/items/currency/corrupted_circuit"), "Portal"),
		DECAPITATION("decapitation", InfusionType.DECAPITATION, Location.BLUESTRIKE, Material.WITHER_SKELETON_SKULL, "Shattered Masks", NamespacedKeyUtils.fromString("epic:r3/items/currency/shattered_mask"), "MasqueradersRuin"),

		REFUND("refund", null, null, Material.GRINDSTONE, null, null, (String[]) null);

		private final String mLabel;
		private final @Nullable InfusionType mInfusionType;
		private final @Nullable Location mLocation;
		private final Material mMaterial;
		private final @Nullable String mDelveMatPlural;
		private final @Nullable NamespacedKey mLootTable;
		private final @Nullable List<String> mScoreboard;

		DelveInfusionSelection(String label, @Nullable InfusionType infusionType, @Nullable Location location, Material material, @Nullable String delveMatPlural, @Nullable NamespacedKey lootTable, @Nullable String... scoreboard) {
			mLabel = label;
			mInfusionType = infusionType;
			mLocation = location;
			mMaterial = material;
			mDelveMatPlural = delveMatPlural;
			mLootTable = lootTable;
			mScoreboard = scoreboard == null ? null : Arrays.asList(scoreboard);
		}

		public static @Nullable DelveInfusionSelection getInfusionSelection(@Nullable String label) {
			if (label == null) {
				return null;
			}
			for (DelveInfusionSelection selection : DelveInfusionSelection.values()) {
				if (selection.getLabel().equals(label)) {
					return selection;
				}
			}
			return null;
		}

		public static DelveInfusionSelection getByType(InfusionType infusionType) {
			for (DelveInfusionSelection infusionSelection : values()) {
				if (infusionSelection.mInfusionType == infusionType) {
					return infusionSelection;
				}
			}
			return REFUND;
		}

		public String getLabel() {
			return mLabel;
		}

		public String getCapitalizedLabel() {
			return StringUtils.capitalizeWords(mLabel);
		}

		public @Nullable InfusionType getInfusionType() {
			return mInfusionType;
		}

		public Material getMaterial() {
			return mMaterial;
		}

		public String getDelveMatPlural() {
			return mDelveMatPlural == null ? "" : mDelveMatPlural;
		}

		public @Nullable NamespacedKey getLootTable() {
			return mLootTable;
		}

		public boolean isUnlocked(Player player) {
			return mScoreboard == null || mScoreboard.stream().anyMatch(s -> s == null || ScoreboardUtils.getScoreboardValue(player, s).orElse(0) >= 1);
		}

		public TextColor getColor() {
			if (mLocation == null) {
				return NamedTextColor.WHITE;
			}
			return mLocation.getColor();
		}
	}

	public static void infuseItem(Player player, ItemStack item, DelveInfusionSelection selection) {
		if (selection.equals(DelveInfusionSelection.REFUND)) {
			refundInfusion(item, player);
			return;
		}

		InfusionType infusionType = selection.getInfusionType();
		if (infusionType == null) {
			return;
		}

		//Assume the player has already paid for this infusion
		int prevLvl = ItemStatUtils.getInfusionLevel(item, infusionType);
		if (prevLvl > 0) {
			ItemStatUtils.removeInfusion(item, infusionType, false);
		}
		ItemStatUtils.addInfusion(item, infusionType, prevLvl + 1, player.getUniqueId());

		EntityUtils.fireworkAnimation(player);
	}

	public static void refundInfusion(ItemStack item, Player player) {
		DelveInfusionSelection infusion = getCurrentInfusion(item);
		if (infusion == null) {
			return;
		}
		InfusionType infusionType = infusion.getInfusionType();
		if (infusionType == null) {
			return;
		}

		int level = getInfuseLevel(item) - 1;
		int levelXp = level;

		ItemStatUtils.removeInfusion(item, infusionType);

		/* Audit */
		String matStr = "";
		int auditLevel = level + 1;

		while (level >= 0) {
			List<ItemStack> mats = getCurrenciesCost(item, infusion, level, player);
			level--;

			/* Audit */
			for (ItemStack it : mats) {
				if (it != null && it.getAmount() > 0) {
					if (!matStr.isEmpty()) {
						matStr += ",";
					}
					matStr += "'" + ItemUtils.getPlainName(it) + ":" + it.getAmount() + "'";
				}
			}

			giveMaterials(player, mats);
		}

		int xp = 0;
		for (int i = 0; i <= levelXp; i++) {
			xp += (int) (XP_COST_PER_LEVEL[i] * (FULL_REFUND ? 1 : REFUND_PERCENT) * item.getAmount());
		}
		ExperienceUtils.setTotalExperience(player, ExperienceUtils.getTotalExperience(player) + xp);

		AuditListener.logPlayer("[Delve Infusion] Refund - player=" + player.getName() + ", item='" + ItemUtils.getPlainName(item) + "', infusion type=" + infusionType
			                        + "', from level=" + auditLevel + ", stack size=" + item.getAmount() + ", refunded materials=" + matStr + ", refunded XP=" + xp);

	}

	private static void giveMaterials(Player player, List<ItemStack> mats) {
		for (ItemStack item : mats) {
			InventoryUtils.giveItem(player, item);
		}
	}

	private static int getInfuseLevel(ItemStack item) {
		int level = 0;
		for (DelveInfusionSelection d : DelveInfusionSelection.values()) {
			level += ItemStatUtils.getInfusionLevel(item, d.getInfusionType());
		}
		return level;
	}

	public static boolean canPayInfusion(ItemStack item, DelveInfusionSelection selection, Player p) {

		if (selection == DelveInfusionSelection.REFUND || p.getGameMode() == GameMode.CREATIVE) {
			return true;
		}
		int targetLevel = getInfuseLevel(item);
		List<ItemStack> mats = getCurrenciesCost(item, selection, targetLevel, p);
		int playerXP = ExperienceUtils.getTotalExperience(p);

		if (playerXP < XP_COST_PER_LEVEL[targetLevel]) {
			return false;
		}

		PlayerInventory inventory = p.getInventory();

		for (ItemStack currencies : mats) {
			if (!inventory.containsAtLeast(currencies, currencies.getAmount())) {
				return false;
			}
		}

		return true;
	}

	public static boolean payInfusion(ItemStack item, DelveInfusionSelection selection, Player p) {
		int targetLevel = getInfuseLevel(item);
		List<ItemStack> mats = getCurrenciesCost(item, selection, targetLevel, p);

		String matStr = mats.stream().filter(it -> it != null && it.getAmount() > 0)
			                .map(it -> "'" + ItemUtils.getPlainName(it) + ":" + it.getAmount() + "'")
			                .collect(Collectors.joining(","));

		//if the player is in creative -> free infusion
		if (selection == DelveInfusionSelection.REFUND || p.getGameMode() == GameMode.CREATIVE) {
			AuditListener.log("[Delve Infusion] Player " + p.getName() + " infused an item while in creative mode! item='" + ItemUtils.getPlainName(item) + "', infusion type=" + selection.mInfusionType
				                  + "', new level=" + (targetLevel + 1) + ", stack size=" + item.getAmount() + ", normal material cost=" + matStr + ", normal XP cost=" + XP_COST_PER_LEVEL[targetLevel]);
			return true;
		}

		int playerXP = ExperienceUtils.getTotalExperience(p);

		if (playerXP < XP_COST_PER_LEVEL[targetLevel]) {
			return false;
		} else {
			ExperienceUtils.setTotalExperience(p, playerXP - XP_COST_PER_LEVEL[targetLevel]);
		}

		PlayerInventory inventory = p.getInventory();

		for (ItemStack currencies : mats) {
			inventory.removeItem(currencies);
		}

		AuditListener.logPlayer("[Delve Infusion] Item infused - player=" + p.getName() + ", item='" + ItemUtils.getPlainName(item) + "', infusion type=" + selection.mInfusionType
			                        + "', new level=" + (targetLevel + 1) + ", stack size=" + item.getAmount() + ", material cost=" + matStr + ", XP cost=" + XP_COST_PER_LEVEL[targetLevel]);

		return true;
	}

	public static List<ItemStack> getCurrenciesCost(ItemStack item, DelveInfusionSelection selection, int level, Player p) {

		List<ItemStack> cost = new ArrayList<>();

		//Get delve mat loot table
		ItemStack delveMats = Objects.requireNonNull(InventoryUtils.getItemFromLootTable(p, Objects.requireNonNull(selection.mLootTable)));
		delveMats.setAmount(MAT_COST_PER_INFUSION[level] * item.getAmount());
		cost.add(delveMats);

		//Get depth mat loot table
		ItemStack depthMats = Objects.requireNonNull(InventoryUtils.getItemFromLootTable(p, DEPTHS_MAT_LOOT_TABLE));
		depthMats.setAmount(MAT_DEPTHS_COST_PER_INFUSION[level] * item.getAmount());
		cost.add(depthMats);
		return cost;
	}

	public static @Nullable DelveInfusionSelection getCurrentInfusion(ItemStack item) {
		for (DelveInfusionSelection infusionSelection : DelveInfusionSelection.values()) {
			if (ItemStatUtils.getInfusionLevel(item, infusionSelection.getInfusionType()) > 0) {
				return infusionSelection;
			}
		}
		return null;
	}

	public static int getInfusionLevel(ItemStack item, DelveInfusionSelection selection) {
		return ItemStatUtils.getInfusionLevel(item, selection.getInfusionType());
	}

	public static int getExpLvlInfuseCost(ItemStack item) {
		int exp = XP_COST_PER_LEVEL[getInfuseLevel(item)];

		switch (exp) {
			case 1395:
				return 30;
			case 2920:
				return 40;
			case 5345:
				return 50;
			case 8670:
				return 60;
			case 12895:
				return 70;
			case 18020:
				return 80;
			default:
				return 0;
		}
	}
}
