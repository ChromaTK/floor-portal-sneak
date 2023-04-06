package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.SoulRendCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.NavigableSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;



public class SoulRend extends Ability {

	private static final double PERCENT_HEAL = 0.2;
	public static final int HEAL_1 = 2;
	public static final int HEAL_2 = 4;
	private static final int RADIUS = 7;
	private static final int COOLDOWN = 20 * 8;
	private static final int HEAL_CAP = 10;
	private static final int ABSORPTION_CAP = 4;
	private static final int ABSORPTION_DURATION = 50;

	public static final String CHARM_RADIUS = "Soul Rend Radius";
	public static final String CHARM_HEAL = "Soul Rend Healing";
	public static final String CHARM_ALLY = "Soul Rend Ally Heal";
	public static final String CHARM_COOLDOWN = "Soul Rend Cooldown";
	public static final String CHARM_CAP = "Soul Rend Heal Cap";

	public static final AbilityInfo<SoulRend> INFO =
		new AbilityInfo<>(SoulRend.class, "Soul Rend", SoulRend::new)
			.linkedSpell(ClassAbility.SOUL_REND)
			.scoreboardId("SoulRend")
			.shorthandName("SR")
			.descriptions(
				"Attacking an enemy with a critical scythe attack heals you for 2 health and 20% of the melee damage dealt, capped at 10 total health. Cooldown: 8s.",
				"Players within 7 blocks of you are now also healed. Flat healing is increased from 2 to 4 health.",
				"Healing above max health, as well as any healing from this skill that remains negated by Dark Pact, is converted into Absorption, up to 4 absorption health, for 2.5s.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(new ItemStack(Material.POTION, 1));

	private final double mHeal;

	private @Nullable DarkPact mDarkPact;

	private final SoulRendCS mCosmetic;

	public SoulRend(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHeal = CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL, isLevelOne() ? HEAL_1 : HEAL_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SoulRendCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mDarkPact = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, DarkPact.class);
		});
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!isOnCooldown()
			    && event.getType() == DamageType.MELEE
			    && PlayerUtils.isFallingAttack(mPlayer)
			    && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			double heal = mHeal + event.getDamage() * PERCENT_HEAL;
			heal = Math.min(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CAP, HEAL_CAP), heal);

			Location loc = enemy.getLocation();
			World world = mPlayer.getWorld();
			mCosmetic.rendHitSound(world, loc);

			if (isLevelOne()) {
				mCosmetic.rendHitParticle1(mPlayer, loc);
			} else {
				mCosmetic.rendHitParticle2(mPlayer, loc, RADIUS);
			}

			NavigableSet<Effect> darkPactEffects = mPlugin.mEffectManager.getEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
			if (darkPactEffects != null) {
				if (mDarkPact != null && mDarkPact.isLevelTwo()) {
					int currPactDuration = darkPactEffects.last().getDuration();
					mPlugin.mEffectManager.clearEffects(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME);
					mCosmetic.rendHealEffect(mPlayer, mPlayer, enemy);
					double healed = PlayerUtils.healPlayer(mPlugin, mPlayer, mHeal);
					mPlugin.mEffectManager.addEffect(mPlayer, DarkPact.PERCENT_HEAL_EFFECT_NAME, new PercentHeal(currPactDuration, -1));

					if (isEnhanced()) {
						// All healing, minus the 2/4 healed through dark pact, converted to absorption
						double absorption = heal - healed;
						absorptionPlayer(mPlayer, absorption, enemy);
					}
				} else if (isEnhanced()) {
					// All healing converted to absorption
					absorptionPlayer(mPlayer, heal, enemy);
				}
			} else {
				healPlayer(mPlayer, heal, enemy);
			}

			if (isLevelTwo()) {
				double allyHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ALLY, heal);
				for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS), true)) {
					healPlayer(p, allyHeal, enemy);
				}
			}

			putOnCooldown();
		}
		return false;
	}

	private void healPlayer(Player player, double heal, LivingEntity enemy) {
		mCosmetic.rendHealEffect(mPlayer, player, enemy);
		double healed = PlayerUtils.healPlayer(mPlugin, player, heal, mPlayer);
		if (isEnhanced()) {
			absorptionPlayer(player, heal - healed, enemy);
		}
	}

	//Handles capping the absorption
	private void absorptionPlayer(Player player, double absorption, LivingEntity enemy) {
		if (absorption <= 0) {
			return;
		}
		absorption = Math.min(ABSORPTION_CAP, absorption);
		AbsorptionUtils.addAbsorption(player, absorption, absorption, ABSORPTION_DURATION);
		mCosmetic.rendAbsorptionEffect(mPlayer, player, enemy);
	}

}
