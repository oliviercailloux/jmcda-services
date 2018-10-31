package org.decisiondeck.xmcda_oo.services.outranking;

import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.decisiondeck.jmcda.services.outranking.Discordance;
import org.junit.Test;

public class DiscordanceTest {

    @Test
    public void testDiscordance() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final Discordance disc = new Discordance();
	final Map<Criterion, SparseAlternativesMatrixFuzzy> res = disc.discordances(testData.getAsProblemData(),
		testData.getThresholds());
	final Set<Criterion> crits = res.keySet();
	for (Criterion criterion : crits) {
	    final SparseAlternativesMatrixFuzzy discRes = res.get(criterion);
	    assertTrue("Discordance does not match.",
		    testData.getDiscordanceByCriteria().get(criterion).approxEquals(discRes, 0.00005f));
	}
    }

}
