package org.decisiondeck.xmcda_oo.services.sorting;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.services.internal.PreferencesToArrays;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.weights.Coalitions;

import com.google.common.collect.BiMap;

public class ElectrePessimisticProgressive {

    private final Set<Criterion> m_seenCrits = new HashSet<Criterion>();
    private final Map<Criterion, Integer> m_critsToInt;
    private final ElectrePessimisticProgressiveWithArrays m_helper;
    private final BiMap<Integer, Category> m_intsToCats;

    public ElectrePessimisticProgressive(CatsAndProfs categories, Coalitions coalitions,
	    EvaluationsRead profilesEvaluations) {
	final PreferencesToArrays preferencesToIndexes = new PreferencesToArrays(categories, coalitions,
		profilesEvaluations);
	m_critsToInt = preferencesToIndexes.getCritsToInt();
	m_intsToCats = preferencesToIndexes.getIntsToCats();

	m_helper = new ElectrePessimisticProgressiveWithArrays(preferencesToIndexes
.getWeights(),
		coalitions.getMajorityThreshold(), preferencesToIndexes.getProfs());
    }

    public void reset() {
	m_helper.reset();
    }

    public void setEvaluation(Criterion crit, double minEval, double maxEval) {
	if (m_seenCrits.add(crit)) {
	    throw new IllegalStateException("Already set evaluation for " + crit + ".");
	}
	final Integer critIdx = m_critsToInt.get(crit);
	if (critIdx == null) {
	    throw new IllegalArgumentException("Unknown criterion " + crit + ".");
	}
	m_helper.setEvaluation(critIdx.intValue(), new double[] { minEval, maxEval });
    }

    public Category getWorstCat() {
	return m_intsToCats.get(Integer.valueOf(m_helper.getWorstCat()));
    }

    public Category getBestCat() {
	return m_intsToCats.get(Integer.valueOf(m_helper.getBestCat()));
    }

}
