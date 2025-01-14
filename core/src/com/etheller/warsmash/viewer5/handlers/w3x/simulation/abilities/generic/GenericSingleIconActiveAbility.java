package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.Aliased;

public interface GenericSingleIconActiveAbility extends CLevelingAbility, SingleOrderAbility, Aliased {

	boolean isToggleOn();

	boolean isAutoCastOn();

	int getAutoCastOnOrderId();

	int getAutoCastOffOrderId();

	int getUIGoldCost();

	int getUILumberCost();

	int getUIManaCost();

	float getUIAreaOfEffect();

}
