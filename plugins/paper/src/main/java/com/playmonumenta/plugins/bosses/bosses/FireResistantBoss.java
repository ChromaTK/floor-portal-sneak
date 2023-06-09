package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellMobEffect;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FireResistantBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_fireresist";
	public static final int detectionRange = 100;

	public FireResistantBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		// Immediately apply the effect, don't wait
		Spell fireresist = new SpellMobEffect(boss, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20, 0, false, false));
		fireresist.run();

		List<Spell> passiveSpells = List.of(fireresist);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
