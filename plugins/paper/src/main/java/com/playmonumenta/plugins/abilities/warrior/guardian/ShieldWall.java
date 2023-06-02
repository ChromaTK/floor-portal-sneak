package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.ShieldWallCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ShieldWall extends Ability {

	private static final int SHIELD_WALL_1_DURATION = 6 * 20;
	private static final int SHIELD_WALL_2_DURATION = 10 * 20;
	private static final int SHIELD_WALL_DAMAGE = 3;
	private static final int SHIELD_WALL_1_COOLDOWN = 20 * 30;
	private static final int SHIELD_WALL_2_COOLDOWN = 20 * 18;
	private static final int SHIELD_WALL_ANGLE = 180;
	private static final float SHIELD_WALL_KNOCKBACK = 0.3f;
	private static final double SHIELD_WALL_RADIUS = 4.0;
	private static final int SHIELD_WALL_HEIGHT = 5;

	public static final String CHARM_DURATION = "Shield Wall Duration";
	public static final String CHARM_DAMAGE = "Shield Wall Damage";
	public static final String CHARM_COOLDOWN = "Shield Wall Cooldown";
	public static final String CHARM_ANGLE = "Shield Wall Angle";
	public static final String CHARM_KNOCKBACK = "Shield Wall Knockback";
	public static final String CHARM_HEIGHT = "Shield Wall Height";

	public static final AbilityInfo<ShieldWall> INFO =
		new AbilityInfo<>(ShieldWall.class, "Shield Wall", ShieldWall::new)
			.linkedSpell(ClassAbility.SHIELD_WALL)
			.scoreboardId("ShieldWall")
			.shorthandName("SW")
			.descriptions(
				String.format("Press the swap key while holding a shield in either hand to create a %s degree arc of particles from 1 block below to %s blocks above the user's location and with a %s block radius in front of the user. " +
					"Enemies that pass through the wall are dealt %s melee damage and knocked back. The wall also blocks all enemy projectiles such as arrows or fireballs. The shield lasts %s seconds. Cooldown: %ss.",
					SHIELD_WALL_ANGLE,
					SHIELD_WALL_HEIGHT,
					(int) SHIELD_WALL_RADIUS,
					SHIELD_WALL_DAMAGE,
					StringUtils.ticksToSeconds(SHIELD_WALL_1_DURATION),
					StringUtils.ticksToSeconds(SHIELD_WALL_1_COOLDOWN)
				),
				String.format("The shield lasts %s seconds instead. Cooldown: %ss.",
					StringUtils.ticksToSeconds(SHIELD_WALL_2_DURATION),
					StringUtils.ticksToSeconds(SHIELD_WALL_2_COOLDOWN)
				)
			)
			.simpleDescription("Deploy a wall that can block projectiles and mobs from entering.")
			.cooldown(SHIELD_WALL_1_COOLDOWN, SHIELD_WALL_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ShieldWall::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP),
				new AbilityTriggerInfo.TriggerRestriction("holding a shield in either hand",
					player -> player.getInventory().getItemInMainHand().getType() == Material.SHIELD || player.getInventory().getItemInOffHand().getType() == Material.SHIELD)))
			.displayItem(Material.STONE_BRICK_WALL);

	private final int mDuration;
	private final int mHeight;
	private final ShieldWallCS mCosmetic;

	public ShieldWall(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, (isLevelOne() ? SHIELD_WALL_1_DURATION : SHIELD_WALL_2_DURATION));
		mHeight = SHIELD_WALL_HEIGHT + (int) CharmManager.getLevel(mPlayer, CHARM_HEIGHT);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ShieldWallCS());
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, SHIELD_WALL_KNOCKBACK);
		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, SHIELD_WALL_DAMAGE);
		double angle = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ANGLE, SHIELD_WALL_ANGLE);

		World world = mPlayer.getWorld();
		mCosmetic.shieldStartEffect(world, mPlayer, SHIELD_WALL_RADIUS);
		putOnCooldown();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		new BukkitRunnable() {
			int mT = 0;
			final Location mLoc = mPlayer.getLocation();
			final List<BoundingBox> mBoxes = new ArrayList<>();
			List<LivingEntity> mMobsAlreadyHit = new ArrayList<>();
			final List<LivingEntity> mMobsHitThisTick = new ArrayList<>();
			boolean mHitboxes = false;

			@Override
			public void run() {
				mT++;
				Vector vec;
				for (int y = -1; y < mHeight; y++) {
					for (double degree = 0; degree < angle; degree += 10) {
						double radian1 = Math.toRadians(degree - 0.5 * angle);
						vec = new Vector(-FastUtils.sin(radian1) * SHIELD_WALL_RADIUS, y, FastUtils.cos(radian1) * SHIELD_WALL_RADIUS);
						vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

						Location l = mLoc.clone().add(vec);
						if (mT % 4 == 0) {
							mCosmetic.shieldWallDot(mPlayer, l, degree, angle, y, mHeight);
						}
						if (!mHitboxes) {
							mBoxes.add(BoundingBox.of(l.clone().subtract(0.6, 0, 0.6),
								l.clone().add(0.6, 5, 0.6)));
						}
					}
					mHitboxes = true;
				}

				for (BoundingBox box : mBoxes) {
					for (Entity e : world.getNearbyEntities(box)) {
						Location eLoc = e.getLocation();
						if (e instanceof Projectile proj) {
							if (proj.getShooter() instanceof LivingEntity shooter && !(shooter instanceof Player)) {
								proj.remove();
								mCosmetic.shieldOnBlock(world, eLoc, mPlayer);
							}
						} else if (e instanceof LivingEntity le && EntityUtils.isHostileMob(e)) {
							boolean shouldKnockback = knockback > 0 && !EntityUtils.isCCImmuneMob(e);
							// Stores mobs hit this tick
							mMobsHitThisTick.add(le);
							// This list does not update to the mobs hit this tick until after everything runs
							if (!mMobsAlreadyHit.contains(le)) {
								mMobsAlreadyHit.add(le);

								DamageUtils.damage(mPlayer, le, new DamageEvent.Metadata(DamageType.MELEE_SKILL, mInfo.getLinkedSpell(), playerItemStats), damage, false, true, false);

								//Bosses should not be affected by slowness or knockback.
								if (shouldKnockback) {
									MovementUtils.knockAway(mLoc, le, knockback, true);
									mCosmetic.shieldOnHit(world, eLoc, mPlayer);
								}
							} else if (shouldKnockback && le.getNoDamageTicks() + 5 < le.getMaximumNoDamageTicks()) {
								/*
								 * This is a temporary fix while we decide how to handle KBR mobs
								 *
								 * If a mob collides with shield wall halfway through its invulnerability period, assume it
								 * resists knockback and give it Slowness V for 5 seconds to simulate the old effect of
								 * halting mobs with stunlock damage, minus the insane damage part.
								 *
								 * This effect is reapplied each tick, so the mob is slowed drastically until 2 seconds
								 * after they leave shield wall hitbox.
								 */
								PotionUtils.applyPotion(mPlayer, le, new PotionEffect(PotionEffectType.SLOW, 20 * 2, 4, true, false));
							}
						}
					}
				}
				/*
				 * Compare the two lists of mobs and only remove from the
				 * actual hit tracker if the mob isn't detected as hit this
				 * tick, meaning it is no longer in the shield wall hitbox
				 * and is thus eligible for another hit.
				 */
				List<LivingEntity> mobsAlreadyHitAdjusted = new ArrayList<>();
				for (LivingEntity mob : mMobsAlreadyHit) {
					if (mMobsHitThisTick.contains(mob)) {
						mobsAlreadyHitAdjusted.add(mob);
					}
				}
				mMobsAlreadyHit = mobsAlreadyHitAdjusted;
				mMobsHitThisTick.clear();
				if (mT >= mDuration) {
					this.cancel();
					mBoxes.clear();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

}
