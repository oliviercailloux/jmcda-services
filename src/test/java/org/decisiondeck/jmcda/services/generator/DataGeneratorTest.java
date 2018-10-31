package org.decisiondeck.jmcda.services.generator;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.NavigableSet;

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
import org.junit.Test;

import com.google.common.collect.Maps;

public class DataGeneratorTest {
    @Test
    public void testSplitSmall() throws Exception {
	final NavigableSet<Alternative> profiles = getThreeProfiles();
	final Map<Criterion, Interval> scales = Maps.newHashMap();
	scales.put(getG1(), Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 0, 1, 1));
	scales.put(getG2(), Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 2, 4, 2));

	final DataGenerator gen = new DataGenerator();
	final Evaluations split = gen.genSplitProfilesEvaluations(profiles, scales);

	final Evaluations expected = EvaluationsUtils.newEvaluationMatrix();
	expected.put(getP1(), getG1(), 1d);
	expected.put(getP2(), getG1(), 1d);
	expected.put(getP3(), getG1(), 1d);
	expected.put(getP1(), getG2(), 4d);
	expected.put(getP2(), getG2(), 4d);
	expected.put(getP3(), getG2(), 4d);

	assertEquals(expected, split);
    }

    Criterion getG4() {
	return new Criterion("g4");
    }

    Alternative getP2() {
	return new Alternative("p2");
    }

    Alternative getP3() {
	return new Alternative("p3");
    }

    Alternative getProf(final String id) {
	return new Alternative(id);
    }

    NavigableSet<Alternative> getFourProfiles() {
	final CatsAndProfs catsAndProfs = Categories.newCatsAndProfs();
	catsAndProfs.addProfile(getP1());
	catsAndProfs.addProfile(getP2());
	catsAndProfs.addProfile(getP3());
	catsAndProfs.addProfile(getProf("p4"));
	return catsAndProfs.getProfiles();
    }

    Alternative getP1() {
	return new Alternative("p1");
    }

    Criterion getG1() {
	return new Criterion("g1");
    }

    Criterion getG2() {
	return new Criterion("g2");
    }

    NavigableSet<Alternative> getThreeProfiles() {
	final CatsAndProfs catsAndProfs = Categories.newCatsAndProfs();
	catsAndProfs.addProfile(getP1());
	catsAndProfs.addProfile(getP2());
	catsAndProfs.addProfile(getP3());
	return catsAndProfs.getProfiles();
    }

    Criterion getG3() {
	return new Criterion("g3");
    }

    @Test
    public void testSplit() throws Exception {
	final NavigableSet<Alternative> profiles = getThreeProfiles();
	final Map<Criterion, Interval> scales = Maps.newHashMap();
	scales.put(getG1(), Intervals.newInterval(PreferenceDirection.MAXIMIZE, 0, 100));
	scales.put(getG2(), Intervals.newInterval(PreferenceDirection.MAXIMIZE, 20, 80));
	scales.put(getG3(), Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 2, 8, 1));
	scales.put(getG4(), Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 0, 6, 2));

	final DataGenerator gen = new DataGenerator();
	final Evaluations split = gen.genSplitProfilesEvaluations(profiles, scales);

	final Evaluations expected = EvaluationsUtils.newEvaluationMatrix();
	expected.put(getP1(), getG1(), 25d);
	expected.put(getP2(), getG1(), 50d);
	expected.put(getP3(), getG1(), 75d);
	expected.put(getP1(), getG2(), 35d);
	expected.put(getP2(), getG2(), 50d);
	expected.put(getP3(), getG2(), 65d);
	expected.put(getP1(), getG3(), 4d);
	expected.put(getP2(), getG3(), 6d);
	expected.put(getP3(), getG3(), 7d);
	expected.put(getP1(), getG4(), 2d);
	expected.put(getP2(), getG4(), 4d);
	expected.put(getP3(), getG4(), 6d);

	assertEquals(expected, split);
    }
}
