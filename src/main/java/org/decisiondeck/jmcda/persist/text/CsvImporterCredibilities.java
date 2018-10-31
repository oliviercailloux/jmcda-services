package org.decisiondeck.jmcda.persist.text;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsUtils;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.group_assignments.IGroupSortingAssignmentsWithCredibilities;

import com.csvreader.CsvReader;
import com.google.common.io.CharSource;

/**
 * <p>
 * Imports credibilities written in a CSV format.
 * </p>
 * <p>
 * Accepts missing values, and even empty evaluations, in which case only the
 * criteria and alternatives are set. Accepts a completely empty row or column,
 * as long as all headers (rows and columns) are set. TODO true?
 * </p>
 *
 * @author Olivier Cailloux
 *
 */
public class CsvImporterCredibilities {
	private CharSource m_source;

	public CsvImporterCredibilities() {
		m_source = null;
	}

	public CsvImporterCredibilities(CharSource source) {
		m_source = source;
	}

	public CharSource getSource() {
		return m_source;
	}

	public IGroupSortingAssignmentsWithCredibilities read(Collection<Category> categories)
			throws InvalidInputException, IOException {
		final Reader reader = m_source.openBufferedStream();
		if (reader == null) {
			throw new InvalidInputException("Source not found.");
		}
		try {
			final CsvReader csvReader = new CsvReader(reader);
			if (!csvReader.readHeaders()) {
				throw new InvalidInputException("Couldn't read headers.");
			}

			final List<String> headers = Arrays.asList(csvReader.getHeaders());
			if (!headers.equals(Arrays.asList("DM", "Alternative", "Category", "Credibility"))) {
				throw new InvalidInputException("Unexpected headers.");
			}
			final IGroupSortingAssignmentsWithCredibilities data = ProblemFactory
					.newGroupSortingResultsWithCredibilities();
			for (Category category : categories) {
				data.getCatsAndProfs().addCategory(category);
			}

			while (csvReader.readRecord()) {
				final String dmId = csvReader.get("DM");
				if (dmId.isEmpty()) {
					throw new InvalidInputException("Empty DM found.");
				}
				final DecisionMaker dm = new DecisionMaker(dmId);
				data.getDms().add(dm);
				final String altId = csvReader.get("Alternative");
				if (altId.isEmpty()) {
					throw new InvalidInputException("Empty alternative found.");
				}
				final Alternative alternative = new Alternative(altId);
				final String catId = csvReader.get("Category");
				if (catId.isEmpty()) {
					throw new InvalidInputException("Empty category found.");
				}
				final Category category = new Category(catId);
				final String credibilityStr = csvReader.get("Credibility");
				final double credibility;
				try {
					credibility = Double.parseDouble(credibilityStr);
				} catch (NumberFormatException exc) {
					throw new InvalidInputException("Invalid credibility string: " + credibilityStr + ".");
				}
				final IOrderedAssignmentsWithCredibilities assignments = data.getAssignments(dm);
				final NavigableMap<Category, Double> current = assignments.getCredibilities(alternative);
				if (current != null && current.containsKey(category)) {
					throw new InvalidInputException(
							"Duplicate entry at: " + dm + ", " + alternative + ", " + category + ".");
				}
				AssignmentsUtils.addToCredibilities(assignments, alternative, category, credibility);
			}
			reader.close();

			return data;
		} finally {
			reader.close();
		}
	}

	public void setSource(CharSource source) {
		m_source = source;
	}
}
