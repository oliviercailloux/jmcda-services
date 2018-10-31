package org.decisiondeck.xmcda_oo.services.sorting;

import java.util.Map;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.sorting.category.Categories;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;

public class PreferencesProbabilisticDifferenceOptimized {
    private static final Logger s_logger = LoggerFactory.getLogger(PreferencesProbabilisticDifferenceOptimized.class);

    private final CatsAndProfs m_categories;
    private final Coalitions m_coalitions1;
    private final EvaluationsRead m_profilesEvaluations1;
    private final Coalitions m_coalitions2;
    private final EvaluationsRead m_profilesEvaluations2;
    private final Map<Criterion, Interval> m_scales;
    private boolean m_computed;
    private final HashBasedTable<Category, Category, Double> m_probsAtLeast = HashBasedTable.create();

    private double m_prob;

    public PreferencesProbabilisticDifferenceOptimized(CatsAndProfs categories, Coalitions coalitions1,
	    EvaluationsRead profilesEvaluations1, Coalitions coalitions2, EvaluationsRead profilesEvaluations2,
	    Map<Criterion, Interval> scales) {
	m_categories = categories;
	m_coalitions1 = coalitions1;
	m_profilesEvaluations1 = profilesEvaluations1;
	m_coalitions2 = coalitions2;
	m_profilesEvaluations2 = profilesEvaluations2;
	m_scales = scales;

	m_computed = false;
	m_prob = -1d;
    }

    public PreferencesProbabilisticDifferenceOptimized(ISortingPreferences pref1, ISortingPreferences pref2) {
	this(pref1.getCatsAndProfs(), pref1.getCoalitions(), pref1.getProfilesEvaluations(), pref2.getCoalitions(),
		pref2.getProfilesEvaluations(), pref1.getScales());
    }

    public double getProb() {
	if (!m_computed) {
	    compute();
	}

	return m_prob;
    }

    private void compute() {
	if (m_computed) {
	    return;
	}
	for (Category cat1 : m_categories.getCategories()) {
	    for (Category cat2 : m_categories.getCategories()) {
		final int compare = m_categories.getCategories().comparator().compare(cat1, cat2);
		final String catWorstName;
		final String catBestName;
		if (compare < 0) {
		    catWorstName = cat1.getId();
		    catBestName = cat2.getId();
		} else if (compare == 0) {
		    catWorstName = cat1.getId();
		    catBestName = cat1.getId() + "-2";
		} else {
		    catWorstName = cat2.getId();
		    catBestName = cat1.getId();
		}
		final CatsAndProfs doubleCats = Categories.newCatsAndProfs();
		doubleCats.addCategory(catWorstName);
		final Alternative newP = new Alternative("newP");
		doubleCats.addProfile(newP);
		doubleCats.addCategory(catBestName);
		final Alternative cat1Down = cat1.getProfileDown();
		final Alternative cat2Down = cat2.getProfileDown();
		final Evaluations profEval1 = EvaluationsUtils.newEvaluationMatrix();
		final Evaluations profEval2 = EvaluationsUtils.newEvaluationMatrix();
		for (Criterion criterion : m_profilesEvaluations1.getColumns()) {
		    final double eval1;
		    if (cat1Down == null) {
			eval1 = m_scales.get(criterion).getMinimum();
		    } else {
			eval1 = m_profilesEvaluations1.getEntry(cat1Down, criterion).doubleValue();
		    }
		    profEval1.put(newP, criterion, eval1);
		    final double eval2;
		    if (cat2Down == null) {
			eval2 = m_scales.get(criterion).getMinimum();
		    } else {
			eval2 = m_profilesEvaluations2.getEntry(cat2Down, criterion).doubleValue();
		    }
		    profEval2.put(newP, criterion, eval2);
		}
		final PreferencesProbabilisticDifference computer = new PreferencesProbabilisticDifference(doubleCats,
			m_coalitions1, profEval1, m_coalitions2, profEval2, m_scales);
		// final double probAllBad = computer.getProbSameAffectation(catWorst);
		// final double probAtLeast = 1d - probAllBad;
		final double probOk = computer.getProbSameAffectation(doubleCats.getCategories().last());
		m_probsAtLeast.put(cat1, cat2, Double.valueOf(probOk));
		s_logger.info("Prob at least {} for dm1, at least {} for dm2 = " + probOk + ".", cat1, cat2);
	    }
	}

	m_prob = 0d;
	final Category best = m_categories.getCategories().last();
	for (Category cat : m_categories.getCategories().headSet(best)) {
	    m_prob += m_probsAtLeast.get(cat, cat).doubleValue();
	    final Category betterCat = m_categories.getCategories().higher(cat);
	    m_prob += m_probsAtLeast.get(betterCat, betterCat).doubleValue();
	    m_prob -= m_probsAtLeast.get(cat, betterCat).doubleValue();
	    m_prob -= m_probsAtLeast.get(betterCat, cat).doubleValue();
	}
	m_prob += m_probsAtLeast.get(best, best).doubleValue();

	m_computed = true;
    }

}
