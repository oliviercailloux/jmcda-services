package org.decisiondeck.xmcda_oo.services;

import static org.junit.Assert.assertTrue;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.utils.matrix.ConstantMatrixFuzzy;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decision_deck.utils.matrix.SparseMatrixFuzzyRead;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.decisiondeck.jmcda.services.utils.CutRel;
import org.junit.Test;

public class CutTest {
    @Test
    public void testCut70() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final SparseAlternativesMatrixFuzzy inpMat = testData.getOutranking();
	final SparseAlternativesMatrixFuzzy expMat = testData.getOutrankingAtDotSeven();
	final SparseMatrixFuzzy<Alternative, Alternative> res = new CutRel<Alternative, Alternative>().cut(inpMat, 0.70f);
	assertTrue("Unexpected cut.", res.approxEquals(expMat, 1e-5f));
    }

    @Test
    public void testCut1() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final SparseAlternativesMatrixFuzzy inpMat = testData.getOutranking();
	final SparseMatrixFuzzyRead<Alternative, Alternative> expMat = new ConstantMatrixFuzzy<Alternative, Alternative>(
		testData.getAlternatives(), testData.getAlternatives(), 0f);
	final SparseMatrixFuzzy<Alternative, Alternative> res = new CutRel<Alternative, Alternative>().cut(inpMat, 1.1f);
	assertTrue("Unexpected cut.", res.approxEquals(expMat, 1e-5f));
    }

    @Test
    public void testCut0() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final SparseAlternativesMatrixFuzzy inpMat = testData.getOutranking();
	final SparseMatrixFuzzyRead<Alternative, Alternative> expMat = new ConstantMatrixFuzzy<Alternative, Alternative>(
		testData.getAlternatives(), testData.getAlternatives(), 1f);
	final SparseMatrixFuzzy<Alternative, Alternative> res = new CutRel<Alternative, Alternative>().cut(inpMat, 0f);
	assertTrue("Unexpected cut.", res.approxEquals(expMat, 1e-5f));
    }
}
