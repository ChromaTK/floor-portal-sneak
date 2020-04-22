package com.playmonumenta.plugins.integrations.luckperms;

import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.User;

public class JoinGuild {
	public static void register(Plugin plugin, LuckPermsApi lp) {
		// joinguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.joinguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));

		CommandAPI.getInstance().register("joinguild", perms, arguments, (sender, args) -> {
			run(plugin, lp, (Player) args[0]);
		});
	}

	private static void run(Plugin plugin, LuckPermsApi lp, Player player) throws WrapperCommandSyntaxException {
		Group currentGuild = LuckPermsIntegration.getGuild(lp, player);
		String currentGuildName = LuckPermsIntegration.getGuildName(currentGuild);
		if (currentGuildName != null) {
			String err = ChatColor.RED + "You are already in the guild '" + currentGuildName + "' !";
			player.sendMessage(err);
			CommandAPI.fail(err);
		}

		// Check for nearby founder
		for (Player p : PlayerUtils.playersInRange(player, 1, false)) {
			if (ScoreboardUtils.getScoreboardValue(p, "Founder") == 1) {
				/* Nearby player is a founder - join to that guild */
				Group group = LuckPermsIntegration.getGuild(lp, p);
				if (group == null) {
					continue;
				} else {
					String guildName = LuckPermsIntegration.getGuildName(group);
					// Add user to guild
					new BukkitRunnable() {
						@Override
						public void run() {
							User user = lp.getUser(player.getUniqueId());
							user.setPermission(lp.getNodeFactory().makeGroupNode(group).build());
							lp.getUserManager().saveUser(user);
							lp.runUpdateTask();
							lp.getMessagingService().ifPresent(MessagingService::pushUpdate);
						}
					}.runTaskAsynchronously(plugin);

					// Success indicators
					player.sendMessage(ChatColor.GOLD + "Congratulations! You have joined " + guildName + "!");
					p.sendMessage(ChatColor.WHITE + player.getName() + ChatColor.GOLD + " has joined your guild");
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					                       "execute at " + player.getName()
					                       + " run summon minecraft:firework_rocket ~ ~1 ~ "
					                       + "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");

					// All done
					return;
				}
			}
		}

		String err = ChatColor.RED + "A founder of the guild you wish to join needs to stand within 1 block of you";
		player.sendMessage(err);
		CommandAPI.fail(err);
	}
}
