package org.decisiondeck.jmcda.services.sorting;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.services.ConsistencyChecker;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.jmcda.structure.sorting.SortingMode;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.services.outranking.Outranking;
import org.decisiondeck.jmcda.services.outranking.OutrankingFull;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignments;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsUtils;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.group_preferences.IGroupSortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.group_results.IGroupSortingResults;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.view.ProblemViewFactory;
import org.decisiondeck.xmcda_oo.services.sorting.SortingAssigner;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

public class SortingFull {

    public SortingFull() {
	m_sharpVetoes = true;
	m_smallestSep = null;
	m_tolerance = Outranking.DEFAULT_TOLERANCE;
    }

    private boolean m_sharpVetoes;
    private Double m_smallestSep;
    private double m_tolerance;

    public boolean isSharpVetoes() {
	return m_sharpVetoes;
    }

    public void setSharpVetoes(boolean sharpVetoes) {
	m_sharpVetoes = sharpVetoes;
    }

    /**
     * <p>
     * Computes the optimistic assignment of the real alternatives contained in the data, using the given data, into the
     * given categories. Each alternative is assigned to the lowest category such that the alternative does not outrank
     * the up profile corresponding to that category and the profile outranks the alternative.
     * </p>
     * <p>
     * For the input to be valid, the weights must be all provided, all evaluations must be provided, all criteria must
     * have preference directions, the set of criteria on which thresholds are defined must be in the set of criteria,
     * the preference threshold must be greater or equal to the indifference threshold for each criteria, the veto
     * thresholds must be greater than or equal to the preference threshold for each criteria. Missing thresholds are
     * accepted. Categories must be complete.
     * </p>
     * 
     * @param data
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             if the input is not valid.
     */
    public IOrderedAssignments optimistic(ISortingPreferences data) throws InvalidInputException {
	return (IOrderedAssignments) assign(SortingMode.OPTIMISTIC, data);
    }

    /**
     * <p>
     * Computes the pessimistic and optimistic assignment of the real alternatives contained in the data, using the
     * given data, into the given categories. Each alternative is assigned to all categories between its pessimistic
     * assignment and optimistic assignment.
     * </p>
     * <p>
     * For the input to be valid, the weights must be all provided, all evaluations must be provided, all criteria must
     * have preference directions, the set of criteria on which thresholds are defined must be in the set of criteria,
     * the preference threshold must be greater or equal to the indifference threshold for each criteria, the veto
     * thresholds must be strictly greater than the preference threshold for each criteria. Missing thresholds are
     * accepted. Categories must be complete.
     * </p>
     * 
     * @param data
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             if the input is not valid.
     */
    public IOrderedAssignmentsToMultiple both(ISortingPreferences data) throws InvalidInputException {
	return (IOrderedAssignmentsToMultiple) assign(SortingMode.BOTH, data);
    }

    /**
     * <p>
     * Computes the pessimistic assignment of the real alternatives contained in the data, using the given data, into
     * the given categories. Each alternative is assigned to the highest category such that the alternative outranks the
     * profile corresponding to that category.
     * </p>
     * <p>
     * For the input to be valid, the weights must be all provided, all evaluations must be provided, all criteria must
     * have preference directions, the set of criteria on which thresholds are defined must be in the set of criteria,
     * the preference threshold must be greater or equal to the indifference threshold for each criteria, the veto
     * thresholds must be greater than or equal to the preference threshold for each criteria. Missing thresholds are
     * accepted. Categories must be complete.
     * </p>
     * 
     * @param problem
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             if the input is not valid.
     */
    public IOrderedAssignments pessimistic(ISortingPreferences problem) throws InvalidInputException {
	return (IOrderedAssignments) assign(SortingMode.PESSIMISTIC, problem);
    }

