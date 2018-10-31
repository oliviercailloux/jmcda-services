package org.decisiondeck.jmcda.services.sorting.assignments;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilitiesRead;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * Computes statistics pertaining to an {@link IOrderedAssignmentsWithCredibilitiesRead} object.
 * 
 * @author Olivier Cailloux
 * 
 */
public class CredibilitiesHelper {
    private double m_minSum;

    public CredibilitiesHelper() {
	m_computed = false;
	m_maxSum = Double.NaN;
	m_minSum = Double.NaN;
	m_minDiff = Double.NaN;
	m_maxValue = Double.NaN;
	m_minValue = Double.NaN;
	m_minMax = Double.NaN;
    }

    /**
     * <p>
     * Statistics must have been computed.
     * </p>
     * <p>
     * Retrieves the greatest sum of credibilities over the set of assigned alternatives.
     * </p>
     * 
     * @return a number positive and non zero.
     */
    public double getMaxSum() {
	Preconditions.checkState(m_computed);
	return m_maxSum;
    }

    /**
     * <p>
     * Statistics must have been computed.
     * </p>
     * <p>
     * Retrieves the maximal credibility value found.
     * </p>
     * 
     * @return a number positive and non zero.
     */
    public double getMaxValue() {
	Preconditions.checkState(m_computed);
	return m_maxValue;
    }

    /**
     * <p>
     * Statistics must have been computed.
     * </p>
     * <p>
     * Retrieves the minimal difference between two credibility values.
     * </p>
     * 
     * @return a number positive and non zero, or <code>null</code> iff all credibility values are equal.
     */
    public Double getMinDiff() {
	Preconditions.checkState(m_computed);
	return Double.isInfinite(m_minDiff) ? null : Double.valueOf(m_minDiff);
    }

    private double m_minValue;
    private boolean m_computed;
    private double m_minDiff;
    private double m_maxSum;
    private double m_maxValue;
    private double m_minMax;

    /**
     * Computes the statistics: finds the maximal credibility value; the maximal sum on the set of assigned
     * alternatives; the minimal difference between two credibility values. The minimal difference exists iff there is
     * at least two different values in the given assignments, equivalently, the minimal difference is not defined iff
     * the given assignments use only one value as credibilities, e.g., it consists of only one assigned alternative
     * which has credibility (3, 3). The credibility values are all positive and non zero, as usual, thus so is also the
     * maximal sum and the maximal value. The minimal difference is positive and non zero as well.
     * 
     * @param assignments
     *            not <code>null</code>, must contain at least one assignment.
     */
    public void computeStatistics(IOrderedAssignmentsWithCredibilitiesRead assignments) {
	Preconditions.checkNotNull(assignments);
	Preconditions.checkArgument(assignments.getAlternatives().size() >= 1);
	m_maxValue = Double.NEGATIVE_INFINITY;
	m_minValue = Double.POSITIVE_INFINITY;
	m_minDiff = Double.POSITIVE_INFINITY;
	m_maxSum = Double.NEGATIVE_INFINITY;
	m_minSum = Double.POSITIVE_INFINITY;
	m_minMax = Double.POSITIVE_INFINITY;

	final TreeSet<Double> allValues = new TreeSet<Double>();

	final Set<Alternative> alternatives = assignments.getAlternatives();
	for (Alternative alternative : alternatives) {
	    final Map<Category, Double> credibilities = assignments.getCredibilities(alternative);
	    double sum = 0d;
	    double localMax = Double.NEGATIVE_INFINITY;
	    for (Category cat : credibilities.keySet()) {
		final Double cred = credibilities.get(cat);
		final double credibility = cred.doubleValue();
		sum += credibility;
		if (credibility > m_maxValue) {
		    m_maxValue = credibility;
		}
		if (credibility < m_minValue) {
		    m_minValue = credibility;
		}
		if (credibility > localMax) {
		    localMax = credibility;
		}
		allValues.add(cred);
	    }
	    if (sum > m_maxSum) {
		m_maxSum = sum;
	    }
	    if (sum < m_minSum) {
		m_minSum = sum;
	    }
	    if (localMax < m_minMax) {
		m_minMax = localMax;
	    }
	}

	final PeekingIterator<Double> valuesIter = Iterators.peekingIterator(allValues.iterator());
	while (valuesIter.hasNext()) {
	    final double v1 = valuesIter.next().doubleValue();
	    if (!valuesIter.hasNext()) {
		break;
	    }
	    final double v2 = valuesIter.peek().doubleValue();
	    final double diff = v2 - v1;
	    if (diff <= 0d) {
		throw new IllegalStateException("Should be sorted.");
	    }
	    if (diff < m_minDiff) {
		m_minDiff = diff;
	    }
	}

	assert !Double.isInfinite(m_maxValue);
	m_computed = true;
    }

    /**
     * @return the minimal credibility value over the set of assigned alternatives, of the highest credibility value for
     *         that alternative.
     */
    public double getMinMaxValue() {
	Preconditions.checkState(m_computed);
	return m_minMax;
    }

    /**
     * @return <code>true</code> iff the statistics have been computed.
     */
    public boolean computed() {
	return m_computed;
    }

    /**
     * <p>
     * Statistics must have been computed.
     * </p>
     * <p>
     * Retrieves the smallest sum of credibilities over the set of assigned alternatives.
     * </p>
     * 
     * @return a number positive and non zero.
     */
    public double getMinSum() {
	Preconditions.checkState(m_computed);
	return m_minSum;
    }

    /**
     * <p>
     * Statistics must have been computed.
     * </p>
     * <p>
     * Retrieves the minimal credibility value found.
     * </p>
     * 
     * @return a number greater than zero.
     */
    public double getMinValue() {
	return m_minValue;
    }
}
