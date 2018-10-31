package org.decisiondeck.jmcda.services.sorting.assignments;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilitiesRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Matches the procedure T2 (see associated article).
 * 
 * @author Olivier Cailloux
 * 
 */
public class EmbeddedCredibilitiesExtractorT2 {
    private static final Logger s_logger = LoggerFactory.getLogger(EmbeddedCredibilitiesExtractorT2.class);

    static public final double DEFAULT_TOLERANCE = 1e-6;
    private final CredibilitiesHelper m_credibilitiesHelper = new CredibilitiesHelper();
    private final IOrderedAssignmentsWithCredibilitiesRead m_assignments;

    public EmbeddedCredibilitiesExtractorT2(IOrderedAssignmentsWithCredibilitiesRead assignments) {
	m_assignments = AssignmentsFactory.newOrderedAssignmentsWithCredibilities(assignments);
    }

    /**
     * <p>
     * Retrieves the “smallest” intervals, i.e., the intervals containing the least possible number of categories, whose
     * sum of credibilities is greater than or equal to the given minimal credibility level. If several intervals are
     * possible, only the most credible intervals, i.e., those whose sum of creadibilities is the highest, are returned.
     * Ex-æquos are still possible, thus more than one such interval may be returned.
     * </p>
     * <p>
     * Example: suppose the given credibilities are, in order: 3, 4, 1, and a minimal credibility of 5 is asked. Two
     * smallest intervals are possible: [c1, c2] and [c2, c3]. Of those, the first one is more credible and is returned
     * in a singleton set. [c1, c2, c3] is not returned because it contains three categories and is thus not among the
     * smallest ones. Now suppose the credibilities are 2, 4, 2. Then two smallest intervals have a minimal credibility
     * of 5: [c1, c2] and [c2, c3]. As they have the same credibility, they will both be returned. If the credibilities
     * are 3, 1, 2, the set of categories c1 and c3 would reach the required credibility but this is not an interval,
     * thus the returned interval will be [c1, c2, c3]. Finally, suppose the credibilities are 2, 2, 0, 3. The smallest
     * interval of credibility at least 4 is [c1, c2].
     * </p>
     * <p>
     * To account for numerical imprecision, two intervals having the same credibility ± {@link #DEFAULT_TOLERANCE} will
     * be considered as having the same credibility. However the minimal credibility is considered strict.
     * </p>
     * 
     * @param credibilities
     *            not <code>null</code>, not empty. The values must be positive or zero.
     * @param minimalCredibility
     *            a positive, not null, number. Must be less than or equal to the sum of the given credibilities,
     *            otherwise no intervals would be valid.
     * @return not <code>null</code>, not empty.
     */
    static public Set<SortedMap<Category, Double>> getSmallestMostCredibleIntervals(
	    SortedMap<Category, Double> credibilities, double minimalCredibility) {
	Preconditions.checkNotNull(credibilities);
	Preconditions.checkArgument(credibilities.size() > 0);
	Preconditions.checkArgument(minimalCredibility > 0d);

	final Set<SortedMap<Category, Double>> validIntervals = Sets.newHashSet();
	final Iterator<Category> beginIterator = credibilities.keySet().iterator();
	int smallestNbCategories = Integer.MAX_VALUE;
	do {
	    Category begin = beginIterator.next();
	    final Iterator<Category> endIter = credibilities.tailMap(begin).keySet().iterator();
	    double sumOfCredibilities = 0d;
	    Category end;
	    int nbCategories = 0;
	    do {
		end = endIter.next();
		final double nextCred = credibilities.get(end).doubleValue();
		assert nextCred >= 0d;
		sumOfCredibilities += nextCred;
		++nbCategories;
	    } while (endIter.hasNext() && sumOfCredibilities < minimalCredibility
		    && nbCategories < smallestNbCategories);
	    if (nbCategories > smallestNbCategories) {
		continue;
	    }
	    if (sumOfCredibilities < minimalCredibility) {
		continue;
	    }

	    final NavigableMap<Category, Double> credibilitiesNav = new TreeMap<Category, Double>(credibilities);
	    if (nbCategories < smallestNbCategories) {
		validIntervals.clear();
	    } else if (nbCategories == smallestNbCategories) {
		/** Ex-æquo, need to add the interval found. */
	    } else {
		throw new IllegalStateException("Unexpected case.");
	    }
	    validIntervals.add(credibilitiesNav.subMap(begin, true, end, true));
	    smallestNbCategories = nbCategories;
	} while (beginIterator.hasNext());

	if (validIntervals.isEmpty()) {
	    throw new IllegalStateException("No intervals found whose credibility reach the given level "
		    + minimalCredibility + ".");
	}

	return validIntervals;
    }

