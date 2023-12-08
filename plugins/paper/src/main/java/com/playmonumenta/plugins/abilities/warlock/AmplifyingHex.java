package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AmplifyingHexCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class AmplifyingHex extends Ability {

	private static final float FLAT_DAMAGE = 2f;
	private static final float DAMAGE_PER_SKILL_POINT = 0.5f;
	private static final int AMPLIFIER_DAMAGE_1 = 1;
	private static final int AMPLIFIER_DAMAGE_2 = 2;
	private static final int AMPLIFIER_CAP_1 = 2;
	private static final int AMPLIFIER_CAP_2 = 3;
	private static final float R1_CAP = 3.5f;
	private static final float R2_CAP = 7f;
	private static final float R3_CAP = 10.5f;
	private static final int RADIUS_1 = 8;
	private static final int RADIUS_2 = 10;
	private static final double ANGLE = 70;
	private static final int COOLDOWN = 20 * 10;
	private static final float KNOCKBACK_SPEED = 0.12f;
	private static final double ENHANCEMENT_HEALTH_THRESHOLD = 0.8;
	private static final double ENHANCEMENT_DAMAGE_MOD = 1.25;

	public static final String CHARM_DAMAGE = "Amplifying Hex Damage";
	public static final String CHARM_RANGE = "Amplifying Hex Range";
	public static final String CHARM_COOLDOWN = "Amplifying Hex Cooldown";
	public static final String CHARM_CONE = "Amplifying Hex Cone";
	public static final String CHARM_POTENCY = "Amplifying Hex Damage per Effect Potency";
	public static final String CHARM_POTENCY_CAP = "Amplifying Hex Potency Cap";
	public static final String CHARM_ENHANCED = "Amplifying Hex Health Threshold";

	public static final AbilityInfo<AmplifyingHex> INFO =
		new AbilityInfo<>(AmplifyingHex.class, "Amplifying Hex", AmplifyingHex::new)
			.linkedSpell(ClassAbility.AMPLIFYING)
			.scoreboardId("AmplifyingHex")
			.shorthandName("AH")
			.descriptions(
				"Left-click while sneaking with a scythe to fire a magic cone up to 8 blocks in front of you, " +
					"dealing 2 + (0.5 * number of Skill Points, capped at the maximum available Skill Points for each Region) magic damage " +
					"to each enemy per debuff (potion effects like Weakness or Wither, as well as Fire and custom effects like Bleed) they have, " +
					"and an extra +1 damage per extra level of debuff, capped at 2 extra levels. 10% Slowness, Weaken, etc. count as one level. Cooldown: 10s.",
				"The range is increased to 10 blocks, extra damage increased to +2 per extra level, and the extra level cap is increased to 3 extra levels.",
				"For every 1% health you have above 80% of your max health, Amplifying Hex will deal 1.25% more damage to enemies and deal 1% max health damage to yourself.")
			.simpleDescription("Deal damage to mobs in front of you for each debuff they have.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", AmplifyingHex::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.DRAGON_BREATH);

	private final float mAmplifierDamage;
	private final int mAmplifierCap;
	private final float mRadius;
	private final float mRegionCap;
	private float mDamage = 0f;

	private final AmplifyingHexCS mCosmetic;

	public AmplifyingHex(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAmplifierDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_POTENCY, isLevelOne() ? AMPLIFIER_DAMAGE_1 : AMPLIFIER_DAMAGE_2);
		mAmplifierCap = (int) CharmManager.calculateFlatAndPercentValue(player, CHARM_POTENCY_CAP, isLevelOne() ? AMPLIFIER_CAP_1 : AMPLIFIER_CAP_2);
		mRadius = (float) CharmManager.getRadius(player, CHARM_RANGE, isLevelOne() ? RADIUS_1 : RADIUS_2);
		mRegionCap = ServerProperties.getAbilityEnhancementsEnabled(player) ? R3_CAP : ServerProperties.getClassSpecializationsEnabled(player) ? R2_CAP : R1_CAP;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AmplifyingHexCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			int charmPower = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.CHARM_POWER).orElse(0);
			charmPower = (charmPower > 0) ? (charmPower / 3) - 2 : 0;
			int totalLevel = AbilityUtils.getEffectiveTotalSkillPoints(player) +
				                 AbilityUtils.getEffectiveTotalSpecPoints(player) +
				                 ScoreboardUtils.getScoreboardValue(player, AbilityUtils.TOTAL_ENHANCE).orElse(0) +
				                 charmPower;
			mDamage = DAMAGE_PER_SKILL_POINT * totalLevel;
		});
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		double angle = Math.min(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE, ANGLE), 180);

		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadiusIncrement = 0.5;

			@Override
			public void run() {
				if (mRadiusIncrement == 0.5) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadiusIncrement += 1.25;
				double degree = 90 - angle;
				// particles about every 10 degrees
				int degreeSteps = ((int) (2 * angle)) / 10;
				double degreeStep = 2 * angle / degreeSteps;
				for (int step = 0; step < degreeSteps; step++, degree += degreeStep) {
					double radian1 = Math.toRadians(degree + mCosmetic.amplifyingAngle(degree, mRadiusIncrement));
					vec = new Vector(FastUtils.cos(radian1) * mRadiusIncrement,
						0.15 + mCosmetic.amplifyingHeight(mRadiusIncrement, mRadius + 1),
						FastUtils.sin(radian1) * mRadiusIncrement);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().clone().add(0, 0.15, 0).add(vec);
					mCosmetic.amplifyingParticle(mPlayer, l);
				}

				if (mRadiusIncrement >= mRadius) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);


		final Location soundLoc = mPlayer.getLocation();
		mCosmetic.amplifyingEffects(mPlayer, mPlayer.getWorld(), soundLoc);

		double maxHealth = EntityUtils.getMaxHealth(mPlayer);
		double percentBoost = 0;
		if (isEnhanced() && mPlayer.getHealth() > maxHealth * (ENHANCEMENT_HEALTH_THRESHOLD + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCED))) {
			percentBoost = mPlayer.getHealth() / maxHealth - (ENHANCEMENT_HEALTH_THRESHOLD + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCED));
			double selfHarm = maxHealth * percentBoost;
			double absorp = mPlayer.getAbsorptionAmount();
			double newAbsorp = absorp - selfHarm;
			if (absorp > 0) {
				AbsorptionUtils.setAbsorption(mPlayer, (float) Math.max(newAbsorp, 0), -1);
			}
			if (newAbsorp < 0) {
				mPlayer.setHealth(maxHealth + newAbsorp);
			}
			//dummy damage
			DamageUtils.damage(null, mPlayer, new DamageEvent.Metadata(DamageType.OTHER, null, null, null), 0.001, true, false, false);

			//multiply percent boost modifier
			percentBoost *= ENHANCEMENT_DAMAGE_MOD;
		}

		Hitbox hitbox = Hitbox.approximateCylinderSegment(LocationUtils.getHalfHeightLocation(mPlayer).add(0, -mRadius, 0), 2 * mRadius, mRadius, Math.toRadians(angle));
		for (LivingEntity mob : hitbox.getHitMobs()) {
			int debuffCount = 0;
			int amplifierCount = 0;
			for (PotionEffectType effectType : AbilityUtils.DEBUFFS) {
				PotionEffect effect = mob.getPotionEffect(effectType);
				if (effect != null) {
					debuffCount++;
					amplifierCount += Math.min(mAmplifierCap, effect.getAmplifier());
				}
			}

			int inferno = Inferno.getInfernoLevel(mPlugin, mob);
			if (mob.getFireTicks() > 0 || inferno > 0) {
				debuffCount++;
				amplifierCount += Math.min(mAmplifierCap, inferno);
			}

			if (EntityUtils.isStunned(mob)) {
				debuffCount++;
			}

			if (EntityUtils.isParalyzed(mPlugin, mob)) {
				debuffCount++;
			}

			if (EntityUtils.isSilenced(mob)) {
				debuffCount++;
			}

			if (EntityUtils.isBleeding(mPlugin, mob)) {
				debuffCount++;
				amplifierCount += Math.min(mAmplifierCap, EntityUtils.getBleedLevel(mPlugin, mob) - 1);
			}

			//Custom slow effect interaction
			if (EntityUtils.isSlowed(mPlugin, mob) && mob.getPotionEffect(PotionEffectType.SLOW) == null) {
				debuffCount++;
				double slowAmp = EntityUtils.getSlowAmount(mPlugin, mob);
				int slowLevel = (int) Math.floor(slowAmp * 10);
				amplifierCount += Math.min((int) mAmplifierCap, Math.max(slowLevel - 1, 0));
			}

			//Custom weaken interaction
			if (EntityUtils.isWeakened(mPlugin, mob)) {
				debuffCount++;
				double weakAmp = EntityUtils.getWeakenAmount(mPlugin, mob);
				int weakLevel = (int) Math.floor(weakAmp * 10);
				amplifierCount += Math.min(mAmplifierCap, Math.max(weakLevel - 1, 0));
			}

			//Custom vuln interaction
			if (EntityUtils.isVulnerable(mPlugin, mob)) {
				debuffCount++;
				double vulnAmp = EntityUtils.getVulnAmount(mPlugin, mob);
				amplifierCount += Math.min(mAmplifierCap, Math.max((int) Math.floor(vulnAmp * 10) - 1, 0));
			}

			//Custom DoT interaction
			if (EntityUtils.hasDamageOverTime(mPlugin, mob)) {
				debuffCount++;
				int dotLevel = (int) EntityUtils.getHighestDamageOverTime(mPlugin, mob);
				amplifierCount += Math.min(mAmplifierCap, dotLevel - 1);
			}

			if (debuffCount > 0) {
				double finalDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, debuffCount * (FLAT_DAMAGE + Math.min(mDamage, mRegionCap)) + amplifierCount * mAmplifierDamage);
				finalDamage *= (1 + percentBoost);
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, finalDamage, mInfo.getLinkedSpell(), true);
				MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, true);
			}
		}
		putOnCooldown();
	}

}
