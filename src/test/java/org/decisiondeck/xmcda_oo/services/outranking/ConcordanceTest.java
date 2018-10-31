package org.decisiondeck.xmcda_oo.services.outranking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.utils.matrix.OneMinusInverseMatrix;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.decisiondeck.jmcda.services.outranking.Concordance;
import org.junit.Test;

public class ConcordanceTest {

    @Test
    public void testConcordance() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final Concordance conc = new Concordance();
	final SparseMatrixFuzzy<Alternative, Alternative> res = conc.concordance(testData.getAsProblemData(),
		testData.getThresholds(), testData.getWeights());
	assertTrue("Concordance does not match.", testData.getConcordance().approxEquals(res, 0.00005f));
    }

    @Test
    public void testConcordanceEquality() throws Exception {
	final Concordance conc = new Concordance();
	// final EvaluationMatrix eval = EvaluationsUtils.newEvaluationMatrix();
	// final Alternative alt1 = new Alternative("01");
	// final Alternative alt2 = new Alternative("02");
	// final Criterion g = new Criterion("g1", PreferenceDirection.MAXIMIZE);
	// eval.put(alt1, g, 3);
	// eval.put(alt2, g, 3);
	final double res = conc.concordancePairwize(3, 3, PreferenceDirection.MAXIMIZE, 0d, 0d);
	assertEquals("Electre1-style concordance for equal perf must be one.", 1d, res, 0.00005f);
    }

    @Test
    public void testPreference() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final Concordance conc = new Concordance();
	final SparseMatrixFuzzy<Alternative, Alternative> res = conc.preference(testData.getAsProblemData(),
		testData.getThresholds(), testData.getWeights());
	assertTrue("Preference does not match.", testData.getPreference().approxEquals(res, 0.00005f));
    }

    @Test
    public void testPreferenceEqualsOneMinusInverseConcordance() throws Exception {
	/**
	 * This uses the fact that Promethee preference on (a, b) equals one minus Electre concordance on (b, a) (with
	 * the appropriate thresholds, etc.).
	 */
	final SixRealCars testData = SixRealCars.getInstance();
	final OneMinusInverseMatrix<Alternative> resInverted = new OneMinusInverseMatrix<Alternative>(
		testData.getConcordance(), 1d);
	final OneMinusInverseMatrix<Alternative> resInvertedTo2 = new OneMinusInverseMatrix<Alternative>(
		testData.getConcordance(), 2d);
	assertTrue(testData.getPreference().approxEquals(resInverted, 0.00005f));
	assertFalse(testData.getPreference().approxEquals(resInvertedTo2, 0.00005f));
    }

    @Test
    public void testPreferenceEquality() throws Exception {
	final Concordance conc = new Concordance();
	final double res = conc.preferencePairwize(3, 3, PreferenceDirection.MAXIMIZE, 0d, 0d);
	assertEquals("Promethee-style preference for equal perf must be zero.", 0d, res, 0.00005f);
    }
}
