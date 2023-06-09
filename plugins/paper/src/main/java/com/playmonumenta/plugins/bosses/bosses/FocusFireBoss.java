package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collections;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FocusFireBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_focusfire";
	public static final int detectionRange = 15;

	public FocusFireBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		//If we hit a player
		if (damagee instanceof Player player) {
			//Set all nearby mobs to target them
			for (LivingEntity le : EntityUtils.getNearbyMobs(mBoss.getLocation(), detectionRange)) {
				if (le instanceof Mob mob && !ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG)) {
					mob.setTarget(player);
				}
			}
			//Let the players know something happened
			player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.3f, 0.9f);
			new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 25, 1.5, 1.5, 1.5).spawnAsEntityActive(mBoss);
		}
	}
}
