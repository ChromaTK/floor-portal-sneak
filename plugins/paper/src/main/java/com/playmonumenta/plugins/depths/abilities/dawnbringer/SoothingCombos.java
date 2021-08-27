package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SoothingCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Soothing Combos";
	public static final int TIME = 20;
	public static final double SPEED_PERCENT = 0.15;
	public static final String SPEED_EFFECT_NAME = "SoothingCombosPercentSpeedEffect";
	public static final double[] DURATION = {2.0, 2.5, 3.0, 3.5, 4.0}; //seconds
	public static final int RANGE = 12;

	private int mComboCount = 0;

	public SoothingCombos(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.HONEYCOMB;
		mTree = DepthsTree.SUNLIGHT;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;

			if (mComboCount >= 3 && mRarity > 0) {

				mComboCount = 0;

				PotionEffect hasteEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, (int) (20 * DURATION[mRarity - 1]), 0, false, true);
				List<Player> players = PlayerUtils.playersInRange(mPlayer.getLocation(), RANGE, true);

				for (Player p : players) {
					p.addPotionEffect(hasteEffect);
					mPlugin.mEffectManager.addEffect(p, SPEED_EFFECT_NAME, new PercentSpeed((int) (20 * DURATION[mRarity - 1]), SPEED_PERCENT, SPEED_EFFECT_NAME));
					mPlayer.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
					mPlayer.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, p.getLocation().add(0, 1, 0), 5, 0.7, 0.7, 0.7, 0.001);
					mPlayer.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.6f);
				}

				Location loc = mPlayer.getLocation().add(0, 1, 0);
				mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.6f);
				mPlayer.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
			}
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Every third melee attack applies " + DepthsUtils.roundPercent(SPEED_PERCENT) + "% speed and Haste I for " + DepthsUtils.getRarityColor(rarity) + DURATION[rarity - 1] + ChatColor.WHITE + " seconds to players within " + RANGE + " blocks, including the user.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SUNLIGHT;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.COMBO;
	}
}
