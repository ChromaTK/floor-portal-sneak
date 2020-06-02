package com.playmonumenta.plugins.bosses.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.playmonumenta.plugins.bosses.spells.Spell;

public class SpellCastEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean mIsCancelled;
	private LivingEntity mBoss;
	private Spell mSpell;

	public SpellCastEvent(LivingEntity boss, Spell spell) {
		mBoss = boss;
		mSpell = spell;
	}

	public LivingEntity getBoss() {
		return mBoss;
	}

	public Spell getSpell() {
		return mSpell;
	}

	@Override
	public boolean isCancelled() {
		return mIsCancelled;
	}

	@Override
	public void setCancelled(boolean arg0) {
		this.mIsCancelled = arg0;
	}

	// Mandatory Event Methods (If you remove these, I'm 99% sure the event will break)

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
