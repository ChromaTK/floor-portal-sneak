package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.imperialconstruct.SpellParadoxSwap;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ParadoxSwapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_paradoxswap";
	public static final int detectionRange = 5;

	public ParadoxSwapBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = List.of(
			new SpellParadoxSwap(plugin, detectionRange, boss)
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
