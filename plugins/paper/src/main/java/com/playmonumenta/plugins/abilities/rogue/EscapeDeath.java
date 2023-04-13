package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class EscapeDeath extends Ability {

	private static final double TRIGGER_THRESHOLD_HEALTH = 10;
	private static final int RANGE = 5;
	private static final int STUN_DURATION = 20 * 3;
	private static final int BUFF_DURATION = 20 * 8;
	private static final int ABSORPTION_HEALTH = 4;
	private static final double SPEED_PERCENT = 0.3;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EscapeDeathPercentSpeedEffect";
	private static final int JUMP_BOOST_AMPLIFIER = 2;
	private static final int COOLDOWN = 60 * 20;
	private static final int ENHANCEMENT_DURATION = 4 * 20;
	private static final double ENHANCEMENT_HEAL_PERCENT = 0.05;
	private static final String ESCAPE_DEATH_ENHANCEMENT_REGEN = "EscapeDeathEnhancementRegenEffect";

	private static final String DISABLE_JUMP_BOOST_TAG = "EscapeDeathNoJumpBoost";

	public static final String CHARM_ABSORPTION = "Escape Death Absorption Health";
	public static final String CHARM_JUMP = "Escape Death Jump Boost Amplifier";
	public static final String CHARM_SPEED = "Escape Death Speed Amplifier";
	public static final String CHARM_COOLDOWN = "Escape Death Cooldown";
	public static final String CHARM_STUN_DURATION = "Escape Death Stun Duration";

	public static final AbilityInfo<EscapeDeath> INFO =
		new AbilityInfo<>(EscapeDeath.class, "Escape Death", EscapeDeath::new)
			.linkedSpell(ClassAbility.ESCAPE_DEATH)
			.scoreboardId("EscapeDeath")
			.shorthandName("ED")
			.descriptions(
				String.format("When taking damage leaves you below %s hearts, throw a paralyzing grenade that stuns all enemies within %s blocks for %s seconds. Cooldown: %ss.",
					(int) TRIGGER_THRESHOLD_HEALTH / 2,
					RANGE,
					STUN_DURATION / 20,
					COOLDOWN / 20),
				String.format("When this skill is triggered, also gain %s Absorption hearts for %s seconds, %s%% Speed, and Jump Boost %s. If damage taken would kill you but could have been prevented by this skill it will instead do so.",
					ABSORPTION_HEALTH / 2,
					BUFF_DURATION / 20,
					(int) (SPEED_PERCENT * 100),
					StringUtils.toRoman(JUMP_BOOST_AMPLIFIER + 1)),
				String.format("When this skill is triggered, gain a regenerating effect that heals you for %s%% hp every second for %ss. The effect is canceled if you take damage from an enemy.",
					(int) (ENHANCEMENT_HEAL_PERCENT * 100),
					ENHANCEMENT_DURATION / 20))
			.simpleDescription("When health drops below a threshold, stun nearby mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("toggleJumpBoost", "toggle jump boost", EscapeDeath::toggleJumpBoost,
				new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true).lookDirections(AbilityTrigger.LookDirection.UP).enabled(false), AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.DRAGON_BREATH)
			.priorityAmount(10000);

	public EscapeDeath(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private void toggleJumpBoost() {
		if (ScoreboardUtils.toggleTag(mPlayer, DISABLE_JUMP_BOOST_TAG)) {
			mPlayer.sendActionBar(Component.text("Escape Death's Jump Boost has been disabled"));
		} else {
			mPlayer.sendActionBar(Component.text("Escape Death's Jump Boost has been enabled"));
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (mPlugin.mEffectManager.hasEffect(mPlayer, ESCAPE_DEATH_ENHANCEMENT_REGEN)
			    && !event.isBlocked()
			    && event.getSource() != null
			    && EntityUtils.isHostileMob(event.getSource())
			    && event.getType() != DamageEvent.DamageType.TRUE) {
			mPlugin.mEffectManager.clearEffects(mPlayer, ESCAPE_DEATH_ENHANCEMENT_REGEN);
		}

		double absorptionHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, ABSORPTION_HEALTH);
		if (!event.isBlocked() && !isOnCooldown()) {
			double newHealth = mPlayer.getHealth() - event.getFinalDamage(true);
			boolean dealDamageLater = newHealth < 0 && newHealth > -absorptionHealth && isLevelTwo();
			if (newHealth <= TRIGGER_THRESHOLD_HEALTH && (newHealth > 0 || dealDamageLater)) {
				mPlugin.mEffectManager.damageEvent(event);
				event.setLifelineCancel(true);
				if (event.isCancelled() || event.isBlocked()) {
					return;
				}

				if (dealDamageLater) {
					event.setCancelled(true);
				}

				putOnCooldown();

				int stunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, STUN_DURATION);
				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RANGE, mPlayer)) {
					EntityUtils.applyStun(mPlugin, stunDuration, mob);
				}

				if (isLevelTwo()) {
					AbsorptionUtils.addAbsorption(mPlayer, absorptionHealth, absorptionHealth, BUFF_DURATION);
					mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(BUFF_DURATION, SPEED_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED), PERCENT_SPEED_EFFECT_NAME));
					if (!mPlayer.getScoreboardTags().contains(DISABLE_JUMP_BOOST_TAG)) {
						mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
							new PotionEffect(PotionEffectType.JUMP, BUFF_DURATION, JUMP_BOOST_AMPLIFIER + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP), true, true));
					}
				}

				if (isEnhanced()) {
					// This check ensures that no "dupe" regeneration runnables occurs.
					// If escape death somehow "double procs", simply reset the ticks variable.
					mPlugin.mEffectManager.addEffect(mPlayer, ESCAPE_DEATH_ENHANCEMENT_REGEN, new CustomRegeneration(ENHANCEMENT_DURATION, ENHANCEMENT_HEAL_PERCENT * EntityUtils.getMaxHealth(mPlayer), mPlugin));
				}

				Location loc = mPlayer.getLocation();
				loc.add(0, 1, 0);

				World world = mPlayer.getWorld();
				new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 80, 0, 0, 0, 0.25).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 125, 0, 0, 0, 0.3).spawnAsPlayerActive(mPlayer);

				world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.75f, 1.5f);
				world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, 0f);

				MessagingUtils.sendActionBarMessage(mPlayer, "Escape Death has been activated");

				if (dealDamageLater) {
					mPlayer.setHealth(1);
					AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) newHealth);
				}
			}
		}
	}

	// this should not happen, but better play it safe
	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}
}
