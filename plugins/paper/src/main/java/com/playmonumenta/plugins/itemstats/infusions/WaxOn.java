package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class WaxOn implements Infusion {

	@Override
	public String getName() {
		return "Wax On";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.WAX_ON;
	}

}
