package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
/*
 * Curse of Anemia - Reduces all healing received by 10% per level.
 */

public class CurseOfAnemia implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.RED + "Curse of Anemia";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onRegain(Plugin plugin, Player player, int level, EntityRegainHealthEvent event) {
		double reducedHealth;
		//Case if player has over 100% reduced hp, make hp gain 0 instead of losing hp
		if (level >= 10) {
			reducedHealth = 0;
		} else {
			reducedHealth = event.getAmount() * (1 - (0.1 * level));
		}
		event.setAmount(reducedHealth);
	}
}
