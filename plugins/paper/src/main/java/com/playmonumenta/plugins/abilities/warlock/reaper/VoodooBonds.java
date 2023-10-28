package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.VoodooBondsCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.VoodooBondsOtherPlayer;
import com.playmonumenta.plugins.effects.VoodooBondsReaper;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class VoodooBonds extends Ability {

	private static final int COOLDOWN_1 = 22 * 20;
	private static final int COOLDOWN_2 = 12 * 20;
	private static final int ACTIVE_RADIUS = 8;
	private static final int PASSIVE_RADIUS = 3;
	private static final double CLEAVE_DAMAGE = 0.15;
	private static final int DURATION_1 = 20 * 5;
	private static final int DURATION_2 = 20 * 7;
	public static final String EFFECT_NAME = "VoodooBondsEffect";

	public static final String CHARM_COOLDOWN = "Voodoo Bonds Cooldown";
	public static final String CHARM_TRANSFER_DAMAGE = "Voodoo Bonds Transfer Damage";
	public static final String CHARM_TRANSFER_TIME = "Voodoo Bonds Transfer Time Limit";
	public static final String CHARM_RECEIVED_DAMAGE = "Voodoo Bonds Received Damage";
	public static final String CHARM_DAMAGE = "Voodoo Bonds Damage";
	public static final String CHARM_RADIUS = "Voodoo Bonds Radius";

	public static final AbilityInfo<VoodooBonds> INFO =
		new AbilityInfo<>(VoodooBonds.class, "Voodoo Bonds", VoodooBonds::new)
			.linkedSpell(ClassAbility.VOODOO_BONDS)
			.scoreboardId("VoodooBonds")
			.shorthandName("VB")
			.descriptions(
				("Melee strikes to a mob apply %s%% of the damage to all mobs of the same type within %s blocks. " +
					"Additionally, right-click while sneaking and looking down to cast a protective spell on all players within an %s block radius. " +
					"The next hit every player (including the Reaper) takes has all damage ignored (or 50%% if attack is from a Boss), " +
					"but that damage will transfer to the Reaper in %ss unless it is passed on again. " +
					"Passing that damage requires a melee strike, in which %s%% of the initial damage blocked is added to the damage of the strike (Bosses are immune to this bonus). " +
					"The damage directed to the Reaper is calculated by the percentage of health the initial hit would have taken from that player, " +
					"and can never kill you, only leave you at 1 HP. Cooldown: %ss.")
					.formatted(
						StringUtils.multiplierToPercentage(CLEAVE_DAMAGE), PASSIVE_RADIUS, ACTIVE_RADIUS,
						StringUtils.ticksToSeconds(DURATION_1), StringUtils.multiplierToPercentage(VoodooBondsReaper.PERCENT_1), StringUtils.ticksToSeconds(COOLDOWN_1)
					),
				"The duration before damage transfer increases to %ss, the on-hit damage when passing a hit increases to %s%% of the blocked damage, and the cooldown is reduced to %ss."
					.formatted(StringUtils.ticksToSeconds(DURATION_2), StringUtils.multiplierToPercentage(VoodooBondsReaper.PERCENT_2), StringUtils.ticksToSeconds(COOLDOWN_2)))
			.simpleDescription("Passively, your attacks also hit nearby mobs of the same type. Activate to protect yourself and nearby players, blocking 1 hit and adding the blocked damage to your next melee strike.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", VoodooBonds::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).lookDirections(AbilityTrigger.LookDirection.DOWN),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.JACK_O_LANTERN);

	private final int mTransferDuration;
	private final VoodooBondsCS mCosmetic;

	public VoodooBonds(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mTransferDuration = CharmManager.getDuration(player, CHARM_TRANSFER_TIME, (isLevelOne() ? DURATION_1 : DURATION_2));
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new VoodooBondsCS());
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		putOnCooldown();
		final double maxRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ACTIVE_RADIUS);
		mCosmetic.bondsStartEffect(mPlayer.getWorld(), mPlayer, maxRadius);
		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, ACTIVE_RADIUS), true)) {
			//better effects
			mCosmetic.bondsApplyEffect(mPlayer, p);
			mPlugin.mEffectManager.addEffect(p, EFFECT_NAME,
				new VoodooBondsOtherPlayer(getModifiedCooldown(), mTransferDuration, mPlayer, mPlugin));
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			EntityType type = enemy.getType();
			Location eLoc = enemy.getLocation();
			double damage = event.getDamage();

			if (mPlugin.mEffectManager.hasEffect(mPlayer, PercentDamageDealt.class)) {
				for (Effect priorityEffects : mPlugin.mEffectManager.getPriorityEffects(mPlayer).values()) {
					if (priorityEffects instanceof PercentDamageDealt damageEffect) {
						EnumSet<DamageType> types = damageEffect.getAffectedDamageTypes();
						if (types == null || types.contains(DamageType.MELEE)) {
							damage = damage * (1 + damageEffect.getMagnitude() * (damageEffect.isBuff() ? 1 : -1));
						}
					}
				}
			}

			for (LivingEntity mob : EntityUtils.getNearbyMobs(eLoc, CharmManager.getRadius(mPlayer, CHARM_RADIUS, PASSIVE_RADIUS), mPlayer)) {
				if (mob.getType().equals(type) && mob != enemy) {
					Location mLoc = mob.getLocation();
					DamageUtils.damage(mPlayer, mob, DamageType.OTHER, damage * CLEAVE_DAMAGE, mInfo.getLinkedSpell(), true);
					mCosmetic.bondsSpreadParticle(mPlayer, mLoc, eLoc);
				}
			}
			return true;
		}
		return false;
	}
}
