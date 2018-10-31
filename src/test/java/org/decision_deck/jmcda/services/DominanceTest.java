package org.decision_deck.jmcda.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.decision_deck.jmcda.services.generator.DataGenerator;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.utils.collection.SetBackedMap;
import org.decision_deck.utils.relation.graph.Preorder;
import org.decisiondeck.jmcda.structure.sorting.problem.group_preferences.IGroupSortingPreferences;
import org.decisiondeck.xmcda_oo.structure.sorting.SortingProblemUtils;
import org.junit.Test;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DominanceTest {
    @Test
    public void testEquals() throws Exception {
	final DataGenerator gen = new DataGenerator();
	final Set<Alternative> alternatives = gen.genAlternatives(3);
	final Set<Criterion> criteria = gen.genCriteria(3);
	final Evaluations evals = EvaluationsUtils.newEvaluationMatrix();
	for (Alternative alternative : alternatives) {
	    double value = 0;
	    for (Criterion criterion : Lists.newLinkedList(criteria)) {
		evals.put(alternative, criterion, value);
		value += 1.5d;
	    }
	}

	final Preorder<Alternative> dominance = new Dominance().getDominanceRelation(evals,
		getPreferenceDirections(criteria));
	final Set<Alternative> all = dominance.get(1);
	assertEquals(3, all.size());
	assertEquals(alternatives, all);
	assertEquals(1, dominance.getRanksCount());
    }

    private SetBackedMap<Criterion, PreferenceDirection> getPreferenceDirections(final Set<Criterion> criteria) {
	return new SetBackedMap<Criterion, PreferenceDirection>(criteria,
		Functions.constant(PreferenceDirection.MAXIMIZE));
    }

    @Test
    public void testTotal() throws Exception {
	final DataGenerator gen = new DataGenerator();
	final Set<Alternative> alternatives = gen.genAlternatives(3);
	final Set<Criterion> criteriaGenerated = gen.genCriteria(3);
	final SetBackedMap<Criterion, PreferenceDirection> map = new SetBackedMap<Criterion, PreferenceDirection>(
		criteriaGenerated, Functions.constant(PreferenceDirection.MAXIMIZE));
	final Map<Criterion, PreferenceDirection> directions = Maps.newLinkedHashMap(map);

	final IGroupSortingPreferences data = SortingProblemUtils.newGroupPreferences();
	data.getAlternatives().addAll(alternatives);
	final Criterion inverted = new Criterion("inverted");
	criteriaGenerated.add(inverted);
	directions.put(inverted, PreferenceDirection.MINIMIZE);
	data.getCriteria().addAll(criteriaGenerated);
	final Set<Criterion> criteria = criteriaGenerated;
	final Evaluations evals = EvaluationsUtils.newEvaluationMatrix();
	double altValue = 1d;
	for (Alternative alternative : data.getAlternatives()) {
	    double value = altValue;
	    for (Criterion criterion : criteria) {
		final double valueSigned;
		valueSigned = directions.get(criterion) == PreferenceDirection.MAXIMIZE ? value : -value;
		evals.put(alternative, criterion, valueSigned);
		value += 1.5d;
	    }
	    altValue *= 2d;
	}

	final Preorder<Alternative> dominance = new Dominance().getDominanceRelation(evals, directions);
	assertEquals(3, dominance.getRanksCount());
	assertEquals(1, dominance.get(1).size());
	assertEquals(1, dominance.get(2).size());
	assertEquals(1, dominance.get(3).size());

	final SortedSet<Alternative> order = dominance.getTotalOrder();
	assertEquals(3, order.size());
	final Iterator<Alternative> iterator = order.iterator();
	assertEquals(dominance.get(3).iterator().next(), iterator.next());
	assertEquals(dominance.get(2).iterator().next(), iterator.next());
	assertEquals(dominance.get(1).iterator().next(), iterator.next());
    }

    @Test
    public void testNoGood() throws Exception {
	final DataGenerator gen = new DataGenerator();
	final Set<Alternative> alternatives = gen.genAlternatives(3);
	final Set<Criterion> criteria = gen.genCriteria(2);
	final Evaluations evals = EvaluationsUtils.newEvaluationMatrix();
	final Iterator<Criterion> criteriaIterator = criteria.iterator();
	{
	    final Criterion g1 = criteriaIterator.next();
	    double critValueAsc = 1d;
	    for (Alternative alternative : alternatives) {
		evals.put(alternative, g1, critValueAsc);
		critValueAsc += 1d;
	    }
	}
	{
	    final Criterion g2 = criteriaIterator.next();
	    double critValueDesc = 10d;
	    for (Alternative alternative : alternatives) {
		evals.put(alternative, g2, critValueDesc);
		critValueDesc -= 1d;
	    }
	}

	final Preorder<Alternative> dominance = new Dominance().getDominanceRelation(evals,
		getPreferenceDirections(criteria));
	assertNull(dominance);
    }
}
