package org.decisiondeck.xmcda_oo.services.sorting;

import static org.junit.Assert.assertTrue;

import org.decision_deck.jmcda.structure.sorting.SortingMode;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.decisiondeck.jmcda.services.sorting.SortingFull;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortingFullTest {
    private static final Logger s_logger = LoggerFactory.getLogger(SortingFullTest.class);

    @Test
    public void testPess() throws Exception {
	testSorting(SortingMode.PESSIMISTIC);
    }

    private void testSorting(final SortingMode mode) throws InvalidInputException {
	final SixRealCars testData = SixRealCars.getInstance();
	final SortingFull svc = new SortingFull();
	final ISortingPreferences problem = ProblemFactory.newSortingPreferences(testData.getAsSortingPreferences70());
	problem.setCoalitions(testData.getCoalitions55());

	s_logger.info("Computing " + mode + " 0.55 assignments.");
	final IOrderedAssignmentsToMultipleRead res55 = svc.assign(mode, problem);
	s_logger.info("Comparing assignments.");
	assertTrue(mode + " 0.55 do not match.", testData.getAssignments55().equals(res55));

	problem.setCoalitions(testData.getCoalitions75());

	s_logger.info("Computing " + mode + " 0.75 assignments.");
	final IOrderedAssignmentsToMultipleRead res75 = svc.assign(mode, problem);
	s_logger.info("Comparing assignments.");
	assertTrue(mode + " 0.75 do not match.", testData.getAssignments75(mode).equals(res75));
    }

    @Test
    public void testOpt() throws Exception {
	testSorting(SortingMode.OPTIMISTIC);
    }

    @Test
    public void testBoth() throws Exception {
	testSorting(SortingMode.BOTH);
    }
}
