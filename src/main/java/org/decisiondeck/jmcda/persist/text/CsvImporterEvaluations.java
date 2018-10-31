package org.decisiondeck.jmcda.persist.text;

import java.io.IOException;
import java.io.Reader;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;

import com.csvreader.CsvReader;
import com.google.common.io.CharSource;

/**
 * <p>
 * Imports evaluations written in a CSV format. One way to create a correct CSV
 * file is to use OpenOffice calc, save as CSV, decimal separator must be dot
 * (use e.g. english linguistics), export parameters for the CSV filter: UTF-8,
 * field-sep comma, text-sep double quote, raw cell data (do not record cell 'as
 * is').
 * </p>
 * <p>
 * Accepts missing values, and even empty evaluations, in which case only the
 * criteria and alternatives are set. Accepts a completely empty row or column,
 * as long as all headers (rows and columns) are set.
 * </p>
 *
 * @author Olivier Cailloux
 *
 */
public class CsvImporterEvaluations {
	private CharSource m_source;

	public CsvImporterEvaluations() {
		m_source = null;
	}

	public CsvImporterEvaluations(CharSource source) {
		m_source = source;
	}

	public CharSource getSource() {
		return m_source;
	}

	public IProblemData read() throws InvalidInputException, IOException {
		final Reader reader = m_source.openBufferedStream();
		if (reader == null) {
			throw new InvalidInputException("Source not found.");
		}
		try {
			final CsvReader csvReader = new CsvReader(reader);
			if (!csvReader.readHeaders()) {
				throw new InvalidInputException("Couldn't read headers.");
			}

			final String[] headers = csvReader.getHeaders();
			if (headers.length < 2) {
				throw new InvalidInputException("Should have at least two columns.");
			}
			// if (!headers[0].equals("Alternative")) {
			// throw new InvalidInputException("First token should be the word
			// 'Alternative'.");
			// }
			final IProblemData data = ProblemFactory.newProblemData();
			final Evaluations evaluations = EvaluationsUtils.newEvaluationMatrix();

			for (int i = 1; i < headers.length; ++i) {
				final String id = headers[i];
				if (id.isEmpty()) {
					throw new InvalidInputException("Empty criterion header found.");
				}
				final Criterion criterion = new Criterion(id);
				data.getCriteria().add(criterion);
			}

			while (csvReader.readRecord()) {
				final String altId = csvReader.get(0);
				if (altId.isEmpty()) {
					throw new InvalidInputException("Empty alternative header found.");
				}
				final Alternative alternative = new Alternative(altId);
				data.getAlternatives().add(alternative);
				final String[] values = csvReader.getValues();
				for (int i = 1; i < values.length; ++i) {
					final String perf = values[i];
					final double perfValue;
					if (perf.trim().isEmpty()) {
						continue;
					}
					try {
						perfValue = Double.valueOf(perf).doubleValue();
					} catch (NumberFormatException exc) {
						throw new InvalidInputException("Invalid number read: " + perf + ".", exc);
					}
					final String criterionId = csvReader.getHeader(i);
					final Criterion criterion = new Criterion(criterionId);
					evaluations.put(alternative, criterion, perfValue);
				}
			}
			reader.close();

			assert (data.getCriteria().containsAll(evaluations.getColumns()));
			assert (data.getAlternatives().containsAll(evaluations.getRows()));
			assert (data.getCriteria().size() == csvReader.getHeaders().length - 1) : "Nb criteria:"
					+ data.getCriteria().size() + ", nb columns: " + csvReader.getHeaders().length + ".";

			data.setEvaluations(evaluations);
			return data;
		} finally {
			reader.close();
		}
	}

	public void setSource(CharSource source) {
		m_source = source;
	}
}
