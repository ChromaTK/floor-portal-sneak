package com.playmonumenta.plugins.bosses.bosses.exalted;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellAxtalMeleeMinions;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;
import com.playmonumenta.plugins.bosses.spells.exalted.SpellAxtalGroundSurge;
import com.playmonumenta.plugins.bosses.spells.exalted.SpellAxtalTotem;
import com.playmonumenta.plugins.bosses.spells.exalted.SpellConditionalTpBehindPlayer;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ExaltedCAxtal extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_ex_caxtal";
	public static final int detectionRange = 110;

	private final TextColor mJade = TextColor.fromCSSHexString("#39b14e");

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.END_PORTAL,
		Material.CHEST,
		Material.TRAPPED_CHEST,
		Material.LAVA
	);

	private double mCoefficient = 1.0;

	public ExaltedCAxtal(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mBoss.setRemoveWhenFarAway(false);

		mBoss.addScoreboardTag("Boss");

		SpellManager activeSpells1 = new SpellManager(Arrays.asList(
			new SpellAxtalGroundSurge(mPlugin, mBoss, 1, detectionRange),
			new SpellAxtalMeleeMinions(plugin, mBoss, 6, 3, 3, 20, 12, "~ExAxtalSummons", true),
			new SpellAxtalTotem(plugin, mBoss, 30),
			new SpellBaseLaser(plugin, boss, 60, 140, false, true, 8 * 20,
				// Tick action per player
				(LivingEntity target, int ticks, boolean blocked) -> {
					World world = boss.getWorld();
					if (ticks % 8 == 0) {
						world.playSound(target.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 2) {
						world.playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 4) {
						world.playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 6) {
						world.playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					}
				},
				// Particles generated by the laser
				(Location loc) -> {
					new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SMOKE_LARGE, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SPELL_MOB, loc, 1, 0.02, 0.02, 0.02, 1).spawnAsEntityActive(boss);
				},
				// TNT generated at the end of the attack
				(LivingEntity player, Location loc, boolean blocked) -> {
					double r = 15;
					int maxDmg = 160;
					for (Player p : PlayerUtils.playersInRange(loc, r, true)) {
						if (p.getLocation().distance(loc) <= r && (LocationUtils.hasLineOfSight(p.getLocation(), loc) || LocationUtils.hasLineOfSight(p.getEyeLocation(), loc))) {
							double dist = p.getLocation().distance(loc);
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, maxDmg * (1 - dist / r), null, false, true, "Corruption Blast");
							PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), p, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 10));
						}
					}
					loc.getWorld().createExplosion(loc, 7, false, true, mBoss);
				})
		));

		SpellManager activeSpells2 = new SpellManager(Arrays.asList(
			new SpellAxtalGroundSurge(mPlugin, mBoss, 2, detectionRange),
			new SpellAxtalMeleeMinions(plugin, mBoss, 8, 3, 3, 20, 12, "~ExAxtalSummons", true),
			new SpellAxtalTotem(plugin, mBoss, 35),
			new SpellBaseLaser(plugin, boss, 60, 140, false, true, 160,
				// Tick action per player
				(LivingEntity target, int ticks, boolean blocked) -> {
					World world = boss.getWorld();
					if (ticks % 8 == 0) {
						world.playSound(target.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 2) {
						world.playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 4) {
						world.playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 6) {
						world.playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					}
				},
				// Particles generated by the laser
				(Location loc) -> {
					new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SMOKE_LARGE, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SPELL_MOB, loc, 1, 0.02, 0.02, 0.02, 1).spawnAsEntityActive(boss);
				},
				// TNT generated at the end of the attack
				(LivingEntity player, Location loc, boolean blocked) -> {
					double r = 15;
					int maxDmg = 160;
					for (Player p : PlayerUtils.playersInRange(loc, r, true)) {
						if (p.getLocation().distance(loc) <= r && (LocationUtils.hasLineOfSight(p.getLocation(), loc) || LocationUtils.hasLineOfSight(p.getEyeLocation(), loc))) {
							double dist = p.getLocation().distance(loc);
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, maxDmg * (1 - dist / r), null, false, true, "Corruption Blast");
							PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), p, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 10));
						}
					}
					loc.getWorld().createExplosion(loc, 7, false, true, mBoss);
				})
		));

		SpellManager activeSpells3 = new SpellManager(Arrays.asList(
			new SpellAxtalGroundSurge(mPlugin, mBoss, 3, detectionRange),
			new SpellAxtalMeleeMinions(plugin, mBoss, 10, 3, 3, 20, 12, "~ExAxtalSummons", true),
			new SpellAxtalTotem(plugin, mBoss, 40),
			new SpellBaseLaser(plugin, boss, 60, 140, false, true, 160,
				// Tick action per player
				(LivingEntity target, int ticks, boolean blocked) -> {
					World world = boss.getWorld();
					if (ticks % 8 == 0) {
						world.playSound(target.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 2) {
						world.playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 4) {
						world.playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 6) {
						world.playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 100f) * 1.5f);
					}
				},
				// Particles generated by the laser
				(Location loc) -> {
					new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SMOKE_LARGE, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SPELL_MOB, loc, 1, 0.02, 0.02, 0.02, 1).spawnAsEntityActive(boss);
				},
				// TNT generated at the end of the attack
				(LivingEntity player, Location loc, boolean blocked) -> {
					double r = 15;
					int maxDmg = 160;
					for (Player p : PlayerUtils.playersInRange(loc, r, true)) {
						if (p.getLocation().distance(loc) <= r && (LocationUtils.hasLineOfSight(p.getLocation(), loc) || LocationUtils.hasLineOfSight(p.getEyeLocation(), loc))) {
							double dist = p.getLocation().distance(loc);
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, maxDmg * (1 - dist / r), null, false, true, "Corruption Blast");
							PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), p, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 10));
						}
					}
					loc.getWorld().createExplosion(loc, 7, false, true, mBoss);
				})
		));
		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellShieldStun(6 * 20),
			// Teleport the boss to targetted player if he is stuck in bedrock
			new SpellConditionalTpBehindPlayer(mPlugin, mBoss,
					b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
					     b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
					     b.getLocation().getBlock().getType() == Material.LAVA)
		);
		SpellManager phaseChangeActive = new SpellManager(Arrays.asList(
			new SpellTpBehindPlayer(plugin, mBoss, 4 * 20 + 1, 110, 50, 10, true)
		));

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss -> {
			PlayerUtils.nearbyPlayersAudience(spawnLoc, detectionRange)
				.sendMessage(Component.text("At l", NamedTextColor.DARK_RED).append(
					Component.text("a", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED)).append(
					Component.text("st, the keys a", NamedTextColor.DARK_RED)).append(
					Component.text("re", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED)).append(
					Component.text("... Th", NamedTextColor.DARK_RED)).append(
					Component.text("e Nigh", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED)).append(
					Component.text("tmare... is h", NamedTextColor.DARK_RED)).append(
					Component.text("e", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED)).append(
					Component.text("re... I can ", NamedTextColor.DARK_RED)).append(
					Component.text("be ", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED)).append(
					Component.text("free...", NamedTextColor.DARK_RED)));
		});
		events.put(67, (mBoss) -> {
			PlayerUtils.nearbyPlayersAudience(spawnLoc, detectionRange)
				.sendMessage(Component.text("She can see us so clearly. So many eyes watching... We are nothing but a show for them...", NamedTextColor.DARK_RED));
			phaseTransition(12, 1, mIgnoredMats, phaseChangeActive, activeSpells2, passiveSpells);
		});
		events.put(33, (mBoss) -> {
			PlayerUtils.nearbyPlayersAudience(spawnLoc, detectionRange)
				.sendMessage(Component.text("Let this dance continue and it will take notice of you for r", NamedTextColor.DARK_RED).append(
					Component.text("ea", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED)).append(
					Component.text("l. Do you really wi", NamedTextColor.DARK_RED)).append(
					Component.text("s", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED)).append(
					Component.text("h to do t", NamedTextColor.DARK_RED)).append(
					Component.text("h", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED)).append(
					Component.text("is?", NamedTextColor.DARK_RED)));
			phaseTransition(16, 1, mIgnoredMats, phaseChangeActive, activeSpells3, passiveSpells);
		});
		events.put(15, (mBoss) -> {
			PlayerUtils.nearbyPlayersAudience(spawnLoc, detectionRange)
				.sendMessage(Component.text("H", TextColor.fromCSSHexString("#39b14e")).append(
					Component.text("OW RESPLEN", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("DENT... THE O", mJade)).append(
					Component.text("RIG", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("IN O", mJade)).append(
					Component.text("F T", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("H", mJade)).append(
					Component.text("E ", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("T", mJade)).append(
					Component.text("APE", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("STRY HAS EN", mJade)).append(
					Component.text("DOWED US WITH THEIR COUN", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("TENANCE. AH", mJade)).append(
					Component.text("HHH... ", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("THI", mJade)).append(
					Component.text("S D", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("AN", mJade)).append(
					Component.text("CE ", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("SHALL BE", mJade)).append(
					Component.text("EXE", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("MPL", mJade)).append(
					Component.text("AR", mJade, TextDecoration.OBFUSCATED)).append(
					Component.text("Y.", mJade)));
			phaseTransition(20, 1.5, mIgnoredMats, phaseChangeActive, activeSpells3, passiveSpells);
		});
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(activeSpells1, passiveSpells, detectionRange, bossBar);

		new BukkitRunnable() {

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
				mCoefficient = BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);
			}
		}.runTaskTimer(mPlugin, 0, 100);
	}

	private void phaseTransition(int radius, double initKBSpeed, EnumSet<Material> mIgnoredMats, SpellManager phaseChangeActive,
	                             SpellManager activeSpells, List<Spell> passiveSpells) {
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 1.0f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2.0f, 0f);

		// knockaway
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), radius, true)) {
			double distance = player.getLocation().distance(bossLoc);
			double speed = initKBSpeed * (1 - distance / radius);
			MovementUtils.knockAway(mBoss.getLocation(), player, (float) speed, false);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 1));
		}

		// replace blocks -> teleport to random player
		setBlocksCircle(bossLoc, radius, Material.MAGMA_BLOCK, mIgnoredMats);
		new BukkitRunnable() {
			int mInc = 0;
			@Override public void run() {
				world.playSound(bossLoc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 1f, 0.7f);
				for (Player p : PlayerUtils.playersInRange(bossLoc, radius, true)) {
					p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.HOSTILE, 0.8f, 0.8f);
					// should not die to phase change (1% max hp)
					BossUtils.bossDamagePercent(mBoss, p, 0.01, "Burning Ground");
				}
				if (mInc == 4) {
					world.playSound(bossLoc, Sound.BLOCK_LAVA_AMBIENT, SoundCategory.HOSTILE, 1.5f, 0.8f);
					setBlocksCircle(bossLoc, radius, Material.LAVA, mIgnoredMats);
					changePhase(phaseChangeActive, Collections.emptyList(), null);
					forceCastSpell(SpellTpBehindPlayer.class);
				}
				if (mInc == 8) {
					changePhase(activeSpells, passiveSpells, null);
					this.cancel();
				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mInc++;
			}
		}.runTaskTimer(mPlugin, 20, 20);
	}

	private void setBlocksCircle(Location bossLoc, int radius, Material mat, EnumSet<Material> mIgnoredMats) {
		new BukkitRunnable() {
			double mRadius = 0.0;
			@Override public void run() {
				List<Block> blocks = new ArrayList<Block>();
				// take max for preventing div by 0
				// max radius = 32, 360 / (32 * 8)) > 1
				// min radius = 1, 360 / 8 < 360
				double inc = Math.min(360.0 / (Math.max(mRadius, 1) * 8.0), 360);
				//get valid blocks in circle
				for (double degrees = 0; degrees <= 360; degrees += inc) {
					// ignore 33% of blocks
					double rng = FastUtils.randomDoubleInRange(0, 1);
					if (rng <= 0.33) {
						continue;
					}
					// block under boss
					if (mRadius < 1) {
						Block block = getBlock(bossLoc.clone());
						if (block.isSolid()) {
							blocks.add(block);
						}
						break;
					}

					//remaining blocks
					Location bLoc = bossLoc.clone().add(FastUtils.cosDeg(degrees) * mRadius, 0, FastUtils.sinDeg(degrees) * mRadius);
					Block block = getBlock(bLoc);
					if (block.isSolid()) {
						// do not replace unbreakable blocks, shift up by 1
						if (mIgnoredMats.contains(block.getType())) {
							block = block.getLocation().add(0, 1, 0).getBlock();
						}
						blocks.add(block);
					}
				}
				// set valid blocks in list
				for (Block b : blocks) {
					b.setType(mat);
				}
				if (mRadius >= radius || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mRadius++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	//only top most block is replaced
	private Block getBlock(Location loc) {
		Block block = loc.getBlock(); // this is usually air or passable block, catch from previous function.
		for (int y = 10; y >= -5; y--) {
			block = loc.clone().add(0, y, 0).getBlock();
			if (block.isSolid()) {
				break;
			}
		}
		return block;
	}

	@Override
	public void onHurt(DamageEvent event) {
		event.setDamage(event.getDamage() / mCoefficient);
	}

	@Override
	public void init() {
		int hpDelta = 5000;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		mCoefficient = BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, hpDelta);
		mBoss.setHealth(hpDelta);

		//launch event related spawn commands
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			MessagingUtils.sendTitle(player, Component.text("C'Axtal", NamedTextColor.DARK_RED, TextDecoration.BOLD),
				Component.text("The Soulspeaker", NamedTextColor.RED, TextDecoration.BOLD));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, true, false, false));
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 100.0f, 0.8f);
			player.sendMessage(Component.text("She can't protect you forever, hero... Be warned...", NamedTextColor.DARK_RED));
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}