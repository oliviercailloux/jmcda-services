package org.decision_deck.services;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * <p>
 * An object that contains statistics about evaluations. Immutable.
 * </p>
 * <p>
 * Note that if the evaluations bound to this object change, the statistics do not change and thus become invalid.
 * </p>
 * 
 * @author Olivier Cailloux
 * 
 */
public class EvaluationsStatistics {
    private static final Logger s_logger = LoggerFactory.getLogger(EvaluationsStatistics.class);

    /**
     * Never <code>null</code>.
     */
    private EvaluationsRead m_evaluations;
    /**
     * one valid float for each criterion in the bound evaluation matrix the last time the computation was called.
     */
    private final Map<Criterion, Double> m_highests = Maps.newHashMap();
    /**
     * one valid float for each criterion in the bound evaluation matrix the last time the computation was called.
     */
    private final Map<Criterion, Double> m_lowests = Maps.newHashMap();

    /**
     * contains no value when the difference can't be computed (only one value found on that criterion).
     */
    private final Map<Criterion, Double> m_minDifferences = Maps.newHashMap();

    private final Map<Criterion, NavigableSet<Double>> m_sortedEvaluations = new HashMap<Criterion, NavigableSet<Double>>();

    /**
     * Computes statistics about the given evaluations.
     * 
     * @param evaluations
     *            not <code>null</code>, non empty.
     */
    public EvaluationsStatistics(EvaluationsRead evaluations) {
	checkNotNull(evaluations);
	checkArgument(!evaluations.isEmpty());

	m_evaluations = evaluations;
	/**
	 * NB we may not delay the computation (using a lazy-init) because of the risk the bound evaluations change. The
	 * other possibility would be to defense-copy them, but that's as much work as computing the statistics
	 * themselves.
	 */
	computeStatistics();
    }

    /**
     * Computes the statistics and remember them.
     * 
     */
    private void computeStatistics() {
	for (Criterion criterion : m_evaluations.getColumns()) {
	    final NavigableSet<Double> values = new TreeSet<Double>();

	    for (Alternative alt : m_evaluations.getRows()) {
		final Double entry = m_evaluations.getEntry(alt, criterion);
		if (entry == null) {
		    continue;
		}
		final float value = entry.floatValue();
		values.add(Double.valueOf(value));
	    }
	    if (values.isEmpty()) {
		throw new IllegalStateException("No evaluation could be found on the given criterion: " + criterion
			+ ".");
	    }
	    final Double highest = values.last();
	    final Double lowest = values.first();
	    m_highests.put(criterion, highest);
	    m_lowests.put(criterion, lowest);
	    m_sortedEvaluations.put(criterion, values);

	    final List<Double> valuesList = new LinkedList<Double>(values);
	    Collections.sort(valuesList);
	    Iterator<Double> valuesIter = valuesList.iterator();
	    double val1 = valuesIter.next().doubleValue();
	    double minDiff = Double.POSITIVE_INFINITY;
	    double minVal1 = 0;
	    double minVal2 = 0;
	    while (valuesIter.hasNext()) {
		final double val2 = valuesIter.next().doubleValue();
		final double diff = val2 - val1;
		if (diff != 0 && diff < minDiff) {
		    minDiff = diff;
		    minVal1 = val1;
		    minVal2 = val2;
		}
		val1 = val2;
	    }
	    if (!Double.isInfinite(minDiff)) {
		m_minDifferences.put(criterion, Double.valueOf(minDiff));
		s_logger.debug("Smallest diff found for {}, val2 : " + minVal2 + " - val1: " + minVal1 + ".", criterion);
	    }

	    s_logger.debug("Extreme values for {}, highest: " + highest + ", lowest: " + lowest + ", min diff: "
		    + m_minDifferences.get(criterion) + ".", criterion);
	}
	assert (m_highests.keySet().equals(m_lowests.keySet()));
	assert (m_highests.keySet().equals(m_sortedEvaluations.keySet()));
    }

    /**
     * Retrieves a copy, or read-only view, of the criteria for which statistics are available.
     * 
     * @return not <code>null</code>.
     */
    public Set<Criterion> getCriteria() {
	return Collections.unmodifiableSet(m_lowests.keySet());
    }

    /**
     * Retrieves a view of the evaluations bound to the given criterion.
     * 
     * @param criterion
     *            not <code>null</code>, must be contained in the evaluations.
     * @return not <code>null</code>.
     */
    public NavigableSet<Double> getSortedEvaluations(Criterion criterion) {
	checkNotNull(criterion);
	checkArgument(m_sortedEvaluations.containsKey(criterion));
	return Sets.unmodifiableNavigableSet(m_sortedEvaluations.get(criterion));
    }

