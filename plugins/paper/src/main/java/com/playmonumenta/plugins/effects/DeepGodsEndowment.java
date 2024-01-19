package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.SoothingCombos;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenCombos;
import com.playmonumenta.plugins.depths.abilities.frostborn.FrigidCombos;
import com.playmonumenta.plugins.depths.abilities.shadow.DarkCombos;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class DeepGodsEndowment extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "DeepGodsEndowment";
	public static final String effectID = "DeepGodsEndowment";

	public static int HITS_NEEDED = 5;

	private int mCurrHits = 0;

	public DeepGodsEndowment(int duration) {
		super(duration, effectID);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (entity instanceof Player player && event.getType() == DamageType.MELEE && player.getCooledAttackStrength(0) == 1) {
			mCurrHits += 1;

			if (mCurrHits >= HITS_NEEDED) {
				mCurrHits = 0;
				int randInt = FastUtils.RANDOM.nextInt(0, 5);

				switch (randInt) {
					case 0 -> soothingCombo(player, enemy);
					case 1 -> earthenCombo(player, enemy);
					case 2 -> volcanicCombo(player, enemy);
					case 3 -> frigidCombo(player, enemy);
					case 4 -> darkCombo(player, enemy);
					default -> {
					}
				}
			}
		}
	}

	public void soothingCombo(Player player, LivingEntity enemy) {
		PotionEffect hasteEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 2, 0, false, true);

		List<Player> players = PlayerUtils.playersInRange(player.getLocation(), 12, true);

		for (Player p : players) {
			p.addPotionEffect(hasteEffect);
			Plugin.getInstance().mEffectManager.addEffect(p, SoothingCombos.SPEED_EFFECT_NAME, new PercentSpeed(20 * 2, 0.1, SoothingCombos.SPEED_EFFECT_NAME));
			new PartialParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsEntityActive(player);
			new PartialParticle(Particle.VILLAGER_HAPPY, p.getLocation().add(0, 1, 0), 5, 0.7, 0.7, 0.7, 0.001).spawnAsEntityActive(player);
			player.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.6f);
		}

		Location loc = player.getLocation().add(0, 1, 0);
		player.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.6f);
		new PartialParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsEntityActive(player);
	}

	public void earthenCombo(Player player, LivingEntity enemy) {
		Plugin.getInstance().mEffectManager.addEffect(player, EarthenCombos.PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(20 * 4, -.08));
		EntityUtils.applySlow(Plugin.getInstance(), 25, .99, enemy);

		Location loc = player.getLocation().add(0, 1, 0);
		World world = player.getWorld();
		Location entityLoc = enemy.getLocation();
		EarthenCombos.playSounds(world, loc);
		new PartialParticle(Particle.CRIT_MAGIC, entityLoc.add(0, 1, 0), 10, 0.5, 0.2, 0.5, 0.65);
		new PartialParticle(Particle.BLOCK_DUST, loc, 15, 0.5, 0.3, 0.5, 0.5, Material.PODZOL.createBlockData()).spawnAsEntityActive(player);
		new PartialParticle(Particle.BLOCK_DUST, loc, 15, 0.5, 0.3, 0.5, 0.5, Material.ANDESITE.createBlockData()).spawnAsEntityActive(player);
	}

	public void volcanicCombo(Player player, LivingEntity enemy) {
		Location location = enemy.getLocation();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(location, 4)) {
			EntityUtils.applyFire(Plugin.getInstance(), 60, mob, (Player) player);
			DamageUtils.damage(player, mob, DamageType.MAGIC, 6, null, true);
		}
		World world = player.getWorld();
		for (int i = 0; i < 360; i += 45) {
			double rad = Math.toRadians(i);
			Location locationDelta = new Location(world, 4 / 2 * FastUtils.cos(rad), 0.5, 4 / 2 * FastUtils.sin(rad));
			location.add(locationDelta);
			new PartialParticle(Particle.FLAME, location, 1).spawnAsEntityActive(player);
			location.subtract(locationDelta);
		}
		world.playSound(location, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.5f, 1);
	}

	public void frigidCombo(Player player, LivingEntity enemy) {
		Location targetLoc = enemy.getLocation();
		World world = targetLoc.getWorld();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, 4)) {
			if (!(mob.getHealth() <= 0)) {
				new PartialParticle(Particle.CRIT_MAGIC, mob.getLocation(), 25, .5, .2, .5, 0.65).spawnAsEntityActive(player);
				EntityUtils.applySlow(Plugin.getInstance(), 2 * 20, 0.2, mob);
				DamageUtils.damage(player, mob, DamageType.MAGIC, 2, null, true);
			}
		}

		Location playerLoc = player.getLocation().add(0, 1, 0);
		FrigidCombos.playSounds(world, playerLoc);
		new PartialParticle(Particle.SNOW_SHOVEL, targetLoc, 25, .5, .2, .5, 0.65).spawnAsEntityActive(player);
	}

	public void darkCombo(Player player, LivingEntity enemy) {
		EntityUtils.applyVulnerability(Plugin.getInstance(), 20 * 3, 0.15, enemy);

		DarkCombos.playSounds(player.getWorld(), player.getLocation());
		new PartialParticle(Particle.SPELL_WITCH, enemy.getLocation(), 15, 0.5, 0.2, 0.5, 0.65).spawnAsEntityActive(player);
		PotionUtils.applyPotion(player, enemy,
				new PotionEffect(PotionEffectType.GLOWING, 20 * 3, 0, true, false));
	}

	public static DeepGodsEndowment deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new DeepGodsEndowment(duration);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Deep God's Endowment";
	}

	@Override
	public String toString() {
		return String.format("DeepGodsEndowment duration:%d", this.getDuration());
	}
}
