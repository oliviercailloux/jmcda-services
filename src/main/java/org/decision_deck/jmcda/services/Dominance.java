package org.decision_deck.jmcda.services;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.utils.relation.graph.Preorder;
import org.decision_deck.utils.relation.graph.Preorders;
import org.decisiondeck.jmcda.exc.InvalidInputException;

import com.google.common.base.Functions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class Dominance {
    /**
     * <p>
     * Returns the dominance relation in the large sense as a complete preorder, if it is possible. Dominance in the
     * large sense means that two alternatives dominate each other if they are ex-æquo. In particular, if all
     * alternatives have equal evaluations, the returned preorder has only one rank.
     * </p>
     * <p>
     * The computed relation is a partial preorder iff there are conflicting evaluations, i.e. one criterion gives an
     * alternative a1 strictly better than a2 while an other one gives the opposite. In such case, this method returns
     * <code>null</code>.
     * </p>
     * 
     * @param evaluations
     *            not null, not empty. Must be complete.
     * @param directions
     *            must contain the preference directions for every criterion contained in the given evaluations.
     * 
     * @return the dominance relation as a preorder, or <code>null</code> iff there are conflicting evaluations.
     * @throws InvalidInputException
     *             if the given evaluations are empty or incomplete, or if some preference directions are missing.
     */
    public Preorder<Alternative> getDominanceRelation(EvaluationsRead evaluations,
	    Map<Criterion, PreferenceDirection> directions) throws InvalidInputException {
	new ConsistencyChecker().assertCompletePreferenceDirectionsDirect(directions);
	if (evaluations.isEmpty()) {
	    throw new InvalidInputException("Evaluations empty.");
	}
	if (!evaluations.isComplete()) {
	    throw new InvalidInputException("Evaluations incomplete.");
	}
	final Ordering<Double> asc = Ordering.natural();
	final Map<Criterion, Ordering<Double>> orderings = Maps.transformValues(directions,
		Intervals.getDirectionToOrderingFunction(asc));
	final Map<Criterion, Map<Alternative, Double>> columnMap;
	columnMap = evaluations.asTable().columnMap();
	// columnMap = EvaluationsUtils.asColumnMap(evaluations);
	final Map<Criterion, Preorder<Alternative>> orderedAlternativesByValues = Maps.transformEntries(columnMap,
		new EntryTransformer<Criterion, Map<Alternative, Double>, Preorder<Alternative>>() {
		    @Override
		    public Preorder<Alternative> transformEntry(Criterion key, Map<Alternative, Double> value) {
			return new Preorder<Alternative>(value.keySet(), orderings.get(key).onResultOf(
				Functions.forMap(value)));
		    }
		});

	final Iterator<Preorder<Alternative>> iteratorPreorders = orderedAlternativesByValues.values().iterator();
	assert (iteratorPreorders.hasNext());
	Preorder<Alternative> preorder = iteratorPreorders.next();
	while (iteratorPreorders.hasNext()) {
	    final Preorder<Alternative> nextPreorder = iteratorPreorders.next();
	    preorder = Preorders.getIntersection(preorder, nextPreorder);
	    if (preorder == null) {
		return null;
	    }
	}
	return preorder;
    }

    /**
     * Retrieves the strict dominance relation. This method result, assuming it is non <code>null</code>, is equivalent
     * to {@link #getDominanceRelation} followed by {@link Preorder#getTotalOrder()}.
     * 
     * @param evaluations
     *            not null, not empty. Must be complete.
     * @param directions
     *            must contain the preference directions for every criterion contained in the given evaluations.
     * 
     * @return the dominance relation as a total order, or <code>null</code> iff there are conflicting evaluations, i.e.
     *         one criterion gives an alternative a1 strictly better than a2 while an other one gives the opposite, or
     *         if some alternatives have identical evaluations (i.e. the large dominance preorder is not a total order).
     * @throws InvalidInputException
     *             if the given evaluations are empty or incomplete, or if some preference directions are missing.
     */
    public NavigableSet<Alternative> getStrictDominanceOrder(EvaluationsRead evaluations,
	    Map<Criterion, PreferenceDirection> directions) throws InvalidInputException {
	final Preorder<Alternative> dominance = getDominanceRelation(evaluations, directions);
	if (dominance == null) {
	    return null;
	}
	return dominance.getTotalOrder();
    }

    private List<Double> getSortedValuesDecr(final SetMultimap<Double, Alternative> values,
	    PreferenceDirection preferenceDirection) throws InvalidInputException {
	checkNotNull(values);
	checkNotNull(preferenceDirection);
	final List<Double> sortedKeys = new LinkedList<Double>(values.keySet());
	final Comparator<Object> order;
	switch (preferenceDirection) {
	case MAXIMIZE:
	    order = Collections.reverseOrder();
	    break;
	case MINIMIZE:
	    order = null;
	    break;
	default:
	    throw new IllegalStateException();
	}
	Collections.sort(sortedKeys, order);
	return sortedKeys;
    }

    private SetMultimap<Double, Alternative> getValues(EvaluationsRead evaluations, Criterion criterion) {
	final SetMultimap<Double, Alternative> values = HashMultimap.create();
	for (Alternative alternative : evaluations.getRows()) {
	    final Double entry = evaluations.getEntry(alternative, criterion);
	    values.put(entry, alternative);
	}
	return values;
    }

    /**
     * Computes the dominance relation in the large sense, i.e. two alternatives dominate each other if they are
     * ex-æquo. In particular, if all alternatives are equal the returned preorder is a complete graph.
     * 
     * @param evaluations
     *            not null, not empty. Must be complete.
     * @param scales
     *            must contain the preference directions for every criterion contained in the given evaluations.
     * 
     * @return the dominance relation as a preorder, or <code>null</code> iff there are conflicting evaluations, i.e.
     *         one criterion gives an alternative a1 strictly better than a2 while an other one gives the opposite.
     * @throws InvalidInputException
     *             if the given evaluations are empty or incomplete, or if some preference directions are missing.
     */
    @Deprecated
    public Preorder<Alternative> getDominanceRelationOld(EvaluationsRead evaluations,
	    Map<Criterion, PreferenceDirection> scales) throws InvalidInputException {
	new ConsistencyChecker().assertCompletePreferenceDirectionsDirect(scales);
	if (evaluations.isEmpty()) {
	    throw new InvalidInputException("Evaluations empty.");
	}
	if (!evaluations.isComplete()) {
	    throw new InvalidInputException("Evaluations incomplete.");
	}

	final Preorder<Alternative> preorder = new Preorder<Alternative>();
	final Iterator<Criterion> iterator = evaluations.getColumns().iterator();
	{
	    final Criterion first = iterator.next();
	    final SetMultimap<Double, Alternative> values = getValues(evaluations, first);
	    final List<Double> sortedKeys = getSortedValuesDecr(values, scales.get(first));
	    for (Double value : sortedKeys) {
		final Set<Alternative> equallyRanked = values.get(value);
		final boolean changed = preorder.putAllAsLowest(equallyRanked);
		assert (changed);
	    }
	}

	while (iterator.hasNext()) {
	    final Criterion criterion = iterator.next();
	    final SetMultimap<Double, Alternative> values = getValues(evaluations, criterion);
	    final List<Double> sortedKeys = getSortedValuesDecr(values, scales.get(criterion));
	    final Iterator<Set<Alternative>> preorderIterator = preorder.asListOfSets().iterator();
	    for (Double value : sortedKeys) {
		final Set<Alternative> equallyRanked = values.get(value);
		final Set<Alternative> left = Sets.newHashSet(equallyRanked);
		while (!left.isEmpty()) {
		    final Set<Alternative> existing = preorderIterator.next();
		    final Set<Alternative> supplExisting = Sets.difference(existing, left).immutableCopy();
		    left.removeAll(existing);
		    if (!left.isEmpty() && !supplExisting.isEmpty()) {
			return null;
		    }
		    if (left.isEmpty()) {
			for (Alternative alternative : supplExisting) {
			    preorder.lower(alternative);
			}
		    }
		}
	    }
	}
	return preorder;
    }
}
