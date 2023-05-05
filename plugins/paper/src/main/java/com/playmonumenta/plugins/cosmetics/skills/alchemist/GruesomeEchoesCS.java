package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class GruesomeEchoesCS extends GruesomeAlchemyCS {
	//Twisted theme

	public static final String NAME = "Gruesome Echoes";

	private static final Color TWISTED_COLOR = Color.fromRGB(127, 0, 0);
	private static final Color ECHO_COLOR = Color.fromRGB(39, 89, 97);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
				"Infuses your alchemy with",
				"ghastly and twisted echoes.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void effectsOnSwap(Player mPlayer, boolean isGruesomeBeforeSwap) {
		if (!isGruesomeBeforeSwap) { // brutal -> gruesome, dark red
			spawnRing(mPlayer.getLocation(), mPlayer, ECHO_COLOR);
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1, 1.25f);
		} else { // gruesome -> brutal, darker blue
			spawnRing(mPlayer.getLocation(), mPlayer, TWISTED_COLOR);
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1, 0.75f);
		}
		new PartialParticle(Particle.SOUL, mPlayer.getLocation().clone().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0.02)
			.spawnAsPlayerActive(mPlayer);
		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 0.9f, 0.7f);
		mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 2f, 0.5f);
	}

	@Override
	public void effectsOnSplash(Player mPlayer, Location loc, boolean isGruesome, double radius, boolean isSpecialPot) {
		loc = loc.clone().add(0, 0.1, 0);
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_HUSK_STEP, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_HUSK_STEP, SoundCategory.PLAYERS, 1f, 0.5f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 35, 0, 0, 0, 0.125)
			.spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SOUL, loc, 15, 0, 0, 0, 0.075)
			.spawnAsPlayerActive(mPlayer);

		Vector colorValues = isGruesome ? new Vector(ECHO_COLOR.getRed(), ECHO_COLOR.getGreen(), ECHO_COLOR.getBlue()).normalize() :
			new Vector(TWISTED_COLOR.getRed(), TWISTED_COLOR.getGreen(), TWISTED_COLOR.getBlue()).normalize();
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, radius, 1, 3,
			Arrays.asList(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) -> {
					new PartialParticle(Particle.SPELL_MOB_AMBIENT, location, 1, colorValues.getX(), colorValues.getY(), colorValues.getZ(), 1).directionalMode(true).spawnAsPlayerActive(mPlayer);
				})
			)
		);
	}

	@Override
	public Color splashColor(boolean isGruesome) {
		if (isGruesome) {
			return ECHO_COLOR;
		} else {
			return TWISTED_COLOR;
		}
	}

	private void spawnRing(Location loc, Player mPlayer, Color color) {
		Location l = loc.clone().add(0, 0.1, 0);
		new BukkitRunnable() {

			double mRadius = 2.75;
			@Override
			public void run() {

				for (int i = 0; i < 3; i++) {
					mRadius -= 0.25;
					for (int degree = 0; degree < 360; degree += 9) {
						double radian = FastMath.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(radian) * mRadius, 0,
							FastUtils.sin(radian) * mRadius);
						Location loc = l.clone().add(vec);
						new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0,
							new Particle.DustOptions(color, 0.75f))
							.spawnAsPlayerActive(mPlayer);
					}
					if (mRadius <= 0) {
						this.cancel();
						return;
					}
				}


			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
