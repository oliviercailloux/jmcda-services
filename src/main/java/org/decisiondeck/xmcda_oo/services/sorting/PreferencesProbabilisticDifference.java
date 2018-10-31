package org.decisiondeck.xmcda_oo.services.sorting;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.services.internal.PreferencesToArrays;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.Weights;
import org.decision_deck.utils.Pair;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given two sets of preference parameters: weights and profiles, compute the probability that an alternative be
 * assigned to the same category according to each preference model. Uses pessimistic Electre TRI sorting, no
 * thresholds.
 * 
 * @author Olivier Cailloux
 * 
 */
public class PreferencesProbabilisticDifference {
    private static final Logger s_logger = LoggerFactory.getLogger(PreferencesProbabilisticDifference.class);

    /**
     * Is not consistent with an intuitive notion of equals (because equal interval size are equal from the pov of this
     * comparator).
     * 
     * @author Olivier Cailloux
     * 
     */
    private static class CompareByIntervalSize implements Comparator<Pair<Double, Double>> {
	public CompareByIntervalSize() {
	    /** Public constructor. */
	}

	@Override
	public int compare(Pair<Double, Double> o1, Pair<Double, Double> o2) {
	    final double min1 = o1.getElt1().doubleValue();
	    final double max1 = o1.getElt2().doubleValue();
	    if (min1 > max1) {
		throw new IllegalStateException("Got strange pair: " + o1 + " (and " + o2 + ").");
	    }
	    final double min2 = o2.getElt1().doubleValue();
	    final double max2 = o2.getElt2().doubleValue();
	    if (min2 > max2) {
		throw new IllegalStateException("Got strange pair: (" + o1 + ") and " + o2 + ".");
	    }
	    return Double.compare(max1 - min1, max2 - min2);
	}
    }

    private static class CompareSumWeights implements Comparator<Criterion> {
	private final Weights m_weights1;
	private final Weights m_weights2;

	public CompareSumWeights(Weights weights1, Weights weights2) {
	    m_weights1 = weights1;
	    m_weights2 = weights2;
	}

	@Override
	public int compare(Criterion o1, Criterion o2) {
	    final double w11 = m_weights1.getWeightBetter(o1);
	    final double w12 = m_weights2.getWeightBetter(o1);
	    final double w1 = w11 + w12;
	    final double w21 = m_weights1.getWeightBetter(o2);
	    final double w22 = m_weights2.getWeightBetter(o2);
	    final double w2 = w21 + w22;
	    return Double.compare(w1, w2);
	}
    }


    private Coalitions m_coalitions1;
    private EvaluationsRead m_profilesEvaluations1;

    private Coalitions m_coalitions2;
    private EvaluationsRead m_profilesEvaluations2;
    private final ElectrePessimisticProgressiveWithArrays m_progress1;
    private final ElectrePessimisticProgressiveWithArrays m_progress2;
    private final PreferencesToArrays m_preferencesToArrays1;
    private final PreferencesToArrays m_preferencesToArrays2;
    private double m_probabilityMatchAtLeast;
    /**
     * array[crit][intervalNumber][0] = lower part of the interval number intervalNumber corresponding to the criterion
     * crit (and higher part for array...[1]).
     */
    private final double[][][] m_intervals;
    private boolean m_computed;
    private double m_probabilityMatchAtMost;
    private final double[] m_probsSameAffectPerCats;

