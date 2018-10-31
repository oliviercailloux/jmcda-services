package org.decisiondeck.jmcda.services.outranking;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.services.ConsistencyChecker;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.matrix.MatrixesMC;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.weights.Weights;
import org.decision_deck.utils.matrix.SparseMatrixFuzzyRead;
import org.decisiondeck.jmcda.exc.InputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * A concordance utility allowing to compute electre-style concordance indices and promethee-style preference indices.
 * 
 * TODO generalize to accept preference and indifference threshold <em>functions</em>, not only values (function of the
 * evaluation).
 * 
 * @author Olivier Cailloux
 * 
 */
public class Concordance {
    private static final Logger s_logger = LoggerFactory.getLogger(Concordance.class);
    private Set<Alternative> m_rows;
    private Set<Alternative> m_columns;

    public Concordance() {
	m_columns = null;
	m_rows = null;
    }

    /**
     * For the input to be valid, all weights must be provided, all criteria must have preference directions, all
     * evaluations must be provided, the set of criteria on which thresholds and weights are defined must be in the set
     * of criteria, the preference threshold must be greater or equal to the indifference threshold for each criteria.
     * Otherwise, an {@link InvalidInputException} is thrown. The scales are not used apart from the preference
     * direction information.
     * 
     * @param data
     *            not <code>null</code>.
     * @param thresholds
     *            not <code>null</code>, may be incomplete.
     * @param weights
     *            not <code>null</code>.
     * @param prometheeStyle
     *            <code>true</code> to compute a preference matrix, as promethee calls it.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             if the input is not valid.
     */
    private SparseAlternativesMatrixFuzzy computeMatrix(IProblemData data, Thresholds thresholds,
	    Weights weights,
	    boolean prometheeStyle) throws InvalidInputException {
	InputCheck.check(m_rows == null || data.getAlternatives().containsAll(m_rows),
		"Restriction on rows is not a subset of the given alternatives.");
	InputCheck.check(m_columns == null || data.getAlternatives().containsAll(m_columns),
		"Restriction on columns is not a subset of the given alternatives.");
	InputCheck.check(data.getCriteria().containsAll(weights.keySet()),
		"Some weights are defined on unknown criteria.");
	new ConsistencyChecker().assertCompleteWeights(data.getCriteria(), weights.keySet());
	InputCheck.check(data.getCriteria().containsAll(thresholds.getCriteria()),
		"Some thresholds are defined on unknown criteria.");
	final ConsistencyChecker consistencyChecker = new ConsistencyChecker();
	consistencyChecker.assertCompleteAlternativesEvaluations(data);
	consistencyChecker.assertCompletePreferenceDirections(data);

	final String debugStr = prometheeStyle ? "preference" : "concordance";
	final Set<Alternative> rows = m_rows == null ? data.getAlternatives() : m_rows;
	final Set<Alternative> columns = m_columns == null ? data.getAlternatives() : m_columns;

	final SparseAlternativesMatrixFuzzy results = MatrixesMC.newAlternativesFuzzy();
	for (final Alternative alt1 : rows) {
	    for (final Alternative alt2 : columns) {
		double total = 0;
		for (final Criterion crit : data.getCriteria()) {
		    final PreferenceDirection direction = data.getScales().get(crit).getPreferenceDirection();
		    final double weightVal = weights.getWeightBetter(crit);
		    final double p = thresholds.containsPreferenceThreshold(crit) ? thresholds
			    .getPreferenceThreshold(crit) : 0;
		    final double q = thresholds.containsIndifferenceThreshold(crit) ? thresholds
			    .getIndifferenceThreshold(crit) : 0;
		    final double evalAlt1 = data.getAlternativesEvaluations().getEntry(alt1, crit).doubleValue();
		    final double evalAlt2 = data.getAlternativesEvaluations().getEntry(alt2, crit).doubleValue();
		    total += (weightVal * concordanceOrPreferencePairwize(crit, evalAlt1, evalAlt2, direction, p, q,
			    prometheeStyle));
		}
		s_logger.debug("Total " + debugStr + " {} over {} = " + total + ".", alt1, alt2);
		total = total / weights.getSum();
		if (total > 1d) {
		    if (total > 1.1d) {
			throw new IllegalStateException("More than 10% error: total = " + total + ", should be ≤ 1.");
		    }
		    total = 1d;
		}
		results.put(alt1, alt2, total);
	    }
	}
	return results;
    }