    static public Set<IOrderedAssignmentsWithCredibilitiesRead> getProductSetOfIntervals(
	    IOrderedAssignmentsWithCredibilitiesRead assignments, double minimalCredibility) {
	return new EmbeddedCredibilitiesExtractorT2(assignments).getProductSetOfIntervals(minimalCredibility);
    }

    /**
     * Retrieves an iterator that will give every assignments that can possibly be built using, for each alternative,
     * one of the intervals given as possible for this alternative. The resulting iterator has a number of different
     * entries that is the product of the number of all sets of possible intervals over all alternatives.
     * 
     * @param categories
     *            not <code>null</code>; is required in order to determine the total ordering of the categories. Must be
     *            a superset of all categories used in the intervals. Will be used as the categories associated to all
     *            the returned assignments objects in the returned set.
     * @param allIntervals
     *            not <code>null</code>; may be empty. Contains the set, for each alternative, of all possible intervals
     *            the alternative may be assigned to. The sets must be non <code>null</code> and non empty.
     * @return not <code>null</code>; empty iff no alternatives have been given, i.e. the given map is empty.
     */
    static public Iterator<IOrderedAssignmentsWithCredibilitiesRead> getProductSetOfIntervalsIterator(
	    SortedSet<Category> categories, Map<Alternative, Set<SortedMap<Category, Double>>> allIntervals) {
	return new ProductSetOfIntervalsIterator(categories, allIntervals);
    }

    static private class ProductSetOfIntervalsIterator implements Iterator<IOrderedAssignmentsWithCredibilitiesRead> {
	private final Map<Alternative, Set<SortedMap<Category, Double>>> m_productIntervals;
	private final LinkedHashMap<Alternative, Iterator<SortedMap<Category, Double>>> m_intervalsIterators = Maps
		.newLinkedHashMap();
	private final IOrderedAssignmentsWithCredibilities m_current;

	/**
	 * @param categories
	 *            not <code>null</code>; is required in order to determine the total ordering of the categories.
	 *            Must be a superset of all categories used in the intervals. Will be used as the categories
	 *            associated to all the returned assignments objects in the returned set.
	 * @param productIntervals
	 *            not <code>null</code>, may be empty. The values may not be <code>null</code>, may not be empty.
	 */
	public ProductSetOfIntervalsIterator(SortedSet<Category> categories,
		Map<Alternative, Set<SortedMap<Category, Double>>> productIntervals) {
	    m_productIntervals = productIntervals;
	    for (Alternative alternative : Sets.newTreeSet(m_productIntervals.keySet()).descendingSet()) {
		final Set<SortedMap<Category, Double>> intervals = m_productIntervals.get(alternative);
		final Iterator<SortedMap<Category, Double>> iterator = intervals.iterator();
		m_intervalsIterators.put(alternative, iterator);
	    }

	    m_current = AssignmentsFactory.newOrderedAssignmentsWithCredibilities();
	    m_current.setCategories(categories);
	    final Iterator<Alternative> alternativesIterator = m_intervalsIterators.keySet().iterator();
	    /** Do not put the first one, it will be put again at the first call to next(). */
	    alternativesIterator.next();
	    while (alternativesIterator.hasNext()) {
		final Alternative alternative = alternativesIterator.next();
		Iterator<SortedMap<Category, Double>> iterator = m_intervalsIterators.get(alternative);
		if (!iterator.hasNext()) {
		    throw new IllegalArgumentException("Sets may not be empty.");
		}
		final SortedMap<Category, Double> first = iterator.next();
		m_current.setCredibilities(alternative, first);
	    }
	}

	@Override
	public boolean hasNext() {
	    for (Iterator<SortedMap<Category, Double>> iterator : m_intervalsIterators.values()) {
		if (iterator.hasNext()) {
		    return true;
		}
	    }
	    return false;
	}

	@Override
	public IOrderedAssignmentsWithCredibilitiesRead next() {
	    for (Alternative alternative : m_intervalsIterators.keySet()) {
		final Iterator<SortedMap<Category, Double>> iterator = m_intervalsIterators.get(alternative);
		if (iterator.hasNext()) {
		    final SortedMap<Category, Double> current = iterator.next();
		    m_current.setCredibilities(alternative, current);
		    return AssignmentsFactory.newOrderedAssignmentsWithCredibilities(m_current);
		}
	    }
	    throw new NoSuchElementException();
	}

