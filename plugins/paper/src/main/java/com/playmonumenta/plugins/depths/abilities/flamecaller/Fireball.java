package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Fireball extends DepthsAbility {

	public static final String ABILITY_NAME = "Fireball";
	private static final int COOLDOWN = 6 * 20;
	private static final int DISTANCE = 10;
	private static final int[] DAMAGE = {8, 10, 12, 14, 16, 20};
	private static final int RADIUS = 3;
	private static final int FIRE_TICKS = 3 * 20;

	public static final String CHARM_COOLDOWN = "Fireball Cooldown";

	public static final DepthsAbilityInfo<Fireball> INFO =
		new DepthsAbilityInfo<>(Fireball.class, ABILITY_NAME, Fireball::new, DepthsTree.FLAMECALLER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.FIREBALL)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Fireball::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.FIREWORK_STAR)
			.descriptions(Fireball::getDescription);

	private final double mRadius;
	private final double mDistance;
	private final double mDamage;
	private final int mFireDuration;

	public Fireball(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.FIREBALL_RADIUS.mEffectName, RADIUS);
		mDistance = CharmManager.getRadius(mPlayer, CharmEffects.FIREBALL_RANGE.mEffectName, DISTANCE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FIREBALL_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mFireDuration = CharmManager.getDuration(mPlayer, CharmEffects.FIREBALL_FIRE_DURATION.mEffectName, FIRE_TICKS);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 2);
		new PartialParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);
		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < mDistance; i++) {
			loc.add(dir);

			if (loc.getBlock().getType().isSolid() || EntityUtils.getNearbyMobs(loc, 1).size() > 0) {
				explode(loc);

				return;
			}
		}

		explode(loc);
	}

	private void explode(Location loc) {
		World world = loc.getWorld();
		double mult = mRadius / RADIUS;
		new PartialParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, (int) (25 * mult), 1.5 * mult, 1.5 * mult, 1.5 * mult, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, loc, (int) (25 * mult), 1.5 * mult, 1.5 * mult, 1.5 * mult, 0).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 1);

		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, mRadius, mPlayer)) {
			EntityUtils.applyFire(mPlugin, mFireDuration, e, mPlayer);
			DamageUtils.damage(mPlayer, e, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell());
		}
	}

	private static Description<Fireball> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Fireball>(color)
			.add("Right click to summon a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius fireball at the location you are looking, up to ")
			.add(a -> a.mDistance, DISTANCE)
			.add(" blocks away. The fireball deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage and sets enemies ablaze for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}
}
