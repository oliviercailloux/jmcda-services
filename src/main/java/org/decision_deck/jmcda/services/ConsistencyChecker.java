package org.decision_deck.jmcda.services;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.Weights;
import org.decision_deck.utils.PredicateUtils;
import org.decision_deck.utils.collection.extensional_order.ExtentionalTotalOrder;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.problem.assignments.ISortingAssignments;
import org.decisiondeck.jmcda.structure.sorting.problem.assignments.ISortingAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.problem.assignments.ISortingAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;
import org.decisiondeck.jmcda.structure.sorting.problem.group_data.IGroupSortingData;
import org.decisiondeck.jmcda.structure.sorting.problem.group_preferences.IGroupSortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class ConsistencyChecker {
    private static final Logger s_logger = LoggerFactory.getLogger(ConsistencyChecker.class);

    public ConsistencyChecker() {
	/** Public constructor. */
    }

    /**
     * Note condition of normalized weights has been removed.
     * 
     * @param preferences
     *            not <code>null</code>.
     * @return boolean.
     */
    public boolean isConsistentPreferences(ISortingPreferences preferences) {
	if (preferences == null) {
	    throw new NullPointerException();
	}

	if (!isConsistentData(preferences)) {
	    return false;
	}

	final EvaluationsRead evaluations = preferences.getProfilesEvaluations();
	boolean evalsIncomp = evaluations.getValueCount() != preferences.getProfiles().size()
		* preferences.getCriteria().size();
	if (!evaluations.isEmpty() && evalsIncomp) {
	    return false;
	}

	if (!evaluations.isEmpty()) {
	    NavigableSet<Alternative> dominance;
	    try {
		dominance = new Dominance().getStrictDominanceOrder(evaluations,
			Intervals.getDirectionsFromScales(preferences.getScales()));
	    } catch (InvalidInputException exc) {
		throw new IllegalStateException(exc);
	    }
	    if (dominance == null) {
		s_logger.info("Preferences are not consistent because profiles evaluations do not constitute a total dominance relation.");
		return false;
	    }
	}

	final Coalitions coalitions = preferences.getCoalitions();
	boolean coalsIncomp = coalitions.getCriteria().size() != preferences.getCriteria().size()
		|| !coalitions.containsMajorityThreshold();
	if (!coalitions.isEmpty() && coalsIncomp) {
	    return false;
	}

	if (!preferences.getThresholds().getPreferenceThresholds().isEmpty()
		&& preferences.getThresholds().getPreferenceThresholds().size() != preferences.getCriteria().size()) {
	    return false;
	}
	if (!preferences.getThresholds().getIndifferenceThresholds().isEmpty()
		&& preferences.getThresholds().getIndifferenceThresholds().size() != preferences.getCriteria().size()) {
	    return false;
	}

	return true;
    }

    public boolean assertConsistentData(ISortingData data) throws InvalidInputException {
	if (data.getAlternativesEvaluations().getValueCount() != data.getAlternatives().size()
		* data.getCriteria().size()) {
	    throw new InvalidInputException("Inconsistent data. Alts evals count: "
		    + data.getAlternativesEvaluations().getValueCount() + "; " + data.getAlternatives().size()
		    + " alternatives; " + data.getCriteria().size() + " criteria.");
	}
	if (!data.getCatsAndProfs().isComplete()) {
	    throw new InvalidInputException("Inconsistent data. Categories are incomplete.");
	}
	if (data.getCatsAndProfs().getProfiles().size() != data.getProfiles().size()) {
	    throw new InvalidInputException("Inconsistent data. Profiles do not match.");
	}
	assertCompletePreferenceDirections(data);
	return true;
    }

    /**
     * Ensures that the given object contains an evaluation for every profile on every criteria contained in the data.
     * Note that this is a stronger condition than checking {@link EvaluationsRead#isComplete()}.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if the evaluations are incomplete.
     */
    public void assertCompleteProfilesEvaluations(ISortingPreferences data) throws InvalidInputException {
	if (data.getProfilesEvaluations().getValueCount() != data.getProfiles().size() * data.getCriteria().size()) {
	    throw new InvalidInputException("Not all evaluations provided. Profiles evals count: "
		    + data.getProfilesEvaluations().getValueCount() + "; " + data.getProfiles().size() + " profiles; "
		    + data.getCriteria().size() + " criteria.");
	}
    }

    /**
     * Ensures that the categories and profiles in the given data are complete. The categories are said to be complete
     * iff at least one category has been set and all categories have both their profiles set, except the worst one
     * which has no down profile, and the best one which has no up profile. This implies that the number of profiles set
     * equals the number of categories minus one. Note that completeness in that sense does not imply that no categories
     * will be added any more to this object.
     * 
     * @see CatsAndProfs
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if the categories and profiles object in the given data are incomplete.
     */
    public void assertCompleteCatsAndProfs(ISortingData data) throws InvalidInputException {
	if (!data.getCatsAndProfs().isComplete()) {
	    throw new InvalidInputException("Categories are incomplete.");
	}
    }

    /**
     * Ensures that the profiles contained in the given data are all contained into the categories and profiles object,
     * or equivalently, ensures that the profiles are all ordered and unambiguously associated to the categories.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if the profiles are not completely ordered.
     */
    public void assertCompleteProfiles(ISortingData data) throws InvalidInputException {
	if (data.getCatsAndProfs().getProfiles().size() != data.getProfiles().size()) {
	    throw new InvalidInputException("Profiles do not match.");
	}
    }

    /**
     * Note condition of normalized weights has been removed.
     * 
     * @param preferences
     *            not <code>null</code>.
     * @return boolean.
     */
    public boolean isConsistentGroupPreferences(IGroupSortingPreferences preferences) {
	if (!isConsistentGroupData(preferences)) {
	    return false;
	}

	boolean evaluationsEmpty = true;
	boolean evaluationsIncompleteFound = false;
	final int fullEvaluationsSize = preferences.getProfiles().size() * preferences.getCriteria().size();
	if (preferences.getSharedProfilesEvaluations().isEmpty()) {
	    for (DecisionMaker dm : preferences.getDms()) {
		final EvaluationsRead evaluations = preferences.getProfilesEvaluations(dm);
		evaluationsEmpty = evaluationsEmpty && evaluations.isEmpty();
		evaluationsIncompleteFound = evaluationsIncompleteFound
			|| evaluations.getValueCount() != fullEvaluationsSize;
	    }
	} else {
	    final EvaluationsRead evaluations = preferences.getSharedProfilesEvaluations();
	    evaluationsEmpty = evaluationsEmpty && evaluations.isEmpty();
	    evaluationsIncompleteFound = evaluationsIncompleteFound
		    || evaluations.getValueCount() != fullEvaluationsSize;
	}
	if (!evaluationsEmpty && evaluationsIncompleteFound) {
	    return false;
	}
	try {
	    if (!evaluationsEmpty) {
		if (preferences.getSharedProfilesEvaluations().isEmpty()) {
		    for (DecisionMaker dm : preferences.getDms()) {
			final EvaluationsRead evaluations = preferences.getProfilesEvaluations(dm);
			final NavigableSet<Alternative> dominance = new Dominance().getStrictDominanceOrder(
				evaluations, Intervals.getDirectionsFromScales(preferences.getScales()));
			if (dominance == null) {
			    return false;
			}
		    }
		} else {
		    final EvaluationsRead evaluations = preferences.getSharedProfilesEvaluations();
		    NavigableSet<Alternative> dominance;
		    dominance = new Dominance().getStrictDominanceOrder(evaluations,
			    Intervals.getDirectionsFromScales(preferences.getScales()));
		    if (dominance == null) {
			return false;
		    }
		}
	    }
	} catch (InvalidInputException exc) {
	    return false;
	}

	boolean coalitionsEmpty = true;
	boolean coalitionsIncompleteFound = false;
	if (preferences.getCoalitions().size() > 0) {
	    for (DecisionMaker dm : preferences.getDms()) {
		final Coalitions coalitions = preferences.getCoalitions(dm);
		coalitionsEmpty = coalitionsEmpty && (coalitions == null || (coalitions.isEmpty()));
		coalitionsIncompleteFound = coalitionsIncompleteFound || coalitions == null
			|| coalitions.getCriteria().size() != preferences.getCriteria().size()
			|| !coalitions.containsMajorityThreshold();
	    }
	}
	if (!coalitionsEmpty && coalitionsIncompleteFound) {
	    s_logger.info("Coalitions non empty and some incomplete found.");
	    return false;
	}

	boolean thresholdsPEmpty = true;
	boolean thresholdsIEmpty = true;
	boolean thresholdsPIncompleteFound = false;
	boolean thresholdsIIncompleteFound = false;
	if (preferences.getThresholds().size() > 0) {
	    for (DecisionMaker dm : preferences.getDms()) {
		final Thresholds thresholds = preferences.getThresholds(dm);
		thresholdsPEmpty = thresholdsPEmpty
			&& (thresholds == null || thresholds.getPreferenceThresholds().isEmpty());
		thresholdsIEmpty = thresholdsIEmpty
			&& (thresholds == null || thresholds.getIndifferenceThresholds().isEmpty());
		thresholdsPIncompleteFound = thresholdsPIncompleteFound
			|| (thresholds == null && !preferences.getCriteria().isEmpty())
			|| (thresholds != null && thresholds.getPreferenceThresholds().size() != preferences
				.getCriteria().size());
		thresholdsIIncompleteFound = thresholdsIIncompleteFound
			|| (thresholds == null && !preferences.getCriteria().isEmpty())
			|| (thresholds != null && thresholds.getIndifferenceThresholds().size() != preferences
				.getCriteria().size());
	    }
	}
	if (!thresholdsPEmpty && thresholdsPIncompleteFound) {
	    return false;
	}
	if (!thresholdsIEmpty && thresholdsIIncompleteFound) {
	    return false;
	}

	return true;
    }

    public boolean isConsistentGroupData(IGroupSortingData data) {
	if (!isConsistentData(data)) {
	    return false;
	}
	if (data.getDms().isEmpty()) {
	    s_logger.info("Inconsistent group data: found no decision makers.");
	    return false;
	}
	return true;
    }

    /**
     * Ensures that every alternative contained in the given data are assigned.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if some assignments are missing.
     */
    public void assertCompleteAssignments(ISortingAssignmentsWithCredibilities data) throws InvalidInputException {
	if (data.getAssignments().getAlternatives().size() != data.getAlternatives().size()) {
	    throw new InvalidInputException("Not all assignments provided.");
	}
    }

    /**
     * <p>
     * Ensures that the profiles evaluations define a total ordering on the profiles in terms of strict dominance; and
     * that this dominance order is the same as the order of the profiles given in the categories and profiles object
     * contained in the given data. This test fails if not every preference directions are set in the given data, as
     * this is a required data to compute the dominance relation.
     * </p>
     * <p>
     * This test does not ensure that the profiles evaluations are complete in the sense of
     * {@link #assertCompleteProfilesEvaluations}: if the profiles are evaluated only on a subset of the set of criteria
     * contained in this object, and these evaluations yield an adequate order, this test will pass.
     * </p>
     * <p>
     * If the given data contain no profiles, this test succeeds. Indeed, the profiles evaluations, empty in that case,
     * are complete as every profile is evaluated on every criteria, the dominance order, empty in that case, is the
     * same as the profiles order, etc.
     * </p>
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if the order of the profiles given in the categories and profiles object is not the same as the order
     *             of the profiles according to their evaluations, or if some preference direction is missing, or if the
     *             profiles evaluations are not a complete matrix (in the sense of {@link EvaluationsRead#isComplete()}
     *             ).
     */
    public void assertDominance(ISortingPreferences data) throws InvalidInputException {
	assertCompletePreferenceDirections(data);

	final NavigableSet<Alternative> dominanceOrder;
	if (data.getProfiles().isEmpty()) {
	    dominanceOrder = ExtentionalTotalOrder.create();
	} else {
	    dominanceOrder = new Dominance().getStrictDominanceOrder(data.getProfilesEvaluations(),
		    Intervals.getDirectionsFromScales(data.getScales()));
	}
	if (dominanceOrder == null) {
	    throw new InvalidInputException(
		    "Profiles do not strictly dominate each other, according to their evaluations.");
	}
	final NavigableSet<Alternative> profilesOrder = data.getCatsAndProfs().getProfiles();
	if (!Iterables.elementsEqual(dominanceOrder, profilesOrder)) {
	    throw new InvalidInputException(
		    "Given profiles order is not the same as profiles order according to dominance.");
	}
    }

    /**
     * Ensures that the preference directions are known for every criteria contained in the given data.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if some preference directions are missing.
     */
    public void assertCompletePreferenceDirections(IProblemData data) throws InvalidInputException {
	final Map<Criterion, Interval> scales = data.getScales();
	assertCompletePreferenceDirections(scales);
    }

    /**
     * Ensures that the preference directions are known for every scale.
     * 
     * @param scales
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if some preference directions are missing.
     */
    public void assertCompletePreferenceDirections(Map<Criterion, Interval> scales) throws InvalidInputException {
	for (Criterion criterion : scales.keySet()) {
	    final PreferenceDirection direction = scales.get(criterion).getPreferenceDirection();
	    if (direction == null) {
		throw new InvalidInputException("Unknown preference direction for criterion " + criterion + ".");
	    }
	}
    }

    /**
     * Ensures that all the given criteria have a weight, thus are contained in the given set of criteria having a
     * weight.
     * 
     * @param criteria
     *            not <code>null</code>.
     * @param withWeights
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if some weights are missing.
     */
    public void assertCompleteWeights(Set<Criterion> criteria, Set<Criterion> withWeights) throws InvalidInputException {
	final SetView<Criterion> noWeights = Sets.difference(criteria, withWeights);
	if (!noWeights.isEmpty()) {
	    throw new InvalidInputException("Some criteria have no associated weights: " + noWeights + ".");
	}
    }

    /**
     * Ensures that the given data have coalitions set with a weight for each criteria in the data and have the majority
     * threshold set, and that the majority threshold is not greater than the sum of the weights + the tolerance set for
     * the coalition. The last check ensures that at least one coalition is sufficient.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if the coalitions are incomplete.
     */
    public void assertCompleteCoalitions(ISortingPreferences data) throws InvalidInputException {
	final Coalitions coalitions = data.getCoalitions();
	if (!coalitions.getCriteria().equals(data.getCriteria())) {
	    throw new InvalidInputException("Incorrect coalitions: got " + coalitions.getCriteria() + " instead of "
		    + data.getCriteria() + ".");
	}
	if (!coalitions.containsMajorityThreshold()) {
	    throw new InvalidInputException("Missing majority threshold.");
	}
	if (coalitions.getMajorityThreshold() > coalitions.getWeights().getSum()) {
	    throw new InvalidInputException(
		    "Meaningless coalitions: the majority threshold is greater than the sum of all weights.");
	}
    }

    /**
     * Ensures that the given data contain a preference threshold (which may be zero) for every criteria in the
     * contained data.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if at least one preference threshold is missing.
     */
    public void assertCompletePreferenceThresholds(ISortingPreferences data) throws InvalidInputException {
	final Map<Criterion, Double> preferenceThresholds = data.getThresholds().getPreferenceThresholds();
	final Set<Criterion> criteria = data.getCriteria();
	final SetView<Criterion> noThresholds = Sets.difference(criteria, preferenceThresholds.keySet());
	if (!noThresholds.isEmpty()) {
	    throw new InvalidInputException("No preference threshold found for " + noThresholds.iterator().next() + ".");
	}
    }

    /**
     * Ensures that the given data contain an indifference threshold (which may be zero) for every criteria in the
     * contained data.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if at least one preference threshold is missing.
     */
    public void assertCompleteIndifferenceThresholds(ISortingPreferences data) throws InvalidInputException {
	if (data.getThresholds().getIndifferenceThresholds().size() != data.getCriteria().size()) {
	    throw new InvalidInputException("Incomplete indifference thresholds.");
	}
    }

    /**
     * Ensures that the given object contains an evaluation for every alternative on every criteria contained in the
     * data. Note that this is a stronger condition than checking {@link EvaluationsRead#isComplete()}.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if the evaluations are incomplete.
     */
    public void assertCompleteAlternativesEvaluations(IProblemData data) throws InvalidInputException {
	if (data.getAlternativesEvaluations().getValueCount() != data.getAlternatives().size()
		* data.getCriteria().size()) {
	    throw new InvalidInputException("Invalid number of evaluations provided. Count: "
		    + data.getAlternativesEvaluations().getValueCount() + "; " + data.getAlternatives().size()
		    + " alternatives; " + data.getCriteria().size() + " criteria.");
	}
    }

    /**
     * Ensures that every alternative contained in the given data are assigned.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if some assignments are missing.
     */
    public void assertCompleteAssignments(ISortingAssignments data) throws InvalidInputException {
	if (data.getAssignments().getAlternatives().size() != data.getAlternatives().size()) {
	    throw new InvalidInputException("Not all assignments provided.");
	}
    }

    /**
     * Ensures that every alternative contained in the given data are assigned.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if some assignments are missing.
     */
    public void assertCompleteAssignments(ISortingAssignmentsToMultiple data) throws InvalidInputException {
	if (data.getAssignments().getAlternatives().size() != data.getAlternatives().size()) {
	    throw new InvalidInputException("Not all assignments provided.");
	}
    }

    /**
     * Ensures that the weights contained in the given data are between zero and one, inclusive.
     * 
     * @param data
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if at least one weight is higher than one.
     */
    public void assertWeightsBelowOne(ISortingPreferences data) throws InvalidInputException {
	final Weights weights = data.getCoalitions().getWeights();
	final Collection<Double> tooBig = Collections2.filter(weights.values(), PredicateUtils.greaterThan(1d));
	if (tooBig.size() >= 1) {
	    throw new InvalidInputException("Found weights greater than one: " + tooBig + ".");
	}
    }

    public boolean isConsistentData(ISortingData data) {
	if (data.getAlternativesEvaluations().getValueCount() != data.getAlternatives().size()
		* data.getCriteria().size()) {
	    s_logger.info("Inconsistent data. Alts evals count: " + data.getAlternativesEvaluations().getValueCount()
		    + "; " + data.getAlternatives().size() + " alternatives; " + data.getCriteria().size()
		    + " criteria.");
	    return false;
	}
	if (!data.getCatsAndProfs().isComplete()) {
	    s_logger.info("Inconsistent data. Categories are incomplete.");
	    return false;
	}
	if (data.getCatsAndProfs().getProfiles().size() != data.getProfiles().size()) {
	    s_logger.info("Inconsistent data. Profiles do not match.");
	    return false;
	}
	try {
	    assertCompletePreferenceDirections(data);
	} catch (InvalidInputException exc) {
	    s_logger.info("Inconsistent data.", exc);
	    return false;
	}
	return true;
    }

    /**
     * Ensures that the preference directions are known for every criterion.
     * 
     * @param directions
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if some preference directions are missing.
     */
    public void assertCompletePreferenceDirectionsDirect(Map<Criterion, PreferenceDirection> directions)
	    throws InvalidInputException {
	for (Criterion criterion : directions.keySet()) {
	    final PreferenceDirection direction = directions.get(criterion);
	    if (direction == null) {
		throw new InvalidInputException("Unknown preference direction for criterion " + criterion + ".");
	    }
	}
    }

    public void assertOrThrow(boolean expression, String errorMessage) throws InvalidInputException {
	if (!expression) {
	    throw new InvalidInputException(errorMessage);
	}
    }

    /**
     * <p>
     * Ensures that every assigned alternative contained in the given data are assigned to an interval over the given
     * ordered set of categories, or equivalently, that each alternative is assigned to a subset of the given categories
     * which has no “hole”, <em>and that the order matches</em>. For example, assignment to {C_2, C_1} is an interval in
     * categories {C_1, C_2, C_3} but the order does not match, hence this method will reject such example.
     * </p>
     * <p>
     * The general set of categories stored in the assignments object is not considered.
     * </p>
     * 
     * @param assignments
     *            not <code>null</code>.
     * @param categories
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if some alternatives are assigned to some categories that do not form an interval over the given set
     *             of categories.
     */
    public void assertToOrderedIntervals(IOrderedAssignmentsToMultipleRead assignments,
	    NavigableSet<Category> categories) throws InvalidInputException {
	for (Alternative alternative : assignments.getAlternatives()) {
	    final Set<Category> assignment = assignments.getCategories(alternative);
	    final Category intervalBegin = assignment.iterator().next();
	    final boolean okay = categories.contains(intervalBegin)
		    && Iterables.elementsEqual(Iterables.limit(categories.tailSet(intervalBegin), assignment.size()),
			    assignment);
	    // final Category intervalEnd = assignment.last();
	    // if (!categories.contains(intervalBegin)) {
	    // throw new InvalidInputException("Assignment of " + alternative + " to " + intervalBegin
	    // + ": not in the given set " + categories + ".");
	    // }
	    // if (!categories.contains(intervalEnd)) {
	    // throw new InvalidInputException("Assignment of " + alternative + " to " + intervalEnd
	    // + ": not in the given set " + categories + ".");
	    // }
	    // final NavigableSet<Category> wholeSubset = categories.subSet(intervalBegin, true, intervalEnd, true);
	    // if (wholeSubset.size() != assignment.size()) {
	    if (!okay) {
		throw new InvalidInputException("Assignment of " + alternative + " to " + assignment
			+ " is not an interval in " + categories + ".");
	    }
	}
    }

    /**
     * <p>
     * Ensures that every assigned alternative contained in the given data are assigned to an interval over the given
     * ordered set of categories, or equivalently, that each alternative is assigned to a subset of the given categories
     * which has no “hole”. Order of assignments is not considered in this method.
     * </p>
     * 
     * @param assignments
     *            not <code>null</code>.
     * @param categories
     *            not <code>null</code>.
     * @throws InvalidInputException
     *             if some alternatives are assigned to some categories that do not form an interval over the given set
     *             of categories.
     * @see #assertToOrderedIntervals(IOrderedAssignmentsToMultipleRead, NavigableSet)
     */
    public void assertToIntervals(IAssignmentsToMultipleRead assignments, NavigableSet<Category> categories)
	    throws InvalidInputException {
	for (Alternative alternative : assignments.getAlternatives()) {
	    final Set<Category> assignment = assignments.getCategories(alternative);
	    if (!categories.containsAll(assignment)) {
		throw new InvalidInputException("Assignment of " + alternative + " to " + assignment
			+ " is not an interval in " + categories + ".");
	    }
	}
    }

}
