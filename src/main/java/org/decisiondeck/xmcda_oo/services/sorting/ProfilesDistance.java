package org.decisiondeck.xmcda_oo.services.sorting;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.DiscreteInterval;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.SortingMode;

/**
 * This object also accepts values outside of the relevant scale's boundaries.
 * 
 * @author Olivier Cailloux
 * 
 */
public class ProfilesDistance {
    private final StandardizeProfiles m_std = new StandardizeProfiles();
    private int m_lastDistanceInAlternatives;
    private double m_lastMaxDist;
    private double m_lastSumDist;
    /**
     * At a given approximation level l, the number of profiles evaluations which are equal to an approximation of l,
     * thus whose distance is lower or equal to l to the other profile evaluation.
     */
    private Map<Double, Integer> m_nbEqualsByApprox = new HashMap<Double, Integer>();

    public ProfilesDistance() {
	m_std.setCrossBoundaries(true);
	m_lastSumDist = -1;
	m_lastMaxDist = -1;
	m_nbEqualsByApprox.put(Double.valueOf(0), Integer.valueOf(-1));
	m_lastDistanceInAlternatives = -1;
    }

    public ProfilesDistanceResult getResults() {
	final ProfilesDistanceResult res = new ProfilesDistanceResult();
	res.setLastDistanceInAlternatives(m_lastDistanceInAlternatives);
	res.setLastMaxDist(m_lastMaxDist);
	res.setLastSumDist(m_lastSumDist);
	res.setNbEqualsByApprox(m_nbEqualsByApprox);
	return res;
    }

    public double getDistance(double value1, double value2, DiscreteInterval scale, SortingMode sortingMode) {
	if (scale == null || sortingMode == null) {
	    throw new NullPointerException("" + value1 + "; " + value2 + scale + sortingMode);
	}
	final double std1 = m_std.getInStandardForm(value1, scale, sortingMode);
	final double std2 = m_std.getInStandardForm(value2, scale, sortingMode);
	final double diffSteps = Math.abs(std2 - std1) / scale.getNonNullStepSize();

	final double dist;
	switch (sortingMode) {
	case OPTIMISTIC:
	case PESSIMISTIC:
	    final long rounded = Math.round(diffSteps);
	    if (Math.abs(rounded - diffSteps) > 1e-6) {
		throw new IllegalStateException(
			"Difference in number of steps of standardized distances should be an integer.");
	    }
	    dist = rounded * scale.getNonNullStepSize();
	    break;
	case BOTH:
	    /** Could be with a half. */
	    final long roundedDoubled = Math.round(diffSteps * 2);
	    if (Math.abs(roundedDoubled - diffSteps * 2) > 1e-6) {
		throw new IllegalStateException(
			"Difference in number of steps of standardized distances should be a multiple of 0.5.");
	    }
	    dist = roundedDoubled / 2d * scale.getNonNullStepSize();
	    break;
	default:
	    throw new IllegalStateException("Unknown sorting mode.");
	}
	return dist;
    }

    /**
     * @param profilesEvaluations1
     *            must contain the same profiles on the same criteria evaluations as the other profiles, and be
     *            complete.
     * @param profilesEvaluations2
     *            see above.
     * @param scales
     *            must have scales assigned to a set of criteria corresponding to the given profiles evaluations.
     * @param sortingMode
     *            the sorting mode used, used to standardize the profiles.
     * @return the sum of the distances on each criteria and profile pair.
     */
    public double getSumDistance(EvaluationsRead profilesEvaluations1, EvaluationsRead profilesEvaluations2,
	    Map<Criterion, DiscreteInterval> scales, SortingMode sortingMode) {
	computeDistances(profilesEvaluations1, profilesEvaluations2, scales, sortingMode);
	return m_lastSumDist;
    }

    public void computeDistances(EvaluationsRead profilesEvaluations1, EvaluationsRead profilesEvaluations2,
	    Map<Criterion, DiscreteInterval> scales, SortingMode sortingMode) {
	if (!profilesEvaluations1.isComplete() || !profilesEvaluations2.isComplete()
		|| !profilesEvaluations1.getRows().equals(profilesEvaluations2.getRows())
		|| !profilesEvaluations1.getColumns().equals(profilesEvaluations2.getColumns())) {
	    throw new IllegalArgumentException("Evaluations 1 and 2 do not match.");
	}
	for (Double approx : m_nbEqualsByApprox.keySet()) {
	    m_nbEqualsByApprox.put(approx, Integer.valueOf(0));
	}
	m_lastSumDist = 0;
	m_lastMaxDist = -1;
	final Set<Alternative> profiles = profilesEvaluations1.getRows();
	final Set<Criterion> criteria = profilesEvaluations1.getColumns();
	for (Alternative profile : profiles) {
	    for (Criterion criterion : criteria) {
		final double eval1 = profilesEvaluations1.getEntry(profile, criterion).doubleValue();
		final double eval2 = profilesEvaluations2.getEntry(profile, criterion).doubleValue();
		final DiscreteInterval scale = scales.get(criterion);
		if (scale == null) {
		    throw new IllegalStateException("Should have a scale for " + criterion + ".");
		}
		final double dist = getDistance(eval1, eval2, scale, sortingMode);
		m_lastSumDist += dist;
		if (dist > m_lastMaxDist) {
		    m_lastMaxDist = dist;
		}
		for (Double approx : m_nbEqualsByApprox.keySet()) {
		    final double approxValue = approx.doubleValue();
		    if (dist <= approxValue) {
			int nbEquals = m_nbEqualsByApprox.get(approx).intValue();
			m_nbEqualsByApprox.put(approx, Integer.valueOf(nbEquals + 1));
		    }
		}
	    }
	}
    }

