package org.decisiondeck.jmcda.services.sorting.assignments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.decision_deck.jmcda.services.generator.DataGenerator;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.sorting.category.Categories;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilitiesRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class EmbeddedCredibilitiesTest {
    @Test
    public void testSmallestIntervals() throws Exception {
	// final ImmutableMap<Category, Double> creds1 = ImmutableMap.of(getC1(), 3d, getC2(), 4d, getC3(), 1d);
	final TreeMap<Category, Double> credibilitiesFirst = getCredibilities(getThreeCategories(), true,
		3d, 4d, 1d);
	final Set<SortedMap<Category, Double>> intervalsFirst = EmbeddedCredibilitiesExtractor
		.getSmallestMostCredibleIntervals(credibilitiesFirst, 5d);
	final Set<SortedMap<Category, Double>> expectedFirst = Sets.newHashSet();
	expectedFirst.add(getC1C2(3d, 4d));
	assertEquals(expectedFirst, intervalsFirst);

	final TreeMap<Category, Double> credibilitiesTwins = getCredibilities(getThreeCategories(), true,
		2d, 4d, 2d);
	final Set<SortedMap<Category, Double>> intervalsTwins = EmbeddedCredibilitiesExtractor
		.getSmallestMostCredibleIntervals(credibilitiesTwins, 5d);
	final Set<SortedMap<Category, Double>> expectedTwins = Sets.newHashSet();
	expectedTwins.add(getC1C2(2d, 4d));
	expectedTwins.add(getC2C3(4d, 2d));
	assertEquals(expectedTwins, intervalsTwins);

	final TreeMap<Category, Double> credibilitiesAll = getCredibilities(getThreeCategories(), true,
		3d, 1d, 2d);
	final Set<SortedMap<Category, Double>> intervalsAll = EmbeddedCredibilitiesExtractor
		.getSmallestMostCredibleIntervals(credibilitiesAll, 5d);
	final Set<SortedMap<Category, Double>> expectedAll = Sets.newHashSet();
	expectedAll.add(credibilitiesAll);
	assertEquals(expectedAll, intervalsAll);

	final TreeMap<Category, Double> credibilitiesHole = getCredibilities(getFourCategories(), true,
		2d, 2d, 0d, 3d);
	final Set<SortedMap<Category, Double>> intervalsHole = EmbeddedCredibilitiesExtractor
		.getSmallestMostCredibleIntervals(credibilitiesHole, 4d);
	final Set<SortedMap<Category, Double>> expectedHole = Sets.newHashSet();
	expectedHole.add(getC1C2(2d, 2d));
	assertEquals(expectedHole, intervalsHole);
    }

    @Test
    public void testProductSet() throws Exception {
	final TreeMap<Category, Double> credibilitiesFirst = getCredibilities(getThreeCategories(), true,
		3d, 4d, 1d);
	final TreeMap<Category, Double> credibilitiesTwins = getCredibilities(getThreeCategories(), true,
		2d, 4d, 2d);
	final TreeMap<Category, Double> credibilitiesAll = getCredibilities(getThreeCategories(), true,
		3d, 1d, 2d);
	final Set<SortedMap<Category, Double>> intervalsFirst = EmbeddedCredibilitiesExtractor
		.getSmallestMostCredibleIntervals(credibilitiesFirst, 5d);
	final Set<SortedMap<Category, Double>> expectedFirst = Sets.newHashSet();
	expectedFirst.add(getC1C2(3d, 4d));
	assertEquals(expectedFirst, intervalsFirst);

	final Set<SortedMap<Category, Double>> intervalsTwins = EmbeddedCredibilitiesExtractor
		.getSmallestMostCredibleIntervals(credibilitiesTwins, 5d);
	final Set<SortedMap<Category, Double>> expectedTwins = Sets.newHashSet();
	expectedTwins.add(getC1C2(2d, 4d));
	expectedTwins.add(getC2C3(4d, 2d));
	assertEquals(expectedTwins, intervalsTwins);

	final Set<SortedMap<Category, Double>> intervalsAll = EmbeddedCredibilitiesExtractor
		.getSmallestMostCredibleIntervals(credibilitiesAll, 5d);
	final Set<SortedMap<Category, Double>> expectedAll = Sets.newHashSet();
	expectedAll.add(credibilitiesAll);
	assertEquals(expectedAll, intervalsAll);

	final IOrderedAssignmentsWithCredibilities assignments = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	assignments.setCategories(getThreeCategories());
	assignments.setCredibilities(getA1(), credibilitiesFirst);
	assignments.setCredibilities(getA2(), credibilitiesTwins);
	assignments.setCredibilities(getA3(), credibilitiesAll);
	final Set<IOrderedAssignmentsWithCredibilitiesRead> productSet = EmbeddedCredibilitiesExtractor
		.getProductSetOfIntervals(assignments, 5d);
	assertEquals(2, productSet.size());

	final IOrderedAssignmentsWithCredibilities expected1 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	expected1.setCategories(getThreeCategories());
	expected1.setCredibilities(getA1(), getC1C2(3d, 4d));
	expected1.setCredibilities(getA2(), getC1C2(2d, 4d));
	expected1.setCredibilities(getA3(), credibilitiesAll);
	final IOrderedAssignmentsWithCredibilities expected2 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	expected2.setCategories(getThreeCategories());
	expected2.setCredibilities(getA1(), getC1C2(3d, 4d));
	expected2.setCredibilities(getA2(), getC2C3(4d, 2d));
	expected2.setCredibilities(getA3(), credibilitiesAll);
	final HashSet<IOrderedAssignmentsWithCredibilities> expected = Sets.newHashSet();
	expected.add(expected1);
	expected.add(expected2);
	assertEquals(expected, productSet);
    }

    SortedMap<Category, Double> getC2C3(double c2, double c3) {
	final SortedMap<Category, Double> c2c3 = Maps.newTreeMap(getThreeCategories().comparator());
	c2c3.put(getC2(), Double.valueOf(c2));
	c2c3.put(getC3(), Double.valueOf(c3));
	return c2c3;
    }

    Category getC3() {
	return new Category("c3");
    }

    Alternative getA2() {
	return new Alternative("a2");
    }

    NavigableSet<Category> getFourCategories() {
	final CatsAndProfs catsAndProfs = Categories.newCatsAndProfs();
	catsAndProfs.addCategory(getC1());
	catsAndProfs.addCategory(getC2());
	catsAndProfs.addCategory(getC3());
	catsAndProfs.addCategory(getC("c4"));
	return catsAndProfs.getCategories();
    }

    Category getC(String categoryName) {
	return new Category(categoryName);
    }

    Category getC2() {
	return new Category("c2");
    }

    Category getC1() {
	return new Category("c1");
    }

    SortedMap<Category, Double> getC1C2(double c1, double c2) {
	final SortedMap<Category, Double> c1c2 = Maps.newTreeMap(getThreeCategories().comparator());
	c1c2.put(getC1(), Double.valueOf(c1));
	c1c2.put(getC2(), Double.valueOf(c2));
	return c1c2;
    }

    Alternative getA3() {
	return new Alternative("a3");
    }

    Alternative getA1() {
	return new Alternative("a1");
    }

    Alternative getAlt(final String id) {
	return new Alternative(id);
    }

    Category getIthCategory(final CatsAndProfs cats, final int countFromOne) {
	final int position = countFromOne - 1;
	final Category cat = Iterables.get(cats.getCategories(), position);
	return cat;
    }

    @Test
    public void testByCredibilityLevelWithZeroes() throws Exception {
	final DataGenerator gen = new DataGenerator();
	final CatsAndProfs cats = gen.genCatsAndProfs(4);
	final IOrderedAssignmentsWithCredibilities input = AssignmentsFactory.newOrderedAssignmentsWithCredibilities();
	input.setCategories(cats.getCategories());
	setCredibilities(input, "a1", 3d, 0d, 5d, 1d);
	setCredibilities(input, "a2", 2d, 5d, 2d, 0d);
	setCredibilities(input, "a3", 2d, 2d, 0d, 3d);

	final IOrderedAssignmentsWithCredibilities output71 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	output71.setCategories(cats.getCategories());
	setCredibilities(output71, "a1", 3d, 0d, 5d, 0d);
	setCredibilities(output71, "a2", 2d, 5d, 0d, 0d);
	setCredibilities(output71, "a3", 2d, 2d, 0d, 3d);

	final IOrderedAssignmentsWithCredibilities output72 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	output72.setCategories(cats.getCategories());
	setCredibilities(output72, "a1", 3d, 0d, 5d, 0d);
	setCredibilities(output72, "a2", 0d, 5d, 2d, 0d);
	setCredibilities(output72, "a3", 2d, 2d, 0d, 3d);

	final Set<IOrderedAssignmentsWithCredibilitiesRead> expected7 = Sets.newHashSet();
	expected7.add(output71);
	expected7.add(output72);

	final IOrderedAssignmentsWithCredibilities output61 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	output61.setCategories(cats.getCategories());
	setCredibilities(output61, "a1", 0d, 0d, 5d, 1d);
	setCredibilities(output61, "a2", 2d, 5d, 0d, 0d);
	setCredibilities(output61, "a3", 2d, 2d, 0d, 3d);

	final IOrderedAssignmentsWithCredibilities output62 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	output62.setCategories(cats.getCategories());
	setCredibilities(output62, "a1", 0d, 0d, 5d, 1d);
	setCredibilities(output62, "a2", 0d, 5d, 2d, 0d);
	setCredibilities(output62, "a3", 2d, 2d, 0d, 3d);

	final Set<IOrderedAssignmentsWithCredibilitiesRead> expected6 = Sets.newHashSet();
	expected6.add(output61);
	expected6.add(output62);

	final IOrderedAssignmentsWithCredibilities output5 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	output5.setCategories(cats.getCategories());
	setCredibilities(output5, "a1", 0d, 0d, 5d, 0d);
	setCredibilities(output5, "a2", 0d, 5d, 0d, 0d);
	setCredibilities(output5, "a3", 0d, 2d, 0d, 3d);

	final Set<IOrderedAssignmentsWithCredibilitiesRead> expected5 = Sets.newHashSet();
	expected5.add(output5);

	final IOrderedAssignmentsWithCredibilities output4 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	output4.setCategories(cats.getCategories());
	setCredibilities(output4, "a1", 0d, 0d, 5d, 0d);
	setCredibilities(output4, "a2", 0d, 5d, 0d, 0d);
	setCredibilities(output4, "a3", 2d, 2d, 0d, 0d);

	final Set<IOrderedAssignmentsWithCredibilitiesRead> expected4 = Sets.newHashSet();
	expected4.add(output4);

	final IOrderedAssignmentsWithCredibilities output3 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	output3.setCategories(cats.getCategories());
	setCredibilities(output3, "a1", 0d, 0d, 5d, 0d);
	setCredibilities(output3, "a2", 0d, 5d, 0d, 0d);
	setCredibilities(output3, "a3", 0d, 0d, 0d, 3d);

	final Set<IOrderedAssignmentsWithCredibilitiesRead> expected3 = Sets.newHashSet();
	expected3.add(output3);

	final NavigableMap<Double, Set<IOrderedAssignmentsWithCredibilitiesRead>> expected = new TreeMap<Double, Set<IOrderedAssignmentsWithCredibilitiesRead>>();
	expected.put(Double.valueOf(3d), expected3);
	expected.put(Double.valueOf(4d), expected4);
	expected.put(Double.valueOf(5d), expected5);
	expected.put(Double.valueOf(6d), expected6);
	expected.put(Double.valueOf(7d), expected7);

	final NavigableMap<Double, Set<IOrderedAssignmentsWithCredibilitiesRead>> results = new EmbeddedCredibilitiesExtractor(
		input).getByCredibilityLevel();

	assertEquals(3d, results.firstKey().doubleValue(), 1e-6d);
	assertEquals(7d, results.lastKey().doubleValue(), 1e-6d);
	assertEquals(5, results.size());

	final Iterator<Set<IOrderedAssignmentsWithCredibilitiesRead>> resultsIterator = results.values().iterator();
	assertEquals(expected3, resultsIterator.next());
	assertEquals(expected4, resultsIterator.next());
	assertEquals(expected5, resultsIterator.next());
	assertEquals(expected6, resultsIterator.next());
	assertEquals(expected7, resultsIterator.next());
	assertFalse(resultsIterator.hasNext());

	assertEquals(expected, results);
    }

    void setCredibilities(IOrderedAssignmentsWithCredibilities assignments, String alternative, double... values) {
	assignments.setCredibilities(getAlt(alternative),
 getCredibilities(assignments.getCategories(), false, values));
    }

    NavigableSet<Category> getThreeCategories() {
	final CatsAndProfs catsAndProfs = Categories.newCatsAndProfs();
	catsAndProfs.addCategory(getC1());
	catsAndProfs.addCategory(getC2());
	catsAndProfs.addCategory(getC3());
	return catsAndProfs.getCategories();
    }

    @Test
    public void testByCredibilityLevel() throws Exception {
	final DataGenerator gen = new DataGenerator();
	final CatsAndProfs cats = gen.genCatsAndProfs(4);
	final IOrderedAssignmentsWithCredibilities input = AssignmentsFactory.newOrderedAssignmentsWithCredibilities();
	input.setCategories(cats.getCategories());
	setCredibilities(input, "a1", 3d, 0d, 5d, 1d);
	setCredibilities(input, "a2", 2d, 5d, 2d, 0d);
	setCredibilities(input, "a3", 2d, 2d, 3d, 0d);

	final IOrderedAssignmentsWithCredibilities expected71 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	expected71.setCategories(cats.getCategories());
	setCredibilities(expected71, "a1", 3d, 0d, 5d, 0d);
	setCredibilities(expected71, "a2", 2d, 5d, 0d, 0d);
	setCredibilities(expected71, "a3", 2d, 2d, 3d, 0d);

	final IOrderedAssignmentsWithCredibilities expected72 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	expected72.setCategories(cats.getCategories());
	setCredibilities(expected72, "a1", 3d, 0d, 5d, 0d);
	setCredibilities(expected72, "a2", 0d, 5d, 2d, 0d);
	setCredibilities(expected72, "a3", 2d, 2d, 3d, 0d);

	final Set<IOrderedAssignmentsWithCredibilitiesRead> expected7 = Sets.newHashSet();
	expected7.add(expected71);
	expected7.add(expected72);

	final IOrderedAssignmentsWithCredibilities expected61 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	expected61.setCategories(cats.getCategories());
	setCredibilities(expected61, "a1", 0d, 0d, 5d, 1d);
	setCredibilities(expected61, "a2", 2d, 5d, 0d, 0d);
	setCredibilities(expected61, "a3", 2d, 2d, 3d, 0d);

	final IOrderedAssignmentsWithCredibilities expected62 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	expected62.setCategories(cats.getCategories());
	setCredibilities(expected62, "a1", 0d, 0d, 5d, 1d);
	setCredibilities(expected62, "a2", 0d, 5d, 2d, 0d);
	setCredibilities(expected62, "a3", 2d, 2d, 3d, 0d);

	final Set<IOrderedAssignmentsWithCredibilitiesRead> expected6 = Sets.newHashSet();
	expected6.add(expected61);
	expected6.add(expected62);

	final IOrderedAssignmentsWithCredibilities expected51 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	expected51.setCategories(cats.getCategories());
	setCredibilities(expected51, "a1", 0d, 0d, 5d, 0d);
	setCredibilities(expected51, "a2", 0d, 5d, 0d, 0d);
	setCredibilities(expected51, "a3", 0d, 2d, 3d, 0d);

	final Set<IOrderedAssignmentsWithCredibilitiesRead> expected5 = Sets.newHashSet();
	expected5.add(expected51);

	final IOrderedAssignmentsWithCredibilities expected31 = AssignmentsFactory
		.newOrderedAssignmentsWithCredibilities();
	expected31.setCategories(cats.getCategories());
	setCredibilities(expected31, "a1", 0d, 0d, 5d, 0d);
	setCredibilities(expected31, "a2", 0d, 5d, 0d, 0d);
	setCredibilities(expected31, "a3", 0d, 0d, 3d, 0d);

	final Set<IOrderedAssignmentsWithCredibilitiesRead> expected3 = Sets.newHashSet();
	expected3.add(expected31);

	final NavigableMap<Double, Set<IOrderedAssignmentsWithCredibilitiesRead>> expected = new TreeMap<Double, Set<IOrderedAssignmentsWithCredibilitiesRead>>();
	expected.put(Double.valueOf(3d), expected3);
	expected.put(Double.valueOf(5d), expected5);
	expected.put(Double.valueOf(6d), expected6);
	expected.put(Double.valueOf(7d), expected7);

	final NavigableMap<Double, Set<IOrderedAssignmentsWithCredibilitiesRead>> results = new EmbeddedCredibilitiesExtractor(
		input).getByCredibilityLevel();

	assertEquals(3d, results.firstKey().doubleValue(), 1e-6d);
	assertEquals(7d, results.lastKey().doubleValue(), 1e-6d);
	assertEquals(4, results.size());

	final Iterator<Set<IOrderedAssignmentsWithCredibilitiesRead>> resultsIterator = results.values().iterator();
	assertEquals(expected3, resultsIterator.next());
	assertEquals(expected5, resultsIterator.next());
	assertEquals(expected6, resultsIterator.next());
	assertEquals(expected7, resultsIterator.next());
	assertFalse(resultsIterator.hasNext());

	assertEquals(expected, results);
    }

    /**
     * A convenience method to build a map representing credibilities of assignments to categories.
     * 
     * @param categories
     *            not <code>null</code>, not empty.
     * @param includeZeroValues
     *            if <code>true</code>, zero values in input are added to the map. If <code>false</code>, zero values
     *            are skipped.
     * @param values
     *            must have the same size as the categories. Not negative. Zero values are not put in the map but are
     *            skipped. At least one value must be non-zero.
     * @return a map representing credibilities, not <code>null</code>, without <code>null</code> values, without
     *         <code>null</code> key, with at least one category set.
     */
    static public TreeMap<Category, Double> getCredibilities(SortedSet<Category> categories, boolean includeZeroValues,
	    double... values) {
	Preconditions.checkNotNull(categories);
	Preconditions.checkArgument(categories.size() == values.length, "Not equal number of categories and values.");

	final TreeMap<Category, Double> thisAss = Maps.newTreeMap(categories.comparator());
	final Iterator<Category> catsIter = categories.iterator();
	for (double cred : values) {
	    final Category cat = catsIter.next();
	    if (cred < 0d) {
		throw new IllegalArgumentException("Found negative credibility.");
	    }
	    if (includeZeroValues || cred > 0d) {
		thisAss.put(cat, Double.valueOf(cred));
	    }
	}
	if (thisAss.isEmpty()) {
	    throw new IllegalArgumentException("No valid values.");
	}
	return thisAss;
    }
}
