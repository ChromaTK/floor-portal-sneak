package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ToughBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tough";
	public static final int detectionRange = 50;

	public static class Parameters extends BossParameters {
		public double HEALTH_INCREASE = 1;
	}

	final Parameters mParam;
	private final ArmorStand mBannerHolder;

	public ToughBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new ToughBoss.Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
		EntityUtils.scaleMaxHealth(mBoss, mParam.HEALTH_INCREASE, "vengeance_modifier");
		ItemStack banner = new ItemStack(Material.RED_BANNER);
		mBannerHolder = mBoss.getWorld().spawn(mBoss.getLocation(), ArmorStand.class);
		mBannerHolder.setSmall(true);
		mBannerHolder.setVisible(false);
		mBannerHolder.setMarker(true);
		mBannerHolder.getEquipment().setHelmet(banner);
		try {
			BossManager.createBoss(null, mBannerHolder, ImmortalPassengerBoss.identityTag);
		} catch (Exception e) {
			com.playmonumenta.plugins.Plugin.getInstance().getLogger().warning("Failed to create boss ImmortalPassengerBoss: " + e.getMessage());
			e.printStackTrace();
		}
		mBoss.addPassenger(mBannerHolder);
	}

	@Override
	public void unload() {
		mBannerHolder.remove();
		EntityUtils.removeAttribute(mBoss, Attribute.GENERIC_MAX_HEALTH, "vengeance_modifier");
	}
}


