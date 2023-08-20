package com.playmonumenta.plugins.inventories;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class WalletManager implements Listener {

	private static final String KEY_PLUGIN_DATA = "Wallet";

	public static final ImmutableMap<Region, ItemStack> REGION_ICONS = ImmutableMap.of(
		Region.VALLEY, ItemUtils.parseItemStack("{id:\"minecraft:cyan_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"sc\",Color:3},{Pattern:\"mc\",Color:11},{Pattern:\"flo\",Color:15},{Pattern:\"bts\",Color:11},{Pattern:\"tts\",Color:11}]},HideFlags:63,display:{Name:'{\"text\":\"King\\'s Valley\",\"italic\":false,\"bold\":true,\"color\":\"aqua\"}'}}}"),
		Region.ISLES, ItemUtils.parseItemStack("{id:\"minecraft:green_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"gru\",Color:5},{Pattern:\"bo\",Color:13},{Pattern:\"mr\",Color:13},{Pattern:\"mc\",Color:5}]},HideFlags:63,display:{Name:'{\"text\":\"Celsian Isles\",\"italic\":false,\"bold\":true,\"color\":\"green\"}'}}}"),
		Region.RING, ItemUtils.parseItemStack("{id:\"minecraft:white_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"ss\",Color:12},{Pattern:\"bts\",Color:13},{Pattern:\"tts\",Color:13},{Pattern:\"gra\",Color:8},{Pattern:\"ms\",Color:13},{Pattern:\"gru\",Color:7},{Pattern:\"flo\",Color:15},{Pattern:\"mc\",Color:0}]},HideFlags:63,display:{Name:'{\"bold\":true,\"italic\":false,\"underlined\":false,\"color\":\"white\",\"text\":\"Architect\\\\u0027s Ring\"}'}}}")
	);

	private static @MonotonicNonNull ImmutableList<ItemStack> MANUAL_SORT_ORDER;

	private static @MonotonicNonNull ImmutableList<ItemStack> MAIN_CURRENCIES;

	/**
	 * Compressible currency items.
	 * If an item has multiple compression levels, they must be ordered from most compressed to least compressed.
	 * NB: some code depends on the assumption that only the base currencies have more than 2 tiers of compression.
	 */
	private static @MonotonicNonNull ImmutableList<CompressionInfo> COMPRESSIBLE_CURRENCIES;

	public static void initialize(Location loc) {

		MAIN_CURRENCIES = Stream.of(
				// r1
				"epic:r1/items/currency/hyper_experience",
				"epic:r1/items/currency/concentrated_experience",
				"epic:r1/items/currency/experience",

				// r2
				"epic:r2/items/currency/hyper_crystalline_shard",
				"epic:r2/items/currency/compressed_crystalline_shard",
				"epic:r2/items/currency/crystalline_shard",

				// r3
				"epic:r3/items/currency/hyperchromatic_archos_ring",
				"epic:r3/items/currency/archos_ring"
			).map(path -> InventoryUtils.getItemFromLootTableOrWarn(loc, NamespacedKeyUtils.fromString(path)))
			                  .filter(Objects::nonNull) // for build shard
			                  .collect(ImmutableList.toImmutableList());

		MANUAL_SORT_ORDER = Stream.of(
				// r1
				"epic:r1/items/currency/pulsating_gold_bar",
				"epic:r1/items/currency/pulsating_gold",
				"epic:r1/fragments/royal_dust",
				"epic:r1/transmog/rare_frag",
				"epic:r1/items/currency/pulsating_dust",
				"epic:r1/items/currency/pulsating_dust_frag",

				// r2
				"epic:r2/items/currency/pulsating_emerald_block",
				"epic:r2/items/currency/pulsating_emerald",
				"epic:r2/fragments/gleaming_dust",
				"epic:r2/transmog/rare_frag",
				"epic:r2/transmog/pulsating_powder",
				"epic:r2/transmog/t5_frag",

				// r3
				"epic:r3/items/currency/pulsating_diamond",
				"epic:r3/items/currency/silver_dust",
				"epic:r3/transmog/melted_candle",
				"epic:r3/items/currency/alacrity_augment",
				"epic:r3/items/currency/fortitude_augment",
				"epic:r3/items/currency/potency_augment",
				"epic:r3/items/currency/pulsating_shard",
				"epic:r3/transmog/pulsating_shard_fragment"
			).map(path -> InventoryUtils.getItemFromLootTableOrWarn(loc, NamespacedKeyUtils.fromString(path)))
			                    .filter(Objects::nonNull) // for build shard
			                    .collect(ImmutableList.toImmutableList());

		COMPRESSIBLE_CURRENCIES = Stream.of(
			// r1
			CompressionInfo.create("epic:r1/items/currency/hyper_experience",
				"epic:r1/items/currency/experience", 64 * 8, loc),
			CompressionInfo.create("epic:r1/items/currency/concentrated_experience",
				"epic:r1/items/currency/experience", 8, loc),

			// r2
			CompressionInfo.create("epic:r2/items/currency/hyper_crystalline_shard",
				"epic:r2/items/currency/crystalline_shard", 64 * 8, loc),
			CompressionInfo.create("epic:r2/items/currency/compressed_crystalline_shard",
				"epic:r2/items/currency/crystalline_shard", 8, loc),

			// r3
			CompressionInfo.create("epic:r3/items/currency/hyperchromatic_archos_ring",
				"epic:r3/items/currency/archos_ring", 64, loc),

			// misc
			CompressionInfo.create("epic:soul/twisted_soul_thread",
				"epic:soul/soul_thread", 8, loc),
			CompressionInfo.create("epic:r2/blackmist/spectral_maradevi",
				"epic:r2/blackmist/spectral_doubloon", 8, loc),
			CompressionInfo.create("epic:r2/items/currency/carnival_ticket_wheel",
				"epic:r2/items/currency/carnival_ticket_bundle", 10, loc),
			CompressionInfo.create("epic:r2/dungeons/rushdown/a_dis_energy",
				"epic:r2/dungeons/rushdown/dis_energy", 100, loc)
		).filter(Objects::nonNull).collect(ImmutableList.toImmutableList());
	}

	private record WalletSettings(Region mMaxRegion, boolean allCurrencies) {
	}

	private static final WalletSettings MAX_SETTINGS = new WalletSettings(Region.RING, true);
	private static final ImmutableMap<ItemUtils.ItemIdentifier, WalletSettings> WALLET_SETTINGS = ImmutableMap.of(
		new ItemUtils.ItemIdentifier(Material.GLASS_BOTTLE, "Experience Flask"), new WalletSettings(Region.VALLEY, false),
		new ItemUtils.ItemIdentifier(Material.CAULDRON, "Experience Bucket"), new WalletSettings(Region.VALLEY, true),
		new ItemUtils.ItemIdentifier(Material.AMETHYST_CLUSTER, "Crystal Cluster"), new WalletSettings(Region.ISLES, false),
		new ItemUtils.ItemIdentifier(Material.AMETHYST_BLOCK, "Crystal Collector"), new WalletSettings(Region.ISLES, true),
		new ItemUtils.ItemIdentifier(Material.PORKCHOP, "Piggy Bank"), new WalletSettings(Region.RING, false),
		new ItemUtils.ItemIdentifier(Material.FLOWER_POT, "Bag of Hoarding"), MAX_SETTINGS
	);

	private static final Map<UUID, Wallet> mWallets = new HashMap<>();

	private static class CompressionInfo {
		private final ItemStack mCompressed;
		private final ItemStack mBase;
		private final int mAmount;

		CompressionInfo(ItemStack compressed, ItemStack base, int amount) {
			mCompressed = compressed;
			mBase = base;
			mAmount = amount;
		}

		static @Nullable CompressionInfo create(String compressed, String base, int amount, Location loc) {
			ItemStack compressedItem = InventoryUtils.getItemFromLootTableOrWarn(loc, NamespacedKeyUtils.fromString(compressed));
			if (compressedItem == null) {
				return null;
			}
			ItemStack baseItem = InventoryUtils.getItemFromLootTableOrWarn(loc, NamespacedKeyUtils.fromString(base));
			if (baseItem == null) {
				return null;
			}
			return new CompressionInfo(compressedItem, baseItem, amount);
		}
	}

	private static class WalletItem {
		final ItemStack mItem;
		long mAmount;

		public WalletItem(ItemStack item, long amount) {
			mItem = item;
			mAmount = amount;
		}

		JsonObject serialize() {
			JsonObject json = new JsonObject();
			json.addProperty("item", ItemUtils.serializeItemStack(mItem));
			json.addProperty("amount", mAmount);
			return json;
		}

		static WalletItem deserialize(JsonObject json) {
			return new WalletItem(ItemUtils.parseItemStack(json.get("item").getAsString()), json.get("amount").getAsLong());
		}
	}

	private static class Wallet {
		final UUID mOwner;
		final ArrayList<WalletItem> mItems = new ArrayList<>();

		Wallet(UUID owner) {
			mOwner = owner;
		}

		private @Nullable WalletItem find(ItemStack currency) {
			for (WalletItem walletItem : mItems) {
				if (walletItem.mItem.isSimilar(currency)) {
					return walletItem;
				}
			}
			return null;
		}

		private void add(Player player, ItemStack currency) {
			if (ItemUtils.isNullOrAir(currency)) {
				throw new IllegalArgumentException("Tried to add air to wallet!");
			}

			// Compressed currencies are stored uncompressed
			CompressionInfo compressionInfo = getCompressionInfo(currency);
			if (compressionInfo != null) {
				int compressedAmount = currency.getAmount();
				currency.setAmount(0);
				ItemStack baseCurrency = ItemUtils.clone(compressionInfo.mBase);
				baseCurrency.setAmount(compressedAmount * compressionInfo.mAmount);
				add(player, baseCurrency);
				return;
			}

			if (!player.getUniqueId().equals(mOwner)) {
				AuditListener.log("+AddItemToWallet: " + player.getName() + " added to wallet of "
					                  + MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(currency));
			}

			for (WalletItem walletItem : mItems) {
				if (walletItem.mItem.isSimilar(currency)) {
					walletItem.mAmount += currency.getAmount();
					currency.setAmount(0);
					return;
				}
			}

			ItemStack clone = ItemUtils.clone(currency);
			clone.setAmount(1);
			mItems.add(new WalletItem(clone, currency.getAmount()));
			currency.setAmount(0);
		}

		private long count(ItemStack currency) {
			CompressionInfo compressionInfo = getCompressionInfo(currency);
			if (compressionInfo != null) {
				return count(compressionInfo.mBase) * compressionInfo.mAmount;
			}
			WalletItem walletItem = find(currency);
			return walletItem != null ? walletItem.mAmount : 0;
		}

		private void remove(Player player, ItemStack currency) {

			// If removing a compressed item, we need to actually remove uncompressed items as only those are stored.
			CompressionInfo compressionInfo = getCompressionInfo(currency);
			if (compressionInfo != null) {
				long toRemove = (long) currency.getAmount() * compressionInfo.mAmount;
				for (Iterator<WalletItem> iterator = mItems.iterator(); iterator.hasNext(); ) {
					WalletItem walletItem = iterator.next();
					if (walletItem.mItem.isSimilar(compressionInfo.mBase)) {
						if (walletItem.mAmount > toRemove) {
							walletItem.mAmount -= toRemove;
						} else {
							// Round down to the nearest multiple of compressionInfo.mAmount
							long adjustedContainedAmount = (walletItem.mAmount / compressionInfo.mAmount) * compressionInfo.mAmount;
							if (adjustedContainedAmount == walletItem.mAmount) {
								iterator.remove();
							} else {
								walletItem.mAmount -= adjustedContainedAmount;
							}
							currency.setAmount((int) (adjustedContainedAmount / compressionInfo.mAmount));
						}
						if (currency.getAmount() > 0 && !player.getUniqueId().equals(mOwner)) {
							AuditListener.log("-RemoveItemFromWallet: " + player.getName() + " removed from wallet of "
								                  + MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(currency));
						}
						return;
					}
				}
				// fall through to normal case in case a compression was added later on
			}

			// Normal item case
			for (Iterator<WalletItem> iterator = mItems.iterator(); iterator.hasNext(); ) {
				WalletItem walletItem = iterator.next();
				if (walletItem.mItem.isSimilar(currency)) {
					if (currency.getAmount() >= walletItem.mAmount) {
						currency.setAmount((int) walletItem.mAmount);
						iterator.remove();
					} else {
						walletItem.mAmount -= currency.getAmount();
					}
					if (currency.getAmount() > 0 && !player.getUniqueId().equals(mOwner)) {
						AuditListener.log("-RemoveItemFromWallet: " + player.getName() + " removed from wallet of "
							                  + MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(currency));
					}
					return;
				}
			}

			currency.setAmount(0);
		}

		JsonObject serialize() {
			JsonObject json = new JsonObject();
			JsonArray itemsArray = new JsonArray();
			for (WalletItem item : mItems) {
				itemsArray.add(item.serialize());
			}
			json.add("items", itemsArray);
			return json;
		}

		static Wallet deserialize(Player player, JsonObject json) {
			Wallet wallet = new Wallet(player.getUniqueId());
			for (JsonElement item : json.getAsJsonArray("items")) {
				wallet.mItems.add(WalletItem.deserialize(item.getAsJsonObject()));
			}
			wallet.mItems.removeIf(item -> ItemUtils.isNullOrAir(item.mItem)); // item has been removed from the game
			return wallet;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		JsonObject walletData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_PLUGIN_DATA);
		if (walletData != null) {
			mWallets.put(player.getUniqueId(), Wallet.deserialize(player, walletData));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		Wallet wallet = mWallets.get(player.getUniqueId());
		if (wallet != null) {
			event.setPluginData(KEY_PLUGIN_DATA, wallet.serialize());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!p.isOnline()) {
				mWallets.remove(p.getUniqueId());
			}
		}, 100);
	}

	private static class WalletGui extends Gui {
		private final Wallet mWallet;
		private final WalletSettings mSettings;
		private final String mPlainName;
		private int mPage;

		public WalletGui(Player player, Wallet wallet, WalletSettings settings, Component displayName) {
			super(player, 6 * 9, displayName);
			mWallet = wallet;
			mSettings = settings;
			mPlainName = MessagingUtils.plainText(displayName);
			setFiller(Material.BLACK_STAINED_GLASS_PANE);
		}

		@Override
		// the loop is exited after the modification, so no CME can happen
		@SuppressWarnings("ModifyCollectionInEnhancedForLoop")
		protected void setup() {

			List<WalletItem> walletItemsCopy = new ArrayList<>(
				mWallet.mItems.stream()
					.map(item -> new WalletItem(ItemUtils.clone(item.mItem), item.mAmount))
					.toList());

			// Add compressed currencies as fake items to be able to retrieve them
			ItemStack lastUsedBase = null;
			ciLoop:
			for (CompressionInfo ci : COMPRESSIBLE_CURRENCIES) {
				for (WalletItem baseWalletItem : walletItemsCopy) {
					if (baseWalletItem.mItem.isSimilar(ci.mBase)) {
						long baseAmount = baseWalletItem.mAmount;
						if (baseAmount >= ci.mAmount || (lastUsedBase != null && lastUsedBase.isSimilar(ci.mBase))) {
							baseWalletItem.mAmount = baseAmount % ci.mAmount;
							lastUsedBase = ci.mBase;
							walletItemsCopy.add(new WalletItem(ItemUtils.clone(ci.mCompressed), baseAmount / ci.mAmount));
							continue ciLoop;
						}
					}
				}
			}

			// Items grouped by region, and sorted within each region
			Map<Region, List<WalletItem>> items =
				walletItemsCopy.stream()
					.sorted(
						// sort main currencies to the very front
						Comparator.comparing((WalletItem item) -> {
								int index = MAIN_CURRENCIES.indexOf(item.mItem);
								return index < 0 ? Integer.MAX_VALUE : index;
							})
							// then sort by location
							.thenComparing((WalletItem item) -> ItemStatUtils.getLocation(item.mItem))
							// then by manual sort order (and manually sorted items are the first in their location)
							.thenComparing((WalletItem item) -> {
								int index = MANUAL_SORT_ORDER.indexOf(item.mItem);
								return index < 0 ? Integer.MAX_VALUE : index;
							})
							// sort compressible currencies from most valuable to least valuable
							.thenComparing((WalletItem item) -> {
								CompressionInfo compressionInfo = getCompressionInfo(item.mItem);
								if (compressionInfo != null) {
									return -compressionInfo.mAmount;
								}
								return COMPRESSIBLE_CURRENCIES.stream().anyMatch(ci -> ci.mBase.isSimilar(item.mItem)) ? 0 : 1;
							})
							// finally, sort by name
							.thenComparing((WalletItem item) -> ItemUtils.getPlainNameIfExists(item.mItem)))
					// group everything by region
					.collect(Collectors.groupingBy((WalletItem item) -> MAIN_CURRENCIES.contains(item.mItem) ? Region.NONE : ItemStatUtils.getRegion(item.mItem)));

			// Fill GUI with items
			boolean showAmounts = mPlayer.getScoreboardTags().contains(CustomContainerItemManager.SHOW_AMOUNTS_TAG);
			boolean showAmountsAsStacks = mPlayer.getScoreboardTags().contains(CustomContainerItemManager.SHOW_AMOUNTS_AS_STACKS_TAG);
			int pos = 0;
			int itemsPerPage = 5 * 8; // top row and left column reserved
			for (Region region : Region.values()) {
				List<WalletItem> regionItems = items.get(region);
				if (regionItems == null) {
					continue;
				}
				boolean firstOfRegion = true;
				pos = pos % 8 == 0 ? pos : pos + 8 - (pos % 8); // start new region on new line
				for (WalletItem item : regionItems) {
					int posInPage = pos - itemsPerPage * mPage;
					if (posInPage < 0 || posInPage >= itemsPerPage) {
						pos++;
						firstOfRegion = true; // always place a region icon at the start of a new page
						continue;
					}
					if (firstOfRegion && REGION_ICONS.containsKey(region)) {
						setItem(9 + posInPage + posInPage / 8, REGION_ICONS.get(region));
					}
					ItemStack displayItem = ItemUtils.clone(item.mItem);
					ItemMeta itemMeta = displayItem.getItemMeta();
					if (itemMeta != null) {
						String amount;
						if (showAmountsAsStacks && item.mItem.getMaxStackSize() > 1 && item.mAmount >= item.mItem.getMaxStackSize()) {
							long stacks = item.mAmount / item.mItem.getMaxStackSize();
							long remaining = item.mAmount % item.mItem.getMaxStackSize();
							amount = item.mAmount + " (" + stacks + " stack" + (stacks == 1 ? "" : "s") + (remaining == 0 ? "" : " + " + remaining) + ")";
						} else {
							amount = "" + item.mAmount;
						}
						Component name = Component.text(amount + " ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
							                 .append(ItemUtils.getDisplayName(item.mItem));
						for (CompressionInfo compressionInfo : COMPRESSIBLE_CURRENCIES) {
							boolean isBase = compressionInfo.mBase.isSimilar(item.mItem);
							if (isBase || compressionInfo.mCompressed.isSimilar(item.mItem)) {
								long count = isBase ? mWallet.count(compressionInfo.mBase) : mWallet.count(compressionInfo.mBase) / compressionInfo.mAmount;
								if (count != item.mAmount) {
									name = name.append(Component.text(" (" + count + " total)", NamedTextColor.GOLD));
								}
								break;
							}
						}
						itemMeta.displayName(name);
						displayItem.setItemMeta(itemMeta);
					}
					if (showAmounts) {
						displayItem.setAmount((int) Math.max(1, Math.min(64, showAmountsAsStacks ? item.mAmount / item.mItem.getMaxStackSize() : item.mAmount)));
					}
					setItem(10 + posInPage + posInPage / 8, new GuiItem(displayItem, false))
						.onClick(event -> {
							ItemStack movedItem = ItemUtils.clone(item.mItem);
							switch (event.getClick()) {
								case LEFT, SHIFT_LEFT -> {
									int maxFit = InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory());
									if (maxFit > 0) {
										movedItem.setAmount(event.getClick() == ClickType.LEFT ? Math.min(movedItem.getMaxStackSize(), maxFit) : maxFit);
										mWallet.remove(mPlayer, movedItem);
										mPlayer.getInventory().addItem(movedItem);
									}
									update();
								}
								case RIGHT, SHIFT_RIGHT -> {
									movedItem.setAmount(1);
									if (InventoryUtils.canFitInInventory(movedItem, mPlayer.getInventory())) {
										mWallet.remove(mPlayer, movedItem);
										mPlayer.getInventory().addItem(movedItem);
										update();
									}
								}
								case SWAP_OFFHAND -> {
									close();
									SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter how many", "items to retrieve"))
										.response((player, lines) -> {
											double retrievedAmount;
											try {
												retrievedAmount = lines[0].isEmpty() ? 0 : parseDoubleOrCalculation(lines[0]);
											} catch (NumberFormatException e) {
												player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
												return false;
											}
											if (retrievedAmount < 0) {
												player.sendMessage(Component.text("Please enter a positive number.", NamedTextColor.RED));
												return false;
											}
											CompressionInfo compressionInfo = getCompressionInfo(item.mItem);
											long countInWallet = mWallet.count(compressionInfo == null ? movedItem : compressionInfo.mBase);
											long desiredAmount = (long) Math.ceil(compressionInfo == null ? retrievedAmount : retrievedAmount * compressionInfo.mAmount);

											// Warn if not enough and exit (to not take out less than expected if not double-checked)
											if (desiredAmount > countInWallet) {
												player.sendMessage(Component.text("There are fewer than the requested amount of items in the bag.", NamedTextColor.RED));
												player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
												open();
												return true;
											}
											if (desiredAmount >= Integer.MAX_VALUE) { // no. even if you really have that many.
												return false;
											}

											ItemStack base = compressionInfo == null ? item.mItem : compressionInfo.mBase;
											// If retrieving items that can be compressed, take out the appropriate amount of compressed/uncompressed
											// Requires special handling for the inventory space calculation in case of partial stacks
											if (isBase(base)) {
												int leftToRemove = (int) desiredAmount;
												List<ItemStack> result = new ArrayList<>();
												for (CompressionInfo ci : COMPRESSIBLE_CURRENCIES) {
													if (ci.mBase.isSimilar(base)) {
														ItemStack res = ItemUtils.clone(ci.mCompressed);
														int compressed = leftToRemove / ci.mAmount;
														res.setAmount(compressed);
														result.add(res);
														leftToRemove -= compressed * ci.mAmount;
														if (leftToRemove == 0) {
															break;
														}
													}
												}
												if (leftToRemove > 0) {
													ItemStack res = ItemUtils.clone(base);
													res.setAmount(leftToRemove);
													result.add(res);
												}
												if (InventoryUtils.canFitInInventory(result, player.getInventory())) {
													ItemStack baseClone = ItemUtils.clone(base);
													baseClone.setAmount((int) desiredAmount);
													mWallet.remove(mPlayer, baseClone);
													result.forEach(mPlayer.getInventory()::addItem);
												} else {
													player.sendMessage(Component.text("Not enough space in inventory for all items. No items have been retrieved.", NamedTextColor.RED));
													player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
												}
											} else {
												// normal case
												if (desiredAmount <= InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory())) {
													movedItem.setAmount((int) desiredAmount);
													mWallet.remove(mPlayer, movedItem);
													mPlayer.getInventory().addItem(movedItem);
												} else {
													player.sendMessage(Component.text("Not enough space in inventory for all items. No items have been retrieved.", NamedTextColor.RED));
													player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
												}
											}
											open();
											return true;
										})
										.reopenIfFail(true)
										.open(mPlayer);
								}
								default -> {
									// Are you happy now, PMD?
								}
							}
						});
					pos++;
					firstOfRegion = false;
				}
			}

			// page arrows and info item
			if (mPage > 0) {
				ItemStack previousPageIcon = new ItemStack(Material.ARROW);
				ItemMeta itemMeta = previousPageIcon.getItemMeta();
				itemMeta.displayName(Component.text("Previous Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
				previousPageIcon.setItemMeta(itemMeta);
				setItem(0, previousPageIcon)
					.onLeftClick(() -> {
						mPage--;
						update();
					});
			}
			{
				ItemStack infoIcon = new ItemStack(Material.DARK_OAK_SIGN);
				ItemMeta itemMeta = infoIcon.getItemMeta();
				itemMeta.displayName(Component.text(mPlainName + " Info", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
				itemMeta.lore(List.of(
					Component.text("Left click here to toggle displaying item counts.", NamedTextColor.WHITE)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("Right click here to toggle showing counts in stacks.", NamedTextColor.WHITE)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("Click on currency items in your inventory to store them.", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("Shift click to store all of the same type.", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("Click on items in the " + mPlainName + " to retrieve them:", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false),
					Component.text(" - ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
						.append(Component.text("Left Click", NamedTextColor.WHITE))
						.append(Component.text(" to retrieve up to a stack", NamedTextColor.GRAY)),
					Component.text(" - ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
						.append(Component.text("Right Click", NamedTextColor.WHITE))
						.append(Component.text(" to retrieve one item only", NamedTextColor.GRAY)),
					Component.text(" - ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
						.append(Component.text("Shift + Left Click", NamedTextColor.WHITE))
						.append(Component.text(" to retrieve everything", NamedTextColor.GRAY)),
					Component.text(" - ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
						.append(Component.keybind("key.swapOffhand", NamedTextColor.WHITE))
						.append(Component.text(" to retrieve a custom amount", NamedTextColor.GRAY)),
					Component.text("   (may be a fraction or simple calculation)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
				));
				infoIcon.setItemMeta(itemMeta);
				setItem(4, infoIcon)
					.onLeftClick(() -> {
						ScoreboardUtils.toggleTag(mPlayer, CustomContainerItemManager.SHOW_AMOUNTS_TAG);
						update();
					})
					.onRightClick(() -> {
						ScoreboardUtils.toggleTag(mPlayer, CustomContainerItemManager.SHOW_AMOUNTS_AS_STACKS_TAG);
						update();
					});
			}
			if (pos > itemsPerPage * (mPage + 1)) {
				ItemStack nextPageIcon = new ItemStack(Material.ARROW);
				ItemMeta itemMeta = nextPageIcon.getItemMeta();
				itemMeta.displayName(Component.text("Next Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
				nextPageIcon.setItemMeta(itemMeta);
				setItem(8, nextPageIcon)
					.onLeftClick(() -> {
						mPage++;
						update();
					});
			}
		}

		@Override
		protected boolean onGuiClick(InventoryClickEvent event) {
			if (Bukkit.getPlayer(mWallet.mOwner) == null) {
				mPlayer.sendMessage(Component.text("Player has logged off!", NamedTextColor.RED));
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				mPlayer.closeInventory();
				return false;
			}
			return true;
		}

		@Override
		protected void onPlayerInventoryClick(InventoryClickEvent event) {
			if (Bukkit.getPlayer(mWallet.mOwner) == null) {
				mPlayer.sendMessage(Component.text("Player has logged off!", NamedTextColor.RED));
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				mPlayer.closeInventory();
				return;
			}
			ItemStack currentItem = event.getCurrentItem();
			if (event.getClick() == ClickType.LEFT
				    && canPutIntoWallet(currentItem, mSettings)) {
				mWallet.add(mPlayer, currentItem);
				update();
			} else if (event.getClick() == ClickType.SHIFT_LEFT && canPutIntoWallet(currentItem, mSettings)) {
				ItemStack combinedItems = ItemUtils.clone(currentItem);
				currentItem.setAmount(0);
				for (ItemStack item : mPlayer.getInventory().getStorageContents()) {
					if (item != null && item.isSimilar(combinedItems)) {
						combinedItems.setAmount(combinedItems.getAmount() + item.getAmount());
						item.setAmount(0);
					}
				}
				mWallet.add(mPlayer, combinedItems);
				update();
			} else if ((event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) && canPutIntoWallet(currentItem, mSettings)) {
				ItemStack oneItem = ItemUtils.clone(currentItem);
				currentItem.setAmount(currentItem.getAmount() - 1);
				oneItem.setAmount(1);
				mWallet.add(mPlayer, oneItem);
				update();
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		WalletSettings settings = getSettings(event.getCurrentItem());
		if (settings != null
			    && event.getCurrentItem().getAmount() == 1
			    && event.getWhoClicked() instanceof Player player
			    && player.hasPermission("monumenta.usewallet")) {

			ItemStack walletItem = event.getCurrentItem();

			Wallet wallet = mWallets.computeIfAbsent(player.getUniqueId(), Wallet::new);

			if (ItemUtils.isNullOrAir(event.getCursor())) {
				if (event.getClick() == ClickType.RIGHT) {
					// open wallet
					event.setCancelled(true);
					if (checkNotSoulbound(player, walletItem)) {
						return;
					}
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
					new WalletGui(player, wallet, settings, walletItem.getItemMeta().displayName()).open();
				} else if (event.getClick() == ClickType.SWAP_OFFHAND) {
					// quick-fill wallet
					event.setCancelled(true);
					GUIUtils.refreshOffhand(event);
					if (checkNotSoulbound(player, walletItem)) {
						return;
					}
					int deposited = 0;
					Map<String, Integer> depositedItems = new TreeMap<>();
					PlayerInventory inventory = player.getInventory();
					for (int i = 0; i < inventory.getSize(); i++) {
						ItemStack item = inventory.getItem(i);
						if (canPutIntoWallet(item, settings)) {
							deposited += item.getAmount();
							depositedItems.merge(ItemUtils.getPlainName(item), item.getAmount(), Integer::sum);
							wallet.add(player, item);
						}
					}
					if (deposited > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
						player.sendMessage(Component.text(deposited + " item" + (deposited == 1 ? "" : "s") + " deposited into your " + ItemUtils.getPlainName(walletItem), NamedTextColor.GOLD)
							                   .hoverEvent(HoverEvent.showText(Component.text(
								                   depositedItems.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
									                   .collect(Collectors.joining("\n")), NamedTextColor.GRAY))));
					}
				}
			} else {
				if (event.getClick() == ClickType.RIGHT) {
					event.setCancelled(true);
					if (checkNotSoulbound(player, walletItem)) {
						return;
					}
					ItemStack cursor = event.getCursor();
					if (canPutIntoWallet(cursor, settings)) {
						wallet.add(player, cursor);
						event.getView().setCursor(cursor);
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
						player.sendMessage(Component.text("Item deposited into your " + ItemUtils.getPlainName(walletItem), NamedTextColor.GOLD));
					} else {
						player.sendMessage(Component.text("This item cannot be put into the " + ItemUtils.getPlainName(walletItem), NamedTextColor.RED));
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if (isWallet(event.getItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (isWallet(event.getItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof ItemFrame)
			    && !(event.getRightClicked() instanceof ArmorStand)
			    && isWallet(event.getPlayer().getEquipment().getItem(event.getHand()))) {
			event.setCancelled(true);
		}
	}

	private static boolean checkNotSoulbound(Player player, ItemStack item) {
		if (ItemStatUtils.getInfusionLevel(item, InfusionType.SOULBOUND) > 0
			    && !player.getUniqueId().equals(ItemStatUtils.getInfuser(item, InfusionType.SOULBOUND))) {
			player.sendMessage(Component.text("This " + ItemUtils.getPlainName(item) + " does not belong to you!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return true;
		}
		return false;
	}

	public static boolean canPutIntoWallet(ItemStack item, WalletSettings settings) {
		return item != null
			       && item.getAmount() > 0
			       && (ItemStatUtils.getTier(item) == Tier.CURRENCY
				           || ItemStatUtils.getTier(item) == Tier.EVENT_CURRENCY
				           || InventoryUtils.testForItemWithLore(item, "Can be put into a wallet."))
			       && ItemStatUtils.getPlayerModified(new NBTItem(item)) == null
			       && ItemStatUtils.getRegion(item).compareTo(settings.mMaxRegion) <= 0
			       && (settings.allCurrencies || ItemStatUtils.getRegion(item).compareTo(settings.mMaxRegion) < 0 || MAIN_CURRENCIES.stream().anyMatch(c -> c.isSimilar(item)));
	}

	private static boolean isBase(ItemStack item) {
		for (CompressionInfo compressionInfo : COMPRESSIBLE_CURRENCIES) {
			if (compressionInfo.mBase.isSimilar(item)) {
				return true;
			}
		}
		return false;
	}
	private static @Nullable CompressionInfo getCompressionInfo(ItemStack item) {
		for (CompressionInfo compressionInfo : COMPRESSIBLE_CURRENCIES) {
			if (compressionInfo.mCompressed.isSimilar(item)) {
				return compressionInfo;
			}
		}
		return null;
	}

	@Contract("null -> null")
	private static @Nullable WalletManager.WalletSettings getSettings(@Nullable ItemStack item) {
		return item == null ? null : WALLET_SETTINGS.get(ItemUtils.getIdentifier(item, false));
	}

	public static boolean isWallet(@Nullable ItemStack itemStack) {
		return getSettings(itemStack) != null;
	}

	public static void registerCommand() {
		new CommandAPICommand("openwallet")
			.withPermission("monumenta.command.openwallet")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player viewer = CommandUtils.getPlayerFromSender(sender);
				Player viewee = (Player) args[0];
				new WalletGui(viewer, mWallets.computeIfAbsent(viewee.getUniqueId(), Wallet::new), MAX_SETTINGS,
					Component.text("Wallet of ", NamedTextColor.GOLD).append(viewee.displayName())).open();
			})
			.register();
	}

	private static final Pattern OPERATION_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([-+*/])\\s*(\\d+(?:\\.\\d+)?)");

	public static double parseDoubleOrCalculation(String line) throws NumberFormatException {
		try {
			return Double.parseDouble(line);
		} catch (NumberFormatException e) {
			Matcher matcher = OPERATION_PATTERN.matcher(line);
			if (matcher.matches()) {
				double n1 = Double.parseDouble(matcher.group(1));
				double n2 = Double.parseDouble(matcher.group(3));
				return switch (matcher.group(2)) {
					case "-" -> n1 - n2;
					case "+" -> n1 + n2;
					case "*" -> n1 * n2;
					case "/" -> n1 / n2;
					default -> 0;
				};
			} else {
				throw e;
			}
		}
	}

}
