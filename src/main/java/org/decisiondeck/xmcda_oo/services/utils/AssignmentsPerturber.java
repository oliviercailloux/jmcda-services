package org.decisiondeck.xmcda_oo.services.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignments;

/**
 * <p>
 * Perturbs assignments. Perturbing an assignment (of an alternative A into a category C1) is transforming it into an
 * assignment of A into a category C2 such that C1 is different than C2.
 * </p>
 * <p>
 * Requires the possibility of having a superset of categories compared to those where alternatives are indeed assigned.
 * </p>
 * 
 * @author Olivier Cailloux
 * 
 */
public class AssignmentsPerturber {

    public AssignmentsPerturber() {
	m_propPerturb = -1;
	m_assignments = null;
    }

    /**
     * May be <code>null</code>.
     */
    private IOrderedAssignments m_assignments;
    private double m_propPerturb;

    /**
     * @param assignments
     *            <code>null</code> to unbind.
     */
    public void bindTo(IOrderedAssignments assignments) {
	m_assignments = assignments;
    }

    /**
     * <p>
     * Assignments must be bound, the proportion of perturbation must be set.
     * </p>
     * <p>
     * Perturbs the assignments this object is bound to. The bound assignments are perturbed according to the proportion
     * previously set. The perturbation is done in place. Only if the proportion is zero or rounds to zero will the
     * assignments be unchanged.
     * </p>
     * 
     * @throws InvalidInputException
     *             Perturbing assignments having only one category is impossible, in that case and if the proportion to
     *             be perturbed leads to perturb at least one assignment (thus does not round to zero), an error is
     *             raised. Exception also raised if the bound assignments are multi-categories and at least one
     *             assignment must be perturbed: perturbation of multi-categories assignments is not defined.</p>
     */
    public void perturb() throws InvalidInputException {
	if (m_propPerturb < 0 || m_propPerturb > 1) {
	    throw new IllegalStateException("Illegal prop perturb: " + m_propPerturb + ".");
	}
	if (m_assignments == null) {
	    throw new IllegalStateException("No assignments bound.");
	}
	final int nbPerturb = (int) Math.round(m_assignments.getAlternatives().size() * m_propPerturb);
	final int nbCats = m_assignments.getCategories().size();
	if (nbPerturb > 0 && nbCats < 2) {
	    throw new InvalidInputException("These assignments contain only " + nbCats
		    + " category, impossible to perturb " + nbPerturb + " assignments.");
	}
	final int nbAlts = m_assignments.getAlternatives().size();
	final List<Alternative> alts = new LinkedList<Alternative>(m_assignments.getAlternatives());
	final List<Category> cats = new LinkedList<Category>(m_assignments.getCategories());
	final Random random = new Random();
	for (int i = 0; i < nbPerturb; ++i) {
	    int altPos = random.nextInt(nbAlts);
	    final Alternative alt = alts.get(altPos);
	    final int destCatPos = random.nextInt(nbCats - 1);
	    final Category origCat = m_assignments.getCategory(alt);
	    final Category destCat = cats.get(destCatPos);
	    final Category realDestCat;
	    if (origCat.equals(destCat)) {
		realDestCat = cats.get(nbCats - 1);
	    } else {
		realDestCat = destCat;
	    }
	    m_assignments.setCategory(alt, realDestCat);
	}
    }

    /**
     * Sets the proportion of assignments to be perturbed.
     * 
     * @param propPerturb
     *            a number between zero and one, or a negative number for not set. If zero, perturb will do nothing. If
     *            one, every assignments will be perturbed.
     */
    public void setProportionPerturbation(double propPerturb) {
	if (propPerturb > 1) {
	    throw new IllegalArgumentException("Illegal proportion perturb: " + propPerturb + ".");
	}
	if (propPerturb < 0) {
	    m_propPerturb = -1;
	} else {
	    m_propPerturb = propPerturb;
	}
    }

    /**
     * @return -1 iff not set, otherwise, a number between zero and one.
     */
    public double getProportionPerturbation() {
	return m_propPerturb;
    }

}
