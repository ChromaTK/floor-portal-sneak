package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

public final class KamikazeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_kamikaze";
	public static final int detectionRange = 30;

	public KamikazeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> new PartialParticle(Particle.SMOKE_NORMAL, boss.getLocation().clone().add(new Location(boss.getWorld(), 0, 1, 0)), 2, 0.5, 1, 0.5, 0).spawnAsEntityActive(boss))
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player) {
			Entity damager = event.getDamager();
			if (damager instanceof Damageable) {
				((Damageable) damager).setHealth(0);
				World world = damager.getWorld();
				world.playSound(damager.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.7f);
				new PartialParticle(Particle.EXPLOSION_LARGE, damager.getLocation(), 10, 0.5, 1, 0.5, 0.05).spawnAsEntityActive(mBoss);
			}
		}
	}

	// This exists because "bossDamagedEntity()" is bugged. Doesn't work with projectiles.
	@Override
	public void bossProjectileHit(ProjectileHitEvent event) {
		if (event.getHitEntity() instanceof Player) {
			ProjectileSource shooter = event.getEntity().getShooter();
			if (shooter instanceof Damageable entity) {
				entity.setHealth(0);
				World world = event.getEntity().getWorld();
				world.playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.7f);
				new PartialParticle(Particle.EXPLOSION_LARGE, entity.getLocation(), 10, 0.5, 1, 0.5, 0.05).spawnAsEntityActive(mBoss);
			}
		}
	}
}
