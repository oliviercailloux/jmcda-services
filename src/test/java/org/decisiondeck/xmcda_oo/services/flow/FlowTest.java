package org.decisiondeck.xmcda_oo.services.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.scores.AlternativesScores;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.junit.Test;

public class FlowTest {
    @Test
    public void testNetFlows() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final Flow flowService = new Flow();
	final SparseMatrixFuzzy<Alternative, Alternative> preference = testData.getPreference();
	final AlternativesScores flowsComp = flowService.getNetFlows(preference);
	final AlternativesScores expected = testData.getNetFlows();
	assertTrue("Scores do not match.", expected.approxEquals(flowsComp, 0.00005f));
    }

    @Test
    public void testFlowsWithConc() throws Exception {
	/** Let's try to cheat by using concordance instead of preference. The resulting net flows should be the same. */
	final SixRealCars testData = SixRealCars.getInstance();
	final Flow flowService = new Flow();
	final SparseMatrixFuzzy<Alternative, Alternative> concordance = testData.getConcordance();
	final AlternativesScores flowsComp = flowService.getNetFlows(concordance);
	final AlternativesScores expected = testData.getNetFlows();
	assertTrue("Flows should also match when using concordance.", expected.approxEquals(flowsComp, 0.00005f));
    }

    @Test
    public void testPositiveFlowsDiffer() throws Exception {
	/**
	 * Contrary to the net flows, the positive (or negative) flows should change if we substitute concordance to
	 * preference.
	 */
	final SixRealCars testData = SixRealCars.getInstance();
	final Flow flowService = new Flow();
	final SparseMatrixFuzzy<Alternative, Alternative> preference = (testData.getPreference());
	final SparseMatrixFuzzy<Alternative, Alternative> concordance = testData.getConcordance();
	final AlternativesScores flowsCompIncorr = flowService.getPositiveFlows(concordance);
	final AlternativesScores flowsComp = flowService.getPositiveFlows(preference);
	assertFalse("Positive flows should change when using concordance.",
		flowsComp.approxEquals(flowsCompIncorr, 0.2f));

    }

    @Test
    public void testPositiveFlows() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final Flow flowService = new Flow();
	final SparseMatrixFuzzy<Alternative, Alternative> preference = testData.getPreference();
	final AlternativesScores flowsComp = flowService.getPositiveFlows(preference);
	final AlternativesScores expected = testData.getPositiveFlows();
	assertTrue("Scores do not match.", expected.approxEquals(flowsComp, 0.00005f));
    }
}
