package org.decisiondeck.xmcda_oo.services.sorting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ElectrePessimisticProgressiveWithArrays {
    private static final Logger s_logger = LoggerFactory.getLogger(ElectrePessimisticProgressiveWithArrays.class);

    private final int m_nbCats;
    /**
     * In array[0], the minimal (worst) category the current alternative may be assigned to; in array[1] the maximal
     * (best) one.
     */
    private final int[] m_catsLimits;
    private final double m_lambda;
    /**
     * In array[crit], the weight of the criterion crit.
     */
    private final double[] m_weights;
    /**
     * In array[cat][0], the min weight for category cat or above ; in array[cat][1], the max weight for category cat.
     */
    private final double[][] m_wLimits;
    /**
     * array[i][j] = evaluation of the profile above category i on the criterion j. worst category is numbered zero;
     * best is numbered nbCats-1.
     */
    private final double[][] m_profs;

    public ElectrePessimisticProgressiveWithArrays(double[] weights, double lambda, double[][] profs) {
	m_weights = weights;
	m_lambda = lambda;
	m_profs = profs;

	m_catsLimits = new int[2];
	m_nbCats = profs.length + 1;
	m_wLimits = new double[m_nbCats][2];
	reset();
    }

    public int getWorstCat() {
	return m_catsLimits[0];
    }

    public int getBestCat() {
	return m_catsLimits[1];
    }

    public void reset() {
	for (int i = 0; i < m_wLimits.length; ++i) {
	    m_wLimits[i][0] = 0d;
	    m_wLimits[i][1] = 1d;
	}
	m_catsLimits[0] = 0;
	m_catsLimits[1] = m_wLimits.length - 1;
    }

    /**
     * @param crit
     * @param interval
     *            should be begin inclusive, end not inclusive.
     * @return
     */
    public int[] setEvaluation(int crit, double[] interval) {
	final double minEval = interval[0];
	final double maxEval = interval[1];
	for (int cat = m_catsLimits[0]; cat <= m_catsLimits[1]; ++cat) {
	    if (cat == 0) {
		m_wLimits[cat][0] += m_weights[crit];
	    } else {
		final double profBelow = m_profs[cat - 1][crit];
		if (minEval >= profBelow) {
		    m_wLimits[cat][0] += m_weights[crit];
		    if (m_wLimits[cat][0] >= m_lambda) {
			m_catsLimits[0] = cat;
		    }
		}
		if (maxEval <= profBelow) {
		    m_wLimits[cat][1] -= m_weights[crit];
		    if (m_wLimits[cat][1] < m_lambda) {
			m_catsLimits[1] = cat - 1;
		    }
		}
	    }
	    s_logger.debug("Weights limits for cat " + cat + ": {} (lambda=" + m_lambda + ").", m_wLimits[cat]);
	    if (m_wLimits[cat][0] > m_wLimits[cat][1] + 1e-6) {
		throw new IllegalStateException("Min weight greater than max weight.");
	    }
	}
	s_logger.debug("Evaluation {} set for crit " + crit + ".", interval);
	return m_catsLimits;
    }

    public boolean isSetCat() {
	return m_catsLimits[0] == m_catsLimits[1];
    }

}
