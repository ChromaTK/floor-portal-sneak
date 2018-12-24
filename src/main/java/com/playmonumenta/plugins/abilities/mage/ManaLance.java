package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class ManaLance extends Ability {

	private static final int MANA_LANCE_1_DAMAGE = 8;
	private static final int MANA_LANCE_2_DAMAGE = 10;
	private static final int MANA_LANCE_1_COOLDOWN = 5 * 20;
	private static final int MANA_LANCE_2_COOLDOWN = 3 * 20;
	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);
	private static final int MANA_LANCE_STAGGER_DURATION = (int)(0.95 * 20);

	public ManaLance(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.MANA_LANCE;
		mInfo.scoreboardId = "ManaLance";
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.cooldown = getAbilityScore() == 1 ? MANA_LANCE_1_COOLDOWN : MANA_LANCE_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean cast() {
		int manaLance = getAbilityScore();

		int extraDamage = manaLance == 1 ? MANA_LANCE_1_DAMAGE : MANA_LANCE_2_DAMAGE;

		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		loc.add(dir);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.125);

		for (int i = 0; i < 8; i++) {
			loc.add(dir);

			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05, 0.05, 0.05, 0.025);
			mWorld.spawnParticle(Particle.REDSTONE, loc, 18, 0.35, 0.35, 0.35, MANA_LANCE_COLOR);

			if (loc.getBlock().getType().isSolid()) {
				loc.subtract(dir.multiply(0.5));
				mWorld.spawnParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125);
				mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				break;
			}
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 0.5)) {
				Spellshock.spellDamageMob(mPlugin, mob, extraDamage, mPlayer, MagicType.ARCANE);
				mob.addPotionEffect(
				    new PotionEffect(PotionEffectType.SLOW, MANA_LANCE_STAGGER_DURATION, 10, true, false));
			}
		}
		PlayerUtils.callAbilityCastEvent(mPlayer, Spells.MANA_LANCE);
		putOnCooldown();
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return !mPlayer.isSneaking() && InventoryUtils.isWandItem(mainHand)
		       && mPlayer.getGameMode() != GameMode.SPECTATOR;
	}

}
