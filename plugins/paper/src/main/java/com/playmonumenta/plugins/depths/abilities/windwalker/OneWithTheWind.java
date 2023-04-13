package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class OneWithTheWind extends DepthsAbility {

	public static final String ABILITY_NAME = "One with the Wind";
	public static final double[] SPEED = {0.16, 0.2, 0.24, 0.28, 0.32, 0.4};
	public static final double[] PERCENT_DAMAGE_RECEIVED = {-.08, -.10, -.12, -.14, -.16, -.20};
	public static final int RANGE = 10;
	public static final String SPEED_EFFECT_NAME = "OneWithTheWindSpeedEffect";
	public static final String RESISTANCE_EFFECT_NAME = "OneWithTheWindResistanceEffect";

	public static final DepthsAbilityInfo<OneWithTheWind> INFO =
		new DepthsAbilityInfo<>(OneWithTheWind.class, ABILITY_NAME, OneWithTheWind::new, DepthsTree.WINDWALKER, DepthsTrigger.PASSIVE)
			.displayItem(Material.LIGHT_GRAY_BANNER)
			.descriptions(OneWithTheWind::getDescription);

	private boolean mActive = false;

	public OneWithTheWind(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		boolean wasActive = mActive;
		if (PlayerUtils.otherPlayersInRange(mPlayer, RANGE, true).size() == 0) {
			mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME, new PercentSpeed(40, SPEED[mRarity - 1], ABILITY_NAME).displaysTime(false));
			mPlugin.mEffectManager.addEffect(mPlayer, RESISTANCE_EFFECT_NAME, new PercentDamageReceived(40, PERCENT_DAMAGE_RECEIVED[mRarity - 1]).displaysTime(false));
			mActive = true;
		} else {
			mActive = mPlugin.mEffectManager.hasEffect(mPlayer, RESISTANCE_EFFECT_NAME);
		}
		if (wasActive != mActive) {
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("If there are no other players in an " + RANGE + " block radius, you gain ")
			.append(Component.text(StringUtils.multiplierToPercentage(-PERCENT_DAMAGE_RECEIVED[rarity - 1]) + "%", color))
			.append(Component.text(" resistance and "))
			.append(Component.text(StringUtils.multiplierToPercentage(SPEED[rarity - 1]) + "%", color))
			.append(Component.text(" speed."));
	}


	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}
}

