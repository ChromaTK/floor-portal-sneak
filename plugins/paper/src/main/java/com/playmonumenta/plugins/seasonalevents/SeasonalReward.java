package com.playmonumenta.plugins.seasonalevents;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SeasonalReward {

	public SeasonalRewardType mType;

	// String data- name of title, or parrot unlock, or ability skin name
	public @Nullable String mData;

	// Int data - for amounts such as number of loot spins
	public int mAmount;

	// Fields for item display in GUI
	public @Nullable String mName;
	public @Nullable String mDescription;
	public @Nullable Material mDisplayItem;
	public @Nullable TextColor mNameColor;
	public @Nullable TextColor mDescriptionColor;
	public @Nullable ItemStack mLootTable;

	public SeasonalReward(SeasonalRewardType type) {
		mType = type;
	}

}
