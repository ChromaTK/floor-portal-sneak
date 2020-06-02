package com.playmonumenta.plugins.abilities.scout.ranger;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class PrecisionStrike extends Ability {

	private static final double PRECISION_STRIKE_Y_VELOCITY_MULTIPLIER = 0.5;
	private static final double PRECISION_STRIKE_Y_VELOCITY_BONUS = 0.4;
	private static final double PRECISION_STRIKE_ACTIVATION_RADIUS = 2;
	private static final int PRECISION_STRIKE_1_DAMAGE = 8;
	private static final int PRECISION_STRIKE_2_DAMAGE = 16;
	private static final double PRECISION_STRIKE_STUN_RADIUS = 3;
	private static final int PRECISION_STRIKE_STUN_DURATION = 1 * 20;
	private static final int PRECISION_STRIKE_VULNERABILITY_DURATION = 3 * 20;
	private static final int PRECISION_STRIKE_1_VULNERABILITY_LEVEL = 3;
	private static final int PRECISION_STRIKE_2_VULNERABILITY_LEVEL = 5;
	private static final int PRECISION_STRIKE_COOLDOWN = 6 * 20;

	private LivingEntity mHitMob;

	public PrecisionStrike(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Precision Strike");
		mInfo.linkedSpell = Spells.PRECISION_STRIKE;
		mInfo.scoreboardId = "PrecisionStrike";
		mInfo.mShorthandName = "PSt";
		mInfo.mDescriptions.add("Sprint right-clicking while not holding a bow, food, trident, potion, or block executes a fast dash that launches the user a short distance. The user stops at the first enemy hit dealing 6 damage and applying 20% Vulnerability for 3 seconds. The hit enemy is also stunned for 1 second. If the ground is hit before an enemy nothing happens. Cooldown: 6s.");
		mInfo.mDescriptions.add("Damage is increased to 10. Vulnerability is increased to 30%. All mobs in a 3 block radius from the enemy you hit are stunned for 1 second.");
		mInfo.cooldown = PRECISION_STRIKE_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if (!ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			// Checks for bows in offhand, and bows, pickaxes, potions, blocks, food, and tridents in mainhand
			return mPlayer.isSprinting() && !InventoryUtils.isBowItem(inMainHand) && !InventoryUtils.isBowItem(inOffHand) &&
			       !InventoryUtils.isPotionItem(inMainHand) && !inMainHand.getType().isBlock() &&
			       !inMainHand.getType().isEdible() && inMainHand.getType() != Material.TRIDENT;
		}
		return false;
	}

	@Override
	public void cast(Action action) {
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 63, 0.25, 0.1, 0.25, 0.2);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.1, 0.25, 0.125);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1.7f);
		int damage = getAbilityScore() == 1 ? PRECISION_STRIKE_1_DAMAGE : PRECISION_STRIKE_2_DAMAGE;
		int level = getAbilityScore() == 1 ? PRECISION_STRIKE_1_VULNERABILITY_LEVEL : PRECISION_STRIKE_2_VULNERABILITY_LEVEL;
		Vector velocity = mPlayer.getLocation().getDirection();
		velocity.setY(velocity.getY() * PRECISION_STRIKE_Y_VELOCITY_MULTIPLIER);
		velocity.add(new Vector(0, PRECISION_STRIKE_Y_VELOCITY_BONUS, 0));
		mPlayer.setVelocity(velocity);

		new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mPlayer.isOnGround() || mPlayer.getLocation().getBlock().getType() == Material.WATER
				    || mPlayer.getLocation().getBlock().getType() == Material.LAVA
				    || mTicks >= PRECISION_STRIKE_COOLDOWN || !mPlayer.isOnline()) {
					this.cancel();
					return;
				}

				mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 5, 0.25, 0.1, 0.25, 0.1);
				for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation().clone().add(mPlayer.getVelocity().normalize()), PRECISION_STRIKE_ACTIVATION_RADIUS)) {
					if (!le.isDead()) {
						EntityUtils.damageEntity(mPlugin, le, damage, mPlayer, null, true, mInfo.linkedSpell);
						mHitMob = le;
						PotionUtils.applyPotion(mPlayer, le, new PotionEffect(PotionEffectType.UNLUCK, PRECISION_STRIKE_VULNERABILITY_DURATION, level, false, true));
						if (!EntityUtils.isElite(le) && !EntityUtils.isBoss(le)) {
							EntityUtils.applyStun(mPlugin, PRECISION_STRIKE_STUN_DURATION, le);
						}
						mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 63, 0.25, 0.1, 0.25, 0.2);
						mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.1, 0.25, 0.125);
						mWorld.playSound(mPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, 2.0f, 0.5f);

						if (getAbilityScore() == 2) {
							for (LivingEntity e : EntityUtils.getNearbyMobs(mHitMob.getLocation(), PRECISION_STRIKE_STUN_RADIUS, mPlayer)) {
								EntityUtils.applyStun(mPlugin, PRECISION_STRIKE_STUN_DURATION, e);
							}
						}
						this.cancel();
						break;
					}
				}
			}
		}.runTaskTimer(mPlugin, 1, 1);
		putOnCooldown();
	}
}
