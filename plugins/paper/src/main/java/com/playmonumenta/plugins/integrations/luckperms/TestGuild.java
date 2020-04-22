package com.playmonumenta.plugins.integrations.luckperms;

import java.util.LinkedHashMap;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.TextArgument;
import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;

public class TestGuild {
	public static void register(Plugin plugin, LuckPermsApi lp) {

		// testguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.testguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("guild name", new TextArgument());

		CommandAPI.getInstance().register("testguild", perms, arguments, (sender, args) -> {
			run(lp, (String) args[1], (Player) args[0]);
		});
	}

	private static void run(LuckPermsApi lp, String guildName, Player player) throws WrapperCommandSyntaxException {
		Group currentGuild = LuckPermsIntegration.getGuild(lp, player);
		String currentGuildName = LuckPermsIntegration.getGuildName(currentGuild);

		if (currentGuildName == null) {
			CommandAPI.fail("Player '" + player.getName() + "' is not in a guild!");
		} else if (!currentGuildName.equalsIgnoreCase(guildName)) {
			CommandAPI.fail("Player '" + player.getName() + "' is in other guild '" + currentGuildName + "'");
		}
	}
}
