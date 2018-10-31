package org.decisiondeck.jmcda.persist.latex;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.utils.StreamUtils;
import org.decisiondeck.jmcda.persist.utils.ExportSettings;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IOrderedAssignmentsWithCredibilitiesRead;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.CharSink;

public class LaTeXExporter extends ExportSettings {
	private static final String MARK_KO = "\\mkkNO";
	private static final String MARK_OK = "\\mkkOK";
	private CharSink m_destination;
	/**
	 * <code>null</code> for no limit: always use a short table. If negative,
	 * will always use a long table.
	 */
	private Integer m_shortTableLimit;
	private Writer m_writer;

	/**
	 * @param destination
	 *            not <code>null</code>.
	 */
	public LaTeXExporter(CharSink destination) {
		Preconditions.checkNotNull(destination);
		m_writer = null;
		m_destination = destination;
		m_shortTableLimit = Integer.valueOf(30);
	}

	/**
	 *
	 * TODO seems like this object closes the received writer in some cases.
	 *
	 * @param writer
	 *            not <code>null</code>.
	 */
	public LaTeXExporter(Writer writer) {
		Preconditions.checkNotNull(writer);
		m_writer = StreamUtils.getBuffered(writer);
		m_destination = null;
		m_shortTableLimit = Integer.valueOf(30);
	}

	public void exportAssignmentsToMultiple(IOrderedAssignmentsToMultipleRead assignments) throws IOException {
		final Set<Category> categoriesAll = assignments.getCategories();
		int nbRecords = assignments.getAlternatives().size();
		final Iterable<String> headers = Iterables.transform(categoriesAll, getCategoriesToString());

		final Writer writer = getEffectiveWriter();

		try {
			final String tableType = writeTableHeaders(nbRecords, headers, writer);

			for (Alternative alternative : interOrderAlternatives(assignments.getAlternatives())) {
				final NavigableSet<Category> categories = assignments.getCategories(alternative);
				writer.write(getAlternativeString(alternative));
				for (Category category : categoriesAll) {
					writer.write("\t&");
					final String mark;
					if (categories.contains(category)) {
						mark = MARK_OK;
					} else {
						mark = MARK_KO;
					}
					writer.write(mark);
				}
				writer.write("\\\\");
				writer.write(System.lineSeparator());
			}

			writer.write("\\end{" + tableType + "}");
			writer.write(System.lineSeparator());
			endWriter(writer);
		} finally {
			closeDestination(writer);
		}
	}

	public void exportAssignmentsWithCredibilities(IOrderedAssignmentsWithCredibilitiesRead assignments)
			throws IOException {
		final Set<Category> categories = assignments.getCategories();
		int nbRecords = assignments.getAlternatives().size();
		final Iterable<String> headers = Iterables.transform(categories, getCategoriesToString());

		final Writer writer = getEffectiveWriter();

		try {
			final String tableType = writeTableHeaders(nbRecords, headers, writer);

			for (Alternative alternative : interOrderAlternatives(assignments.getAlternatives())) {
				final Map<Category, Double> credibilities = assignments.getCredibilities(alternative);
				if (credibilities.isEmpty()) {
					continue;
				}
				writer.write(getAlternativeString(alternative));
				for (Category category : categories) {
					writer.write("\t&");
					final Double credibility = credibilities.get(category);
					final double credibilityValue;
					if (credibility != null) {
						credibilityValue = credibility.doubleValue();
					} else {
						credibilityValue = 0;
					}
					writer.write(getNumberString(credibilityValue));
				}
				writer.write("\\\\");
				writer.write(System.lineSeparator());
			}

			writer.write("\\end{" + tableType + "}");
			writer.write(System.lineSeparator());
			endWriter(writer);
		} finally {
			closeDestination(writer);
		}
	}

	public void exportEvaluations(EvaluationsRead evaluations) throws IOException {
		Set<Criterion> criteria = interOrderCriteria(evaluations.getColumns());
		Set<Alternative> alternatives = interOrderAlternatives(evaluations.getRows());
		boolean longTable = useLongTable(alternatives.size());
		final String tableType = longTable ? "longtable" : "tabular";
		final Writer writer = getEffectiveWriter();
		try {
			writer.write("\\begin{" + tableType + "}{l");
			writer.write(Strings.repeat("r", criteria.size()));
			// m_writer.write(Strings.repeat("l", m_attributes.size()));
			writer.write("}");
			writer.write(System.lineSeparator());
			// m_writer.write("alt");
			for (Criterion criterion : criteria) {
				writer.write("\t&" + getCriterionString(criterion));
			}
			// for (Criterion attribute : m_attributes.keySet()) {
			// m_writer.write("\t&" + attribute.getId());
			// }
			writer.write("\\\\");
			writer.write(System.lineSeparator());
			writer.write("\\hline");
			writer.write(System.lineSeparator());
			if (longTable) {
				writer.write("\\endhead");
				writer.write(System.lineSeparator());
			}
			for (Alternative alternative : alternatives) {
				writer.write(getAlternativeString(alternative));
				for (Criterion criterion : criteria) {
					final Double entry = evaluations.getEntry(alternative, criterion);
					writer.write("\t&");
					if (entry != null) {
						writer.write(getNumberString(entry.doubleValue()));
					}
				}
				// for (Criterion attribute : m_attributes.keySet()) {
				// final String value =
				// m_attributes.get(attribute).get(alternative);
				// m_writer.write("\t&" + value);
				// }
				writer.write("\\\\");
				writer.write(System.lineSeparator());
			}

			writer.write("\\end{" + tableType + "}");
			writer.write(System.lineSeparator());
			endWriter(writer);
		} finally {
			closeDestination(writer);
		}
	}

