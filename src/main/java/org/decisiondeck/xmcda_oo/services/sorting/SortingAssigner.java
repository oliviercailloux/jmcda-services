package org.decisiondeck.xmcda_oo.services.sorting;

import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.matrix.SparseAlternativesMatrixFuzzy;
import org.decision_deck.jmcda.structure.sorting.SortingMode;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decisiondeck.jmcda.exc.InputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignments;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsUtils;

import com.google.common.base.Preconditions;

public class SortingAssigner {

	/**
	 * The allowed difference between a one, or a zero, and the observed values
	 * in the outranking matrix.
	 */
	static public double BINARY_TOLERANCE = 1e-3d;
	private IOrderedAssignmentsToMultiple m_allAssignments;
	private IOrderedAssignments m_assignments;

	public SortingAssigner() {
		m_allAssignments = null;
		m_assignments = null;
	}

	/**
	 * <p>
	 * Assigns each alternative according to the given sorting mode.
	 * </p>
	 * <p>
	 * The input is valid if the given alternatives and the profiles in the
	 * categories are a subset of the alternatives found in the outranking
	 * matrix, the categories are complete, the outranking is a binary matrix,
	 * thus has only one and zero, and has entries for all alternatives and
	 * profiles consider. A small tolerance is allowed, e.g. 1-1e-5 will count
	 * as a one.
	 * </p>
	 * <p>
	 * If the sorting mode is BOTH, the returned object implements
	 * {@link IOrderedAssignmentsToMultiple}, otherwise, it implements
	 * {@link IOrderedAssignments}.
	 * </p>
	 *
	 * @param mode
	 *            not <code>null</code>.
	 * @param alternatives
	 *            the alternatives to assign.
	 * @param outranking
	 *            not <code>null</code>.
	 * @param categories
	 *            not <code>null</code>.
	 * @return not <code>null</code>.
	 * @throws InvalidInputException
	 *             if the input data is invalid.
	 */
	public IOrderedAssignmentsToMultipleRead assign(SortingMode mode, Set<Alternative> alternatives,
			SparseAlternativesMatrixFuzzy outranking, CatsAndProfs categories) throws InvalidInputException {
		switch (mode) {
		case OPTIMISTIC:
			return optimistic(alternatives, outranking, categories);
		case PESSIMISTIC:
			return pessimistic(alternatives, outranking, categories);
		case BOTH:
			return both(alternatives, outranking, categories);
		default:
			throw new IllegalStateException("Unknown mode.");
		}
	}

	/**
	 * <p>
	 * Assigns each alternative to both the optimistic and the pessimistic
	 * categories, as well as to each category in between.
	 * </p>
	 * <p>
	 * The input is valid if the given alternatives and the profiles in the
	 * categories are a subset of the alternatives found in the outranking
	 * matrix, the categories are complete, the outranking is a binary matrix,
	 * thus has only one and zero, and has entries for all alternatives and
	 * profiles consider. A small tolerance is allowed, e.g. 1-1e-5 will count
	 * as a one.
	 * </p>
	 *
	 * @param alternatives
	 *            the alternatives to assign.
	 * @param outranking
	 *            not <code>null</code>.
	 * @param categories
	 *            not <code>null</code>.
	 * @return not <code>null</code>.
	 * @throws InvalidInputException
	 *             if the input data is invalid.
	 */
	public IOrderedAssignmentsToMultiple both(Set<Alternative> alternatives, SparseAlternativesMatrixFuzzy outranking,
			CatsAndProfs categories) throws InvalidInputException {
		Preconditions.checkNotNull(alternatives);
		Preconditions.checkNotNull(outranking);
		Preconditions.checkNotNull(categories);
		m_allAssignments = AssignmentsFactory.newOrderedAssignmentsToMultiple();
		m_allAssignments.setCategories(categories.getCategories());
		m_assignments = null;
		assignOptimistic(alternatives, outranking, categories);
		assignPessimistic(alternatives, outranking, categories);
		return m_allAssignments;
	}

	/**
	 * <p>
	 * Assigns each alternative to the lowest category such that the alternative
	 * does not outrank the up profile corresponding to that category and the
	 * profile outranks the alternative.
	 * </p>
	 * <p>
	 * The input is valid if the given alternatives and the profiles in the
	 * categories are a subset of the alternatives found in the outranking
	 * matrix, the categories are complete, the outranking is a binary matrix,
	 * thus has only one and zero, and has entries for all alternatives and
	 * profiles consider. A small tolerance is allowed, e.g. 1-1e-5 will count
	 * as a one.
	 * </p>
	 *
	 * @param alternatives
	 *            the alternatives to assign.
	 * @param outranking
	 *            not <code>null</code>.
	 * @param categories
	 *            not <code>null</code>.
	 * @return not <code>null</code>.
	 * @throws InvalidInputException
	 *             if the input data is invalid.
	 */
	public IOrderedAssignments optimistic(Set<Alternative> alternatives, SparseAlternativesMatrixFuzzy outranking,
			CatsAndProfs categories) throws InvalidInputException {
		Preconditions.checkNotNull(alternatives);
		Preconditions.checkNotNull(outranking);
		Preconditions.checkNotNull(categories);
		m_allAssignments = null;
		m_assignments = AssignmentsFactory.newOrderedAssignments();
		m_assignments.setCategories(categories.getCategories());
		assignOptimistic(alternatives, outranking, categories);
		return m_assignments;
	}

