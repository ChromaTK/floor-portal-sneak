package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLeapAttack;
import com.playmonumenta.plugins.bosses.spells.SpellDuelist;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class WrathBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_wrath";

	public static class Parameters extends BossParameters {
		public int DETECTION = 32;

		public int COOLDOWN = 20 * 3;
		public int MIN_RANGE = 8;
		public int RUN_DISTANCE = 2;
		public int VELOCITY_MULTIPLIER = 1;
		public double DAMAGE_RADIUS = 2.5;
		public int DAMAGE = 18;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";
	}

	private final Parameters mParams;

	public WrathBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(new SpellBaseLeapAttack(plugin, boss, mParams.DETECTION,
			mParams.MIN_RANGE, mParams.RUN_DISTANCE, mParams.COOLDOWN, mParams.VELOCITY_MULTIPLIER,
			// Initiate Aesthetic
			(World world, Location loc) -> {
				new PartialParticle(Particle.VILLAGER_ANGRY, loc, 10, 0.5, 0.5, 0.5, 0).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ENTITY_VINDICATOR_HURT, SoundCategory.HOSTILE, 1f, 0.5f);
			},
			// Leap Aesthetic
			(World world, Location loc) -> {
				new PartialParticle(Particle.CLOUD, loc, 20, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 0.5f);
			},
			// Leaping Aesthetic
			(World world, Location loc) -> {
				new PartialParticle(Particle.FLAME, loc, 3, 0, 0, 0, 0.1).spawnAsEntityActive(boss);
				new PartialParticle(Particle.VILLAGER_ANGRY, loc, 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(boss);
			},
			// Hit Action
			(World world, @Nullable Player player, Location loc, Vector dir) -> {
				new BukkitRunnable() {
					final World mWorld = world;
					final Location mLocation = loc;
					final Vector mDirection = dir;
					int mTime = 0;

					@Override
					public void run() {
						if (EntityUtils.shouldCancelSpells(mBoss)) {
							this.cancel();
						}
						mTime++;
						if (mTime <= 5) {
							Location locParticle = mBoss.getLocation().add(0, 1.5, 0);
							Vector sideways = new Vector(mDirection.getZ(), 1, -mDirection.getX()).multiply(3);
							Vector forward = mDirection.clone().multiply(3);
							locParticle.subtract(sideways.clone().multiply(0.5));
							locParticle.subtract(forward.clone().multiply(0.5));
							locParticle.add(forward.clone().multiply(Math.sin(Math.PI / 10 * mTime)));
							locParticle.add(sideways.clone().multiply(Math.cos(Math.PI / 10 * mTime)));
							new PartialParticle(Particle.SWEEP_ATTACK, locParticle, 4, 0.5, 0.5, 0.5, 0).spawnAsEntityActive(boss);

							if (mTime == 2) {
								new PartialParticle(Particle.CRIT, mLocation, 100, 0, 0, 0, 0.5).spawnAsEntityActive(boss);
								new PartialParticle(Particle.CRIT_MAGIC, mLocation, 100, 2, 2, 2, 0).spawnAsEntityActive(boss);
								mWorld.playSound(mLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 1f);
								mWorld.playSound(mLocation, Sound.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 1f, 1f);
								for (Player p : PlayerUtils.playersInRange(mLocation.add(mDirection), mParams.DAMAGE_RADIUS, true)) {
									BossUtils.blockableDamage(mBoss, p, DamageType.MELEE, mParams.DAMAGE, mParams.SPELL_NAME, mBoss.getLocation());
								}
							}
						} else if (mTime <= 10) {
							Location locParticle = mBoss.getLocation().add(0, 1.5, 0);
							Vector sideways = new Vector(-mDirection.getZ(), 1, mDirection.getX()).multiply(3);
							Vector forward = mDirection.clone().multiply(3);
							locParticle.subtract(sideways.clone().multiply(0.5));
							locParticle.subtract(forward.clone().multiply(0.5));
							locParticle.add(forward.clone().multiply(Math.sin(Math.PI / 10 * (mTime - 5))));
							locParticle.add(sideways.clone().multiply(Math.cos(Math.PI / 10 * (mTime - 5))));
							new PartialParticle(Particle.SWEEP_ATTACK, locParticle, 4, 0.5, 0.5, 0.5, 0).spawnAsEntityActive(boss);

							if (mTime == 7) {
								new PartialParticle(Particle.CRIT, mLocation, 200, 0, 0, 0, 1).spawnAsEntityActive(boss);
								new PartialParticle(Particle.CRIT_MAGIC, mLocation, 200, 2, 2, 2, 0).spawnAsEntityActive(boss);
								mWorld.playSound(mLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 1f);
								mWorld.playSound(mLocation, Sound.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 1f, 1f);
								for (Player p : PlayerUtils.playersInRange(mLocation.add(mDirection), mParams.DAMAGE_RADIUS, true)) {
									// Bypasses iframes
									BossUtils.blockableDamage(mBoss, p, DamageType.MELEE, mParams.DAMAGE, true, true, mParams.SPELL_NAME, mBoss.getLocation(), (int) (20 * mParams.DAMAGE / 2.5), (int) (mParams.DAMAGE / 5));
								}
							}
						} else {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}, null, null), new SpellDuelist(plugin, boss, mParams.COOLDOWN, mParams.DAMAGE)));

		Bukkit.getScheduler().runTaskLater(plugin, () -> EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1), 1);

		super.constructBoss(activeSpells, Collections.emptyList(), mParams.DETECTION, null);
	}


}
