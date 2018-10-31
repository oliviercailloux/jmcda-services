package org.decisiondeck.jmcda.structure.sorting.category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Iterator;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.sorting.category.Categories;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.junit.Test;

public class CatsInverterTest {
    @Test
    public void testInvert() throws Exception {
	final CatsAndProfs catsAndProfs = Categories.newCatsAndProfs();
	catsAndProfs.addCategory("C1");
	catsAndProfs.addProfile(new Alternative("p12"));
	assertEquals(new Category("C1"), catsAndProfs.getCategoryDown(new Alternative("p12")));
	assertNull(catsAndProfs.getCategoryUp(new Alternative("p12")));
	catsAndProfs.addCategory("C2");
	catsAndProfs.addCategory("C3");
	assertNull(catsAndProfs.getProfileDown("C3"));
	catsAndProfs.addCategory("C4");
	catsAndProfs.setProfileUp("C4", new Alternative("p4a"));
	assertNull(catsAndProfs.getProfileDown("C3"));
	assertNull(catsAndProfs.getCategoryUp(new Alternative("p4a")));
	catsAndProfs.addCategory("C4bis");
	catsAndProfs.setProfileUp("C4bis", new Alternative("p4b"));
	catsAndProfs.addCategory("C5");
	assertEquals(new Alternative("p4a"), catsAndProfs.getProfileUp("C4"));
	catsAndProfs.setProfileUp("C5", new Alternative("p5a"));
	assertNull(catsAndProfs.getCategoryUp(new Alternative("p5a")));
	catsAndProfs.addProfile(new Alternative("p5b"));
	assertEquals(new Alternative("p5a"), catsAndProfs.getProfileUp("C5"));
	catsAndProfs.removeCategory("C4bis");
	assertEquals(new Alternative("p4a"), catsAndProfs.getProfileUp("C4"));
	assertEquals(new Alternative("p4b"), catsAndProfs.getProfileDown("C5"));

	final CatsAndProfs expected = Categories.newCatsAndProfs();
	expected.addCategory("C6");
	expected.setProfileDown("C6", new Alternative("p5b"));
	assertEquals(new Alternative("p5b"), expected.getProfileDown("C6"));
	expected.addCategory("C5");
	assertNull(expected.getProfileUp("C5"));
	expected.setProfileUp("C6", new Alternative("p5a"));
	assertEquals(new Alternative("p5a"), expected.getProfileDown("C5"));
	expected.addProfile(new Alternative("p4b"));
	expected.addCategory("C4bis");
	expected.addProfile(new Alternative("p4a"));
	expected.addCategory("C4");
	expected.addCategory("C3");
	expected.addCategory("C2");
	expected.setProfileUp("C2", new Alternative("p12"));
	expected.addCategory("C1");
	expected.removeCategory("C6");
	expected.removeCategory("C4bis");
	final Iterator<Alternative> iterator = expected.getProfiles().iterator();
	assertEquals(new Alternative("p5b"), iterator.next());
	assertEquals(new Alternative("p5a"), iterator.next());
	assertEquals(new Alternative("p5a"), expected.getProfileDown("C5"));

	final CatsAndProfs inverted = Categories.newCatsAndProfs();
	CatsInverter.copyInverseToTarget(catsAndProfs, inverted);
	assertEquals(expected, inverted);

    }
}
