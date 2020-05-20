package com.playmonumenta.plugins.abilities.scout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

public class FinishingBlow extends Ability {

	private static final Particle.DustOptions FINISHING_BLOW_COLOR = new Particle.DustOptions(Color.fromRGB(168, 0, 0), 1.0f);
	private static final int FINISHING_BLOW_DURATION = 20 * 5;
	private static final int FINISHING_BLOW_1_DAMAGE = 3;
	private static final int FINISHING_BLOW_2_DAMAGE = 6;
	private static final int FINISHING_BLOW_DAMAGE_MULTIPLIER = 2;
	private static final double FINISHING_BLOW_THRESHOLD = 0.5;

	public static class Counter {
		LivingEntity mMob;
		int mTicksLeft = FINISHING_BLOW_DURATION;

		Counter(LivingEntity mob) {
			mMob = mob;
		}
	}

	private Map<UUID, Counter> mMarkedMobs = new HashMap<UUID, Counter>();
	private int mDamageBonus;

	public FinishingBlow(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Finishing Blow");
		mInfo.scoreboardId = "FinishingBlow";
		mInfo.mDescriptions.add("Enemies hit by your fully-charged arrows are marked for 5 seconds. Striking a marked enemy with a melee attack detonates the mark to deal them 3 bonus damage, doubled against enemies below 50% health.");
		mInfo.mDescriptions.add("Base damage increased to 6.");
		mDamageBonus = getAbilityScore() == 1 ? FINISHING_BLOW_1_DAMAGE : FINISHING_BLOW_2_DAMAGE;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			Iterator<Map.Entry<UUID, Counter>> iter = mMarkedMobs.entrySet().iterator();
			while (iter.hasNext()) {
				Counter counter = iter.next().getValue();
				counter.mTicksLeft -= 5;

				Location loc = counter.mMob.getLocation().add(0, 1, 0);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.5, 0.25, 0.02);
				mWorld.spawnParticle(Particle.CRIT_MAGIC, loc, 1, 0.25, 0.5, 0.25, 0);

				if (counter.mTicksLeft <= 0 || counter.mMob.isDead() || !counter.mMob.isValid()) {
					iter.remove();
				}
			}
		}
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (arrow.isCritical() && damagee instanceof LivingEntity) {
			mMarkedMobs.put(damagee.getUniqueId(), new Counter(damagee));
		}
		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && mMarkedMobs.containsKey(event.getEntity().getUniqueId())) {
			LivingEntity mob = (LivingEntity) event.getEntity();
			mMarkedMobs.remove(mob.getUniqueId());

			Location loc = mob.getLocation().add(0, 1, 0);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.25f);
			mWorld.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, 1f, 1.75f);
			mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, 1f, 1.25f);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.8f, 0.65f);
			mWorld.spawnParticle(Particle.CRIT, loc, 10, 0.25, 0.5, 0.25, 0.4);
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 20, 0.25, 0.5, 0.25, 0.1);
			mWorld.spawnParticle(Particle.BLOCK_CRACK, loc, 20, 0.25, 0.5, 0.25, 0.4, Bukkit.createBlockData("redstone_wire[power=8]"));

			if (mob.getHealth() / mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() <= FINISHING_BLOW_THRESHOLD) {
				mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8f, 1.75f);
				mWorld.spawnParticle(Particle.CRIT, loc, 15, 0.25, 0.5, 0.25, 0.4);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 25, 0.35, 0.5, 0.35, 1.2, FINISHING_BLOW_COLOR);

				event.setDamage(event.getDamage() + mDamageBonus * FINISHING_BLOW_DAMAGE_MULTIPLIER);
			} else {
				event.setDamage(event.getDamage() + mDamageBonus);
			}
		}

		return true;
	}
}
