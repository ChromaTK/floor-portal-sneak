package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class EarthenCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Earthen Combos";
	public static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "EarthenCombosPercentDamageReceivedEffect";
	private static final double[] PERCENT_DAMAGE_RECEIVED = {-.08, -.10, -.12, -.14, -.16, -.20};
	private static final int DURATION = 20 * 4;
	private static final int ROOT_DURATION = 25;

	public static final DepthsAbilityInfo<EarthenCombos> INFO =
		new DepthsAbilityInfo<>(EarthenCombos.class, ABILITY_NAME, EarthenCombos::new, DepthsTree.EARTHBOUND, DepthsTrigger.COMBO)
			.displayItem(Material.WOODEN_SWORD)
			.descriptions(EarthenCombos::getDescription);

	private int mComboCount = 0;

	public EarthenCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;

			if (mComboCount >= 3 && mRarity > 0) {
				mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(DURATION, PERCENT_DAMAGE_RECEIVED[mRarity - 1]));
				mComboCount = 0;
				EntityUtils.applySlow(mPlugin, ROOT_DURATION, .99, enemy);

				Location loc = mPlayer.getLocation().add(0, 1, 0);
				World world = mPlayer.getWorld();
				Location entityLoc = enemy.getLocation();
				world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 0.8f, 0.65f);
				world.playSound(loc, Sound.BLOCK_NETHER_BRICKS_BREAK, SoundCategory.PLAYERS, 0.8f, 0.45f);
				new PartialParticle(Particle.CRIT_MAGIC, entityLoc.add(0, 1, 0), 10, 0.5, 0.2, 0.5, 0.65).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.BLOCK_DUST, loc.add(0, 1, 0), 15, 0.5, 0.3, 0.5, 0.5, Material.PODZOL.createBlockData()).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.BLOCK_DUST, loc.add(0, 1, 0), 15, 0.5, 0.3, 0.5, 0.5, Material.ANDESITE.createBlockData()).spawnAsPlayerActive(mPlayer);
			}
			return true;
		}
		return false;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Every third melee attack gives you ")
			.append(Component.text(StringUtils.multiplierToPercentage(-PERCENT_DAMAGE_RECEIVED[rarity - 1]) + "%", color))
			.append(Component.text(" resistance for " + DURATION / 20 + " seconds and roots the enemy for " + ROOT_DURATION / 20.0 + " seconds."));
	}


}

