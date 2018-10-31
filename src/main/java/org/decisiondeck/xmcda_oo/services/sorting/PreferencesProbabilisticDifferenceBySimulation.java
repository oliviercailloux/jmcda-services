package org.decisiondeck.xmcda_oo.services.sorting;

import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.services.generator.RandomGenerator;
import org.decisiondeck.jmcda.services.sorting.SortingFull;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignments;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;

/**
 * Given two sets of preference parameters: weights and profiles, compute the probability that an alternative be
 * assigned to the same category according to each preference model. Uses pessimistic Electre TRI sorting, no
 * thresholds.
 * 
 * @author Olivier Cailloux
 * 
 */
public class PreferencesProbabilisticDifferenceBySimulation {
    private final ISortingPreferences m_pref1;
    private final ISortingPreferences m_pref2;
    private double m_prob;
    private int m_nbEq;
    private final int m_nbAlternatives;

    /**
     * @param pref1
     *            not <code>null</code>.
     * @param pref2
     *            not <code>null</code>.
     * @param nbAlternatives
     *            the number of random alternatives whose evaluations must be generated for the simulation.
     */
    public PreferencesProbabilisticDifferenceBySimulation(ISortingPreferences pref1, ISortingPreferences pref2,
	    int nbAlternatives) {
	m_pref1 = ProblemFactory.newSortingPreferences(pref1);
	m_pref2 = ProblemFactory.newSortingPreferences(pref2);
	m_nbAlternatives = nbAlternatives;
	m_prob = -1d;
	m_nbEq = -1;

	if (!m_pref1.getCriteria().equals(m_pref2.getCriteria())) {
	    throw new IllegalArgumentException("Not same criteria for both preference models.");
	}

    }

    public double getProbabilityMatch() throws InvalidInputException {
	if (m_prob == -1d) {
	    compute();
	}
	return m_prob;
    }

    private void compute() throws InvalidInputException {
	if (m_prob != -1d) {
	    return;
	}
	final Set<Criterion> criteria = m_pref1.getCriteria();

	final RandomGenerator gen = new RandomGenerator();
	gen.setCriteria(criteria);
	gen.genAlternatives(m_nbAlternatives);
	final Evaluations randEvals = gen.genAlternativesEvaluations();
	m_pref1.setEvaluations(randEvals);
	m_pref2.setEvaluations(randEvals);

	final IOrderedAssignments assigned1 = new SortingFull().pessimistic(m_pref1);
	final IOrderedAssignments assigned2 = new SortingFull().pessimistic(m_pref2);

	m_nbEq = 0;
	for (Alternative alternative : m_pref1.getAlternatives()) {
	    final Category ass1 = assigned1.getCategory(alternative);
	    final Category ass2 = assigned2.getCategory(alternative);
	    if (ass1.equals(ass2)) {
		++m_nbEq;
	    }
	}

	m_prob = ((double) m_nbEq) / m_pref1.getAlternatives().size();
    }

    public int getNbMatch() throws InvalidInputException {
	if (m_prob == -1d) {
	    compute();
	}
	return m_nbEq;
    }

}
