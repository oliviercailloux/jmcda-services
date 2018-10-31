package org.decisiondeck.xmcda_oo.structure.sorting;

import static com.google.common.base.Preconditions.checkNotNull;

import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decisiondeck.jmcda.structure.sorting.category.CatsInverter;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;

public class SortingProblemInverter {

    /**
     * Note that the profiles evaluations must also change: for a pessimistic only evaluation with no thresholds,
     * suffices to adjust the profile evaluation to the level above (round "up") (for criteria to max) instead of
     * adjusting it to the level below (round "down") (for criteria to min). TODO make this clearer.
     * 
     * @param data
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    static public ISortingData getInverse(ISortingData data) {
	checkNotNull(data);
	final ISortingData inverse = ProblemFactory.newSortingData();
	inverse.setEvaluations(data.getAlternativesEvaluations());
	inverse.getCriteria().addAll(data.getCriteria());
	inverse.getAlternatives().addAll(data.getAlternatives());
	inverse.getProfiles().addAll(data.getProfiles());
	for (Criterion criterion : data.getCriteria()) {
	    final Interval scale = data.getScales().get(criterion);
	    final PreferenceDirection invert;
	    switch (scale.getPreferenceDirection()) {
	    case MAXIMIZE:
		invert = PreferenceDirection.MINIMIZE;
		break;
	    case MINIMIZE:
		invert = PreferenceDirection.MAXIMIZE;
		break;
	    default:
		throw new IllegalStateException("Unknown case.");
	    }
	    inverse.setScale(criterion,
		    Intervals.newUnrestrictedInterval(invert, scale.getMinimum(), scale.getMaximum()));
	}
	CatsInverter.copyInverseToTarget(data.getCatsAndProfs(), inverse.getCatsAndProfs());
	return inverse;
    }

}
