package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.utils.BossUtils;

public class SpellTsunamiCharge extends SpellBaseCharge {
	public SpellTsunamiCharge(Plugin plugin, LivingEntity boss, int range, float damage) {
		super(plugin, boss, range, 25,
		      // Warning sound/particles at boss location and slow boss
		      (LivingEntity player) -> {
		          boss.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0);
		          boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
		          boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);
		      },
		      // Warning particles
		      (Location loc) -> {
		          loc.getWorld().spawnParticle(Particle.DRIP_WATER, loc, 1, 1, 1, 1, 0);
		      },
		      // Charge attack sound/particles at boss location
		      (LivingEntity player) -> {
		          boss.getWorld().spawnParticle(Particle.CLOUD, boss.getLocation(), 100, 2, 2, 2, 0);
		          boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1f, 1.5f);
		      },
		      // Attack hit a player
		      (LivingEntity player) -> {
		          player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation(), 80, 1, 1, 1, 0);
		          BossUtils.bossDamage(boss, (LivingEntity) player, damage);
		      },
		      // Attack particles
		      (Location loc) -> {
		          loc.getWorld().spawnParticle(Particle.WATER_WAKE, loc, 1, 0.02, 0.02, 0.02, 0);
		      },
		      // Ending particles on boss
		      () -> {
		          boss.getWorld().spawnParticle(Particle.CLOUD, boss.getLocation(), 200, 2, 2, 2, 0);
		      });
	}
}
