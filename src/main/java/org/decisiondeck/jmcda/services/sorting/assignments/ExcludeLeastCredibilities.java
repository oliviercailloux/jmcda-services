package org.decisiondeck.jmcda.services.sorting.assignments;

import static com.google.common.base.Preconditions.checkState;

import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;

import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilitiesRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsUtils;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsViewFactory;

import com.google.common.base.Preconditions;

public class ExcludeLeastCredibilities {

    private final IOrderedAssignmentsWithCredibilitiesRead m_assignments;
    private static final double TOLERANCE = 1e-5;

    public ExcludeLeastCredibilities(IOrderedAssignmentsWithCredibilitiesRead assignments) {
	m_assignments = AssignmentsFactory.newOrderedAssignmentsWithCredibilities(assignments);
    }

    /**
     * <p>
     * The bound assignments must have at least one assigned alternative. The sum of the credibilities, for each
     * alternative, must be constant. TODO CHECK THIS - To account for possible numerical imprecision, the minimal
     * difference between any two credibility values must be 10 times greater than the {@link #DEFAULT_TOLERANCE}, or
     * all values must be equal (only one value used).
     * </p>
     * <p>
     * This method computes a set of new assignments objects from the assignments bound to this object. The new
     * assignments are ordered by credibility level. The assignments object corresponding to a given credibility level
     * comprises, for each assigned alternative, the credibilities given in input, excluded the credibilities less or
     * equal to the complementary of the cedibility level to the sum of credibilities. For example, at a credibility
     * level of 8, assuming a constant sum of 10, those credibilites of 2 or lower are excluded.
     * </p>
     * <p>
     * The highest excluded credibility level in the returned map is the one just lower to the minimum over all
     * alternatives of the highest credibility value in the input assignments of that alternative. Equivalently, it is
     * the highest credibility value such that every alternative in the bound assignments have at least one category
     * associated to at least that value of credibility. The lowest credibility level in the returned map is the sum of
     * credibilities minus that highest excluded credibility level value.
     * </p>
     * 
     * @return not <code>null</code>, not empty.
     */
    public NavigableMap<Double, IOrderedAssignmentsWithCredibilitiesRead> getByCredibilityLevel() {
	Preconditions.checkState(m_assignments.getAlternatives().size() >= 1);
	final CredibilitiesHelper helper = new CredibilitiesHelper();
	helper.computeStatistics(m_assignments);
	final double maxSum = helper.getMaxSum();
	final double sum = maxSum;
	final double minSum = helper.getMinSum();
	checkState(Math.abs(maxSum - minSum) < TOLERANCE);
	final Double minDiff = helper.getMinDiff();
	final double minMax = helper.getMinMaxValue();

	final TreeMap<Double, IOrderedAssignmentsWithCredibilitiesRead> res = new TreeMap<Double, IOrderedAssignmentsWithCredibilitiesRead>();
	/** First: exclude zero. */
	res.put(Double.valueOf(sum), m_assignments);

	/** Then: exclude step by step. */
	final NavigableSet<Double> credibilityLevels = AssignmentsUtils.getCredibilityLevels(m_assignments);
	for (Double level : credibilityLevels.headSet(Double.valueOf(minMax))) {
	    assert minDiff != null : "Here we should have more than one different credibility levels as we have excluded the min max one.";
	    final double offset = minDiff.doubleValue() / 2d;
	    final double toExclude = level.doubleValue();
	    final double toKeep = toExclude + offset;
	    res.put(Double.valueOf(sum - toExclude), AssignmentsViewFactory.getAssignmentsGEQ(m_assignments, toKeep));
	}
	return res;
    }

}
