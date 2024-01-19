package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Spawn mobs around boss locations
 */
public class SpellBaseSummon extends Spell {

	@FunctionalInterface
	public interface GetSpawningLocations {
		/**
		 * Must return a list of location where the spawning will start
		 */
		List<Location> run();
	}

	@FunctionalInterface
	public interface GetMobQuantity {
		/**
		 * Must return the number of entity that will be summoned
		 */
		int run();
	}

	@FunctionalInterface
	public interface SummonMobAt {
		/**
		 * Given a valid locationto target, summon the mob
		 * <p>
		 * Must return the entity spawned for cancellation purposes
		 */
		@Nullable Entity run(Location loc, int times);
	}

	@FunctionalInterface
	public interface SummonAesthetic {
		void run(LivingEntity mob, Location loc, int ticks);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mCooldown;
	private final int mSummoningDuration;
	private final float mDeepness;
	private final boolean mCanBeStopped;
	private final boolean mCanMove;
	private final boolean mSingleTarget;
	private final GetMobQuantity mSummonQuantity;
	private final GetSpawningLocations mSpawningLocations;
	private final SummonMobAt mSummon;
	private final SummonAesthetic mBossAnimation;
	private final SummonAesthetic mSummonAnimation;
	private final List<Vector> mLocationOffsets;

	//keep track of how many times the spell cast
	private int mTimes = 0;
	private int mSummonNum = 0;
	private List<Location> mLocations = new ArrayList<>();

	/**
	 * @param plugin            Plugin
	 * @param boss              the entity that will cast this spell
	 * @param cooldown          cooldown
	 * @param summoningDuration how much the summoning will last
	 * @param summonRange       range of how distance the spawn can be
	 * @param canBeStopped      if the spell can be stopped when the bos is stunned
	 * @param canMove           if the bos can move while casting
	 * @param singleTarget      if the spell will only launch at one random pos
	 * @param summonQuantity    return the number of mob that will be spawned at each pos
	 * @param spawningLocations return a list of spawning locations
	 * @param summonMob         return the mob spawned at the given location
	 * @param bossAnimation     animation for the boss
	 * @param summonAnimation   animation for each summon mob
	 */
	public SpellBaseSummon(
		Plugin plugin,
		LivingEntity boss,
		int cooldown,
		int summoningDuration,
		double summonRange,
		float deepness,
		boolean canBeStopped,
		boolean canMove,
		boolean singleTarget,
		GetMobQuantity summonQuantity,
		GetSpawningLocations spawningLocations,
		SummonMobAt summonMob,
		SummonAesthetic bossAnimation,
		SummonAesthetic summonAnimation
	) {

		mPlugin = plugin;
		mBoss = boss;
		mCooldown = cooldown;
		mSummoningDuration = summoningDuration;
		mDeepness = deepness;
		mCanBeStopped = canBeStopped;
		mCanMove = canMove;
		mSingleTarget = singleTarget;
		mSummonQuantity = summonQuantity;
		mSpawningLocations = spawningLocations;
		mSummon = summonMob;
		mBossAnimation = bossAnimation;
		mSummonAnimation = summonAnimation;

		// Calculate a reference list of offsets to randomly try when spawning mobs
		mLocationOffsets = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			double r = FastUtils.randomDoubleInRange(summonRange * 0.5, summonRange);
			double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);

			double x = r * Math.cos(theta);
			double z = r * Math.sin(theta);

			mLocationOffsets.add(new Vector(x, 0, z));
		}
	}

	@Override
	public void run() {
		mTimes++;

		if (!mCanMove) {
			EntityUtils.selfRoot(mBoss, mSummoningDuration);
		}

		aestheticBoss();

		if (mSingleTarget) {
			Collections.shuffle(mLocations);
			Location summonLoc = mLocations.get(0);
			summoningRunnable(mSummonNum, summonLoc);
		} else {
			for (Location summonLoc : mLocations) {
				summoningRunnable(mSummonNum, summonLoc);
			}
		}

	}

	@Override
	public boolean canRun() {
		mSummonNum = mSummonQuantity.run();
		mLocations = mSpawningLocations.run();
		return mSummonNum > 0 && mLocations != null && !mLocations.isEmpty();
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}


	private void aestheticBoss() {
		new BukkitRunnable() {
			int mTimer = 0;

			@Override
			public void run() {
				if (mBoss == null || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				if (mCanBeStopped && EntityUtils.shouldCancelSpells(mBoss)) {
					this.cancel();
					return;
				}

				mBossAnimation.run(mBoss, mBoss.getLocation(), mTimer);

				if (mTimer >= mSummoningDuration) {
					this.cancel();
					return;
				}

				mTimer++;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				if (!mCanMove) {
					EntityUtils.cancelSelfRoot(mBoss);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void summoningRunnable(int summonQuantity, Location summonLoc) {
		Collections.shuffle(mLocationOffsets);
		int index = 0;
		for (int i = 0; i < summonQuantity; i++) {

			Vector offset = mLocationOffsets.get(index);
			Location loc = summonLoc.clone().add(offset);

			//don't summon inside solid block
			int attempts = 0;
			while (loc.clone().add(0, 1, 0).getBlock().getType().isSolid() || loc.clone().add(0, 2, 0).getBlock().getType().isSolid() || !loc.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
				index = (index + 1 >= mLocationOffsets.size() ? 0 : index + 1);
				loc = summonLoc.clone().add(mLocationOffsets.get(index));
				attempts++;

				//No valid location exists
				if (attempts > mLocationOffsets.size()) {
					return;
				}
			}

			Entity entity = mSummon.run(loc.clone().subtract(0, mDeepness, 0), mTimes);
			if (entity instanceof Mob mob) {
				mob.setAI(false);

				//summoning
				new BukkitRunnable() {
					int mTimer = 0;

					@Override
					public void run() {
						if (mBoss == null || !mBoss.isValid()) {
							this.cancel();
							return;
						}

						if (mCanBeStopped && EntityUtils.shouldCancelSpells(mBoss)) {
							this.cancel();
							return;
						}

						if (mTimer >= mSummoningDuration) {
							mob.setAI(true);

							this.cancel();
							return;
						}

						Location mobLoc = mob.getLocation().add(0, mDeepness / mSummoningDuration, 0);
						mob.teleport(mobLoc);
						mSummonAnimation.run(mob, mobLoc, mTimer);

						mTimer += 1;
					}

					@Override
					public synchronized void cancel() {
						super.cancel();
						mob.setGlowing(false);
						//if the cast is not over remove the mob
						if (mTimer < mSummoningDuration) {
							mob.remove();
						}
					}


				}.runTaskTimer(mPlugin, 0, 1);
			}

			index = (index + 1 >= mLocationOffsets.size() ? 0 : index + 1);

		}
	}
}
