package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.WardingRemedyCS;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WardingRemedy extends Ability {

	private static final int WARDING_REMEDY_1_COOLDOWN = 20 * 30;
	private static final int WARDING_REMEDY_2_COOLDOWN = 20 * 25;
	private static final int WARDING_REMEDY_PULSES = 8;
	private static final int WARDING_REMEDY_PULSE_DELAY = 10;
	private static final int WARDING_REMEDY_ABSORPTION = 1;
	private static final int WARDING_REMEDY_MAX_ABSORPTION = 6;
	private static final int WARDING_REMEDY_ABSORPTION_DURATION = 20 * 30;
	private static final int WARDING_REMEDY_RANGE = 12;
	private static final double WARDING_REMEDY_HEAL_MULTIPLIER = 0.1;
	private static final double WARDING_REMEDY_ACTIVE_RADIUS = 6;

	public static final String CHARM_COOLDOWN = "Warding Remedy Cooldown";
	public static final String CHARM_PULSES = "Warding Remedy Pulses";
	public static final String CHARM_DELAY = "Warding Remedy Pulse Delay";
	public static final String CHARM_ABSORPTION = "Warding Remedy Absorption Health";
	public static final String CHARM_MAX_ABSORPTION = "Warding Remedy Max Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Warding Remedy Absorption Duration";
	public static final String CHARM_RADIUS = "Warding Remedy Radius";
	public static final String CHARM_HEALING = "Warding Remedy Healing Bonus";

	public static final AbilityInfo<WardingRemedy> INFO =
		new AbilityInfo<>(WardingRemedy.class, "Warding Remedy", WardingRemedy::new)
			.linkedSpell(ClassAbility.WARDING_REMEDY)
			.scoreboardId("WardingRemedy")
			.shorthandName("WR")
			.descriptions(
				("Swap hands while sneaking to give players (including yourself) " +
				"within a %s block radius %s absorption health every %ss for %ss, lasting %ss, " +
				"up to %s absorption health. Cooldown: %ss.")
					.formatted(
							StringUtils.to2DP(WARDING_REMEDY_ACTIVE_RADIUS),
							WARDING_REMEDY_ABSORPTION,
							StringUtils.ticksToSeconds(WARDING_REMEDY_PULSE_DELAY),
							StringUtils.ticksToSeconds(WARDING_REMEDY_PULSES * WARDING_REMEDY_PULSE_DELAY),
							StringUtils.ticksToSeconds(WARDING_REMEDY_ABSORPTION_DURATION),
							WARDING_REMEDY_MAX_ABSORPTION,
							StringUtils.ticksToSeconds(WARDING_REMEDY_1_COOLDOWN)
					),
				("You and allies in a %s block radius passively gain %s%% increased healing while having " +
				"absorption health, and cooldown decreased to %ss.")
					.formatted(
							StringUtils.to2DP(WARDING_REMEDY_RANGE),
							StringUtils.multiplierToPercentage(WARDING_REMEDY_HEAL_MULTIPLIER),
							StringUtils.ticksToSeconds(WARDING_REMEDY_2_COOLDOWN)
					)
			)
			.simpleDescription("Periodically grant absorption to you and nearby allies, for a short period of time.")
			.cooldown(WARDING_REMEDY_1_COOLDOWN, WARDING_REMEDY_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WardingRemedy::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(Material.GOLDEN_CARROT);

	private final WardingRemedyCS mCosmetic;

	public WardingRemedy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new WardingRemedyCS());
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		int delay = CharmManager.getDuration(mPlayer, CHARM_DELAY, WARDING_REMEDY_PULSE_DELAY);
		int pulses = WARDING_REMEDY_PULSES + (int) CharmManager.getLevel(mPlayer, CHARM_PULSES);
		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, WARDING_REMEDY_ACTIVE_RADIUS);
		double absorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, WARDING_REMEDY_ABSORPTION);
		double maxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MAX_ABSORPTION, WARDING_REMEDY_MAX_ABSORPTION);
		int absorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, WARDING_REMEDY_ABSORPTION_DURATION);

		mCosmetic.remedyStartEffect(world, loc, mPlayer, radius);

		cancelOnDeath(new BukkitRunnable() {
			int mPulses = 0;
			int mTick = delay;

			@Override
			public void run() {
				Location playerLoc = mPlayer.getLocation();

				mCosmetic.remedyPeriodicEffect(playerLoc.clone().add(0, 0.5, 0), mPlayer, mTick + mPulses * delay);

				if (mTick >= delay) {
					mCosmetic.remedyPulseEffect(world, playerLoc, mPlayer, mPulses, pulses, radius);

					for (Player p : PlayerUtils.playersInRange(playerLoc, radius, true)) {
						AbsorptionUtils.addAbsorption(p, absorption, maxAbsorption, absorptionDuration);
						mCosmetic.remedyApplyEffect(mPlayer, p);
					}
					mTick = 0;
					mPulses++;
					if (mPulses >= pulses) {
						this.cancel();
					}
				}

				mTick++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		//Triggers four times a second

		if (isLevelOne()) {
			return;
		}

		double healing = WARDING_REMEDY_HEAL_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALING);
		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, WARDING_REMEDY_RANGE), true)) {
			if (AbsorptionUtils.getAbsorption(p) > 0) {
				mPlugin.mEffectManager.addEffect(p, "WardingRemedyBonusHealing", new PercentHeal(20, healing).displaysTime(false));
				mCosmetic.remedyHealBuffEffect(mPlayer, p);
			}
		}
	}

}