	@Override
	public void remove() {
	    throw new UnsupportedOperationException();
	}

    }

    /**
     * <p>
     * The bound assignments must have at least one assigned alternative. To account for possible numerical imprecision,
     * the minimal difference between any two credibility values must be 10 times greater than the
     * {@link #DEFAULT_TOLERANCE}, or all values must be equal (only one value used).
     * </p>
     * <p>
     * This method computes a set of new assignments objects from the assignments bound to this object. The new
     * assignments are ordered by credibility level. The set of assignments objects corresponding to a given credibility
     * level is the set of assignments objects that can be reached by taking the product set of the “smallest” intervals
     * reaching at least that credibility: see {@link #getProductSetOfIntervals}. The lowest credibility level in the
     * returned map is greater than or equal to the lowest credibility value in the input assignments: it is the lowest
     * credibility value such that every alternative in the bound assignments have at least one category associated to
     * at least that value.
     * </p>
     * <p>
     * The rationale is that at a given credibility level, we consider the assignment information as precise as
     * possible, thus as constraining as possible, thus with categories as narrow as possible, while consisting only of
     * information reaching at least the considered level of credibility.
     * <p>
     * The assignments corresponding to a credibility level <em>l</em> are not necessarily narrower than the assignments
     * corresponding to a higher credibility level <em>l'</em>. Consider an alternative having credibilities of
     * assignment (in order of the categories) 2, 2, 0, 3, and consider the relevant intervals reaching at least a
     * credibility level of 4. There is no such interval of size one, and the interval counting two categories having
     * the highest credibility and reaching at least a credibility of 4 is the interval [c1, c2]. Now consider the
     * intervals reaching a credibility of 3. The relevant smallest interval is [c4], which is not included in the
     * former one.
     * </p>
     * 
     * @return not <code>null</code>, not empty.
     */
    public NavigableMap<Double, Set<IOrderedAssignmentsWithCredibilitiesRead>> getByCredibilityLevel() {
	Preconditions.checkState(m_assignments.getAlternatives().size() >= 1);

	final TreeMap<Double, Set<IOrderedAssignmentsWithCredibilitiesRead>> res = new TreeMap<Double, Set<IOrderedAssignmentsWithCredibilitiesRead>>();
	final Double minDiff = getStats().getMinDiff();
	final double offset;
	if (minDiff == null) {
	    s_logger.info("Only one value in the given assignments.");
	    offset = getStats().getMaxValue();
	} else {
	    offset = minDiff.doubleValue();
	}
	assert offset > 10 * DEFAULT_TOLERANCE;
	final double start = getStats().getMinSum() - offset / 2d;
	/** Currentmin must be a bit below the level we target to account for numerical errors. */
	double currentMin = start;
	while (currentMin >= getStats().getMinValue()) {
	    final Set<IOrderedAssignmentsWithCredibilitiesRead> productSet = getProductSetOfIntervals(currentMin);
	    assert productSet.size() >= 1;
	    final Double overallCredibility = Double.valueOf(getMinSum(productSet));
	    if (res.containsKey(overallCredibility)) {
		final Set<IOrderedAssignmentsWithCredibilitiesRead> resAtOverall = res.get(overallCredibility);
		assert productSet.equals(resAtOverall) : "Expected equal " + productSet + ", " + resAtOverall + ".";
	    } else {
		res.put(overallCredibility, productSet);
	    }
	    currentMin -= offset;
	}
	return res;
    }

    /**
     * Note that this method does not consider possible numerical errors and thus may fail if precision is needed.
     * 
     * @param productSet
     *            not <code>null</code>, not empty.
     * @return a value greater than zero.
     */
    private double getMinSum(final Set<IOrderedAssignmentsWithCredibilitiesRead> productSet) {
	final Collection<Double> minSums = Collections2.transform(productSet,
		new Function<IOrderedAssignmentsWithCredibilitiesRead, Double>() {
		    @Override
		    public Double apply(IOrderedAssignmentsWithCredibilitiesRead input) {
			final CredibilitiesHelper helper = new CredibilitiesHelper();
			helper.computeStatistics(input);
			return Double.valueOf(helper.getMinSum());
		    }
		});
	// final Double minSum = Iterables.getOnlyElement(Ordering.<Double> natural().leastOf(minSums, 1));
	final Double minSum = Collections.min(minSums);
	return minSum.doubleValue();
    }

