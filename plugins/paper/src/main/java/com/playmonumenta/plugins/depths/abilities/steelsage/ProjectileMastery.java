package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ProjectileMastery extends DepthsAbility {

	public static final String ABILITY_NAME = "Projectile Mastery";
	public static final double[] SPELL_MOD = {1.1, 1.125, 1.15, 1.175, 1.2, 1.25};

	public static final DepthsAbilityInfo<ProjectileMastery> INFO =
		new DepthsAbilityInfo<>(ProjectileMastery.class, ABILITY_NAME, ProjectileMastery::new, DepthsTree.STEELSAGE, DepthsTrigger.PASSIVE)
			.displayItem(Material.BOW)
			.descriptions(ProjectileMastery::getDescription);

	public ProjectileMastery(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE || event.getType() == DamageType.PROJECTILE_SKILL) {
			event.setDamage(event.getDamage() * SPELL_MOD[mRarity - 1]);
		}
		return false; // only changes event damage
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Your projectile damage is multiplied by ")
			.append(Component.text(SPELL_MOD[rarity - 1], color))
			.append(Component.text("."));
	}
}

