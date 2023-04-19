package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.phases.Phase;
import com.playmonumenta.plugins.events.DamageEvent;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class BossPhasesList {

	private final List<Phase> mPhases;

	private BossPhasesList(List<Phase> phases) {
		mPhases = phases;
	}

	private List<Phase> getClonePhaseList() {
		return new ArrayList<>(mPhases);
	}

	public void addBossPhases(BossPhasesList other) {
		mPhases.addAll(other.mPhases);
	}

	public void onSpawn(LivingEntity boss) {
		for (Phase phase : getClonePhaseList()) {
			if (phase.onSpawn(boss) && !phase.isReusable()) {
				mPhases.remove(phase);
			}
		}
	}

	public void onDeath(LivingEntity boss) {
		for (Phase phase : getClonePhaseList()) {
			if (phase.onDeath(boss) && !phase.isReusable()) {
				mPhases.remove(phase);
			}
		}
	}

	public void onDamage(LivingEntity boss, LivingEntity damagee, DamageEvent event) {
		for (Phase phase : getClonePhaseList()) {
			if (phase.onDamage(boss, damagee, event) && !phase.isReusable()) {
				mPhases.remove(phase);
			}
		}
	}

	public void onHurt(LivingEntity boss, @Nullable LivingEntity damager, DamageEvent event) {
		for (Phase phase : getClonePhaseList()) {
			if (phase.onHurt(boss, damager, event) && !phase.isReusable()) {
				mPhases.remove(phase);
			}
		}
	}

	public void onBossCastAbility(LivingEntity boss, SpellCastEvent event) {
		for (Phase phase : getClonePhaseList()) {
			if (phase.onBossCastAbility(boss, event) && !phase.isReusable()) {
				mPhases.remove(phase);
			}
		}
	}

	public void tick(LivingEntity boss, int ticks) {
		for (Phase phase : getClonePhaseList()) {
			if (phase.tick(boss, ticks) && !phase.isReusable()) {
				mPhases.remove(phase);
			}
		}
	}

	public void onCustom(LivingEntity boss, String key) {
		for (Phase phase : getClonePhaseList()) {
			if (phase.onCustom(boss, key) && !phase.isReusable()) {
				mPhases.remove(phase);
			}
		}
	}


	public static BossPhasesList emptyPhaseList() {
		return new BossPhasesList(new ArrayList<>());
	}

	public static List<Tooltip<String>> suggestionPhases(SuggestionInfo info) {
		StringReader reader = new StringReader(info.currentArg());
		ParseResult<Phase> phaseParseResult = Phase.fromReader(reader);
		if (phaseParseResult.getResult() == null) {
			return Objects.requireNonNull(phaseParseResult.getTooltip());
		}

		return List.of();
	}

	public static ParseResult<BossPhasesList> fromReader(StringReader reader) {
		if (!reader.advance("[")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "[", "[...]")));
		}
		if (!reader.advance("(")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", "[...]")));
		}

		String phaseName = reader.readUntil(",");
		if (phaseName == null) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "specificPhaseName", "name")));
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "[...]")));
		}

		Boolean reusable = reader.readBoolean();
		if (reusable == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(2);
			String soFar = reader.readSoFar();
			suggArgs.add(Tooltip.ofString(soFar + "false", "not reusable"));
			suggArgs.add(Tooltip.ofString(soFar + "true", "reusable"));
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		if (!reader.advance(",")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "[...]")));
		}

		ParseResult<Phase> phaseResult = Phase.fromReader(reader);
		if (phaseResult.getResult() == null) {
			return ParseResult.of(Objects.requireNonNull(phaseResult.getTooltip()));
		}
		Phase phase = phaseResult.getResult();
		phase.setName(phaseName);
		phase.setReusable(reusable);

		if (!reader.advance(")")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", "[...]")));
		}
		if (!reader.advance("]")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "]", "[...]")));
		}

		List<Phase> phases = new ArrayList<>();
		phases.add(phase);
		return ParseResult.of(new BossPhasesList(phases));

	}

}
