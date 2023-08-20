package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.bukkit.common.wrappers.DataWatcher;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityMetadataHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.EntityHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;

/**
 * Replaces entities' names with "Grumm" if they have the tag "boss_upside_down" to cause them to be rendered upside-down, without having to give them that name server-side.
 */
public class EntityNameReplacer extends PacketAdapter {
	public EntityNameReplacer(Plugin plugin) {
		super(plugin, PacketType.Play.Server.ENTITY_METADATA);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		PacketContainer packet = event.getPacket();
		Entity entity = packet.getEntityModifier(event).read(0);
		if (entity != null && entity.getScoreboardTags().contains("boss_upside_down")) {
			PacketPlayOutEntityMetadataHandle handle = PacketPlayOutEntityMetadataHandle.createHandle(packet.getHandle());
			List<DataWatcher.PackedItem<Object>> metadataItems = handle.getMetadataItems();
			for (int i = 0; i < metadataItems.size(); i++) {
				DataWatcher.PackedItem<Object> metadataItem = metadataItems.get(i);
				if (metadataItem.isForKey(EntityHandle.DATA_CUSTOM_NAME)) {
					metadataItems.set(i, metadataItem.cloneWithValue(Optional.of(NmsUtils.getVersionAdapter().toVanillaChatComponent(Component.text("Grumm")))));
					break;
				}
			}
		}
	}

}
