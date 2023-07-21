package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;

public class SpellBaseParticleAura extends Spell {

	@FunctionalInterface
	public interface ParticleEffect {
		/**
		 * Runs particles
		 */
		void run(LivingEntity boss);
	}


	private final LivingEntity mBoss;
	private final int mTicksPerIteration;
	private final ParticleEffect[] mEffects;
	private int mEffectIter;

	public SpellBaseParticleAura(LivingEntity boss, int ticksPerIteration, ParticleEffect... effects) {
		this.mBoss = boss;
		this.mTicksPerIteration = ticksPerIteration;
		this.mEffects = effects;
		this.mEffectIter = 0;
	}

	@Override
	public void run() {
		mEffectIter++;
		if (mEffectIter >= mTicksPerIteration) {
			mEffectIter = 0;
			if (EntityUtils.isStunned(mBoss) || EntityUtils.isSilenced(mBoss) || !mBoss.isValid()) {
				return;
			}
			for (ParticleEffect effect : mEffects) {
				effect.run(mBoss);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
