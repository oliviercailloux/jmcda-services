package org.decisiondeck.xmcda_oo.services.sorting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.DiscreteInterval;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.sorting.SortingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this only when using true criteria, i.e. having no indifference or preference thresholds. Is meaningless when a
 * preference or indifference threshold is used with the criteria. If using veto thresholds, the standardization works
 * only for sharp vetoes. If all these conditions are fulfilled, the resulting profiles and vetoes are equivalent (will
 * give the same results when a sorting of the appropriate mode is applied) to the original ones.
 * 
 * @author Olivier Cailloux
 * 
 */
public class StandardizeProfiles {
    private static final Logger s_logger = LoggerFactory.getLogger(StandardizeProfiles.class);

    private boolean m_crossBoundaries;

    public StandardizeProfiles() {
	m_crossBoundaries = false;
    }

    /**
     * Please use only in the absence of veto thresholds.
     * 
     * @param profilesEvaluations
     *            the profiles evaluations to standardize.
     * @param scales
     *            the scales to take into account. Must be defined on the same set of criteria as the evaluations.
     * @param sortingMode
     *            the mode to take into account when standardizing.
     * @return a new evaluation matrix containing the values standardized according to the given scale and mode.
     */
    public Evaluations getInStandardForm(EvaluationsRead profilesEvaluations,
 Map<Criterion, DiscreteInterval> scales,
	    SortingMode sortingMode) {
	if (!scales.keySet().equals(profilesEvaluations.getColumns())) {
	    throw new IllegalArgumentException("Scales must be on the same criteria than profiles evaluations.");
	}
	if (!profilesEvaluations.isComplete()) {
	    throw new IllegalArgumentException("Evaluations should be complete.");
	}
	final Evaluations std = EvaluationsUtils.newEvaluationMatrix();
	for (Alternative profile : profilesEvaluations.getRows()) {
	    for (Criterion criterion : profilesEvaluations.getColumns()) {
		final double value = profilesEvaluations.getEntry(profile, criterion).doubleValue();
		final DiscreteInterval scale = scales.get(criterion);
		final double stdValue = getInStandardForm(value, scale, sortingMode);
		std.put(profile, criterion, stdValue);
	    }
	}
	return std;
    }

    /**
     * @param value
     *            if outside the scale's boundaries, and this object is asked to stay inside boundaries (the default),
     *            the value is returned untouched.
     * @param scale
     *            the scale to use.
     * @param sortingMode
     *            the sorting mode to take into account when transforming the value.
     * @return the value in standard form considering the given scale.
     */
    public double getInStandardForm(double value, DiscreteInterval scale,
	    SortingMode sortingMode) {
	checkNotNull(scale);
	checkNotNull(sortingMode);
	checkArgument(scale.getPreferenceDirection() != null);
	final boolean outBoundaries = !Intervals.inBoundaries(scale, value)
		|| (sortingMode == SortingMode.PESSIMISTIC && value == scale.getWorst())
		|| (sortingMode == SortingMode.OPTIMISTIC && value == scale.getBest());
	if (!m_crossBoundaries) {
	    if (outBoundaries) {
		s_logger.info("Given value (" + value
			+ ") for standardization is outside or too near boundaries, doing nothing.");
		return value;
	    }
	}
	if (m_crossBoundaries && outBoundaries) {
	    throw new UnsupportedOperationException("Not implemented yet.");
	}
	assert (!outBoundaries);

	/**
	 * If optimistic, and scale is to maximize: We should get the lower step and move higher a half step. If we are
	 * exactly on a step, it matters whether we move a half step higher from the lower step or a half step lower
	 * from the higher step, because if we are exactly on a step, the lower and the higher step are the same. In
	 * that case, we do want to move lower in case of optimistic mode and maximise scale. On the contrary, if we are
	 * between two steps, it does not matter which strategy we follow, as the result will be the same.
	 */
	final int sign;
	switch (sortingMode) {
	case OPTIMISTIC:
	    /** We should get the lower step and move higher a half step, or inverse if to minimize. */
	    sign = scale.getDirectionAsSign();
	    break;
	case PESSIMISTIC:
	    sign = -scale.getDirectionAsSign();
	    break;
	case BOTH:
	    final double div2 = (value - scale.getMinimum()) / scale.getNonNullStepSize();
	    if (scale.contains(value)) {
		/** We are exactly on a step, we can't move. */
		sign = 0;
	    } else {
		/** Sign does not matter as we are not exactly on a step. */
		sign = 1;
	    }
	    break;
	default:
	    throw new IllegalStateException("Unknown sorting mode.");
	}

	final double startingStep;
	if (sign >= 0) {
	    final double lowStep = ((int) Math.floor((value - scale.getMinimum()) / scale.getNonNullStepSize()))
		    * scale.getNonNullStepSize() + scale.getMinimum();
	    startingStep = lowStep;
	} else {
	    final double highStep = ((int) Math.ceil((value - scale.getMinimum()) / scale.getNonNullStepSize()))
		    * scale.getNonNullStepSize() + scale.getMinimum();
	    startingStep = highStep;
	}

	final double stdValue;
	stdValue = startingStep + sign * (scale.getNonNullStepSize() / 2d);
	return stdValue;
    }

    public boolean isCrossBoundaries() {
	return m_crossBoundaries;
    }

    /**
     * Allows or disallows this object to cross scale's boundaries. If <code>false</code>, this object will never return
     * values outside of the relevant scale's boundaries.
     * 
     * @param crossBoundaries
     *            <code>true</code> to allow this object to cross boundaries.
     */
    public void setCrossBoundaries(boolean crossBoundaries) {
	m_crossBoundaries = crossBoundaries;
    }
}
