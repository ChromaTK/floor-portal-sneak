package com.playmonumenta.plugins.delves.abilities;

import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.bosses.bosses.FireBombTossBoss;
import com.playmonumenta.plugins.bosses.bosses.FlameTrailBoss;
import com.playmonumenta.plugins.bosses.bosses.NovaBoss;
import com.playmonumenta.plugins.bosses.bosses.ProjectileBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Infernal {

	private static final EnumSet<DamageType> ENVIRONMENTAL_DAMAGE_CAUSES = EnumSet.of(
			DamageType.AILMENT,
			DamageType.FALL
	);

	private static final double ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL = 0.1;

	private static final double BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL = 0.2;

	private static final double ABILITY_CHANCE_PER_LEVEL = 0.06;

	private static final List<List<String>> ABILITY_POOL_R1;
	private static final List<List<String>> ABILITY_POOL_R2;
	private static final List<List<String>> ABILITY_POOL_R3;

	private static final String TRACKING_SPELL_NAME = "Infernal Missile";
	private static final String NOVA_SPELL_NAME = "Infernal Nova";
	private static final String FLAME_TRAIL_SPELL_NAME = "Infernal Trail";
	private static final String BOMB_TOSS_SPELL_NAME = "Infernal Bomb";
	public static final ImmutableSet<String> SPELL_NAMES = ImmutableSet.of(TRACKING_SPELL_NAME, NOVA_SPELL_NAME, FLAME_TRAIL_SPELL_NAME, BOMB_TOSS_SPELL_NAME);

	static {
		ABILITY_POOL_R1 = new ArrayList<>();
		ABILITY_POOL_R2 = new ArrayList<>();
		ABILITY_POOL_R3 = new ArrayList<>();

		List<String> seekingProjectileBoss = new ArrayList<>();
		seekingProjectileBoss.add(ProjectileBoss.identityTag);
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[damage=10,distance=64,speed=0.4,delay=20,cooldown=240,turnradius=0.11,effects=[(fire,60)],spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_BLAZE_AMBIENT,1.2,0.5)],soundlaunch=[(ENTITY_BLAZE_SHOOT,0.5,0.5)],soundprojectile=[(ENTITY_BLAZE_BURN,0.4,0.2)],soundhit=[(ENTITY_GENERIC_EXPLODE,0.5,0.5)]]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[particlelaunch=[(EXPLOSION_LARGE,1)],particleprojectile=[(FLAME,3,0,0,0,0.1),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],particlehit=[(FLAME,50,0,0,0,0.3)]]");
		ABILITY_POOL_R1.add(seekingProjectileBoss);
		seekingProjectileBoss = new ArrayList<>();
		seekingProjectileBoss.add(ProjectileBoss.identityTag);
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[damage=20,distance=64,speed=0.6,delay=20,cooldown=240,turnradius=0.11,effects=[(fire,100)],spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_BLAZE_AMBIENT,1.2,0.5)],soundlaunch=[(ENTITY_BLAZE_SHOOT,0.5,0.5)],soundprojectile=[(ENTITY_BLAZE_BURN,0.4,0.2)],soundhit=[(ENTITY_GENERIC_EXPLODE,0.5,0.5)]]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[particlelaunch=[(EXPLOSION_LARGE,1)],particleprojectile=[(FLAME,3,0,0,0,0.1),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],particlehit=[(FLAME,50,0,0,0,0.3)]]");
		ABILITY_POOL_R2.add(seekingProjectileBoss);
		seekingProjectileBoss = new ArrayList<>();
		seekingProjectileBoss.add(ProjectileBoss.identityTag);
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[damage=25,distance=64,speed=0.6,delay=20,cooldown=240,turnradius=0.11,effects=[(fire,100)],spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_BLAZE_AMBIENT,1.2,0.5)],soundlaunch=[(ENTITY_BLAZE_SHOOT,0.5,0.5)],soundprojectile=[(ENTITY_BLAZE_BURN,0.4,0.2)],soundhit=[(ENTITY_GENERIC_EXPLODE,0.5,0.5)]]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[particlelaunch=[(EXPLOSION_LARGE,1)],particleprojectile=[(FLAME,3,0,0,0,0.1),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],particlehit=[(FLAME,50,0,0,0,0.3)]]");
		ABILITY_POOL_R3.add(seekingProjectileBoss);

		List<String> flameNovaBoss = new ArrayList<>();
		flameNovaBoss.add(NovaBoss.identityTag);
		flameNovaBoss.add(NovaBoss.identityTag + "[damage=9,duration=70,detection=20,effects=[(fire,60)],spellname=\"" + NOVA_SPELL_NAME + "\"]");
		flameNovaBoss.add(NovaBoss.identityTag + "[soundcharge=BLOCK_FIRE_AMBIENT,soundchargevolume=2,soundcast=[(ENTITY_WITHER_SHOOT,1.5,0.65)]]");
		flameNovaBoss.add(NovaBoss.identityTag + "[particleair=[(LAVA,2,4.5,4.5,4.5,0.05)],particleload=[(FLAME,1,0.25,0.25,0.25,0.1)],particleexplode=[(FLAME,1,0.1,0.1,0.1,0.3),(SMOKE_NORMAL,2,0.25,0.25,0.25,0.1)]]");
		ABILITY_POOL_R1.add(flameNovaBoss);
		flameNovaBoss = new ArrayList<>();
		flameNovaBoss.add(NovaBoss.identityTag);
		flameNovaBoss.add(NovaBoss.identityTag + "[damage=17,duration=70,detection=20,effects=[(fire,80)],spellname=\"" + NOVA_SPELL_NAME + "\"]");
		flameNovaBoss.add(NovaBoss.identityTag + "[soundcharge=BLOCK_FIRE_AMBIENT,soundchargevolume=2,soundcast=[(ENTITY_WITHER_SHOOT,1.5,0.65)]]");
		flameNovaBoss.add(NovaBoss.identityTag + "[particleair=[(LAVA,2,4.5,4.5,4.5,0.05)],particleload=[(FLAME,1,0.25,0.25,0.25,0.1)],particleexplode=[(FLAME,1,0.1,0.1,0.1,0.3),(SMOKE_NORMAL,2,0.25,0.25,0.25,0.1)]]");
		ABILITY_POOL_R2.add(flameNovaBoss);
		flameNovaBoss = new ArrayList<>();
		flameNovaBoss.add(NovaBoss.identityTag);
		flameNovaBoss.add(NovaBoss.identityTag + "[damage=25,duration=70,detection=20,effects=[(fire,80)],spellname=\"" + NOVA_SPELL_NAME + "\"]");
		flameNovaBoss.add(NovaBoss.identityTag + "[soundcharge=BLOCK_FIRE_AMBIENT,soundchargevolume=2,soundcast=[(ENTITY_WITHER_SHOOT,1.5,0.65)]]");
		flameNovaBoss.add(NovaBoss.identityTag + "[particleair=[(LAVA,2,4.5,4.5,4.5,0.05)],particleload=[(FLAME,1,0.25,0.25,0.25,0.1)],particleexplode=[(FLAME,1,0.1,0.1,0.1,0.3),(SMOKE_NORMAL,2,0.25,0.25,0.25,0.1)]]");
		ABILITY_POOL_R3.add(flameNovaBoss);

		List<String> flameTrailBoss = new ArrayList<>();
		flameTrailBoss.add(FlameTrailBoss.identityTag);
		flameTrailBoss.add(FlameTrailBoss.identityTag + "[damage=9,spellname=\"" + FLAME_TRAIL_SPELL_NAME + "\"]");
		ABILITY_POOL_R1.add(flameTrailBoss);
		flameTrailBoss = new ArrayList<>();
		flameTrailBoss.add(FlameTrailBoss.identityTag);
		flameTrailBoss.add(FlameTrailBoss.identityTag + "[damage=15,spellname=\"" + FLAME_TRAIL_SPELL_NAME + "\"]");
		ABILITY_POOL_R2.add(flameTrailBoss);
		flameTrailBoss = new ArrayList<>();
		flameTrailBoss.add(FlameTrailBoss.identityTag);
		flameTrailBoss.add(FlameTrailBoss.identityTag + "[damage=20,spellname=\"" + FLAME_TRAIL_SPELL_NAME + "\"]");
		ABILITY_POOL_R3.add(flameTrailBoss);

		List<String> fireBombTossBoss = new ArrayList<>();
		fireBombTossBoss.add(FireBombTossBoss.identityTag);
		fireBombTossBoss.add(FireBombTossBoss.identityTag + "[damage=24,spellname=\"" + BOMB_TOSS_SPELL_NAME + "\"]");
		ABILITY_POOL_R1.add(fireBombTossBoss);
		fireBombTossBoss = new ArrayList<>();
		fireBombTossBoss.add(FireBombTossBoss.identityTag);
		fireBombTossBoss.add(FireBombTossBoss.identityTag + "[damage=48,spellname=\"" + BOMB_TOSS_SPELL_NAME + "\"]");
		ABILITY_POOL_R2.add(fireBombTossBoss);
		fireBombTossBoss = new ArrayList<>();
		fireBombTossBoss.add(FireBombTossBoss.identityTag);
		fireBombTossBoss.add(FireBombTossBoss.identityTag + "[damage=72,spellname=\"" + BOMB_TOSS_SPELL_NAME + "\"]");
		ABILITY_POOL_R3.add(fireBombTossBoss);

	}

	public static final String DESCRIPTION = "Enemies gain fiery abilities.";

	public static String[] rankDescription(int level) {
			return new String[]{
				"Enemies have a " + Math.round(ABILITY_CHANCE_PER_LEVEL * level * 100) + "% chance to be Infernal.",
				"Players take +" + Math.round(BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level * 100) + "% Burning Damage",
				"and +" + Math.round(ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level * 100) + "% Environmental Damage."
			};
	}

	public static void applyDamageModifiers(DamageEvent event, int level) {
		if (level == 0) {
			return;
		}

		if (event.getType() == DamageType.FIRE) {
			event.setDamage(event.getDamage() * (1 + BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level));
		} else if (ENVIRONMENTAL_DAMAGE_CAUSES.contains(event.getType())) {
			event.setDamage(event.getDamage() * (1 + ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level));
		}
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (FastUtils.RANDOM.nextDouble() < ABILITY_CHANCE_PER_LEVEL * level && !DelvesUtils.isDelveMob(mob)) {
			Player nearestPlayer = EntityUtils.getNearestPlayer(mob.getLocation(), 64);
			List<List<String>> abilityPool = new ArrayList<>(ServerProperties.getClassSpecializationsEnabled(nearestPlayer) ? (ServerProperties.getAbilityEnhancementsEnabled(nearestPlayer) ? ABILITY_POOL_R3 : ABILITY_POOL_R2) : ABILITY_POOL_R1);
			abilityPool.removeIf(ability -> mob.getScoreboardTags().contains(ability.get(0)));
			List<String> ability = abilityPool.get(FastUtils.RANDOM.nextInt(abilityPool.size()));
			for (String abilityTag : ability) {
				mob.addScoreboardTag(abilityTag);
			}
		}
	}
}
