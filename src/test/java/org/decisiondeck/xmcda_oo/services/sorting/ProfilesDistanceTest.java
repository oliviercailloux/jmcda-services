package org.decisiondeck.xmcda_oo.services.sorting;

import static org.junit.Assert.assertEquals;

import org.decision_deck.jmcda.structure.interval.DiscreteInterval;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.sorting.SortingMode;
import org.junit.Test;

public class ProfilesDistanceTest {
    @Test
    public void testDistance() throws Exception {
	final ProfilesDistance dist = new ProfilesDistance();
	final DiscreteInterval scale1 = Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 0,
		100, 1).getAsDiscreteInterval();
	final double distp1 = dist.getDistance(1.5d, 1d, scale1, SortingMode.PESSIMISTIC);
	assertEquals(1d, distp1, 1e-6);
	final double distp01 = dist.getDistance(0.1d, 1d, scale1, SortingMode.PESSIMISTIC);
	assertEquals(0d, distp01, 1e-6);
	final double distp19 = dist.getDistance(1d, 1.9d, scale1, SortingMode.PESSIMISTIC);
	assertEquals(1d, distp19, 1e-6);
	final double distp1519 = dist.getDistance(1.5d, 1.9d, scale1, SortingMode.PESSIMISTIC);
	assertEquals(0d, distp1519, 1e-6);
	final double distp1901 = dist.getDistance(1.9d, 0.1d, scale1, SortingMode.PESSIMISTIC);
	assertEquals(1d, distp1901, 1e-6);

	final DiscreteInterval scale10 = Intervals.newDiscreteInterval(PreferenceDirection.MAXIMIZE, 3, 103,
		10).getAsDiscreteInterval();
	final double dist10p94 = dist.getDistance(10d, 94d, scale10, SortingMode.PESSIMISTIC);
	assertEquals(90d, dist10p94, 1e-6);
	final double dist10p11 = dist.getDistance(10d, 14d, scale10, SortingMode.PESSIMISTIC);
	assertEquals(10d, dist10p11, 1e-6);
	final double dist10p10 = dist.getDistance(13d, 10d, scale10, SortingMode.PESSIMISTIC);
	assertEquals(0d, dist10p10, 1e-6);
	// final double dist10p2 = dist.getDistance(3d, 2d, scale10, SortingMode.PESSIMISTIC);
	// assertEquals(0d, dist10p2, 1e-6);

	// final double dist10bothp2 = dist.getDistance(3d, 2d, scale10, SortingMode.BOTH);
	// assertEquals(5d, dist10bothp2, 1e-6);
    }
}