    public PreferencesProbabilisticDifference(CatsAndProfs categories, Coalitions coalitions1,
	    EvaluationsRead profilesEvaluations1, Coalitions coalitions2, EvaluationsRead profilesEvaluations2,
	    Map<Criterion, Interval> scales) {
	if (!profilesEvaluations1.getColumns().equals(profilesEvaluations2.getColumns())) {
	    throw new IllegalArgumentException("Criteria do not match.");
	}
	if (!coalitions1.getCriteria().equals(coalitions2.getCriteria())) {
	    throw new IllegalArgumentException("Criteria do not match.");
	}
	final Set<Criterion> cols1 = profilesEvaluations1.getColumns();
	final Set<Criterion> cols2 = coalitions1.getCriteria();
	if (!cols1.equals(cols2)) {
	    throw new IllegalArgumentException("Criteria do not match.");
	}
	if (!profilesEvaluations1.getRows().equals(profilesEvaluations2.getRows())) {
	    throw new IllegalArgumentException("Profiles do not match.");
	}

	final Set<Alternative> profiles = profilesEvaluations1.getRows();
	m_coalitions1 = coalitions1;
	m_profilesEvaluations1 = profilesEvaluations1;
	m_coalitions2 = coalitions2;
	m_profilesEvaluations2 = profilesEvaluations2;

	m_probsSameAffectPerCats = new double[profiles.size() + 1];
	for (int i = 0; i < m_probsSameAffectPerCats.length; ++i) {
	    m_probsSameAffectPerCats[i] = 0d;
	}

	final List<Criterion> critsOrder = new LinkedList<Criterion>(profilesEvaluations1.getColumns());
	Collections.sort(critsOrder, new CompareSumWeights(coalitions1.getWeights(), coalitions2.getWeights()));
	m_preferencesToArrays1 = new PreferencesToArrays(categories, coalitions1, critsOrder, profilesEvaluations1);
	m_preferencesToArrays1.setBounds(scales);
	m_preferencesToArrays2 = new PreferencesToArrays(categories, coalitions2, critsOrder, profilesEvaluations2);
	m_preferencesToArrays2.setBounds(scales);

	m_intervals = new double[critsOrder.size()][][];
	for (int crit = 0; crit < critsOrder.size(); ++crit) {
	    final Set<Double> profEvals = new HashSet<Double>();
	    for (Alternative prof : profiles) {
		final double prof1 = profilesEvaluations1.getEntry(prof, critsOrder.get(crit)).doubleValue();
		profEvals.add(Double.valueOf(prof1));
		final double prof2 = profilesEvaluations2.getEntry(prof, critsOrder.get(crit)).doubleValue();
		profEvals.add(Double.valueOf(prof2));
	    }
	    final Interval scale = scales.get(critsOrder.get(crit));
	    if (scale.getStepSize() == null) {
		throw new UnsupportedOperationException("Scale should be a step scale.");
	    }
	    final double step = scale.getStepSize().doubleValue();
	    profEvals.add(Double.valueOf(scale.getMinimum()));
	    final double maxNotInclusive = scale.getMaximum() + step;
	    profEvals.add(Double.valueOf(maxNotInclusive));
	    if (step != 1d) {
		throw new UnsupportedOperationException(
			"Steps different than one not yet supported (step used in proba computation).");
	    }
	    final List<Double> profEvalsList = new LinkedList<Double>(profEvals);
	    Collections.sort(profEvalsList);
	    /**
	     * NB intervals are begin inclusive, end not inclusive. This is a generalisation of the continuous case: end
	     * not inclusive minus begin inclusive equals the range.
	     */
	    final List<Pair<Double, Double>> intervalsList = new LinkedList<Pair<Double, Double>>();
	    double previous = profEvalsList.get(0).doubleValue();
	    for (int i = 1; i < profEvalsList.size(); ++i) {
		double current = profEvalsList.get(i).doubleValue();
		final Pair<Double, Double> pair = new Pair<Double, Double>(Double.valueOf(previous),
			Double.valueOf(current));
		intervalsList.add(pair);
		previous = current;
	    }
	    Collections.sort(intervalsList, new CompareByIntervalSize());
	    m_intervals[crit] = new double[intervalsList.size()][2];
	    for (int intvl = 0; intvl < intervalsList.size(); ++intvl) {
		m_intervals[crit][intvl][0] = intervalsList.get(intvl).getElt1().doubleValue();
		m_intervals[crit][intvl][1] = intervalsList.get(intvl).getElt2().doubleValue();
		s_logger.info("Interval " + intvl + " for crit " + crit + ": {}.", m_intervals[crit][intvl]);
	    }
	}

	m_progress1 = new ElectrePessimisticProgressiveWithArrays(m_preferencesToArrays1.getWeights(), coalitions1
.getMajorityThreshold(), m_preferencesToArrays1.getProfs());
	m_progress2 = new ElectrePessimisticProgressiveWithArrays(m_preferencesToArrays2.getWeights(), coalitions2
.getMajorityThreshold(), m_preferencesToArrays2.getProfs());

	m_computed = false;
	m_probabilityMatchAtLeast = 0d;
	m_probabilityMatchAtMost = 1d;
    }