    /**
     * @param profilesEvaluations1
     *            a set of profiles.
     * @param profilesEvaluations2
     *            an other set of profiles.
     * @param alternatives
     *            must be evaluated on the same criteria.
     * @return the total number of evaluations of alternatives that are in between (not strict) both profiles
     *         evaluations.
     */
    public int getDistanceInAlternatives(EvaluationsRead profilesEvaluations1, EvaluationsRead profilesEvaluations2,
	    EvaluationsRead alternatives) {
	if (!profilesEvaluations1.isComplete() || !profilesEvaluations2.isComplete()
		|| !profilesEvaluations1.getRows().equals(profilesEvaluations2.getRows())
		|| !profilesEvaluations1.getColumns().equals(profilesEvaluations2.getColumns())
		|| !profilesEvaluations1.getColumns().equals(alternatives.getColumns())) {
	    throw new IllegalArgumentException("Evaluations 1 and 2 or alternatives do not match.");
	}
	final Set<Alternative> profiles = profilesEvaluations1.getRows();
	final Set<Criterion> criteria = profilesEvaluations1.getColumns();
	int wrong = 0;
	for (Criterion criterion : criteria) {
	    for (Alternative profile : profiles) {
		final double eval1 = profilesEvaluations1.getEntry(profile, criterion).doubleValue();
		final double eval2 = profilesEvaluations2.getEntry(profile, criterion).doubleValue();
		final double smallest = Math.min(eval1, eval2);
		final double biggest = Math.max(eval1, eval2);
		for (Alternative alternative : alternatives.getRows()) {
		    final double altEval = alternatives.getEntry(alternative, criterion).doubleValue();
		    if (altEval >= smallest && altEval <= biggest) {
			++wrong;
		    }
		}
	    }
	}
	m_lastDistanceInAlternatives = wrong;
	return m_lastDistanceInAlternatives;
    }

    public void setComputeApproximations(Set<Double> approximationLevels) {
	m_nbEqualsByApprox.clear();
	m_nbEqualsByApprox.put(Double.valueOf(0), Integer.valueOf(-1));
	for (Double approx : approximationLevels) {
	    m_nbEqualsByApprox.put(approx, Integer.valueOf(-1));
	}
    }

    /**
     * @param profilesEvaluations1
     *            must contain the same profiles on the same criteria evaluations as the other profiles, and be
     *            complete.
     * @param profilesEvaluations2
     *            see above.
     * @param scales
     *            must have scales assigned to a set of criteria corresponding to the given profiles evaluations.
     * @param sortingMode
     *            the sorting mode used, used to standardize the profiles.
     * @return the maximum distance found among all criteria and profiles pair comparison.
     */
    public double getMaxDistance(EvaluationsRead profilesEvaluations1, EvaluationsRead profilesEvaluations2,
	    Map<Criterion, DiscreteInterval> scales, SortingMode sortingMode) {
	computeDistances(profilesEvaluations1, profilesEvaluations2, scales, sortingMode);
	return m_lastMaxDist;
    }

    /**
     * @return -1 if not yet computed or given data was empty (no distance found).
     */
    public double getLastMaxDist() {
	return m_lastMaxDist;
    }

    /**
     * @return -1 if not yet computed.
     */
    public double getLastSumDist() {
	return m_lastSumDist;
    }

    /**
     * TODO remove method duplication with {@link ProfilesDistanceResult}.
     * 
     * @return the number of equals with an approximation of zero.
     */
    public int getNbEquals() {
	return getNbEquals(0);
    }

    public int getNbEquals(final double approx) {
	return m_nbEqualsByApprox.get(Double.valueOf(approx)).intValue();
    }

    /**
     * @return a copy of the current results. Will contain values of -1 iff not yet computed, otherwise, only positive
     *         or null values.
     */
    public NavigableMap<Double, Integer> getNbEqualsByApprox() {
	return new TreeMap<Double, Integer>(m_nbEqualsByApprox);
    }

    public int getLastDistanceInAlternatives() {
	return m_lastDistanceInAlternatives;
    }

    public void resetComputations() {
	m_lastSumDist = -1;
	m_lastMaxDist = -1;
	m_lastDistanceInAlternatives = -1;
	// setComputeApproximations(Collections.<Double>emptySet());
	for (Double approx : m_nbEqualsByApprox.keySet()) {
	    m_nbEqualsByApprox.put(approx, Integer.valueOf(-1));
	}
    }
}
