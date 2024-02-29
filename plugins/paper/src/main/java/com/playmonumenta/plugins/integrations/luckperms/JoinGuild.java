package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinGuild {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		// joinguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.joinguild");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.ManyPlayers("player"));

		new CommandAPICommand("joinguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandSender callee = sender;
				if (callee instanceof ProxiedCommandSender proxiedCommandSender) {
					callee = proxiedCommandSender.getCallee();
				}
				if (callee instanceof Player founder) {
					if (!ServerProperties.getShardName().contains("build")) {
						run(plugin, founder, (List<Player>) args[0]);
					}
				} else {
					callee.sendMessage(Component.text("This command may only be run as a player.", NamedTextColor.RED));
				}
			})
			.register();
	}

	private static void run(Plugin plugin, Player founder, List<Player> players) throws WrapperCommandSyntaxException {
		Group currentGuild = LuckPermsIntegration.getGuild(founder);
		String currentGuildName = LuckPermsIntegration.getGuildName(currentGuild);
		if (currentGuildName != null &&
			ScoreboardUtils.getScoreboardValue(founder, "Founder").orElse(0) != 1) {
			Component err = Component.text("You are not a founder of '" + currentGuildName + "' !", NamedTextColor.RED);
			throw CommandAPI.failWithAdventureComponent(err);
		}
		if (currentGuildName == null) {
			founder.sendMessage(Component.text("You are not currently in a guild.", NamedTextColor.RED));
			return;
		}
		players.removeIf(player -> founder.getName().equalsIgnoreCase(player.getName()));
		if (players.size() == 0) {
			founder.sendMessage(Component.text("No other players found on the pedestal to add to your guild.", NamedTextColor.RED));
		}

		// Check nearby players, add if not in guild and not founder of something
		for (Player p : players) {
			if (ScoreboardUtils.getScoreboardValue(p, "Founder").orElse(0) == 0) {
				Group group = LuckPermsIntegration.getGuild(p);
				if (group != null) {
					p.sendMessage(Component.text("You are already a part of another guild, please leave your current guild before trying again.", NamedTextColor.RED));
					continue;
				}
				// Add user to guild
				new BukkitRunnable() {
					@Override
					public void run() {
						User user = LuckPermsIntegration.UM.getUser(p.getUniqueId());
						if (user == null) {
							founder.sendMessage(Component.text("Failed to add " + p.getName() + " to your guild (luckperms error).", NamedTextColor.RED));
							p.sendMessage(Component.text("Failed to join guild (luckperms error).", NamedTextColor.RED));
							return;
						}
						user.data().add(InheritanceNode.builder(currentGuild).build());
						LuckPermsIntegration.UM.saveUser(user).whenComplete((unused, ex) -> {
							if (ex != null) {
								MessagingUtils.sendStackTrace(founder, ex);
								MessagingUtils.sendStackTrace(p, ex);
								ex.printStackTrace();
							} else {
								Bukkit.getScheduler().runTask(plugin, () -> {
									// Success indicators
									p.sendMessage(Component.text("Congratulations! You have joined " + currentGuildName + "!", NamedTextColor.GOLD));
									founder.sendMessage(Component.text(p.getName(), NamedTextColor.WHITE).append(Component.text(" has joined your guild", NamedTextColor.GOLD)));
									MonumentaNetworkChatIntegration.refreshPlayer(p);
									NmsUtils.getVersionAdapter().runConsoleCommandSilently(
										"execute at " + p.getName()
											+ " run summon minecraft:firework_rocket ~ ~1 ~ "
											+ "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");
								});
							}
						});
						LuckPermsIntegration.pushUserUpdate(user);
					}
				}.runTaskAsynchronously(plugin);
			} else {
				Group group = LuckPermsIntegration.getGuild(p);
				if (group != null) {
					p.sendMessage(Component.text("You are marked as a founder but have no current guild, please contact a moderator.", NamedTextColor.RED));
					continue;
				}
				p.sendMessage(Component.text("You are the founder of another guild, please leave your current guild before trying again.", NamedTextColor.RED));
			}
		}
	}
}