    /**
     * <P>
     * Finds the worst evaluation in the bound evaluations on the given criterion when the statistics were computed,
     * i.e., the smallest if the criterion is to be maximized, the highest if it should be minimal.
     * </P>
     * 
     * @param criterion
     *            the criterion to find the anti-ideal for. Must be in this object.
     * @return the worst (highest or smallest) evaluation on the given criterion.
     */
    public double getAntiIdeal(Criterion criterion, PreferenceDirection preferenceDirection) {
	if (m_highests == null) {
	    throw new IllegalStateException("Statistics are invalid. Compute them first.");
	}
	final boolean max = preferenceDirection == PreferenceDirection.MAXIMIZE;
	Double antiIdeal;
	if (max) {
	    antiIdeal = m_lowests.get(criterion);
	} else {
	    antiIdeal = m_highests.get(criterion);
	}
	if (antiIdeal == null) {
	    throw new IllegalStateException("Criterion was not in the evaluation matrix when statistics were computed.");
	}
	return antiIdeal.doubleValue();
    }

    /**
     * <P>
     * Finds the lowest evaluation that existed in the bound evaluations on the given criterion when the statistics were
     * computed.
     * </P>
     * 
     * @param criterion
     *            not <code>null</code>, must be in this object.
     * @return the lowest evaluation on the given criterion.
     */
    public double getMinimum(Criterion criterion) {
	if (m_highests == null) {
	    throw new IllegalStateException("Statistics are invalid. Compute them first.");
	}
	final Double lowest = m_lowests.get(criterion);
	if (lowest == null) {
	    throw new IllegalStateException("Criterion was not present in evaluations when statistics were computed.");
	}
	return lowest.doubleValue();
    }

    /**
     * <p>
     * Retrieves the maximal evaluation difference in the given matrix on the given criterion, i.e., the highest
     * difference between any two evaluations on the given criterion.
     * </p>
     * 
     * @param criterion
     *            the criterion to find the maximal difference for. Must be in this object.
     * @return the maximal difference on the given criterion (strictly positive), and zero if all evaluations on the
     *         given criterion are identical.
     */
    public double getMaxDifference(Criterion criterion) {
	final Double highest = m_highests.get(criterion);
	final Double lowest = m_lowests.get(criterion);
	if ((highest == null) != (lowest == null)) {
	    throw new IllegalStateException("Should be a highest if and only if there is a lowest.");
	}
	if (highest == null || lowest == null) {
	    throw new IllegalStateException("Criterion " + criterion
		    + "was not in the evaluations when statistics where computed.");
	}
	return highest.doubleValue() - lowest.doubleValue();
    }

    /**
     * <P>
     * Finds the minimal evaluation difference in the given matrix on the given criterion, i.e., the smallest difference
     * between any two different evaluation values on the given criterion.
     * </P>
     * 
     * @param criterion
     *            the criterion to find the minimal difference for. Must be in this object.
     * @return the minimal difference on the given criterion (strictly positive), or <code>null</code> iff all
     *         evaluations on the given criterion are identical.
     */
    public Double getMinDifference(Criterion criterion) {
	Preconditions.checkState(m_highests.containsKey(criterion), "Criterion " + criterion + " not found.");
	final Double min = m_minDifferences.get(criterion);
	assert (min == null || min.doubleValue() > 0);
	return min;
    }

    /**
     * <P>
     * Finds the best evaluation that existed in the bound evaluations on the given criterion when the statistics were
     * computed, i.e., the highest if the criterion is to be maximized, the smallest if it should be minimal.
     * </P>
     * 
     * @param criterion
     *            the criterion to find the ideal for. Must have a preference direction. Must be in this object.
     * @return the best (highest or smallest) evaluation on the given criterion.
     */
    public double getIdeal(Criterion criterion, PreferenceDirection direction) {
	if (m_highests == null) {
	    throw new IllegalStateException("Statistics are invalid. Compute them first.");
	}
	final boolean max = direction == PreferenceDirection.MAXIMIZE;
	Double ideal;
	if (max) {
	    ideal = m_highests.get(criterion);
	} else {
	    ideal = m_lowests.get(criterion);
	}
	if (ideal == null) {
	    throw new IllegalStateException("Criterion was not present in evaluations when statistics were computed.");
	}
	return ideal.doubleValue();
    }

    /**
     * <P>
     * Finds the highest evaluation that existed in the bound evaluations on the given criterion when the statistics
     * were computed.
     * </P>
     * 
     * @param criterion
     *            not <code>null</code>, must be in this object.
     * @return the highest evaluation on the given criterion.
     */
    public double getMaximum(Criterion criterion) {
	if (m_highests == null) {
	    throw new IllegalStateException("Statistics are invalid. Compute them first.");
	}
	final Double highest = m_highests.get(criterion);
	if (highest == null) {
	    throw new IllegalStateException("Criterion was not present in evaluations when statistics were computed.");
	}
	return highest.doubleValue();
    }

}
