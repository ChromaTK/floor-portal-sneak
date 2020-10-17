package com.playmonumenta.plugins.tracking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

public class FishingHookTracking implements EntityTracking {
	private HashMap<UUID, FishHook> mEntities = new HashMap<UUID, FishHook>();

	@Override
	public void addEntity(Entity entity) {

	}

	public void addEntity(Player player, Entity entity) {
		mEntities.put(player.getUniqueId(), (FishHook)entity);
	}

	@Override
	public void removeEntity(Entity entity) {

	}

	public void removeEntity(Player player) {
		UUID uuid = player.getUniqueId();

		FishHook entity = mEntities.get(uuid);
		if (entity != null) {
			entity.remove();
		}

		mEntities.remove(uuid);
	}

	public boolean containsEntity(Player player) {
		return mEntities.get(player.getUniqueId()) != null;
	}

	@Override
	public void update(World world, int ticks) {

	}

	@Override
	public void unloadTrackedEntities() {
		Iterator<Entry<UUID, FishHook>> iter = mEntities.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<UUID, FishHook> hook = iter.next();
			hook.getValue().remove();
		}

		mEntities.clear();
	}
}
