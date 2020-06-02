package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.DeathsTouch;
import com.playmonumenta.plugins.abilities.warlock.reaper.HungeringVortex;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.FractalEnervation;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Level 1: When you hit an enemy with another skill they gain 15% vulnerability
 * for 5 seconds. Level 2: gain +1 armor and +.5 armor toughness for every ability they have on
 * cooldown lasting until skills come off cooldown.
 */

public class BlasphemousAura extends Ability {

	private static final int BLASPHEMY_VULN_LEVEL = 2;
	private static final int BLASPHEMY_VULN_DURATION = 5 * 20;
	private static final double BLASPHEMY_ARMOR_INCREMENT = 1;
	private static final double BLASPHEMY_TOUGHNESS_INCREMENT = 0.5;

	private int oldBonus = 0;

	public BlasphemousAura(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Blasphemous Aura");
		mInfo.scoreboardId = "BlasphemousAura";
		mInfo.mShorthandName = "BA";
		mInfo.mDescriptions.add("When you hit an enemy with a skill other than Blasphemous Aura they gain 15% vulnerability for 5 seconds.");
		mInfo.mDescriptions.add("The warlock gains +1 armor and +.5 armor toughness for every ability they have on cooldown lasting until skills come off cooldown.");
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		PotionUtils.applyPotion(mPlayer, event.getDamaged(), new PotionEffect(PotionEffectType.UNLUCK, BLASPHEMY_VULN_DURATION, BLASPHEMY_VULN_LEVEL, false, true));
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (getAbilityScore() > 1) {
			GraspingClaws gc = AbilityManager.getManager().getPlayerAbility(mPlayer, GraspingClaws.class);
			DarkPact dp = AbilityManager.getManager().getPlayerAbility(mPlayer, DarkPact.class);
			Ability[] abilities = new Ability[8];
			abilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, AmplifyingHex.class);
			abilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, ConsumingFlames.class);
			abilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, SoulRend.class);
			abilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, Exorcism.class);
			abilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, HungeringVortex.class);
			abilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, DeathsTouch.class);
			abilities[6] = AbilityManager.getManager().getPlayerAbility(mPlayer, FractalEnervation.class);
			abilities[7] = AbilityManager.getManager().getPlayerAbility(mPlayer, WitheringGaze.class);

			int bonus = 0;
			// Dark Pact will always give armor bonus to promote reaper being tanklock spec
			if (dp != null) {
				bonus++;
			}
			if (gc != null && gc.onCooldown()) {
				bonus++;
			}
			for (int i = 0; i < abilities.length; i++) {
				if (abilities[i] != null && abilities[i].isOnCooldown()) {
					bonus++;
				}
			}

			AttributeInstance armor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
			armor.setBaseValue(armor.getBaseValue() + (bonus - oldBonus) * BLASPHEMY_ARMOR_INCREMENT);
			AttributeInstance toughness = mPlayer.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);
			toughness.setBaseValue(toughness.getBaseValue() + (bonus - oldBonus) * BLASPHEMY_TOUGHNESS_INCREMENT);

			oldBonus = bonus;
		}
	}
}
