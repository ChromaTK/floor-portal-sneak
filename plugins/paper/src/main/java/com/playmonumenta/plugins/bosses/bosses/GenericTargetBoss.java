package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class GenericTargetBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_generictarget";

	public static class Parameters extends BossParameters {
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET.clone().setOptional(false);
		public boolean TARGET_EVERY_TICK = false;
		public int DELAY = 10;
	}

	private @Nullable LivingEntity mLastTarget = null;

	public GenericTargetBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob mob)) {
			throw new Exception(identityTag + " only works on mobs!");
		}

		if (boss instanceof Wolf || boss instanceof Golem || boss instanceof Dolphin || boss instanceof Ocelot) {
			boss.setRemoveWhenFarAway(true);
		}

		final Parameters param = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell targetSpell = new Spell() {

			@Override
			public void run() {
				if (EntityUtils.isStunned(mob)) {
					return;
				}

				if (mLastTarget != mob.getTarget() && mob.getTarget() != null) {
					mLastTarget = mob.getTarget();
				}

				if (mLastTarget != null) {
					if (!mLastTarget.isValid() || mLastTarget.isDead() || (mLastTarget instanceof Player player && AbilityUtils.isStealthed(player))) {
						mLastTarget = null;
						mob.setTarget(null);
					}
				}

				if (mLastTarget == null || param.TARGET_EVERY_TICK) {
					List<? extends LivingEntity> targets = param.TARGETS.getTargetsList(mob);
					targets.removeIf(le -> le instanceof Player player && AbilityUtils.isStealthed(player));
					if (targets.size() > 0) {
						mob.setTarget(targets.get(0));
						mLastTarget = targets.get(0);
					} else {
						mLastTarget = null;
						mob.setTarget(null);
					}
				}

				mob.setTarget(mLastTarget);
			}

			@Override
			public int cooldownTicks() {
				return 5;
			}

		};

		super.constructBoss(SpellManager.EMPTY, List.of(targetSpell), (int) (param.TARGETS.getRange() * 1.5), null, param.DELAY);
	}

	public void setTarget(LivingEntity target) {
		mLastTarget = target;
	}

}

