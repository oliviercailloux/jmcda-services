package org.decisiondeck.xmcda_oo.services.sorting;

import static org.junit.Assert.assertEquals;

import org.decision_deck.jmcda.structure.interval.DiscreteInterval;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.sorting.SortingMode;
import org.junit.Test;

public class StandardizeProfilesTest {
    @Test
    public void testStandardize() throws Exception {
	final StandardizeProfiles std = new StandardizeProfiles();
	final DiscreteInterval scale9 = Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 9, 109,
		10).getAsDiscreteInterval();
	final double stdVal7 = std.getInStandardForm(7d, scale9, SortingMode.PESSIMISTIC);
	assertEquals(7d, stdVal7, 1e-6);
	final double stdVal9 = std.getInStandardForm(9d, scale9, SortingMode.PESSIMISTIC);
	assertEquals(9d, stdVal9, 1e-6);
	final double stdVal10 = std.getInStandardForm(10.5d, scale9, SortingMode.PESSIMISTIC);
	assertEquals(14d, stdVal10, 1e-6);
	final double stdVal19 = std.getInStandardForm(19d, scale9, SortingMode.PESSIMISTIC);
	assertEquals(14d, stdVal19, 1e-6);
	final double stdVal40 = std.getInStandardForm(40d, scale9, SortingMode.PESSIMISTIC);
	assertEquals(44d, stdVal40, 1e-6);

	final double opt9 = std.getInStandardForm(9d, scale9, SortingMode.OPTIMISTIC);
	assertEquals(14d, opt9, 1e-6);
	final double opt19 = std.getInStandardForm(19d, scale9, SortingMode.OPTIMISTIC);
	assertEquals(24d, opt19, 1e-6);
	final double opt40 = std.getInStandardForm(40d, scale9, SortingMode.OPTIMISTIC);
	assertEquals(44d, opt40, 1e-6);

	final double both9 = std.getInStandardForm(9d, scale9, SortingMode.BOTH);
	assertEquals(9d, both9, 1e-6);
	final double both19 = std.getInStandardForm(19d, scale9, SortingMode.BOTH);
	assertEquals(19d, both19, 1e-6);
	final double both40 = std.getInStandardForm(40d, scale9, SortingMode.BOTH);
	assertEquals(44d, both40, 1e-6);

	final DiscreteInterval scale5 = Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, -10, 10,
		5).getAsDiscreteInterval();
	final double stdVal8 = std.getInStandardForm(-8d, scale5, SortingMode.PESSIMISTIC);
	assertEquals(-7.5d, stdVal8, 1e-6);
	final double stdVal0 = std.getInStandardForm(0d, scale5, SortingMode.PESSIMISTIC);
	assertEquals(-2.5d, stdVal0, 1e-6);
	final double stdValMax = std.getInStandardForm(10d, scale5, SortingMode.PESSIMISTIC);
	assertEquals(7.5d, stdValMax, 1e-6);

	final DiscreteInterval scaleInv9 = Intervals.newDiscreteInterval(PreferenceDirection.MINIMIZE, 9,
		109, 10).getAsDiscreteInterval();
	final double inv9 = std.getInStandardForm(9d, scaleInv9, SortingMode.PESSIMISTIC);
	assertEquals(14d, inv9, 1e-6);
	final double inv19 = std.getInStandardForm(19d, scaleInv9, SortingMode.PESSIMISTIC);
	assertEquals(24d, inv19, 1e-6);
	final double inv40 = std.getInStandardForm(40d, scaleInv9, SortingMode.PESSIMISTIC);
	assertEquals(44d, inv40, 1e-6);

	final double invOpt9 = std.getInStandardForm(9d, scaleInv9, SortingMode.OPTIMISTIC);
	assertEquals(9d, invOpt9, 1e-6);
	final double invOpt19 = std.getInStandardForm(19d, scaleInv9, SortingMode.OPTIMISTIC);
	assertEquals(14d, invOpt19, 1e-6);
	final double invOpt40 = std.getInStandardForm(40d, scaleInv9, SortingMode.OPTIMISTIC);
	assertEquals(44d, invOpt40, 1e-6);

	final double invBoth9 = std.getInStandardForm(9d, scaleInv9, SortingMode.BOTH);
	assertEquals(9d, invBoth9, 1e-6);
	final double invBoth19 = std.getInStandardForm(19d, scaleInv9, SortingMode.BOTH);
	assertEquals(19d, invBoth19, 1e-6);
	final double invBoth40 = std.getInStandardForm(40d, scaleInv9, SortingMode.BOTH);
	assertEquals(44d, invBoth40, 1e-6);
    }
}
