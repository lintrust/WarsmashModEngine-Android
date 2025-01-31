package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.keeper;

import com.etheller.warsmash.units.manager.MutableObjectData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.*;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

import java.util.ArrayList;
import java.util.List;

public class CAbilityForceOfNature extends CAbilityPointTargetSpellBase {

	private final AnyMatchingDestFinder anyMatchingDestFinder;
	private int numberOfSummonedUnits;
	private War3ID summonedUnitId;
	private War3ID buffId;
	private float areaOfEffect;

	public CAbilityForceOfNature(int handleId, War3ID alias) {
		super(handleId, alias);
		anyMatchingDestFinder = new AnyMatchingDestFinder();
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.forceofnature;
	}

	@Override
	public float getUIAreaOfEffect() {
		return areaOfEffect;
	}

	@Override
	public void populateData(MutableObjectData.MutableGameObject worldEditorAbility, int level) {
		numberOfSummonedUnits =
				worldEditorAbility.getFieldAsInteger(AbilityFields.ForceOfNature.NUMBER_OF_SUMMONED_UNITS, level);
		summonedUnitId =
				War3ID.fromString(worldEditorAbility.getFieldAsString(AbilityFields.ForceOfNature.SUMMONED_UNIT_TYPE,
						level));
		buffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level);
		areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT, level);
	}

	@Override
	protected void innerCheckCanTarget(CSimulation game, CUnit unit, int orderId, AbilityPointTarget target,
									   AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		game.getWorldCollision().enumDestructablesInRange(target.getX(), target.getY(), areaOfEffect,
				anyMatchingDestFinder.reset(game, unit));
		if (!anyMatchingDestFinder.foundMatch) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_TREE);
		}
		else {
			super.innerCheckCanTarget(game, unit, orderId, target, receiver);
		}
	}

	@Override
	public boolean doEffect(CSimulation simulation, CUnit caster, AbilityTarget target) {
		List<CDestructable> trees = new ArrayList<>();
		simulation.getWorldCollision().enumDestructablesInRange(target.getX(), target.getY(), areaOfEffect,
				(enumDest) -> {
			if (enumDest.canBeTargetedBy(simulation, caster, getTargetsAllowed())) {
				trees.add(enumDest);
			}
			return trees.size() >= numberOfSummonedUnits;
		});
		for (CDestructable tree : trees) {
			tree.setLife(simulation, 0);

			CUnit summonedUnit = simulation.createUnitSimple(summonedUnitId, caster.getPlayerIndex(), tree.getX(),
					tree.getY(), simulation.getGameplayConstants().getBuildingAngle());
			summonedUnit.addClassification(CUnitClassification.SUMMONED);
			summonedUnit.add(simulation, new CBuffTimedLife(simulation.getHandleIdAllocator().createId(), buffId,
					getDuration(), false));
		}
		return false;
	}

	private class AnyMatchingDestFinder implements CDestructableEnumFunction {
		private CSimulation game;
		private CUnit unit;
		private boolean foundMatch = false;

		public AnyMatchingDestFinder reset(CSimulation game, CUnit unit) {
			this.game = game;
			this.unit = unit;
			this.foundMatch = false;
			return this;
		}

		@Override
		public boolean call(CDestructable enumDest) {
			if (enumDest.canBeTargetedBy(game, unit, getTargetsAllowed())) {
				foundMatch = true;
			}
			return foundMatch;
		}
	}
}
