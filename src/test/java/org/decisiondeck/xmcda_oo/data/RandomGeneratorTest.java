package org.decisiondeck.xmcda_oo.data;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decisiondeck.jmcda.services.generator.RandomGenerator;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.junit.Test;

public class RandomGeneratorTest {
    @Test
    public void testRandom() throws Exception {
	RandomGenerator gen = new RandomGenerator();
	gen.genAlternatives(100);
	gen.genDms(3);
	Map<DecisionMaker, Set<Alternative>> attributions = gen.genAlternativesAttribution(10);
	assertEquals(3, attributions.size());
	for (DecisionMaker dm : attributions.keySet()) {
	    Set<Alternative> attr = attributions.get(dm);
	    assertEquals(10, attr.size());
	}
	ISortingPreferences view = gen.getAsSortingPreferences();
	assertEquals(100, view.getAlternatives().size());
	assertEquals(100, view.getAllAlternatives().size());
    }
}
