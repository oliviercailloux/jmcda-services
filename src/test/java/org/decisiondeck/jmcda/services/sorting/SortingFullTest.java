package org.decisiondeck.jmcda.services.sorting;

import static org.junit.Assert.assertEquals;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDASortingProblemReader;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignments;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.junit.Test;

import com.google.common.io.Resources;

public class SortingFullTest {
    @Test
    public void testNonNormalized() throws Exception {
	final ISortingPreferences sortingPreferences = new XMCDASortingProblemReader(
		Resources.asByteSource(getClass().getResource(
			"Ten alternatives, Three categories, Non normalized weights.xml")))
		.readSortingResultsToMultiple();

	final SortingFull sorter = new SortingFull();
	final IOrderedAssignments assigned = sorter.pessimistic(sortingPreferences);
	final Category catAlt09 = assigned.getCategory(new Alternative("Alt09"));
	assertEquals(new Category("Cat2"), catAlt09);
    }
}
