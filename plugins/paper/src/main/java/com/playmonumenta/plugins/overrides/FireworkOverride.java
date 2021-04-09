package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class FireworkOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (item != null && block != null) {
			if (!block.getType().isInteractable()) {
				ItemMeta meta = item.getItemMeta();
				if (meta != null) {
					if (meta.hasDisplayName() && meta.getDisplayName().contains("Signal Flare")) {
						plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.GLOWING, 10 * 20, 0));
					}
				}
			}
		}

		return true;
	}
}
