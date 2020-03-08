package com.playmonumenta.plugins.utils;

import java.util.UUID;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.network.SocketManager;
import com.playmonumenta.plugins.packets.BroadcastCommandPacket;
import com.playmonumenta.plugins.packets.BungeeGetServerListPacket;
import com.playmonumenta.plugins.packets.BungeeSendPlayerPacket;
import com.playmonumenta.plugins.packets.ShardTransferPlayerDataPacket;
import com.playmonumenta.plugins.player.PlayerData;

public class NetworkUtils {

	public static void sendPlayer(Plugin plugin, Player player, String server) throws Exception {
		sendPlayer(plugin, player.getName(), player.getUniqueId(), server);
	}

	public static void sendPlayer(Plugin plugin, String playerName, UUID playerUUID, String server) throws Exception {
		SocketManager.sendPacket(new BungeeSendPlayerPacket(server, playerName, playerUUID));

		// Success, print transfer message request to log
		plugin.getLogger().info("Requested bungeecord transfer " + playerName + " to " + server);
	}

	public static boolean transferPlayerData(Plugin plugin, Player player, String server) throws Exception {
		return SocketManager.sendPacket(new ShardTransferPlayerDataPacket(server,
		                                                                  player.getName(),
		                                                                  player.getUniqueId(),
		                                                                  PlayerData.convertToString(plugin, player)));
	}

	public static void getServerList(Plugin plugin, Player player) throws Exception {
		SocketManager.sendPacket(new BungeeGetServerListPacket(player.getName(), player.getUniqueId()));

		// Success, print transfer message request to log
		plugin.getLogger().info("Requested server list for " + player.getName());
	}

	public static void broadcastCommand(Plugin plugin, String command) throws Exception {
		SocketManager.sendPacket(new BroadcastCommandPacket(command));

		// Success, print transfer message request to log
		plugin.getLogger().fine("Requested broadcast of command '" + command + "'");
	}
}
