package com.playmonumenta.plugins.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritIce;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.windwalker.OneWithTheWind;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles communication with an (optional) client mod.
 */
public class ClientModHandler {

	public static final String CHANNEL_ID = "monumenta:client_channel_v1";

	private static @MonotonicNonNull ClientModHandler INSTANCE = null;

	private final MonumentaClasses mClasses;

	private final Plugin mPlugin;

	private final Gson mGson;

	public ClientModHandler(Plugin plugin) {
		this.mPlugin = plugin;
		mGson = new GsonBuilder().create();
		Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_ID);
		mClasses = new MonumentaClasses();
		INSTANCE = this;
	}

	public static void updateAbility(Player player, ClassAbility classAbility) {
		if (INSTANCE == null) {
			return;
		}
		Ability ability = INSTANCE.mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilityIgnoringSilence(classAbility);
		if (ability == null) {
			return;
		}
		updateAbility(player, ability);
	}

	/**
	 * Sends an ability update to the player.
	 * <p>
	 * Does nothing if the player does not have a compatible client mod installed, or if the ability makes no sense to send to clients (see {@link #shouldHandleAbility(Player, Ability)}).
	 */
	public static void updateAbility(Player player, Ability ability) {
		if (INSTANCE == null || !playerHasClientMod(player) || !shouldHandleAbility(player, ability)) {
			return;
		}
		ClassAbility classAbility = ability.getInfo().getLinkedSpell();
		int remainingCooldown = classAbility == null ? 0 : INSTANCE.mPlugin.mTimers.getCooldown(player.getUniqueId(), classAbility);
		int charges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getCharges() : 0;

		AbilityUpdatePacket packet = new AbilityUpdatePacket();
		packet.name = getAbilityName(ability);
		packet.remainingCooldown = remainingCooldown;
		packet.remainingCharges = charges;
		packet.mode = ability.getMode();
		packet.initialDuration = ability.getInitialDuration();
		packet.remainingDuration = ability.getRemainingDuration();
		INSTANCE.sendPacket(player, packet);
	}

	/**
	 * Sends a class update to the player.
	 * <p>
	 * Does nothing if the player does not have a compatible client mod installed.
	 */
	public static void updateAbilities(Player player) {
		if (INSTANCE == null || !playerHasClientMod(player)) {
			return;
		}

		ClassUpdatePacket.ClientModAbilityInfo[] abilities =
			INSTANCE.mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilitiesIgnoringSilence().stream()
				.filter(ability -> ClientModHandler.shouldHandleAbility(player, ability))
				.map(ability -> {
					ClassAbility classAbility = ability.getInfo().getLinkedSpell();
					int remainingCooldown = classAbility == null ? 0 : INSTANCE.mPlugin.mTimers.getCooldown(player.getUniqueId(), classAbility);
					int charges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getCharges() : 0;
					int maxCharges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getMaxCharges() : 0;

					ClassUpdatePacket.ClientModAbilityInfo info = new ClassUpdatePacket.ClientModAbilityInfo();
					info.name = getAbilityName(ability);
					info.className = getAbilityClassName(ability);
					info.remainingCooldown = remainingCooldown;
					info.initialCooldown = ability.getInfo().getModifiedCooldown(player, ability.getAbilityScore());
					info.remainingCharges = charges;
					info.maxCharges = maxCharges;
					info.mode = ability.getMode();
					info.initialDuration = ability.getInitialDuration();
					info.remainingDuration = ability.getRemainingDuration();
					return info;
				})
				.sorted(Comparator.comparing(i -> i.name == null ? "" : i.name))
				.toArray(ClassUpdatePacket.ClientModAbilityInfo[]::new);

		ClassUpdatePacket packet = new ClassUpdatePacket();
		packet.abilities = abilities;
		INSTANCE.sendPacket(player, packet);
	}

	public static void silenced(Player player, int duration) {
		if (INSTANCE == null) {
			return;
		}
		PlayerStatusPacket packet = new PlayerStatusPacket();
		packet.silenceDuration = duration;
		INSTANCE.sendPacket(player, packet);
	}

	public static void updateStrikeChests(Player player, int newLimit, @Nullable Integer count) {
		if (INSTANCE == null) {
			return;
		}
		StrikeChestUpdatePacket packet = new StrikeChestUpdatePacket();
		packet.newLimit = newLimit;
		packet.count = count;
		INSTANCE.sendPacket(player, packet);
	}

	private void sendPacket(Player player, Packet packet) {
		player.sendPluginMessage(mPlugin, CHANNEL_ID, mGson.toJson(packet).getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * @return Whether we're sending data for the given ability to clients
	 */
	private static boolean shouldHandleAbility(Player player, Ability ability) {
		return ability != null
			       && (ability.getInfo().getBaseCooldown(player, ability.getAbilityScore()) > 0 || ability instanceof AbilityWithChargesOrStacks
				           || ability instanceof AlchemicalArtillery || ability instanceof Swiftness || ability instanceof OneWithTheWind); // these are passives with modes
	}

	private static @Nullable String getAbilityName(Ability ability) {
		// The ClassAbility name is preferable if it exists (e.g. for the two Elemental Spirits)
		if (ability.getInfo().getLinkedSpell() != null) {
			return ability.getInfo().getLinkedSpell().getName();
		}
		return ability.getInfo().getDisplayName();
	}

	private static @Nullable String getAbilityClassName(Ability ability) {
		if (ability instanceof DepthsAbility) {
			DepthsTree depthsTree = ((DepthsAbility) ability).getInfo().getDepthsTree();
			if (depthsTree != null) {
				return depthsTree.getDisplayName();
			}
		}
		if (ability instanceof AlchemistPotions) {
			return "Alchemist";
		}
		if (ability instanceof ElementalSpiritIce) {
			return "Mage";
		}
		for (PlayerClass playerClass : INSTANCE.mClasses.mClasses) {
			Predicate<AbilityInfo<?>> sameClass = abi -> abi.getAbilityClass() == ability.getClass();
			if (playerClass.mAbilities.stream().anyMatch(sameClass)
				|| playerClass.mSpecOne.mAbilities.stream().anyMatch(sameClass)
				|| playerClass.mSpecTwo.mAbilities.stream().anyMatch(sameClass)) {
				return playerClass.mClassName;
			}
		}
		return null;
	}

	private static boolean playerHasClientMod(Player player) {
		return player.getListeningPluginChannels().contains(CHANNEL_ID);
	}

	// ------ packets ------

	private interface Packet {
	}

	/**
	 * Sent whenever a player's class is updated.
	 */
	@SuppressWarnings("unused")
	private static class ClassUpdatePacket implements Packet {

		final String _type = "ClassUpdatePacket";

		ClientModAbilityInfo @Nullable [] abilities;

		public static class ClientModAbilityInfo {

			@Nullable String name;
			@Nullable String className;

			int remainingCooldown;
			int initialCooldown;

			int remainingCharges;
			int maxCharges;

			@Nullable String mode;

			@Nullable Integer remainingDuration;
			@Nullable Integer initialDuration;

		}

	}

	/**
	 * Sent whenever an ability is used or changed in any way
	 */
	@SuppressWarnings("unused")
	private static class AbilityUpdatePacket implements Packet {

		final String _type = "AbilityUpdatePacket";

		@Nullable String name;

		// className is not required, as a player should never have multiple abilities with the same name

		int remainingCooldown;

		int remainingCharges;

		@Nullable String mode;

		@Nullable Integer remainingDuration;
		@Nullable Integer initialDuration;

	}

	/**
	 * Custom player status effects that effect skills
	 */
	@SuppressWarnings("unused")
	private static class PlayerStatusPacket implements Packet {

		final String _type = "PlayerStatusPacket";

		int silenceDuration;

	}


	/**
	 * Sent whenever the number of chests in a strike changes
	 */
	@SuppressWarnings("unused")
	private static class StrikeChestUpdatePacket implements Packet {

		final String _type = "StrikeChestUpdatePacket";

		int newLimit;

		@Nullable Integer count;

	}

}
