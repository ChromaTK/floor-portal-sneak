package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class GallerySpeedEffect extends GalleryStackableEffect {

	private static final int SPEED_EFFECT_MAX_STACK = 5;
	private static final double SPEED_EFFECT_PER_STACK = 0.1;

	public GallerySpeedEffect() {
		super(GalleryEffectType.SPEED);
	}

	@Override
	public void playerGainEffect(GalleryPlayer galleryPlayer) {
		super.playerGainEffect(galleryPlayer);
		Player player = galleryPlayer.getPlayer();
		if (player == null) {
			return;
		}
		EntityUtils.addAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier("GallerySpeedEffect", SPEED_EFFECT_PER_STACK * mStacks, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	@Override
	public void playerLoseEffect(GalleryPlayer galleryPlayer) {
		super.playerLoseEffect(galleryPlayer);
		Player player = galleryPlayer.getPlayer();
		if (player == null) {
			return;
		}
		EntityUtils.removeAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, "GallerySpeedEffect");
	}

	@Override
	public int getMaxStacks() {
		return SPEED_EFFECT_MAX_STACK;
	}

	@Override
	public void refresh(GalleryPlayer galleryPlayer) {
		Player player = galleryPlayer.getPlayer();
		if (galleryPlayer.isOnline() && player != null) {
			EntityUtils.removeAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, "GallerySpeedEffect");
			EntityUtils.addAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier("GallerySpeedEffect", SPEED_EFFECT_PER_STACK * mStacks, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}
}
