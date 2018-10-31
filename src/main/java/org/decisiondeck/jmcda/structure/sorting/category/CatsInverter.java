package org.decisiondeck.jmcda.structure.sorting.category;

import java.util.NavigableSet;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;

public class CatsInverter {

    static public boolean copyInverseToTarget(CatsAndProfs source, CatsAndProfs target) {
	if (target.equals(source)) {
	    return false;
	}

	target.clear();

	Alternative freeProfile = null;
	Category freeCategory = null;

	for (Category newCategory : source.getCategories().descendingSet()) {
	    final Alternative profileUp = newCategory.getProfileUp();
	    if (profileUp != null) {
		final NavigableSet<Alternative> nextProfiles;
		if (target.getProfiles().isEmpty()) {
		    nextProfiles = source.getProfiles().tailSet(profileUp, true);
		} else {
		    nextProfiles = source.getProfiles().subSet(profileUp, true, target.getProfiles().last(), false);
		}
		for (Alternative nextProfile : nextProfiles.descendingSet()) {
		    if (freeCategory == null) {
			target.addProfile(nextProfile);
		    } else {
			target.setProfileUp(freeCategory.getId(), nextProfile);
			freeCategory = null;
		    }
		    freeProfile = nextProfile;
		}
	    }
	    final Category newSimpleCategory = new Category(newCategory.getId());
	    if (freeProfile == null) {
		target.addCategory(newSimpleCategory);
	    } else {
		target.setCategoryUp(freeProfile, newSimpleCategory);
		freeProfile = null;
	    }
	    freeCategory = newSimpleCategory;
	}

	/** first profiles in source, tail in target. */
	final NavigableSet<Alternative> tailProfiles;
	if (!source.getProfiles().isEmpty()) {
	    if (!target.getProfiles().isEmpty()) {
		tailProfiles = source.getProfiles().headSet(target.getProfiles().last(), false);
	    } else {
		tailProfiles = source.getProfiles();
	    }
	    for (Alternative tailProfile : tailProfiles.descendingSet()) {
		target.addProfile(tailProfile);
	    }
	}

	return true;
    }

}
