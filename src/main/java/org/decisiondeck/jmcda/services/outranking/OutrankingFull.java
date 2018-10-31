package org.decisiondeck.jmcda.services.outranking;

import java.util.Map;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;

public class OutrankingFull {

    public OutrankingFull() {
	m_sharpVetoes = false;
	m_smallestSep = null;
	m_tolerance = Outranking.DEFAULT_TOLERANCE;
    }

    private boolean m_sharpVetoes;
    private Double m_smallestSep;
    private double m_tolerance;

    /**
     * <p>
     * Computes an outranking relation as in electre methods. The returned matrix is fuzzy iff the concordance or
     * discordance relation is fuzzy and no cutting threshold is given.
     * </p>
     * <p>
     * For the input to be valid, the weights must be all provided, all evaluations must be provided, all criteria must
     * have preference directions, the set of criteria on which thresholds are defined must be in the set of criteria,
     * the preference threshold must be greater or equal to the indifference threshold for each criteria, the veto
     * thresholds must be greater than or equal to the preference threshold for each criteria. Otherwise, an
     * InvalidInputException is thrown.
     * </p>
     * 
     * @param data
     *            not <code>null</code>.
     * @param thresholds
     *            the thresholds to use (missing thresholds are accepted). Not <code>null</code>.
     * @param coalitions
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             iff the input is not valid.
     */
    public SparseAlternativesMatrixFuzzy getOutranking(IProblemData data, Thresholds thresholds,
	    Coalitions coalitions) throws InvalidInputException {

	final SparseMatrixFuzzy<Alternative, Alternative> concs = new Concordance().concordance(data, thresholds,
		coalitions.getWeights());

	final Discordance discordance = new Discordance();
	discordance.setSharpVetoes(m_sharpVetoes);
	final Map<Criterion, SparseAlternativesMatrixFuzzy> discs = discordance.discordances(data, thresholds);
	Double discordanceSmallestSep = discordance.getSmallestSep();

	final Outranking outranking = new Outranking();
	outranking.setTolerance(m_tolerance);
	final Double cutThreshold = !coalitions.containsMajorityThreshold() ? null : Double.valueOf(coalitions
		.getMajorityThreshold() / coalitions.getWeights().getSum());
	// TODO find some way to round properly.
	final Double cutThresholdRound = cutThreshold == null ? null : cutThreshold.doubleValue() < 1 + 1e6
		&& cutThreshold.doubleValue() > 1 ? Double.valueOf(1d) : cutThreshold;
	final SparseAlternativesMatrixFuzzy outrankingMatrix = outranking.getOutrankingWithCut(data.getAlternatives(),
		data.getCriteria(), concs, discs, cutThresholdRound);

	m_smallestSep = outranking.getSmallestSep();
	if (discordanceSmallestSep != null
		&& (m_smallestSep == null || discordanceSmallestSep.doubleValue() < m_smallestSep.doubleValue())) {
	    m_smallestSep = discordanceSmallestSep;
	}

	return outrankingMatrix;
    }

    public boolean isSharpVetoes() {
	return m_sharpVetoes;
    }

    public void setSharpVetoes(boolean sharpVetoes) {
	m_sharpVetoes = sharpVetoes;
    }

    /**
     * Retrieves the smallest difference, in absolute value, between the majority threshold and any outranking value,
     * and between any veto threshold and any difference of performance over the computed values, if sharp vetoes are to
     * be used. Useful to check for possible numerical errors. The number is positive or nul. If using sharp vetoes, it
     * represents the largest quantity that may be added or substracted from the majority threshold and all veto
     * thresholds without changing the outranking relation.
     * 
     * @return <code>null</code> if no outranking computation has been asked or no values were found when asked for an
     *         outranking (because of an empty set of alternatives).
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
}
