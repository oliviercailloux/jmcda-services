package org.decisiondeck.jmcda.persist;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.HashSet;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decision_deck.jmcda.structure.thresholds.ThresholdsUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResultsToMultiple;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

public class IrisExportTest {
    @Test(expected = InvalidInputException.class)
    public void testEmpty() throws Exception {
	new IrisExporter(new StringWriter()).export(ProblemFactory.newSortingResultsToMultiple());
    }

    @Test
    public void testEmptyButNice() throws Exception {
	final StringWriter out = new StringWriter();
	final IrisExporter exporter = new IrisExporter(out);
	exporter.setNice(true);
	exporter.export(ProblemFactory.newSortingResultsToMultiple());
	final String expected = Resources.toString(getClass().getResource("irisEmpty.tri"), Charsets.US_ASCII);
	assertEquals("Written different than expected.", expected, out.toString());
    }

    @Test
    public void testMinimal() throws Exception {
	// TODO(); //remove need for scales (only p dir is needed)!
	final ISortingResultsToMultiple data = ProblemFactory.newSortingResultsToMultiple();
	final Criterion g1 = new Criterion("g1");
	final Thresholds crits = ThresholdsUtils.newThresholds();
	crits.setPreferenceThreshold(g1, 0d);
	crits.setIndifferenceThreshold(g1, 0d);
	data.setThresholds(crits);
	data.setScale(g1, Intervals.newDirection(PreferenceDirection.MAXIMIZE));
	final Alternative a1 = new Alternative("1");
	final Alternative p1 = new Alternative("p1");
	data.setEvaluation(a1, g1, Double.valueOf(0d));

	final Evaluations evals = EvaluationsUtils.newEvaluationMatrix();
	evals.put(p1, g1, 0);

	data.setProfilesEvaluations(evals);

	final Category c1 = new Category("cat1");
	final Category c2 = new Category("cat2");
	data.getCatsAndProfs().addCategory(c1);
	data.getCatsAndProfs().addProfile(p1);
	data.getCatsAndProfs().addCategory(c2);

	final HashSet<Category> c1c2 = Sets.newHashSet();
	c1c2.add(c1);
	c1c2.add(c2);
	data.getAssignments().setCategories(a1, c1c2);

	final StringWriter out = new StringWriter();
	final IrisExporter exporter = new IrisExporter(out);
	exporter.export(data);
	final String expected = Resources.toString(getClass().getResource("irisMinimal.tri"), Charsets.US_ASCII);
	assertEquals("Written different than expected.", expected, out.toString());
    }
}
