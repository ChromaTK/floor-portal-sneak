package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HowlingWinds extends DepthsAbility {

	public static final String ABILITY_NAME = "Howling Winds";
	public static final int COOLDOWN = 25 * 20;
	public static final int DAMAGE_RADIUS = 4;
	public static final int PULL_RADIUS = 16;
	public static final int DISTANCE = 6;
	public static final int[] PULL_INTERVAL = {20, 18, 16, 14, 12, 8};
	public static final int DURATION_TICKS = 6 * 20;
	public static final double PULL_VELOCITY = 0.6;
	public static final double BASE_RATIO = 0.15;

	public static final DepthsAbilityInfo<HowlingWinds> INFO =
		new DepthsAbilityInfo<>(HowlingWinds.class, ABILITY_NAME, HowlingWinds::new, DepthsTree.WINDWALKER, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.HOWLINGWINDS)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HowlingWinds::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.HOPPER))
			.descriptions(HowlingWinds::getDescription);

	public HowlingWinds(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 0.8f, 0.25f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1.0f, 1.2f);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 15, 0.25f, 0.1f, 0.25f).spawnAsPlayerActive(mPlayer);
		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < DISTANCE; i++) {
			loc.add(dir);

			new PartialParticle(Particle.FIREWORKS_SPARK, loc, 5, 0.1, 0.1, 0.1, 0.1).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CLOUD, loc, 5, 0.1, 0.1, 0.1, 0.1).spawnAsPlayerActive(mPlayer);
			int size = EntityUtils.getNearbyMobs(loc, 2, mPlayer).size();
			if (loc.getBlock().getType().isSolid() || i >= DISTANCE - 1 || size > 0) {
				explode(loc);
				break;
			}
		}
	}

	private void explode(Location loc) {
		World world = mPlayer.getWorld();
		new PartialParticle(Particle.CLOUD, loc, 35, 4, 4, 4, 0.125).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 25, 2, 2, 2, 0.125).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8f, 1f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mTicks % PULL_INTERVAL[mRarity - 1] == 0) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, PULL_RADIUS)) {
						if (!EntityUtils.isCCImmuneMob(mob)) {
							Vector vector = mob.getLocation().toVector().subtract(loc.toVector());
							double ratio = BASE_RATIO + vector.length() / PULL_RADIUS;
							mob.setVelocity(mob.getVelocity().add(vector.normalize().multiply(PULL_VELOCITY).multiply(-ratio).add(new Vector(0, 0.1 + 0.2 * ratio, 0))));
						}
					}
					if (mTicks <= DURATION_TICKS - 5 * 20) {
						world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.8f, 1);
					}
				}
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 6, 2, 2, 2, 0.1).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15).spawnAsPlayerActive(mPlayer);
				if (mTicks >= DURATION_TICKS) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		Component s = PULL_INTERVAL[rarity - 1] == 20 ? Component.empty() : Component.text("s");
		return Component.text("Swap hands to summon a hurricane that lasts " + DURATION_TICKS / 20 + " seconds at the location you are looking at, up to " + DISTANCE + " blocks away. The hurricane pulls enemies within " + PULL_RADIUS + " blocks towards its center every ")
			.append(Component.text(StringUtils.to2DP(PULL_INTERVAL[rarity - 1] / 20.0), color))
			.append(Component.text(" second"))
			.append(s)
			.append(Component.text(". Cooldown: " + COOLDOWN / 20 + "s."));
	}


}

