package org.decisiondeck.jmcda.services.outranking;

import java.util.HashMap;
import java.util.Map;

import org.decision_deck.jmcda.services.ConsistencyChecker;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.MatrixesMC;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decisiondeck.jmcda.exc.InputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class Discordance {
    private static final Logger s_logger = LoggerFactory.getLogger(Discordance.class);
    private boolean m_sharpVetoes;
    private Double m_smallestSep;
    private double m_internalSmallestSep;

    public Discordance() {
	m_smallestSep = null;
	m_sharpVetoes = false;
	m_internalSmallestSep = Double.POSITIVE_INFINITY;
    }

    /**
     * <p>
     * Computes the discordance matrixes for each criterion in the given data.
     * </p>
     * <p>
     * For the input to be valid, all evaluations must be given, all preference directions must be defined, the set of
     * criteria on which thresholds are given must be a subset of the set of criteria in the problem data, the veto
     * thresholds must be greater than or equal to the preference threshold for each criteria. Otherwise, an
     * {@link InvalidInputException} is thrown.
     * </p>
     * 
     * @param data
     *            not <code>null</code>.
     * @param thresholds
     *            not <code>null</code>, only the preferences and vetoes thresholds are used, some may be missing. If
     *            this object uses sharp vetoes, only the vetoes are used.
     * @return not <code>null</code>, one entry per criteria, no <code>null</code> value.
     * @throws InvalidInputException
     *             if the input is not valid.
     */
    public Map<Criterion, SparseAlternativesMatrixFuzzy> discordances(IProblemData data,
	    Thresholds thresholds) throws InvalidInputException {
	final ConsistencyChecker consistencyChecker = new ConsistencyChecker();
	consistencyChecker.assertCompleteAlternativesEvaluations(data);
	consistencyChecker.assertCompletePreferenceDirections(data);
	InputCheck.check(data.getCriteria().containsAll(thresholds.getCriteria()),
		"Thresholds are defined on some unknown criterion.");
	final Map<Criterion, SparseAlternativesMatrixFuzzy> discordanceMatrixes = new HashMap<Criterion, SparseAlternativesMatrixFuzzy>();
	if (m_sharpVetoes) {
	    m_internalSmallestSep = Double.POSITIVE_INFINITY;
	    m_smallestSep = null;
	}

	for (final Criterion crit : data.getCriteria()) {
	    s_logger.debug("Computing discordance matrix of " + crit + ".");
	    final SparseAlternativesMatrixFuzzy discMatrix = MatrixesMC.newAlternativesFuzzy();
	    for (final Alternative alt1 : data.getAlternatives()) {
		for (final Alternative alt2 : data.getAlternatives()) {
		    final double perf1 = data.getAlternativesEvaluations().getEntry(alt1, crit).doubleValue();
		    final double perf2 = data.getAlternativesEvaluations().getEntry(alt2, crit).doubleValue();
		    final PreferenceDirection direction = data.getScales().get(crit).getPreferenceDirection();
		    final double value;
		    if (!thresholds.containsVetoThreshold(crit)) {
			value = 0;
		    } else {
			final Double vetoThreshold = Double.valueOf(thresholds.getVetoThreshold(crit));
			if (m_sharpVetoes) {
			    final boolean valueBin = discordancePairwizeBinary(perf1, perf2, crit, direction,
				    vetoThreshold);
			    value = valueBin ? 1 : 0;
			} else {
			    final double pValue = thresholds.containsPreferenceThreshold(crit) ? thresholds
				    .getPreferenceThreshold(crit) : 0;
			    value = discordancePairwize(perf1, perf2, crit, direction, pValue, vetoThreshold);
			}
		    }
		    discMatrix.put(alt1, alt2, value);
		    /** Big perf impact, apparently! */
		    // s_logger.debug("Discordance of {} over {} = " + value + ".", alt1, alt2);
		}
	    }
	    discordanceMatrixes.put(crit, discMatrix);
	}
	if (!Double.isInfinite(m_internalSmallestSep)) {
	    m_smallestSep = Double.valueOf(m_internalSmallestSep);
	}

	return discordanceMatrixes;
    }

    /**
     * Evaluates the discordance between two alternatives having the given evaluations.
     * 
     * @param eval1
     *            the evaluation of the first alternative from the point of view of the considered criterion.
     * @param eval2
     *            the evaluation of the second alternative from the point of view of the considered criterion.
     * @param criterion
     *            only used for the error message string in case of exception, may be <code>null</code>.
     * @param direction
     *            not <code>null</code>.
     * @param p
     *            the preference threshold of the considered criterion.
     * @param v
     *            the veto threshold of the considered criterion. Must be greater than or equal to the preference
     *            threshold. If <code>null</code>, the returned discordance is necessarily zero. Note that veto is
     *            always positive or nul, irrespective of the preference direction.
     * @return the discordance of the given criterion over the fact that the first alternative (having the first
     *         evaluation) would be preferred to the second one (with the second evaluation).
     * @throws InvalidInputException
     *             if the veto threshold is lower than or equal to the preference threshold.
     */
    public double discordancePairwize(double eval1, double eval2, Criterion criterion, PreferenceDirection direction,
	    double p, Double v) throws InvalidInputException {
	Preconditions.checkNotNull(direction);
	if (v == null) {
	    return 0;
	}
	final double perfDiff;
	switch (direction) {
	case MAXIMIZE:
	    perfDiff = eval2 - eval1;
	    break;
	case MINIMIZE:
	    perfDiff = -(eval2 - eval1);
	    break;
	default:
	    throw new IllegalStateException("Criterion " + criterion + " is not to be minimized nor maximized.");
	}

	final double vThresh = v.doubleValue();
	if (vThresh < p) {
	    throw new InvalidInputException("Veto threshold is lower that preference threshold (crit " + criterion
		    + ").");
	}
	final double disc;
	if (perfDiff <= p) {
	    disc = 0;
	} else if (perfDiff > vThresh) {
	    disc = 1;
	} else {
	    assert (p != vThresh);
	    disc = (perfDiff - p) / (vThresh - p);
	}
	return disc;

    }

    /**
     * Evaluates the binary discordance between two alternatives having the given evaluations: returns <code>true</code>
     * (a discordance situation) iff the evaluation difference is greater than the given veto.
     * 
     * @param eval1
     *            the evaluation of the first alternative from the point of view of the considered criterion (on the
     *            criterion scale).
     * @param eval2
     *            the evaluation of the second alternative from the point of view of the considered criterion (on the
     *            criterion scale).
     * @param criterion
     *            only used for the error message string in case of exception, may be <code>null</code>.
     * @param direction
     *            not <code>null</code>.
     * @param vetoThreshold
     *            the veto threshold of the considered criterion. If <code>null</code>, this method necessarily returns
     *            <code>false</code>.
     * @return the discordance of the given criterion over the fact that the first alternative (having the first
     *         evaluation) would be preferred to the second one (with the second evaluation).
     */
    private boolean discordancePairwizeBinary(double eval1, double eval2, Criterion criterion,
	    PreferenceDirection direction, Double vetoThreshold) {
	if (vetoThreshold == null) {
	    return false;
	}
	final double perfDiff;
	switch (direction) {
	case MAXIMIZE:
	    perfDiff = eval2 - eval1;
	    break;
	case MINIMIZE:
	    perfDiff = -(eval2 - eval1);
	    break;
	default:
	    throw new IllegalStateException("Criterion " + criterion + " is not to be minimized nor maximized.");
	}

	final double diff = perfDiff - vetoThreshold.doubleValue();
	final double sep = Math.abs(diff);
	if (sep < m_internalSmallestSep) {
	    m_internalSmallestSep = sep;
	}
	return perfDiff > vetoThreshold.doubleValue();
    }

    public boolean isSharpVetoes() {
	return m_sharpVetoes;
    }

    public void setSharpVetoes(boolean sharpVetoes) {
	m_sharpVetoes = sharpVetoes;
    }

    /**
     * Retrieves the smallest difference, in absolute value, between any veto and any difference of performance used in
     * the computation, if sharp vetoes are to be used. Useful for sensitivity analysis or to check for possible
     * numerical errors. The number is positive or nul. It represents the largest quantity that may be added or
     * substracted from all the veto thresholds without changing the discordance relation.
     * 
     * @return <code>null</code> if no computation have been preformed or no values were found when asked for a
     *         computation or sharp vetoes are not used.
     */
    public Double getSmallestSep() {
	return m_smallestSep;
    }

}
