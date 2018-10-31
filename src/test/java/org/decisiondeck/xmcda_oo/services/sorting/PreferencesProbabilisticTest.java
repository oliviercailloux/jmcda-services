package org.decisiondeck.xmcda_oo.services.sorting;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.decision_deck.jmcda.services.generator.DataGenerator;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.sorting.category.Categories;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.CoalitionsUtils;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDASortingProblemWriter;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.group_preferences.IGroupSortingPreferences;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.decisiondeck.xmcda_oo.structure.sorting.SortingProblemUtils;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class PreferencesProbabilisticTest {
	@Test
	public void testProba1() throws Exception {
		// final SixRealCarsDataOnlyMax data = new SixRealCarsDataOnlyMax();
		// final Weights weights = data.retrieveWeights();
		// final IEvaluations profilesEvaluations =
		// data.getProfilesEvaluations();
		// final Map<Criterion, StepScale> scales = data.getScales();

		final Criterion g1 = new Criterion("g01");
		final Criterion g2 = new Criterion("g02");
		final Alternative pBM = new Alternative("pBM");
		final Alternative pMG = new Alternative("pMG");
		final Evaluations profs = EvaluationsUtils.newEvaluationMatrix();
		profs.put(pBM, g1, 10d);
		profs.put(pBM, g2, 15d);
		profs.put(pMG, g1, 20d);
		profs.put(pMG, g2, 30d);
		final Coalitions coalitions = CoalitionsUtils.newCoalitions();
		coalitions.putWeight(g1, 0.6d);
		coalitions.putWeight(g2, 0.4d);
		coalitions.setMajorityThreshold(.7d);
		final Evaluations profilesEvaluations = profs;
		final Map<Criterion, Interval> scales = Maps.newHashMap();
		scales.put(g1, Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 0d, 99d, 1d));
		scales.put(g2, Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 0d, 99d, 1d));
		final CatsAndProfs categories = Categories.newCatsAndProfs();
		categories.addCategory("bad");
		categories.addCategory("medium");
		categories.addCategory("good");
		categories.setProfileUp("bad", pBM);
		categories.setProfileUp("medium", pMG);
		final PreferencesProbabilisticDifference prob = new PreferencesProbabilisticDifference(categories, coalitions,
				profilesEvaluations, coalitions, profilesEvaluations, scales);
		assertEquals("Unexpected proba.", 1d, prob.getProbabilityMatch(), 1e-6);
	}

	@Test
	public void testProba1Opt() throws Exception {
		// final SixRealCarsDataOnlyMax data = new SixRealCarsDataOnlyMax();
		// final Weights weights = data.retrieveWeights();
		// final IEvaluations profilesEvaluations =
		// data.getProfilesEvaluations();
		// final Map<Criterion, StepScale> scales = data.getScales();

		final Criterion g1 = new Criterion("g01");
		final Criterion g2 = new Criterion("g02");
		final Alternative pBM = new Alternative("pBM");
		final Alternative pMG = new Alternative("pMG");
		final Evaluations profs = EvaluationsUtils.newEvaluationMatrix();
		profs.put(pBM, g1, 10d);
		profs.put(pBM, g2, 15d);
		profs.put(pMG, g1, 20d);
		profs.put(pMG, g2, 30d);
		final Coalitions coalitions = CoalitionsUtils.newCoalitions();
		coalitions.putWeight(g1, 0.6d);
		coalitions.putWeight(g2, 0.4d);
		coalitions.setMajorityThreshold(.7d);
		final Map<Criterion, Interval> scales = Maps.newHashMap();
		scales.put(g1, Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 0d, 99d, 1d));
		scales.put(g2, Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 0d, 99d, 1d));
		final CatsAndProfs categories = Categories.newCatsAndProfs();
		categories.addCategory("bad");
		categories.addCategory("medium");
		categories.addCategory("good");
		categories.setProfileUp("bad", pBM);
		categories.setProfileUp("medium", pMG);

		final ISortingPreferences pref1 = ProblemFactory.newSortingPreferences();
		pref1.getCatsAndProfs().addAll(categories);
		pref1.setCoalitions(coalitions);
		pref1.setProfilesEvaluations(profs);
		for (Criterion criterion : scales.keySet()) {
			final Interval scale = scales.get(criterion);
			pref1.setScale(criterion, scale);
		}

		final PreferencesProbabilisticDifferenceOptimized prob = new PreferencesProbabilisticDifferenceOptimized(pref1,
				pref1);
		assertEquals("Unexpected proba.", 1d, prob.getProb(), 1e-6);
	}

	void generate() throws IOException {
		final DataGenerator gen = new DataGenerator();

		final Coalitions coalitions1 = CoalitionsUtils.newCoalitions();
		final Coalitions coalitions2 = CoalitionsUtils.newCoalitions();
		final Criterion g1 = new Criterion("g1");
		final Criterion g2 = new Criterion("g2");
		final Criterion g3 = new Criterion("g3");
		coalitions1.putWeight(g1, .3d);
		coalitions1.putWeight(g2, .3d);
		coalitions1.putWeight(g3, .4d);
		coalitions1.setMajorityThreshold(.6d);
		coalitions2.putWeight(g1, .7d);
		coalitions2.putWeight(g2, .1d);
		coalitions2.putWeight(g3, .2d);
		coalitions2.setMajorityThreshold(.8d);

		final CatsAndProfs categories = gen.genCatsAndProfs(3);
		final Alternative p1 = categories.getProfiles().first();
		final Alternative p2 = categories.getProfiles().last();
		final Evaluations profiles1 = EvaluationsUtils.newEvaluationMatrix();
		final Evaluations profiles2 = EvaluationsUtils.newEvaluationMatrix();
		profiles1.put(p1, g1, 20d);
		profiles1.put(p1, g2, 30d);
		profiles1.put(p1, g3, 50d);
		profiles1.put(p2, g1, 70d);
		profiles1.put(p2, g2, 80d);
		profiles1.put(p2, g3, 60d);
		profiles2.put(p1, g1, 40d);
		profiles2.put(p1, g2, 40d);
		profiles2.put(p1, g3, 20d);
		profiles2.put(p2, g1, 50d);
		profiles2.put(p2, g2, 90d);
		profiles2.put(p2, g3, 30d);

		final IGroupSortingPreferences data = ProblemFactory.newGroupSortingPreferences(null, null, categories, null,
				null, null);
		final ISortingPreferences pref1 = ProblemFactory.newSortingPreferences();
		SortingProblemUtils.copyDataToTarget(data, pref1);
		pref1.setProfilesEvaluations(profiles1);
		pref1.setCoalitions(coalitions1);

		final ISortingPreferences pref2 = ProblemFactory.newSortingPreferences();
		SortingProblemUtils.copyDataToTarget(data, pref2);
		pref2.setProfilesEvaluations(profiles2);
		pref2.setCoalitions(coalitions2);

		new XMCDASortingProblemWriter(Files.asByteSink(new File("pref1.xml"))).writePreferences(pref1);
		new XMCDASortingProblemWriter(Files.asByteSink(new File("pref2.xml"))).writePreferences(pref2);
	}
}
