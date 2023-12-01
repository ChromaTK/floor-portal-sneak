package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Optional;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class LuckPermsIntegration {
	protected static @MonotonicNonNull LuckPerms LP = null;
	protected static @MonotonicNonNull UserManager UM = null;
	protected static @MonotonicNonNull GroupManager GM = null;

	public static void enable(Plugin plugin) {
		plugin.getLogger().info("Enabling LuckPerms integration");
		LP = LuckPermsProvider.get();
		UM = LP.getUserManager();
		GM = LP.getGroupManager();

		CreateGuild.register(plugin);
		JoinGuild.register(plugin);
		PromoteGuild.register();
		LeaveGuild.register(plugin);
		TestGuild.register();
		TeleportGuild.register();
		SetGuildTeleport.register(plugin);
	}

	public static void setPermission(Player player, String permission, boolean value) {
		UM.modifyUser(player.getUniqueId(), user -> {
			// Add the permission
			user.data().add(Node.builder(permission).value(value).build());
		});
	}

	public static @Nullable Group getGuild(Player player) {
		User user = UM.getUser(player.getUniqueId());
		if (user == null) {
			return null;
		}
		for (Group group : user.getInheritedGroups(QueryOptions.nonContextual())) {
			for (MetaNode node : group.getNodes(NodeType.META)) {
				if (node.getMetaKey().equals("guildname")) {
					return group;
				}
			}
		}

		return null;
	}

	public static @Nullable String getGuildName(@Nullable Group group) {
		if (group == null) {
			return null;
		}

		for (MetaNode node : group.getNodes(NodeType.META)) {
			if (node.getMetaKey().equals("guildname")) {
				return node.getMetaValue();
			}
		}

		return null;
	}

	public static void setGuildTp(Group group, Plugin plugin, Location loc) {
		// Remove all the other guildtp meta nodes
		for (MetaNode node : group.getNodes(NodeType.META)) {
			if (node.getMetaKey().equals("guildtp")) {
				group.data().remove(node);
			}
		}

		group.data().add(MetaNode.builder("guildtp", LocationUtils.locationToString(loc)).build());

		new BukkitRunnable() {
			@Override
			public void run() {
				GM.saveGroup(group).whenComplete((unused, ex) -> {
					if (ex != null) {
						ex.printStackTrace();
					}
				});
				pushUpdate();
			}
		}.runTaskAsynchronously(plugin);
	}

	public static @Nullable Location getGuildTp(World world, Group group) {
		try {
			for (MetaNode node : group.getNodes(NodeType.META)) {
				if (node.getMetaKey().equals("guildtp")) {
					return LocationUtils.locationFromString(world, node.getMetaValue());
				}
			}
		} catch (Exception e) {
			return null;
		}

		return null;
	}

	public static String getCleanGuildName(String guildName) {
		// Guild name sanitization for command usage
		return guildName.toLowerCase().replace(" ", "_");
	}

	public static void pushUpdate() {
		Optional<MessagingService> mso = LP.getMessagingService();
		if (mso.isPresent()) {
			mso.get().pushUpdate();
		}
	}

	public static void pushUserUpdate(User user) {
		Optional<MessagingService> mso = LP.getMessagingService();
		if (mso.isPresent()) {
			mso.get().pushUserUpdate(user);
		}
	}
}
