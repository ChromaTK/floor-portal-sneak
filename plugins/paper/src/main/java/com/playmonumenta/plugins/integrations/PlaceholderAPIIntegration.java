package com.playmonumenta.plugins.integrations;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {
	Plugin mPlugin;

	public PlaceholderAPIIntegration(Plugin plugin) {
		super();
		plugin.getLogger().info("Enabling PlaceholderAPI integration");
		mPlugin = plugin;
	}

	@Override
	public String getIdentifier() {
		return "monumenta";
	}

	@Override
	public @Nullable String getPlugin() {
		return null;
	}

	@Override
	public String getAuthor() {
		return "Team Epic";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}

	@Override
	public @Nullable String onPlaceholderRequest(Player player, String identifier) {

		if (identifier.startsWith("loot_table:")) {
			String lootTable = identifier.substring("loot_table:".length());
			ItemStack item = InventoryUtils.getItemFromLootTable(Bukkit.getWorlds().get(0).getSpawnLocation(), NamespacedKeyUtils.fromString(lootTable));
			if (item == null) {
				return "";
			} else {
				Component name = item.getItemMeta().displayName();
				if (name == null) {
					name = Component.translatable(item.getType().getTranslationKey());
				}
				return MiniMessage.miniMessage().serialize(name.hoverEvent(item.asHoverEvent()));
			}
		}

		if (player == null) {
			return "";
		}

		// %monumenta_class%
		if (identifier.equalsIgnoreCase("class")) {
			// TODO: This really should use the standard thing in Plugin.java... but it's
			// currently a pile of crap and this is actually less awful
			switch (ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0)) {
			case 0:
				return "No class";
			case 1:
				return "Mage";
			case 2:
				return "Warrior";
			case 3:
				return "Cleric";
			case 4:
				return "Rogue";
			case 5:
				return "Alchemist";
			case 6:
				return "Scout";
			case 7:
				return "Warlock";
			default:
				return "Unknown class";

			}
		}

		// %monumenta_level%
		if (identifier.equalsIgnoreCase("level")) {
			return Integer.toString(ScoreboardUtils.getScoreboardValue(player, "TotalLevel").orElse(0));
		}

		if (identifier.equalsIgnoreCase("shard")) {
			String toCut = player.getWorld().getName();
			String mask = "Project_Epic-";
			if (toCut.length() > mask.length()) {
				String finalString = toCut.substring(mask.length(), toCut.length());
				return finalString;
			}
		}

		//Player equipped title
		if (identifier.equalsIgnoreCase("title")) {
			Cosmetic title = CosmeticsManager.getInstance().getActiveCosmetic(player, CosmeticType.TITLE);
			if (title != null) {
				return title.getName() + " ";
			} else {
				return "";
			}
		}

		return null;
	}
}
