package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class RadiantBlessing extends DepthsAbility {

	public static final String ABILITY_NAME = "Radiant Blessing";
	private static final int HEALING_RADIUS = 18;
	private static final int COOLDOWN = 22 * 20;
	private static final double[] PERCENT_DAMAGE = {0.12, 0.15, 0.18, 0.21, 0.24, 0.3};
	private static final int DURATION = 10 * 20;
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "RadiantBlessingPercentDamageReceivedEffect";
	private static final double PERCENT_DAMAGE_RECEIVED = -0.2;

	public static final DepthsAbilityInfo<RadiantBlessing> INFO =
		new DepthsAbilityInfo<>(RadiantBlessing.class, ABILITY_NAME, RadiantBlessing::new, DepthsTree.DAWNBRINGER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.RADIANT_BLESSING)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", RadiantBlessing::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE), HOLDING_WEAPON_RESTRICTION))
			.displayItem(Material.SUNFLOWER)
			.descriptions(RadiantBlessing::getDescription);

	public RadiantBlessing(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		Location userLoc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		for (Player p : PlayerUtils.playersInRange(userLoc, HEALING_RADIUS, true)) {
			Location loc = p.getLocation();
			mPlugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(DURATION, PERCENT_DAMAGE_RECEIVED));
			mPlugin.mEffectManager.addEffect(p, ABILITY_NAME, new PercentDamageDealt(DURATION, PERCENT_DAMAGE[mRarity - 1]));
			new PartialParticle(Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
		}

		world.playSound(userLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
		world.playSound(userLoc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.0f);

		putOnCooldown();
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Left click while sneaking and holding a weapon to enchant players within " + HEALING_RADIUS + " blocks, including yourself, with " + StringUtils.multiplierToPercentage(-PERCENT_DAMAGE_RECEIVED) + "% resistance and ")
			.append(Component.text(StringUtils.multiplierToPercentage(PERCENT_DAMAGE[rarity - 1]) + "%", color))
			.append(Component.text(" increased damage for " + DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s."));
	}

}