	/**
	 * <p>
	 * Assigns each alternative to the highest category such that the
	 * alternative outranks the profile corresponding to that category.
	 * </p>
	 * <p>
	 * The input is valid if the given alternatives and the profiles in the
	 * categories are a subset of the alternatives found in the outranking
	 * matrix, the categories are complete, the outranking is a binary matrix,
	 * thus has only one and zero, and has entries for all alternatives and
	 * profiles consider. A small tolerance is allowed, e.g. 1-1e-5 will count
	 * as a one.
	 * </p>
	 *
	 * @param alternatives
	 *            the alternatives to assign.
	 * @param outranking
	 *            not <code>null</code>.
	 * @param categories
	 *            not <code>null</code>.
	 * @return not <code>null</code>.
	 * @throws InvalidInputException
	 *             if the input data is invalid.
	 */
	public IOrderedAssignments pessimistic(Set<Alternative> alternatives, SparseAlternativesMatrixFuzzy outranking,
			CatsAndProfs categories) throws InvalidInputException {
		Preconditions.checkNotNull(alternatives);
		Preconditions.checkNotNull(outranking);
		Preconditions.checkNotNull(categories);
		m_allAssignments = null;
		m_assignments = AssignmentsFactory.newOrderedAssignments();
		m_assignments.setCategories(categories.getCategories());
		assignPessimistic(alternatives, outranking, categories);
		return m_assignments;
	}

	private void assignOptimistic(Set<Alternative> alternatives, SparseAlternativesMatrixFuzzy outranking,
			CatsAndProfs categories) throws InvalidInputException {
		InputCheck.check(categories.isComplete(), "Given categories are incomplete.");
		for (Alternative alternative : alternatives) {
			Category catAssigned = null;
			for (Category category : categories.getCategories()) {
				final Alternative profileUp = category.getProfileUp();
				if (profileUp == null) {
					assert category.equals(categories.getCategories().last());
					catAssigned = category;
					break;
				}
				if (outranks(outranking, profileUp, alternative) && !outranks(outranking, alternative, profileUp)) {
					catAssigned = category;
					break;
				}
			}
			assert(catAssigned != null);

			setCategory(alternative, catAssigned);
		}
	}

	private void assignPessimistic(Set<Alternative> alternatives, SparseAlternativesMatrixFuzzy outranking,
			CatsAndProfs categories) throws InvalidInputException {
		InputCheck.check(categories.isComplete(), "Given categories " + categories + " are incomplete.");
		for (Alternative alternative : alternatives) {
			Category catAssigned = null;
			for (Category category : categories.getCategoriesFromBest()) {
				final Alternative profileDown = category.getProfileDown();
				if (profileDown == null) {
					assert category.equals(categories.getCategories().first());
					catAssigned = category;
					break;
				}
				if (outranks(outranking, alternative, profileDown)) {
					catAssigned = category;
					break;
				}
			}
			assert(catAssigned != null);

			setCategory(alternative, catAssigned);
		}
	}

	private boolean outranks(SparseAlternativesMatrixFuzzy outranking, Alternative alternative,
			final Alternative profile) throws InvalidInputException {
		final Double entry = outranking.getEntry(alternative, profile);
		if (entry == null) {
			throw new InvalidInputException("Missing outranking information for " + alternative + ", " + profile + ".");
		}
		final double value = entry.doubleValue();
		final boolean outranks = 1d + BINARY_TOLERANCE >= value && value >= 1d - BINARY_TOLERANCE;
		final boolean notOutranks = 0d + BINARY_TOLERANCE >= value && value >= 0d - BINARY_TOLERANCE;
		InputCheck.check(outranks || notOutranks,
				"Non boolean outranking value=" + value + " for " + alternative + ", " + profile + ".");
		return outranks;
	}

	private void setCategory(Alternative alternative, Category category) {
		if (m_assignments != null) {
			m_assignments.setCategory(alternative, category);
		}
		if (m_allAssignments != null) {
			AssignmentsUtils.addToCategories(m_allAssignments, alternative, category);
		}
	}

}