    /**
     * <p>
     * Computes a matrix containing the preference indices, a la Promethee, for all the alternatives in the given data.
     * The returned matrix is complete.
     * </p>
     * <p>
     * For the input to be valid, all weights must be provided, all criteria must have preference directions, all
     * evaluations must be provided, the set of criteria on which thresholds are defined must be in the set of criteria,
     * the preference threshold must be greater or equal to the indifference threshold for each criteria. Otherwise, an
     * {@link InvalidInputException} is thrown. The scales are not used apart from the preference direction information.
     * </p>
     * 
     * 
     * @param data
     *            not <code>null</code>.
     * @param thresholds
     *            not <code>null</code>. Missing thresholds are considered zero. Vetoes are not used.
     * @param weights
     *            must contain a weight for each of the criteria to consider. Not <code>null</code>. Do not have to be
     *            normalized.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             if the input is not valid.
     */
    public SparseAlternativesMatrixFuzzy preference(IProblemData data, Thresholds thresholds,
	    Weights weights)
	    throws InvalidInputException {
	return computeMatrix(data, thresholds, weights, true);
    }

    /**
     * Computes the concordance index between two alternatives on a given criterion, i.e. the degree to which the first
     * alternative is preferred (à la Electre) to the second, on a zero to one scale, from the point of view of the
     * given criterion (and not considering discordance and vetoes).
     * 
     * @param evalAlt1
     *            evaluation of the first alternative.
     * @param evalAlt2
     *            evaluation of the second alternative.
     * @param direction
     *            not <code>null</code>.
     * @param p
     *            the preference threshold.
     * @param q
     *            the indifference threshold.
     * @return a number between zero and one.
     * @throws InvalidInputException
     *             if the preference threshold is strictly smaller than the indifference threshold.
     */
    public double concordancePairwize(double evalAlt1, double evalAlt2, PreferenceDirection direction, double p,
	    double q) throws InvalidInputException {
	return concordanceOrPreferencePairwize(null, evalAlt1, evalAlt2, direction, p, q, false);
    }

    public double preferencePairwize(double evalAlt1, double evalAlt2, PreferenceDirection direction, double p, double q)
	    throws InvalidInputException {
	return concordanceOrPreferencePairwize(null, evalAlt1, evalAlt2, direction, p, q, true);
    }

    private double concordanceOrPreferencePairwize(Criterion crit, double evalAlt1, double evalAlt2,
	    PreferenceDirection direction, double p, double q, boolean prometheeStyle) throws InvalidInputException {
	checkNotNull(direction);
	final double perf1 = evalAlt1;
	final double perf2 = evalAlt2;
	final double perfDiff;
	switch (direction) {
	case MAXIMIZE:
	    perfDiff = perf1 - perf2;
	    break;
	case MINIMIZE:
	    perfDiff = -(perf1 - perf2);
	    break;
	default:
	    throw new IllegalStateException();
	}

	if (p < q) {
	    final String message = crit == null ? "Preference treshold (" + p
		    + ") is smaller than indifference threshold (" + q + ")." : "Criterion " + crit
		    + " has preference treshold (" + p + ") smaller than indifference threshold (" + q + ").";
	    throw new InvalidInputException(message);
	}

	if (p == q && perfDiff == p) {
	    return prometheeStyle ? 0 : 1;
	}

	final double firstThresh;
	final double secondThresh;
	if (prometheeStyle) {
	    firstThresh = q;
	    secondThresh = p;
	} else {
	    firstThresh = -p;
	    secondThresh = -q;
	}

	final double concordanceIndication;
	if (perfDiff <= firstThresh) {
	    concordanceIndication = 0;
	} else if (perfDiff >= secondThresh) {
	    concordanceIndication = 1;
	} else {
	    /**
	     * Here we know that secondThresh != firstThresh (otherwise, impossible to reach here). So we may divide.
	     */
	    concordanceIndication = ((perfDiff - firstThresh) / (secondThresh - firstThresh));
	}

	return concordanceIndication;
    }

    public Map<Criterion, SparseAlternativesMatrixFuzzy> concordances(IProblemData data, Thresholds thresholds)
	    throws InvalidInputException {
	checkNotNull(data);
	checkNotNull(thresholds);
	InputCheck.check(m_rows == null || data.getAlternatives().containsAll(m_rows),
		"Restriction on rows is not a subset of the given alternatives.");
	InputCheck.check(m_columns == null || data.getAlternatives().containsAll(m_columns),
		"Restriction on columns is not a subset of the given alternatives.");
	InputCheck.check(data.getCriteria().containsAll(thresholds.getCriteria()),
		"Some thresholds are defined on unknown criteria.");
	final ConsistencyChecker consistencyChecker = new ConsistencyChecker();
	consistencyChecker.assertCompleteAlternativesEvaluations(data);
	consistencyChecker.assertCompletePreferenceDirections(data);

	final boolean prometheeStyle = false;

	final Set<Alternative> rows = m_rows == null ? data.getAlternatives() : m_rows;
	final Set<Alternative> columns = m_columns == null ? data.getAlternatives() : m_columns;

	final Map<Criterion, SparseAlternativesMatrixFuzzy> allConcs = new HashMap<Criterion, SparseAlternativesMatrixFuzzy>();
	for (Criterion criterion : data.getCriteria()) {
	    /**
	     * Here we could restrict the data to only one criterion using a view, give this criterion a weight of 1,
	     * then simply:
	     */
	    // final IAltFuzzyMatrix concs = concordance(data, thresholds, weights);

	    final SparseAlternativesMatrixFuzzy concs = MatrixesMC.newAlternativesFuzzy();
	    allConcs.put(criterion, concs);
	    final PreferenceDirection direction = data.getScales().get(criterion).getPreferenceDirection();
	    for (final Alternative alt1 : rows) {
		for (final Alternative alt2 : columns) {
		    final double p = thresholds.containsPreferenceThreshold(criterion) ? thresholds
			    .getPreferenceThreshold(criterion) : 0;
		    final double q = thresholds.containsIndifferenceThreshold(criterion) ? thresholds
			    .getIndifferenceThreshold(criterion) : 0;
		    final double evalAlt1 = data.getAlternativesEvaluations().getEntry(alt1, criterion).doubleValue();
		    final double evalAlt2 = data.getAlternativesEvaluations().getEntry(alt2, criterion).doubleValue();
		    final double conc = concordanceOrPreferencePairwize(criterion, evalAlt1, evalAlt2, direction, p, q,
			    prometheeStyle);
		    final String debugStr = prometheeStyle ? "preference" : "concordance";
		    s_logger.debug("Resulting " + debugStr + " {} over {} = " + conc + ".", alt1, alt2);
		    concs.put(alt1, alt2, conc);
		}
	    }
	}
	return allConcs;
    }

