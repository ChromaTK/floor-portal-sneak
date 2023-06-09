package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class NoExperienceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_no_exp";
	public static final int detectionRange = 30;

	public NoExperienceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		// Boss effectively does nothing
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event != null) {
			event.setDroppedExp(0);
		}
	}
}
