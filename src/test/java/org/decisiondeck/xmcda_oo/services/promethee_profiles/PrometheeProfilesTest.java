package org.decisiondeck.xmcda_oo.services.promethee_profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.jmcda.structure.scores.AlternativesScores;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.thresholds.ThresholdsUtils;
import org.decision_deck.jmcda.structure.weights.Weights;
import org.decision_deck.jmcda.structure.weights.WeightsUtils;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.decisiondeck.jmcda.services.outranking.Concordance;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;
import org.decisiondeck.xmcda_oo.services.flow.Flow;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

public class PrometheeProfilesTest {
    private static final Logger s_logger = LoggerFactory.getLogger(PrometheeProfilesTest.class);

    @Test
    public void testProfiles() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final PrometheeProfiles profs = new PrometheeProfiles();

	s_logger.info("Computing profiles.");
	final Evaluations res = profs.computeProfiles(testData.getAsProblemData(), testData.getThresholds());
	s_logger.info("Comparing profiles.");
	assertTrue("Profiles do not match.", testData.getPrometheeProfiles().approxEquals(res, 0.00005f));
    }

    @Test
    public void testCompareProfilesToNetFlows() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final PrometheeProfiles profilesSvc = new PrometheeProfiles();

	s_logger.info("Computing profiles.");
	final Evaluations profiles = profilesSvc
		.computeProfiles(testData.getAsProblemData(), testData.getThresholds());
	s_logger.info("Computing scores.");
	final AlternativesScores scoresFromProfiles = new AlternativesScores();
	final Weights weights = testData.getWeights();
	for (Alternative alternative : profiles.getRows()) {
	    double score = 0;
	    for (Criterion criterion : profiles.getColumns()) {
		final double weight = weights.getWeightBetter(criterion);
		final double profile = profiles.getEntry(alternative, criterion).doubleValue();
		score = score + weight * profile;
	    }
	    scoresFromProfiles.put(alternative, (float) score);
	}

	s_logger.info("Computing net flows.");
	final AlternativesScores netFlows = new Flow().getNetFlows(new Concordance().preference(
		testData.getAsProblemData(), testData.getThresholds(), testData.getWeights()));
	s_logger.info("Comparing scores from profiles to net flows.");
	assertEquals(6, netFlows.entrySet().size());
	assertTrue("scores do not match.", scoresFromProfiles.approxEquals(netFlows, 0.00005d));
    }

    @SuppressWarnings("boxing")
    @Test
    public void testCompareProfilesToNetFlowOneCrit() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final PrometheeProfiles profilesSvc = new PrometheeProfiles();

	s_logger.info("Computing profiles.");
	final Evaluations profiles = profilesSvc
		.computeProfiles(testData.getAsProblemData(), testData.getThresholds());

	final Flow flowSvc = new Flow();
	final Concordance concSvc = new Concordance();
	final Thresholds thresholds = testData.getThresholds();
	for (Criterion criterion : testData.getCriteria()) {
	    final Weights uniqueWeight = WeightsUtils.newWeights();
	    final double weight = testData.getWeights().getWeightBetter(criterion);
	    uniqueWeight.putWeight(criterion, weight);
	    final Thresholds uniqueThresholds = ThresholdsUtils.newThresholds();
	    uniqueThresholds.getPreferenceThresholds().put(criterion, thresholds.getPreferenceThreshold(criterion));
	    uniqueThresholds.getIndifferenceThresholds().put(criterion, thresholds.getIndifferenceThreshold(criterion));
	    final Predicate<Criterion> thisCriterion = Predicates.equalTo(criterion);
	    final EvaluationsRead restrictedEvaluations = EvaluationsUtils.getFilteredView(
		    testData.getAlternativesEvaluations(), null, thisCriterion);
	    final IProblemData uniqueData = ProblemFactory.newProblemData(restrictedEvaluations,
		    Maps.filterKeys(testData.getScales(), thisCriterion));
	    final SparseAlternativesMatrixFuzzy pref = concSvc.preference(uniqueData, uniqueThresholds, uniqueWeight);
	    // testData.retrieveAlternatives(), uniqueCriterion,
	    // uniqueDirection, uniquePref, uniqueIndiff, uniqueWeight, restrictedEvaluations);
	    final AlternativesScores netFlows = flowSvc.getNetFlows(pref);
	    for (Alternative alt : profiles.getRows()) {
		final double profileValue = profiles.getEntry(alt, criterion).doubleValue();
		final double flowValue = netFlows.get(alt).doubleValue();
		assertEquals("Profile different than net flow with unique criterion.", flowValue, profileValue, 1e-5);
	    }
	}
    }
}
