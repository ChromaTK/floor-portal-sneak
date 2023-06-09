package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.utils.BossUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BlueEarthBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blueearth";
	public static final int detectionRange = 20;

	public static final double[] KB_RESIST = {0, 0.1, 0.15, 0.2};
	public static final String KB_RESIST_EFFECT_NAME = "BossBlueEarthKBResistEffect";

	private int mBlueTimeOfDay = 0;

	public BlueEarthBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mBlueTimeOfDay = BossUtils.getBlueTimeOfDay(boss);

		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				if (mBlueTimeOfDay > 0) {
					EffectManager.getInstance().addEffect(mBoss, KB_RESIST_EFFECT_NAME, new PercentKnockbackResist(100, KB_RESIST[mBlueTimeOfDay], KB_RESIST_EFFECT_NAME));
				}
			})
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null, 100, 20);
	}
}
