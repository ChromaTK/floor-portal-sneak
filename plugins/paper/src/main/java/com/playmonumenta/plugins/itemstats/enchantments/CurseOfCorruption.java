package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseOfCorruption implements Enchantment {

	private static List<Player> mCorruptionPlayers = new ArrayList<>();

	@Override
	public String getName() {
		return "Curse of Corruption";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_CORRUPTION;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CURSE_OF_CORRUPTION) > 1) {
			if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.PLOTS_POSSIBLE)) {
				mCorruptionPlayers.add(player);
				if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
					plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.BLINDNESS, 1000000, 0, true, false));
				}
				plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW, 1000000, (int) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CURSE_OF_CORRUPTION) - 2, true, false));
				plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW_DIGGING, 1000000, (int) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CURSE_OF_CORRUPTION) - 1, true, false));
			}
		} else if (mCorruptionPlayers.remove(player)) {
			plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.BLINDNESS);
			plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.SLOW);
			plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.SLOW_DIGGING);
		}
	}

}
