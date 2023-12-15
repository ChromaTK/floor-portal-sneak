package com.playmonumenta.plugins.bosses.spells.cluckingop;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellEruption extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;

	public SpellEruption(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 30, true);
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				new PartialParticle(Particle.LAVA, mBoss.getLocation(), 20, 0.15, 0, 0.15, 0.175).spawnAsEntityActive(mBoss);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.5f, 1f);
				for (Player player : players) {
					new PartialParticle(Particle.LAVA, player.getLocation(), 10, 0.15, 0, 0.15, 0.175).spawnAsEntityActive(mBoss);
				}

				if (mT >= 3) {
					this.cancel();
					for (Player player : players) {
						player.setVelocity(new Vector(0, 2, 0));
						player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 10));
						new PartialParticle(Particle.FLAME, player.getLocation(), 150, 0, 0, 0, 0.175).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.SMOKE_LARGE, player.getLocation(), 75, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
						world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1);
						BossUtils.blockableDamage(mBoss, player, DamageType.BLAST, 1);
						new BukkitRunnable() {

							@Override
							public void run() {
								BossUtils.blockableDamage(mBoss, player, DamageType.BLAST, 1);
								new PartialParticle(Particle.FLAME, player.getLocation(), 150, 0, 0, 0, 0.175).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.SMOKE_LARGE, player.getLocation(), 75, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
								world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1);
								player.setVelocity(new Vector(0, -2, 0));
								new BukkitRunnable() {

									@Override
									public void run() {
										new PartialParticle(Particle.SMOKE_LARGE, player.getLocation(), 1, 0, 0, 0, 0.05).spawnAsEntityActive(mBoss);
										if (PlayerUtils.isOnGround(player) || player.isDead() || !player.isOnline()) {
											this.cancel();
											new BukkitRunnable() {

												@Override
												public void run() {
													BossUtils.blockableDamage(mBoss, player, DamageType.BLAST, 1);
													new PartialParticle(Particle.FLAME, player.getLocation(), 150, 0, 0, 0, 0.175).spawnAsEntityActive(mBoss);
													new PartialParticle(Particle.SMOKE_LARGE, player.getLocation(), 75, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
													world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1);
													world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, SoundCategory.HOSTILE, 1, 0);
												}

											}.runTaskLater(mPlugin, 1);
										}
									}

								}.runTaskTimer(mPlugin, 0, 1);
							}

						}.runTaskLater(mPlugin, 15);
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 15);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 5;
	}
}
