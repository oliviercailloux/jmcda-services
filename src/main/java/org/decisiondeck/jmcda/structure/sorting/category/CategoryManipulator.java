package org.decisiondeck.jmcda.structure.sorting.category;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.NavigableSet;

import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;

public class CategoryManipulator {

    /**
     * <p>
     * Adds the given categories to the given target. It must be possible to determine the order of the given categories
     * among the existing categories in the target, otherwise, this method throws an exception and the target is left
     * unchanged.
     * </p>
     * <p>
     * TODO not implemented yet.
     * </p>
     * 
     * @param categories
     *            not <code>null</code>.
     * @param target
     *            not <code>null</code>.
     */
    static public void addTo(NavigableSet<Category> categories, CatsAndProfs target) {
	/**
	 * algorithm applicable for every target corresponding to a set defined with an external order. Should
	 * generalize.
	 */
	/** Do it in two steps: first, determine if applicable (this is a method per se), then do it. */
	checkNotNull(categories);
	checkNotNull(target);
	final NavigableSet<Category> targetCategories = target.getCategories();
	if (categories.isEmpty()) {
	    return;
	}

	/** Determine the head categories. */
	final Iterator<Category> iterator = categories.iterator();
	final Category first = iterator.next();
	@SuppressWarnings("unused")
	final NavigableSet<Category> headCategories;
	Category endOfHead = first;
	while (!targetCategories.contains(endOfHead) && iterator.hasNext()) {
	    endOfHead = iterator.next();
	}
	if (targetCategories.contains(endOfHead) && !targetCategories.first().equals(endOfHead)) {
	    throw new IllegalStateException("Impossible to determine relative order of the first categories, from "
		    + first + " to " + endOfHead + ", because the latter is not the beginning of the target.");
	} else if (targetCategories.contains(endOfHead) && !targetCategories.first().equals(endOfHead)) {
	    headCategories = categories.subSet(first, true, endOfHead, false);
	} else {
	    /** Iterator is exhausted. */
	    if (targetCategories.isEmpty()) {
		headCategories = categories;
	    } else {
		throw new IllegalStateException(
			"Impossible to determine relative order of the given categories because they are disjoint from the target categories.");
	    }
	}

	// for (Category toAdd : headCategories) {
	/** to do: Implement adding categories to target. */
	// }

	Category category = endOfHead;
	/** Here endOfHead is contained in the target, except if target is empty. */
	while (targetCategories.contains(category)) {
	    @SuppressWarnings("unused")
	    final NavigableSet<Category> interval;
	    final Category next;
	    if (targetCategories.last().equals(category)) {
		interval = categories.tailSet(category, false);
		next = null;
	    } else {
		final Category higher = targetCategories.higher(category);
		interval = categories.subSet(category, false, higher, false);
		next = higher;
	    }
	    /** Insert contents of interval after category. */
	    // for (Category toAdd : interval) {
	    //
	    // }
	    category = next;
	}

	throw new UnsupportedOperationException();
    }

}