	/**
	 * @return <code>null</code> for no limit: always use a short table. If
	 *         negative, will always use a long table.
	 */
	public Integer getShortTableLimit() {
		return m_shortTableLimit;
	}

	/**
	 * Retrieves the writer this object uses. Writing to this writer is
	 * permitted and may be used to avoid interleaving calls to this object and
	 * external writes with flushes. Note that the returned writer is not
	 * necessarily the writer given by the user, e.g. because of possible
	 * buffering in between.
	 *
	 * @return not <code>null</code>.
	 */
	public Writer getWriter() {
		return m_writer;
	}

	public void setDestination(CharSink destination) {
		if (destination == null) {
			throw new NullPointerException();
		}
		m_destination = destination;
	}

	/**
	 * Sets the limit in number of rows after which this object will use a long
	 * table (if the number of rows is greater than the provided limit) instead
	 * of a short one.
	 *
	 * @param shortTableLimit
	 *            <code>null</code> for no limit: always use a short table. If
	 *            negative, will always use a long table.
	 */
	public void setShortTableLimit(Integer shortTableLimit) {
		m_shortTableLimit = shortTableLimit;
	}

	public void write(List<List<String>> rows) throws IOException {
		if (rows.isEmpty()) {
			return;
		}
		final int rowSize;
		{
			final List<String> first = rows.iterator().next();
			rowSize = first.size();
		}
		checkArgument(rowSize >= 1);

		boolean longTable = useLongTable(rows.size());
		final String tableType = longTable ? "longtable" : "tabular";
		final Writer writer = getEffectiveWriter();
		try {
			writer.write("\\begin{" + tableType + "}{");
			writer.write(Strings.repeat("l", rowSize));
			writer.write("}");
			writer.write(System.lineSeparator());
			for (List<String> row : rows) {
				final Iterator<String> rowIterator = row.iterator();
				checkArgument(row.size() == rowSize);
				{
					final String firstElement = rowIterator.next();
					if (firstElement != null) {
						writer.write(firstElement);
					}
				}
				while (rowIterator.hasNext()) {
					writer.write("\t&");
					final String element = rowIterator.next();
					if (element != null) {
						writer.write(element);
					}
				}
				writer.write("\\\\");
				writer.write(System.lineSeparator());
			}
			writer.write("\\end{" + tableType + "}");
			writer.write(System.lineSeparator());
			endWriter(writer);
		} finally {
			closeDestination(writer);
		}
	}

	public void writeLn(String string) throws IOException {
		checkState(m_writer != null);
		m_writer.write(string);
		m_writer.write(System.lineSeparator());
	}

	private void closeDestination(Writer writer) throws IOException {
		checkState(m_destination != null);
		writer.close();
	}

	private void endWriter(Writer writer) throws IOException {
		if (m_destination == null) {
			writer.flush();
		} else {
			closeDestination(writer);
		}
	}

	private Writer getEffectiveWriter() throws IOException {
		checkState(m_writer != null || m_destination != null);
		if (m_writer != null) {
			return m_writer;
		}
		return m_destination.openBufferedStream();
	}

	private boolean useLongTable(int nbRecords) {
		if (m_shortTableLimit == null) {
			return false;
		}
		return nbRecords > m_shortTableLimit.intValue();
	}

	private String writeTableHeaders(int nbRecords, final Iterable<String> headers, Writer writer) throws IOException {
		boolean longTable = useLongTable(nbRecords);
		final String tableType = longTable ? "longtable" : "tabular";

		final Set<String> headersSet = Sets.newLinkedHashSet(headers);
		writer.write("\\begin{" + tableType + "}{");
		writer.write("l");
		writer.write(Strings.repeat("r", headersSet.size()));
		writer.write("}");
		writer.write(System.lineSeparator());
		// writer.write("alt");
		for (String string : headersSet) {
			writer.write("\t&" + string);
		}
		writer.write("\\\\");
		writer.write(System.lineSeparator());
		writer.write("\\hline");
		writer.write(System.lineSeparator());
		if (longTable) {
			writer.write("\\endhead");
			writer.write(System.lineSeparator());
		}
		return tableType;
	}

}
