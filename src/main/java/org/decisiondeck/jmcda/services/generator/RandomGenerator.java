package org.decisiondeck.jmcda.services.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.decision_deck.jmcda.services.Dominance;
import org.decision_deck.jmcda.services.generator.DataGenerator;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.interval.DirectedIntervalImpl;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.AlternativeEvaluations;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.CoalitionsUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class RandomGenerator extends DataGenerator {

    private final Map<Criterion, RandomEvaluator> m_randomEvaluators = Maps.newLinkedHashMap();
    private final Random m_random;
    public static final int MAX_WEIGHTS_SPREAD = 8;

    public RandomGenerator() {
	m_random = new Random();
    }

    /**
     * Generates random alternatives evaluations. Uses alternatives, criteria, scales.
     * 
     * @return not <code>null</code>, a copy.
     */
    public Evaluations genAlternativesEvaluationsWithDominance() {
	final Evaluations inputEvals = genAlternativesEvaluationsAllDifferent();
	final Map<Criterion, Interval> scales = getWriteableData().getScales();
	final Map<Criterion, PreferenceDirection> directions = Maps.transformValues(scales,
		DirectedIntervalImpl.getToPreferenceDirectionFunction());
	final Ordering<Double> asc = Ordering.natural();
	final Map<Criterion, Ordering<Double>> orderings = Maps.transformValues(directions,
		Intervals.getDirectionToOrderingFunction(asc));
	final Evaluations ordered = EvaluationsUtils.newEvaluationMatrix();
	// final Function<Map<Alternative, Double>, ArrayList<Double>> toValues = new Function<Map<Alternative, Double>,
	// ArrayList<Double>>() {
	// @Override
	// public ArrayList<Double> apply(Map<Alternative, Double> input) {
	// return Lists.newArrayList(input.values());
	// }
	// };
	// final Map<Criterion, ArrayList<Double>> inputValues = Maps.transformValues(inputEvals.asTable().columnMap(),
	// toValues);
	final LinkedHashMap<Criterion, List<Double>> sortedValues = Maps.newLinkedHashMap();
	for (Criterion criterion : inputEvals.getColumns()) {
	    // final ArrayList<Double> values = Lists.newArrayList(inputEvals.asTable().columnMap().get(criterion)
	    // .values());
	    // Collections.sort(values, orderings.get(criterion));
	    final List<Double> values = orderings.get(criterion).sortedCopy(
		    inputEvals.asTable().columnMap().get(criterion).values());
	    sortedValues.put(criterion, values);
	}
	int i = 0;
	for (Alternative alternative : inputEvals.getRows()) {
	    for (Criterion criterion : inputEvals.getColumns()) {
		final List<Double> values = sortedValues.get(criterion);
		final Double value = values.get(i);
		ordered.put(alternative, criterion, value.doubleValue());
	    }
	    ++i;
	}
	try {
	    assert (new Dominance().getStrictDominanceOrder(ordered, directions) != null);
	} catch (InvalidInputException exc) {
	    throw new IllegalStateException(exc);
	}
	return ordered;
	// final SetBackedMap<Criterion, ArrayList<Double>> sortedValues = SetBackedMap
	// .<Criterion, ArrayList<Double>> create(inputValues.keySet(),
	// new Function<Criterion, ArrayList<Double>>() {
	// @Override
	// public ArrayList<Double> apply(Criterion input) {
	// final ArrayList<Double> values = inputValues.get(input);
	// Collections.sort(values, orderings.get(input));
	// return values;
	// }
	// });

    }

    private void initRandomEvaluators() {
	checkState(getWriteableData().getScales().keySet().equals(getWriteableData().getCriteria()));
	final Map<Criterion, Interval> scales = getWriteableData().getScales();
	for (Criterion criterion : scales.keySet()) {
	    final Interval scale = scales.get(criterion);
	    final RandomEvaluator randomEvaluator = new RandomEvaluator(scale, m_random);
	    m_randomEvaluators.put(criterion, randomEvaluator);
	}
    }

    /**
     * Generates random coalitions. Uses criteria.
     * 
     * @return not <code>null</code>.
     */
    public Coalitions genCoalitions() {
	final Random randomWeights = new Random();
	final Coalitions coalitions = CoalitionsUtils.newCoalitions();
	for (Criterion criterion : getWriteableData().getCriteria()) {
	    final double randomWeight = randomWeights.nextInt(MAX_WEIGHTS_SPREAD) + 1;
	    coalitions.putWeight(criterion, randomWeight);
	}
	final double randomLambda;
	final int lambdaChoice = randomWeights.nextInt(7);
	switch (lambdaChoice) {
	case 0:
	    randomLambda = 5d / 10d;
	    break;
	case 1:
	    randomLambda = 6d / 10d;
	    break;
	case 2:
	    randomLambda = 2d / 3d;
	    break;
	case 3:
	    randomLambda = 7d / 10d;
	    break;
	case 4:
	    randomLambda = 3d / 4d;
	    break;
	case 5:
	    randomLambda = 8d / 10d;
	    break;
	case 6:
	    randomLambda = 9d / 10d;
	    break;
	default:
	    throw new IllegalStateException();
	}
	coalitions.setMajorityThreshold(randomLambda * coalitions.getWeights().getSum());

	getWriteableData().setSharedCoalitions(coalitions);

	return coalitions;
    }

    /**
     * Generates random alternatives evaluations. Uses alternatives, criteria, scales.
     * 
     * @return not <code>null</code>, a copy.
     */
    public Evaluations genAlternativesEvaluations() {
	initRandomEvaluators();

	Evaluations evaluations = EvaluationsUtils.newEvaluationMatrix();

	for (Alternative alt : getWriteableData().getAlternatives()) {
	    for (Criterion crit : m_randomEvaluators.keySet()) {
		final double eval = m_randomEvaluators.get(crit).getRandomEvaluation();
		evaluations.put(alt, crit, eval);
		getWriteableData().setEvaluation(alt, crit, Double.valueOf(eval));
	    }
	}

	return evaluations;
    }

    /**
     * Generates random alternatives evaluations. Uses alternatives, criteria, scales.
     * 
     * @return not <code>null</code>, a copy.
     */
    public Evaluations genAlternativesEvaluationsAllDifferent() {
	initRandomEvaluators();

	Evaluations evaluations = EvaluationsUtils.newEvaluationMatrix();

	for (Alternative alt : getWriteableData().getAlternatives()) {
	    final AlternativeEvaluations accepted;
	    while (true) {
		final AlternativeEvaluations these = new AlternativeEvaluations();
		for (Criterion crit : m_randomEvaluators.keySet()) {
		    final double eval = m_randomEvaluators.get(crit).getRandomEvaluation();
		    these.putEvaluation(crit, Double.valueOf(eval));
		}
		if (!EvaluationsUtils.contains(evaluations, these)) {
		    accepted = these;
		    break;
		}
	    }
	    for (Criterion crit : getWriteableData().getCriteria()) {
		final double eval = accepted.getEvaluation(crit).doubleValue();
		evaluations.put(alt, crit, eval);
		getWriteableData().setEvaluation(alt, crit, Double.valueOf(eval));
	    }
	}

	return evaluations;
    }

    /**
     * Generates one random alternative evaluations. Uses criteria, scales.
     * 
     * @return not <code>null</code>, a copy.
     */
    public AlternativeEvaluations genAlternativeEvaluations() {
	initRandomEvaluators();

	final AlternativeEvaluations evaluations = new AlternativeEvaluations();

	for (Criterion crit : m_randomEvaluators.keySet()) {
	    final double eval = m_randomEvaluators.get(crit).getRandomEvaluation();
	    evaluations.putEvaluation(crit, Double.valueOf(eval));
	}
	return evaluations;
    }

    /**
     * Generates random coalitions for each decision maker. Uses dms, criteria.
     * 
     * @return not <code>null</code>.
     */
    public Map<DecisionMaker, Coalitions> genAllCoalitions() {
	final Map<DecisionMaker, Coalitions> allCoalitions = Maps.newLinkedHashMap();
	for (DecisionMaker dm : getWriteableData().getDms()) {
	    allCoalitions.put(dm, genCoalitions());
	}
	return allCoalitions;
    }

    /**
     * <p>
     * Generates an attribution of a set of alternatives to a set of decision makers. Each decision maker is given a
     * given number of alternatives chosen randomly among the set of alternatives. The attributed alternatives sets are
     * not necessarily disjoint, i.e. the random picking is done with replacement.
     * </p>
     * <p>
     * If the total number of alternatives equals the number of alternatives per decision maker, each decision maker is
     * attributed the full set of alternatives.
     * </p>
     * <p>
     * Uses dms, alternatives. Number of alternatives contained in this object may be larger than the number of
     * alternatives to assign, but not smaller.
     * </p>
     * 
     * @param nbAlternativesPerDm
     *            number of alternatives to attribute to each decision maker.
     * 
     * @return for each decision maker, a set of alternatives, subset of the full set of alternatives. The sets and the
     *         map are mutable.
     */
    public Map<DecisionMaker, Set<Alternative>> genAlternativesAttribution(int nbAlternativesPerDm) {
	checkState(getWriteableData().getAlternatives().size() > 0);
	checkArgument(getWriteableData().getAlternatives().size() >= nbAlternativesPerDm);
	final Random random = new Random();
	final ArrayList<Alternative> alternativesList = Lists.newArrayList(getWriteableData().getAlternatives());
	final Map<DecisionMaker, Set<Alternative>> attribution = Maps.newLinkedHashMap();
	for (DecisionMaker dm : getWriteableData().getDms()) {
	    Collections.shuffle(alternativesList, random);
	    attribution.put(dm, Sets.newLinkedHashSet(alternativesList.subList(0, nbAlternativesPerDm)));
	}
	return attribution;
    }

}
