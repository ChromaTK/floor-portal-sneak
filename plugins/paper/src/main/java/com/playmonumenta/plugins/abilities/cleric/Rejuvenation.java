package com.playmonumenta.plugins.abilities.cleric;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Rejuvenation extends Ability {

	private static final int REJUVENATION_RADIUS = 12;
	private static final String REJUVENATION_METADATA_KEY = "RejuvenationMetadataKey";

	private int mTimer = 0;

	public Rejuvenation(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Rejuvenation");
		mInfo.scoreboardId = "Rejuvenation";
		mInfo.mShorthandName = "Rjv";
		mInfo.mDescriptions.add("You regenerate 5% of your max health every 3 seconds.");
		mInfo.mDescriptions.add("All other players in a 12 block radius also regenerate 5% of their max health every 3 seconds.");
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			//  Don't trigger this if dead!
			if (!mPlayer.isDead()) {
				// 5 ticks because it triggers on four hertz.
				mTimer += 5;
				if (mTimer % 60 == 0) {
					int rejuvenation = getAbilityScore();
					for (Player p : PlayerUtils.playersInRange(mPlayer, REJUVENATION_RADIUS, true)) {
						// Don't buff players that have their class disabled or who have PvP enabled
						if (p.getScoreboardTags().contains("disable_class") || AbilityManager.getManager().isPvPEnabled(p)) {
							continue;
						}

						if (MetadataUtils.checkOnceThisTick(mPlugin, p, REJUVENATION_METADATA_KEY)) {
							//  If this is us or we're allowing anyone to get it.
							if (p == mPlayer || rejuvenation > 1) {
								double oldHealth = p.getHealth();
								PlayerUtils.healPlayer(p, p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.05);
								if (p.getHealth() > oldHealth) {
									mWorld.spawnParticle(Particle.HEART, (p.getLocation()).add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001);
								}
							}
						}
					}
				}
			}
		}
	}

}
