package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.CrowdControlImmunity;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class CrowdControlImmunityBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_ccimmune";
	public static final int detectionRange = 45;

	public CrowdControlImmunityBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = List.of(
			new CrowdControlImmunity(boss)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		Effect effect = event.getEffect();

		if (effect.getClass() == PercentSpeed.class && effect.getMagnitude() < 0) {
			effect.setDuration(0);
		}
	}
}
