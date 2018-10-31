package org.decisiondeck.xmcda_oo.services.outranking;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.utils.matrix.SparseMatrixFuzzyRead;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.decisiondeck.jmcda.services.outranking.Concordance;
import org.decisiondeck.jmcda.services.outranking.Discordance;
import org.decisiondeck.jmcda.services.outranking.Outranking;
import org.junit.Test;

public class OutrankingTest {
    @Test
    public void testOutranking() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final SparseMatrixFuzzyRead<Alternative, Alternative> conc = new Concordance().concordance(
		testData.getAsProblemData(), testData.getThresholds(), testData.getWeights());
	final Map<Criterion, SparseAlternativesMatrixFuzzy> discs = new Discordance().discordances(testData.getAsProblemData(),
		testData.getThresholds());
	final SparseAlternativesMatrixFuzzy outr = new Outranking().getOutranking(testData.getAlternatives(), testData.getCriteria(),
		conc, discs);
	assertTrue("Outranking does not match.", testData.getOutranking().approxEquals(outr, 0.00005f));
    }
}