    /**
     * Retrieves the smallest credibility level found in an edge of the credibilities among all assignments. For
     * example, if an assignment has credibilities (in order of the categories) 5, 3, 1, then the considered numbers are
     * 5 and 1, and the returned number (supposing no smaller credibility level is found in other entries) would be 1.
     * 
     * @param assignmentsSet
     *            not <code>null</code>, not empty, at least one entry containing at least one alternative.
     * @return a number greater than zero.
     */
    static public double getSmallestEdge(Set<IOrderedAssignmentsWithCredibilitiesRead> assignmentsSet) {
	double smallest = Double.POSITIVE_INFINITY;
	for (IOrderedAssignmentsWithCredibilitiesRead assignments : assignmentsSet) {
	    for (Alternative alternative : assignments.getAlternatives()) {
		final NavigableMap<Category, Double> credibilities = assignments.getCredibilities(alternative);
		final double first = credibilities.values().iterator().next().doubleValue();
		if (first < smallest) {
		    smallest = first;
		}
		final double last = credibilities.descendingMap().values().iterator().next().doubleValue();
		if (last < smallest) {
		    smallest = last;
		}
	    }
	}
	assert !Double.isInfinite(smallest);
	return smallest;
    }

    /**
     * <p>
     * Retrieves the product set of all possible “smallest” intervals of the bound assignments.
     * </p>
     * <p>
     * First, computes the “smallest” intervals of the bound assignments, i.e., the intervals containing the least
     * possible number of categories, whose sum of credibilities is greater than or equal to the given minimal
     * credibility level. If several intervals are possible, only the most credible intervals, i.e., those whose sum of
     * creadibilities is the highest, are returned. Ex-æquos are still possible, thus more than one such interval may be
     * returned.
     * </p>
     * <p>
     * Then, computes the product set of all these intervals.
     * </p>
     * <p>
     * For example, consider an assignments object of two alternatives into three categories, with a1 having
     * credibilities 3, 4, 1 and a2 having credibilities 2, 4, 2. Suppose the requested minimal credibility is 5. Then
     * this method returns two assignment objects. In each of them a1 is assigned into the only possible smallest
     * interval, that is the first two categories with credibilities 3 and 4. That is because two categories is the
     * smallest number of categories required to reach a credibility level of 5, and this combination is the highest
     * credibility with two categories. Regarding a2, in one of the returned assignments objects, a2 is assigned to c1
     * and c2 (with credibilities 2 and 4); in the other one a2 is assigned to c2 and c3 (with credibilities 4 and 2).
     * </p>
     * <p>
     * To account for numerical imprecision, two intervals having the same credibility ± {@link #DEFAULT_TOLERANCE} will
     * be considered as having the same credibility. However the minimal credibility is considered strict.
     * </p>
     * 
     * @param minimalCredibility
     *            a number greater than zero. Must be less than or equal to the smallest sum of credibilities found in
     *            the bound assignments, otherwize it is impossible to find adequate intervals.
     * @return not <code>null</code>, empty iff no alternatives are assigned in the input assignments.
     */
    public Set<IOrderedAssignmentsWithCredibilitiesRead> getProductSetOfIntervals(double minimalCredibility) {
	final Map<Alternative, Set<SortedMap<Category, Double>>> allIntervals = Maps.newLinkedHashMap();
	final Set<Alternative> alternatives = m_assignments.getAlternatives();
	for (Alternative alternative : alternatives) {
	    final NavigableMap<Category, Double> credibilities = AssignmentsUtils.getCredibilitiesWithZeroes(
		    m_assignments, alternative);
	    final Set<SortedMap<Category, Double>> intervals = getSmallestMostCredibleIntervals(credibilities,
		    minimalCredibility);
	    allIntervals.put(alternative, intervals);
	}
	final Set<IOrderedAssignmentsWithCredibilitiesRead> product = Sets.newLinkedHashSet();
	final Iterator<IOrderedAssignmentsWithCredibilitiesRead> iterator = getProductSetOfIntervalsIterator(
		m_assignments.getCategories(), allIntervals);
	Iterators.addAll(product, iterator);
	return product;
    }

    /**
     * The bound assignments must contain at least one assignment.
     * 
     * @return not <code>null</code>.
     */
    private CredibilitiesHelper getStats() {
	if (!m_credibilitiesHelper.computed()) {
	    m_credibilitiesHelper.computeStatistics(m_assignments);
	}
	return m_credibilitiesHelper;
    }

}
