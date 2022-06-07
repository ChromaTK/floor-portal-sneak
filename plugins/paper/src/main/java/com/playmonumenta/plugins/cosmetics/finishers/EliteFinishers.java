package com.playmonumenta.plugins.cosmetics.finishers;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class EliteFinishers {

	private static final ImmutableMap<String, EliteFinisher> FINISHERS =
		ImmutableMap.<String, EliteFinisher>builder()
			.put(LightningFinisher.NAME, new LightningFinisher())
			.put(WarmFireworkFinisher.NAME, new WarmFireworkFinisher())
			.put(CoolFireworkFinisher.NAME, new CoolFireworkFinisher())
			.put(VictoryThemeFinisher.NAME, new VictoryThemeFinisher())
			.put(BirthdayThemeFinisher.NAME, new BirthdayThemeFinisher())
			.put(CakeifyFinisher.NAME, new CakeifyFinisher())
			.build();

	// Delegate based on elite finisher name
	public static void activateFinisher(Player p, Entity killedMob, Location loc, String finisherName) {
		EliteFinisher finisher = FINISHERS.get(finisherName);
		if (finisher != null) {
			finisher.run(p, killedMob, loc);
		}
	}

	public static Material getDisplayItem(String finisherName) {
		EliteFinisher finisher = FINISHERS.get(finisherName);
		if (finisher != null) {
			return finisher.getDisplayItem();
		} else {
			return Material.FIREWORK_ROCKET;
		}
	}

	public static String[] getNames() {
		return FINISHERS.keySet().toArray(new String[0]);
	}

}
