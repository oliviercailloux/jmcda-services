package org.decisiondeck.jmcda.services.outranking;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.MatrixesMC;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.utils.matrix.SparseMatrixFuzzyRead;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO optimize for speed (~ 500 alts, 3 crits => more than one second needed).
 * 
 * Uses a tolerance (a number positive or zero). The coalition (sum of weights) is considered as winning (in favor of
 * the outranking) even if it is below the cut (majority) threshold when it is not more below than tolerance. This
 * permits to avoid numerical errors (e.g. round the weights then the winning coalitions change).
 * 
 * @author Olivier Cailloux
 * 
 */
public class Outranking {
    private static final Logger s_logger = LoggerFactory.getLogger(Outranking.class);
    private Double m_smallestSep;
    private double m_tolerance;
    static public final double DEFAULT_TOLERANCE = 1e-5d;

    public Outranking() {
	m_smallestSep = null;
	m_tolerance = DEFAULT_TOLERANCE;
    }

    public SparseAlternativesMatrixFuzzy getOutranking(Set<Alternative> alts, Set<Criterion> crits,
	    SparseMatrixFuzzyRead<Alternative, Alternative> concs,
	    Map<Criterion, ? extends SparseMatrixFuzzyRead<Alternative, Alternative>> discs) throws InvalidInputException {
	return getOutrankingWithCut(alts, crits, concs, discs, null);
    }

    /**
     * Computes an outranking relation as in electre methods.
     * 
     * @param alts
     *            the alternatives to compute the relation on. Not <code>null</code>.
     * @param crits
     *            the criteria to consider. Not <code>null</code>.
     * @param concs
     *            the (between zero and one or binary) concordance relation to use.
     * @param discs
     *            the (between zero and one or binary) discordance relation to use.
     * @param cutThreshold
     *            to majority threshold, between zero and one, where to cut the outranking relation to one when greater
     *            than or equal to the threshold. <code>null</code> for no cut.
     * @return the computed outranking relation, between zero and one iff the concordance or discordance relation is
     *         between zero and one and no cutting threshold is used.
     * @throws InvalidInputException
     *             if a concordance or discordance entry is missing.
     */
    public SparseAlternativesMatrixFuzzy getOutrankingWithCut(Set<Alternative> alts, Set<Criterion> crits,
	    SparseMatrixFuzzyRead<Alternative, Alternative> concs,
	    Map<Criterion, ? extends SparseMatrixFuzzyRead<Alternative, Alternative>> discs, Double cutThreshold)
	    throws InvalidInputException {
	checkArgument(cutThreshold == null || (cutThreshold.doubleValue() >= 0 && cutThreshold.doubleValue() <= 1),
		"Invalid " + cutThreshold + ".");
	if (cutThreshold != null) {
	    m_smallestSep = null;
	}
	final SparseAlternativesMatrixFuzzy outranking = MatrixesMC.newAlternativesFuzzy();

	double smallestSep = Double.POSITIVE_INFINITY;
	for (final Alternative alt1 : alts) {
	    for (final Alternative alt2 : alts) {
		final Double entry = concs.getEntry(alt1, alt2);
		if (entry == null) {
		    throw new InvalidInputException("Missing concordance entry at " + alt1 + ", " + alt2 + ".");
		}
		final double c = entry.doubleValue();
		final double complC = 1 - c;

		double outr = c;
		for (final Criterion crit : crits) {
		    final Double discEntry = discs.get(crit).getEntry(alt1, alt2);
		    if (discEntry == null) {
			throw new InvalidInputException("Missing discordance entry at " + alt1 + ", " + alt2 + ", "
				+ crit + ".");
		    }
		    final double disc = discEntry.doubleValue();
		    if (disc > c) {
			outr = outr * (1 - disc) / complC;
		    } else if (disc == 1) {
			/** TODO check that case. */
			outr = 0;
		    }
		}
		if (cutThreshold == null) {
		    outranking.put(alt1, alt2, outr);
		    s_logger.debug("Outranking of {} over {} = " + outr + ".", alt1, alt2);
		} else {
		    final double diff = outr - cutThreshold.doubleValue();
		    final double sep = Math.abs(diff);
		    if (sep < smallestSep) {
			smallestSep = sep;
		    }
		    final int value = diff >= -m_tolerance ? 1 : 0;
		    outranking.put(alt1, alt2, value);
		    s_logger.debug("Outranking of {} over {} = " + outr + " (after cut: " + value + ").", alt1, alt2);
		}
	    }
	}
	if (!Double.isInfinite(smallestSep)) {
	    m_smallestSep = Double.valueOf(smallestSep);
	}

	return outranking;
    }

    public double getTolerance() {
	return m_tolerance;
    }

    public void setTolerance(double tolerance) {
	m_tolerance = tolerance;
    }

    /**
     * Retrieves the smallest difference, in absolute value, between the cut and any outranking value. Useful for
     * sensitivity analysis or to check for possible numerical errors. The number is positive or nul. It represents the
     * largest quantity that may be added or substracted from the cut without changing the outranking relation.
     * 
     * @return <code>null</code> if no outranking with cut has been asked or no values were found when asked for an
     *         outranking (empty set of alternatives).
     */
    public Double getSmallestSep() {
	return m_smallestSep;
    }
}
