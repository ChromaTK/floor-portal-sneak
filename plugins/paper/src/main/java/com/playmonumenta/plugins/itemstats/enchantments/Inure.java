package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import java.util.NavigableSet;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Inure implements Enchantment {

	private static final double ARMOR_BONUS_PER_LEVEL = 0.2;
	private static final int PAST_HIT_DURATION_TIME = 20 * 60;
	private static final String INURE_MELEE_NAME = "MeleeInureEffect";
	private static final String INURE_PROJ_NAME = "ProjectileInureEffect";
	private static final String INURE_MAGIC_NAME = "MagicInureEffect";
	private static final String INURE_BLAST_NAME = "BlastInureEffect";
	private static final String INURE_MEMORY_NAME = "MemoryInureEffect";

	@Override
	public String getName() {
		return "Inure";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INURE;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked()) {
			return;
		}
		if (event.getType() == DamageType.MELEE) {
			clearInure(plugin, player);
			plugin.mEffectManager.addEffect(player, INURE_MELEE_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME));
		} else if (event.getType() == DamageType.PROJECTILE) {
			clearInure(plugin, player);
			plugin.mEffectManager.addEffect(player, INURE_PROJ_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME));
		} else if (event.getType() == DamageType.MAGIC) {
			clearInure(plugin, player);
			plugin.mEffectManager.addEffect(player, INURE_MAGIC_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME));
		} else if (event.getType() == DamageType.BLAST) {
			clearInure(plugin, player);
			plugin.mEffectManager.addEffect(player, INURE_BLAST_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME));
		}
	}

	public void clearInure(Plugin plugin, Player player) {
		plugin.mEffectManager.clearEffects(player, INURE_MELEE_NAME);
		plugin.mEffectManager.clearEffects(player, INURE_PROJ_NAME);
		plugin.mEffectManager.clearEffects(player, INURE_MAGIC_NAME);
		plugin.mEffectManager.clearEffects(player, INURE_BLAST_NAME);
	}

	public static double applyInure(DamageEvent event, Plugin plugin, Player player) {
		NavigableSet<Effect> melee = plugin.mEffectManager.getEffects(player, INURE_MELEE_NAME);
		NavigableSet<Effect> proj = plugin.mEffectManager.getEffects(player, INURE_PROJ_NAME);
		NavigableSet<Effect> magic = plugin.mEffectManager.getEffects(player, INURE_MAGIC_NAME);
		NavigableSet<Effect> blast = plugin.mEffectManager.getEffects(player, INURE_BLAST_NAME);
		NavigableSet<Effect> memory = plugin.mEffectManager.getEffects(player, INURE_MEMORY_NAME);
		if ((event.getType() == DamageType.MELEE && melee != null) ||
			(event.getType() == DamageType.PROJECTILE && proj != null) ||
			(event.getType() == DamageType.MAGIC && magic != null) ||
			(event.getType() == DamageType.BLAST && blast != null)) {
			// max inure
			player.playSound(player.getLocation(), Sound.BLOCK_SOUL_SOIL_HIT, SoundCategory.HOSTILE, 0.7f, 1.0f);
			plugin.mEffectManager.addEffect(player, INURE_MEMORY_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME));
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.INURE) * ARMOR_BONUS_PER_LEVEL;
		} else if (memory != null) {
			player.playSound(player.getLocation(), Sound.BLOCK_SOUL_SOIL_BREAK, SoundCategory.HOSTILE, 0.7f, 1.0f);
			plugin.mEffectManager.clearEffects(player, INURE_MEMORY_NAME);
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.INURE) * ARMOR_BONUS_PER_LEVEL / 2;
		} else {
			return 0;
		}
	}

}
