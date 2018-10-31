package org.decisiondeck.jmcda.persist.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.utils.collection.extensional_order.ExtentionalTotalOrder;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;
import org.decisiondeck.jmcda.structure.sorting.problem.group_assignments.IGroupSortingAssignmentsWithCredibilities;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

public class ImportCsvTest {
	@SuppressWarnings("boxing")
	@Test
	public void testImportCredibilities() throws Exception {
		final DecisionMaker dm1 = new DecisionMaker("dm1");
		final DecisionMaker dm2 = new DecisionMaker("dm2");
		final DecisionMaker dm3 = new DecisionMaker("dm3");
		final Alternative a1 = new Alternative("a1");
		final Alternative a2 = new Alternative("a2");
		final Alternative a3 = new Alternative("a3");
		final Category c1 = new Category("c1");
		final Category c2 = new Category("c2");
		final Category c3 = new Category("c3");
		final Category c4 = new Category("c4");
		final List<Category> categoriesList = Arrays.asList(c1, c2, c3, c4);
		ExtentionalTotalOrder<Category> expectedCategories = ExtentionalTotalOrder.create(categoriesList);

		final IOrderedAssignmentsWithCredibilities expected1 = AssignmentsFactory
				.newOrderedAssignmentsWithCredibilities();
		expected1.setCategories(expectedCategories);
		final Map<Category, Double> dm1a1 = Maps.newHashMap();
		dm1a1.put(c2, 4d);
		dm1a1.put(c3, 3d);
		expected1.setCredibilities(a1, dm1a1);
		final Map<Category, Double> dm1a2 = Maps.newHashMap();
		dm1a2.put(c1, 4d);
		dm1a2.put(c2, 3d);
		expected1.setCredibilities(a2, dm1a2);
		final IOrderedAssignmentsWithCredibilities expected2 = AssignmentsFactory
				.newOrderedAssignmentsWithCredibilities();
		expected2.setCategories(expectedCategories);
		final Map<Category, Double> dm2a1 = Maps.newHashMap();
		dm2a1.put(c1, 1d);
		dm2a1.put(c2, 4d);
		expected2.setCredibilities(a1, dm2a1);
		expected2.setCredibilities(a2, Collections.singletonMap(c3, 3d));
		final IOrderedAssignmentsWithCredibilities expected3 = AssignmentsFactory
				.newOrderedAssignmentsWithCredibilities();
		expected3.setCategories(expectedCategories);
		expected3.setCredibilities(a3, Collections.singletonMap(c4, 6d));

		final CsvImporterCredibilities csv = new CsvImporterCredibilities(
				Resources.asCharSource(getClass().getResource("credibilities.csv"), Charsets.UTF_8));
		final IGroupSortingAssignmentsWithCredibilities data = csv.read(categoriesList);
		final Set<Alternative> alternatives = data.getAlternatives();
		final Set<DecisionMaker> dms = data.getDms();
		assertTrue(Iterables.elementsEqual(dms, Arrays.asList(dm1, dm2, dm3)));
		assertTrue(Iterables.elementsEqual(alternatives, Arrays.asList(a1, a2, a3)));
		final IOrderedAssignmentsWithCredibilities assignments1 = data.getAssignments(dm1);
		final IOrderedAssignmentsWithCredibilities assignments2 = data.getAssignments(dm2);
		final IOrderedAssignmentsWithCredibilities assignments3 = data.getAssignments(dm3);
		assertEquals("Incorrect assignment 1.", expected1, assignments1);
		assertEquals("Incorrect assignment 2.", expected2, assignments2);
		assertEquals("Incorrect assignment 3.", expected3, assignments3);
	}

	@Test
	public void testImportEvaluations() throws Exception {
		final CsvImporterEvaluations csv = new CsvImporterEvaluations(
				Resources.asCharSource(getClass().getResource("interviews_2009.csv"), Charsets.UTF_8));
		final IProblemData data = csv.read();
		final Set<Alternative> alternatives = data.getAlternatives();
		final Set<Criterion> criteria = data.getCriteria();
		final EvaluationsRead evaluations = data.getAlternativesEvaluations();
		assertEquals("Incorrect first alternative.", new Alternative("GRIMALDI"), alternatives.iterator().next());
		assertEquals("Incorrect first criterion.", new Criterion("3rd"), criteria.iterator().next());
		assertEquals("Incorrect alternatives number.", 71, alternatives.size());
		assertEquals("Incorrect criteria number.", 6, criteria.size());
		assertEquals("Incorrect evaluations number.", 398, evaluations.getValueCount());
	}
}