    /**
     * Restricts the rows of the matrixes this object returns to those given. This must be a subset of the rows normally
     * returned. This call only affects computations asked for after this method has been called, already computed
     * objects are not affected.
     * 
     * @param rows
     *            <code>null</code> for no restriction.
     */
    public void setTargetRows(Set<Alternative> rows) {
	m_rows = rows == null ? null : Sets.newLinkedHashSet(rows);
    }

    /**
     * Restricts the columns of the matrixes this object returns to those given. This must be a subset of the rows
     * normally returned. This call only affects computations asked for after this method has been called, already
     * computed objects are not affected.
     * 
     * @param columns
     *            <code>null</code> for no restriction.
     */
    public void setTargetColumns(Set<Alternative> columns) {
	m_columns = columns == null ? null : Sets.newLinkedHashSet(columns);
    }

    /**
     * <p>
     * Computes a matrix containing the concordance indices for all the alternatives in the given data. The returned
     * matrix is complete.
     * </p>
     * <p>
     * For the input to be valid, all weights must be provided, all criteria must have preference directions, all
     * evaluations must be provided, the set of criteria on which thresholds are defined must be in the set of criteria,
     * the preference threshold must be greater or equal to the indifference threshold for each criteria. Otherwise, an
     * {@link InvalidInputException} is thrown. The scales are not used apart from the preference direction information.
     * </p>
     * 
     * 
     * @param data
     *            not <code>null</code>.
     * @param thresholds
     *            not <code>null</code>. Missing thresholds are considered zero. Vetoes are not used.
     * @param weights
     *            must contain a weight for each of the criteria to consider. Not <code>null</code>. Do not have to be
     *            normalized.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             if the input is not valid.
     */
    public SparseAlternativesMatrixFuzzy concordance(IProblemData data, Thresholds thresholds,
	    Weights weights)
	    throws InvalidInputException {
	return computeMatrix(data, thresholds, weights, false);
    }

    @Deprecated
    // think again for a better interface.
    static public Map<Criterion, ? extends SparseMatrixFuzzyRead<Alternative, Alternative>> concordances(
	    ISortingPreferences sortingProblem) throws InvalidInputException {
	final Concordance concordance = new Concordance();
	concordance.setTargetRows(sortingProblem.getAlternatives());
	final Set<Alternative> columns = sortingProblem.getProfiles();
	final IProblemData data = ProblemFactory.newProblemData(
		EvaluationsUtils.merge(sortingProblem.getAlternativesEvaluations(),
			sortingProblem.getProfilesEvaluations()), sortingProblem.getScales());
	InputCheck.check(data.getAlternatives().containsAll(columns),
		"Restriction on columns is not a subset of the given alternatives.");
	concordance.setTargetColumns(columns);
	return concordance.concordances(data, sortingProblem.getThresholds());
    }

    @Deprecated
    static public Map<Criterion, ? extends SparseMatrixFuzzyRead<Alternative, Alternative>> concordances(
	    Set<Alternative> targetRows, Set<Alternative> targetColumns, EvaluationsRead evaluations,
	    Map<Criterion, PreferenceDirection> directions) throws InvalidInputException {
	final Concordance concordance = new Concordance();
	concordance.setTargetRows(targetRows);
	final Set<Alternative> columns = targetColumns;
	final IProblemData data = ProblemFactory.newProblemData(evaluations,
		Intervals.getScalesFromDirections(directions));
	InputCheck.check(data.getAlternatives().containsAll(columns),
		"Restriction on columns is not a subset of the given alternatives.");
	concordance.setTargetColumns(columns);
	return concordance.concordances(data, null);
    }

}
