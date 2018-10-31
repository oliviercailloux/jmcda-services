package org.decision_deck.jmcda.services.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.Coalitions;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class PreferencesToArrays {

    private final BiMap<Category, Integer> m_catsToInt = HashBiMap.create();
    /**
     * array[i][j] = evaluation of the profile above category i on the criterion j.
     */
    private final double[][] m_profs;
    private final double m_weights[];
    private double[][] m_bounds;
    private final Map<Criterion, Integer> m_critsToInt = new HashMap<Criterion, Integer>();

    public PreferencesToArrays(CatsAndProfs categories, Coalitions coalitions, EvaluationsRead profilesEvaluations) {
	this(categories, coalitions, coalitions.getCriteria(), profilesEvaluations);
    }

    public PreferencesToArrays(CatsAndProfs categories, Coalitions coalitions,
	    Collection<Criterion> criteriaOrdering, EvaluationsRead profilesEvaluations) {
	if (!categories.isComplete()) {
	    throw new IllegalArgumentException("Categories must be complete.");
	}
	m_profs = new double[categories.getCategories().size() - 1][coalitions.getCriteria().size()];
	m_weights = new double[coalitions.getCriteria().size()];

	int catNb = 0;
	for (Category cat : categories.getCategories()) {
	    m_catsToInt.put(cat, Integer.valueOf(catNb));
	    ++catNb;
	}
	// for (int cat = 0; cat < categories.size() - 1; ++cat) {
	// m_catsToInt.put(categories.get(cat), Integer.valueOf(cat));
	// }

	int crit = 0;
	for (Criterion criterion : criteriaOrdering) {
	    m_critsToInt.put(criterion, Integer.valueOf(crit));
	    final SortedSet<Category> categoriesExceptBest = categories.getCategories().headSet(
		    categories.getCategories().last());
	    for (Category cat : categoriesExceptBest) {
		final Alternative profile = categories.getProfileUp(cat.getId());
		final double eval = profilesEvaluations.getEntry(profile, criterion).doubleValue();
		m_profs[m_catsToInt.get(cat).intValue()][crit] = eval;
	    }
	    // for (int cat = 0; cat < categories.size() - 1; ++cat) {
	    // final double eval = profilesEvaluations.getEntry(categories.get(cat).getProfileUp(), criterion)
	    // .doubleValue();
	    // m_profs[cat][crit] = eval;
	    // }
	    m_weights[crit] = coalitions.getWeight(criterion);
	    ++crit;
	}
	m_bounds = null;
    }

    public Map<Criterion, Integer> getCritsToInt() {
	return m_critsToInt;
    }

    public double[][] getProfs() {
	return m_profs;
    }

    public double[] getWeights() {
	return m_weights;
    }

    public void setBounds(Map<Criterion, Interval> scales) {
	if (scales == null) {
	    throw new NullPointerException();
	}
	if (!scales.keySet().equals(m_critsToInt.keySet())) {
	    throw new IllegalArgumentException("Invalid criteria set: " + scales.keySet() + ", "
		    + m_critsToInt.keySet() + ".");
	}
	m_bounds = new double[m_critsToInt.keySet().size()][2];
	for (Criterion crit : scales.keySet()) {
	    final int critIdx = m_critsToInt.get(crit).intValue();
	    final Interval scale = scales.get(crit);
	    m_bounds[critIdx][0] = scale.getMinimum();
	    m_bounds[critIdx][1] = scale.getMaximum();
	}

    }

    /**
     * @return <code>null</code> iff not set.
     */
    public double[][] getBounds() {
	return m_bounds;
    }

    public Map<Category, Integer> getCatsToInt() {
	return m_catsToInt;
    }

    public BiMap<Integer, Category> getIntsToCats() {
	return m_catsToInt.inverse();
    }

}