    public PreferencesProbabilisticDifference(ISortingPreferences pref1, ISortingPreferences pref2) {
	this(pref1.getCatsAndProfs(), pref1.getCoalitions(), pref1.getProfilesEvaluations(), pref2.getCoalitions(),
		pref2.getProfilesEvaluations(), pref1.getScales());
    }

    public double getProbabilityMatch() {
	if (!m_computed) {
	    compute();
	}
	return m_probabilityMatchAtLeast;
    }

    private void compute() {
	if (m_computed) {
	    return;
	}

	int[] currentIntervalsIdx = new int[m_intervals.length];
	for (int crit = 0; crit < m_intervals.length; ++crit) {
	    currentIntervalsIdx[crit] = -1;
	}
	int curCrit = 0;

	do {
	    m_progress1.reset();
	    m_progress2.reset();
	    for (int crit = 0; crit < curCrit; ++crit) {
		final int intervalIndex = currentIntervalsIdx[crit];
		final double[] interval = m_intervals[crit][intervalIndex];
		m_progress1.setEvaluation(crit, interval);
		m_progress2.setEvaluation(crit, interval);
	    }
	    {
		currentIntervalsIdx[curCrit] += 1;
		final int intervalIndex = currentIntervalsIdx[curCrit];
		final double[] interval = m_intervals[curCrit][intervalIndex];
		m_progress1.setEvaluation(curCrit, interval);
		m_progress2.setEvaluation(curCrit, interval);
		++curCrit;
	    }
	    while (!m_progress1.isSetCat() || !m_progress2.isSetCat()) {
		currentIntervalsIdx[curCrit] = 0;
		final int intervalIndex = currentIntervalsIdx[curCrit];
		final double[] interval = m_intervals[curCrit][intervalIndex];
		m_progress1.setEvaluation(curCrit, interval);
		m_progress2.setEvaluation(curCrit, interval);
		++curCrit;
	    }
	    --curCrit;

	    // print, compute proba... with currentIntervals.
	    print(currentIntervalsIdx, curCrit);

	    while (curCrit >= 0 && (currentIntervalsIdx[curCrit] == m_intervals[curCrit].length - 1)) {
		--curCrit;
		// remove evaluation?
	    }
	} while (curCrit >= 0);

	/** A very minimal level of imprecision that should be achieved. */
	final double imprec = 0.05d;
	if (m_probabilityMatchAtLeast - m_probabilityMatchAtMost > imprec) {
	    throw new IllegalStateException("Invalid computation: prob match at least = " + m_probabilityMatchAtLeast
		    + ", at most = " + m_probabilityMatchAtMost + ".");
	}
	m_computed = true;
    }

    private void print(int[] currentIntervalsIdx, int curCrit) {
	final double stepSize = 1d;

	double prob = 1d;
	for (int crit = 0; crit <= curCrit; ++crit) {
	    final int index = currentIntervalsIdx[crit];
	    final double[] interval;
	    interval = m_intervals[crit][index];
	    final double[] bounds = m_preferencesToArrays1.getBounds()[crit];
	    final double intervalLength = interval[1] - interval[0];
	    final double maxBoundNotInclusive = bounds[1] + stepSize;
	    final double boundsLength = maxBoundNotInclusive - bounds[0];
	    final double prop = intervalLength / boundsLength;
	    prob *= prop;
	    s_logger.debug("Crit " + crit + ", interval: {}.", interval);
	}
	for (int crit = curCrit + 1; crit < currentIntervalsIdx.length; ++crit) {
	    final double[] bounds = m_preferencesToArrays1.getBounds()[crit];
	    s_logger.debug("Crit " + crit + ", interval: {}.", bounds);
	}

	if (m_progress1.getBestCat() == m_progress2.getBestCat()) {
	    m_probsSameAffectPerCats[m_progress1.getBestCat()] += prob;
	    m_probabilityMatchAtLeast += prob;
	} else {
	    m_probabilityMatchAtMost -= prob;
	}
	s_logger.debug("Match: at least " + m_probabilityMatchAtLeast + ", at most " + m_probabilityMatchAtMost + ".");
    }

    public double getProbSameAffectation(Category cat) {
	if (!m_computed) {
	    compute();
	}
	return m_probsSameAffectPerCats[m_preferencesToArrays1.getCatsToInt().get(cat).intValue()];
    }

}
