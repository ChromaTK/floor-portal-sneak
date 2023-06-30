package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CornucopiaFinisher implements EliteFinisher {

	public static final String NAME = "Cornucopia";

	private static final HashMap<UUID, Integer> mMobsKilled = new HashMap<>();
	public static final Material[] foodList = {
		Material.BAKED_POTATO,
		Material.BREAD,
		Material.COOKED_CHICKEN,
		Material.CARROT,
		Material.APPLE,
		Material.PUMPKIN_PIE,
		Material.SWEET_BERRIES
	};

	@Override
	public void run(Player p, Entity killedMob, Location loc) {

		int mobsKilled = mMobsKilled.computeIfAbsent(p.getUniqueId(), key -> 1);

		throwFood(0, loc);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks <= 57) {
					playSong(mobsKilled, mTicks, loc);
				}
				if (mTicks == 10) {
					throwFood(30, loc);
				} else if (mTicks == 20) {
					throwFood(60, loc);
				} else if (mTicks == 100) {
					Collection<Item> potentialCakes = loc.getWorld().getNearbyEntitiesByType(Item.class, loc, 20.0);
					for (Item i : potentialCakes) {
						if (i.getScoreboardTags().contains(NAME)) {
							i.remove();
						}
					}
					if (mobsKilled >= 2) {
						mMobsKilled.put(p.getUniqueId(), 1);
					} else {
						mMobsKilled.put(p.getUniqueId(), mobsKilled + 1);
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}

	public static void playSong(int variant, int ticks, Location loc) {
		World world = loc.getWorld();
		if (variant == 1) {
			switch (ticks) {
				case 0 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G1);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G1);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
				}
				case 3 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.B5);
				case 6 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
				case 9 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.A3);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F11);
				}
				case 12 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
				case 15 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F11);
				case 18 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.B5);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G13);
				}
				case 21 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
				case 24 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G13);
				case 27 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
				case 33 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS9);
				case 36 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS9);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.GS14);
				}
				case 39 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS9);
				case 42 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.GS14);
				case 45 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F11);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.AS16);
				}
				case 48 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F11);
				case 51 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.AS16);
				case 54 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G13);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
				}
				default -> {
				}
			}
		} else {
			switch (ticks) {
				case 0 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D20);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.AS4);
				}
				case 3 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.AS16);
				case 6 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F11);
				case 9 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G13);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS9);
				}
				case 12 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS9);
				case 15 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.AS4);
				case 18 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C18);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.GS2);
				}
				case 21 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.GS14);
				case 24 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS9);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.DS9);
				}
				case 36 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F11);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.CS7);
				}
				case 39 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.CS7);
				case 42 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.GS2);
				case 45 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G1);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F11);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.G13);
				}
				case 48 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
				case 51 ->
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.B5);
				case 54 -> {
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
					world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.C6);
				}
				default -> {
				}
			}
		}
	}

	public static void throwFood(int offset, Location loc) {
		for (int i = 0; i < 4; i++) {
			ItemStack food = new ItemStack(foodList[FastUtils.RANDOM.nextInt(foodList.length)]);
			ItemMeta foodMeta = food.getItemMeta();
			foodMeta.displayName(Component.text("Cornucopia Food " + offset + i + "!", NamedTextColor.WHITE)
				.decoration(TextDecoration.ITALIC, false));
			food.setItemMeta(foodMeta);
			Item foodItem = loc.getWorld().dropItem(loc, food);
			foodItem.setPickupDelay(Integer.MAX_VALUE);
			foodItem.addScoreboardTag(NAME);
			foodItem.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
			int degrees = offset + (i * 90);
			foodItem.setVelocity(new Vector(0.2 * FastUtils.sinDeg(degrees), 0.6, 0.2 * FastUtils.cosDeg(degrees)));
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_CARROT;
	}

}