    /**
     * <p>
     * Computes the assignment of the real alternatives contained in the data, using the given given data, into the
     * given categories.
     * </p>
     * <p>
     * For the input to be valid, the weights must be all provided, all evaluations must be provided, all criteria must
     * have preference directions, the set of criteria on which thresholds are defined must be in the set of criteria,
     * the preference threshold must be greater or equal to the indifference threshold for each criteria, the veto
     * thresholds must be greater than or equal to the preference threshold for each criteria. Missing thresholds are
     * accepted. Categories must be complete.
     * </p>
     * <p>
     * If the sorting mode is BOTH, the returned object implements {@link IOrderedAssignmentsToMultiple}, otherwise, it
     * implements {@link IOrderedAssignments}.
     * </p>
     * TODO improve performances by computing only required subset of outranking.
     * 
     * @param mode
     *            not <code>null</code>.
     * @param problem
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             if the input is not valid.
     */
    public IOrderedAssignmentsToMultipleRead assign(SortingMode mode, ISortingPreferences problem)
	    throws InvalidInputException {
	Preconditions.checkNotNull(mode);
	Preconditions.checkNotNull(problem);
	new ConsistencyChecker().assertCompleteCoalitions(problem);

	final OutrankingFull outr = new OutrankingFull();
	outr.setSharpVetoes(m_sharpVetoes);
	outr.setTolerance(m_tolerance);
	final SparseAlternativesMatrixFuzzy outranking = outr.getOutranking(
ProblemFactory.newProblemData(
		EvaluationsUtils.merge(problem.getAlternativesEvaluations(), problem.getProfilesEvaluations()),
		problem.getScales()), problem.getThresholds(),
		problem.getCoalitions());
	m_smallestSep = outr.getSmallestSep();
	final IOrderedAssignmentsToMultipleRead assignments = new SortingAssigner().assign(mode,
		problem.getAlternatives(), outranking, problem.getCatsAndProfs());
	return assignments;
    }

    /**
     * Retrieves the smallest difference, in absolute value, between the majority threshold and any outranking value.
     * Useful for sensitivity analysis or to check for possible numerical errors. The number is positive or nul. It
     * represents the largest quantity that may be added or substracted from the majority threshold without changing the
     * outranking relation (and thus the sorting results).
     * 
     * @return <code>null</code> if no sorting computation has been asked or no values were found when asked for one
     *         (because of an empty set of alternatives).
     */
    public Double getSmallestSep() {
	return m_smallestSep;
    }

    public double getTolerance() {
	return m_tolerance;
    }

    public void setTolerance(double tolerance) {
	m_tolerance = tolerance;
    }

    static public void pessimisticInto(Map<DecisionMaker, Set<Alternative>> alternatives, IGroupSortingResults results)
	    throws InvalidInputException {
	Preconditions.checkNotNull(alternatives);
	Preconditions.checkNotNull(results);
	final SortingFull sorting = new SortingFull();
	final Map<DecisionMaker, IOrderedAssignments> allAssignments = sorting.pessimisticAll(results, alternatives);
	for (DecisionMaker dm : results.getDms()) {
	    final IOrderedAssignments assignments = allAssignments.get(dm);
	    AssignmentsUtils.copyOrderedAssignmentsToTarget(assignments, results.getAssignments(dm));
	}
    }

    public Map<DecisionMaker, IOrderedAssignments> pessimisticAll(IGroupSortingPreferences problem,
	    Map<DecisionMaker, Set<Alternative>> alternatives) throws InvalidInputException {
	checkArgument(problem.getDms().equals(alternatives.keySet()), "Different " + problem.getDms() + " and "
		+ alternatives.keySet() + ".");
	final Map<DecisionMaker, IOrderedAssignments> allAssignments = Maps.newHashMap();
	for (DecisionMaker dm : problem.getDms()) {
	    final ISortingPreferences oneProblem = problem.getPreferences(dm);
	    final ISortingPreferences individualProblem = ProblemViewFactory.getRestrictedPreferences(oneProblem,
		    Predicates.in(alternatives.get(dm)), null);
	    final IOrderedAssignments assignments = pessimistic(individualProblem);
	    allAssignments.put(dm, assignments);
	}
	return allAssignments;
    }

}
