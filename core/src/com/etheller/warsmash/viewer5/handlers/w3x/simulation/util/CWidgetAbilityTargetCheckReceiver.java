package com.etheller.warsmash.viewer5.handlers.w3x.simulation.util;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;

public class CWidgetAbilityTargetCheckReceiver implements AbilityTargetCheckReceiver<CWidget> {
	public static final CWidgetAbilityTargetCheckReceiver INSTANCE = new CWidgetAbilityTargetCheckReceiver();

	private CWidget target;

	@Override
	public void targetOk(final CWidget target) {
		this.target = target;
	}

	@Override
	public void mustTargetTeamType(final TeamType correctType) {
		this.target = null;
	}

	@Override
	public void mustTargetType(final TargetType correctType) {
		this.target = null;
	}

	@Override
	public void mustTargetResources() {
		this.target = null;
	}

	@Override
	public void targetOutsideRange(final double howMuch) {
		this.target = null;
	}

	@Override
	public void notAnActiveAbility() {
		this.target = null;
	}

	@Override
	public void targetNotVisible() {
		this.target = null;
	}

	@Override
	public void targetTooComplicated() {
		this.target = null;
	}

	@Override
	public void targetNotInPlayableMap() {
		this.target = null;
	}

	@Override
	public void orderIdNotAccepted() {
		this.target = null;
	}

	public CWidget getTarget() {
		return this.target;
	}

}