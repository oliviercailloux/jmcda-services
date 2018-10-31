package org.decisiondeck.jmcda.persist.text;

import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.utils.ExportSettings;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultipleRead;

import com.csvreader.CsvWriter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * No need to close this object after use: the user has initialized the stream so let him close it after use himself.
 * Just don't forget to {@link #flush()} after use (heh).
 * 
 * @author Olivier Cailloux
 * 
 */
public class CsvExporter extends ExportSettings implements Flushable {
    private final CsvWriter m_writer;
    private String m_prefix;

    /**
     * @param writer
     *            not <code>null</code>.
     */
    public CsvExporter(CsvWriter writer) {
	Preconditions.checkNotNull(writer);
	m_writer = writer;
	m_prefix = null;
    }

    @Override
    public void flush() throws IOException {
	m_writer.flush();
    }

    /**
     * Writes the data to the given writer, using a comma as the column delimiter.
     * 
     * @param writer
     *            The target to write the column delimited data to. Not <code>null</code>.
     */
    public CsvExporter(Writer writer) {
	Preconditions.checkNotNull(writer);
	m_writer = new CsvWriter(writer, ',');
	m_prefix = null;
    }

    public void setDelimiter(final char delimiter) {
	m_writer.setDelimiter(delimiter);
    }

    public void exportAssignmentsToMultiple(IAssignmentsToMultipleRead assignments, boolean exportCategories)
	    throws IOException {
	final Set<Category> allCategories = assignments.getCategories();
	final LinkedHashMap<Category, Integer> categoryToPosition = Maps.newLinkedHashMap();
	int position = 0;
	for (Category category : allCategories) {
	    categoryToPosition.put(category, Integer.valueOf(position));
	    ++position;
	}
	if (exportCategories) {
	    m_writer.write("");
	    exportCategories(allCategories);
	}
	for (Alternative alternative : interOrderAlternatives(assignments.getAlternatives())) {
	    writePrefix();
	    m_writer.write(getAlternativesToString().apply(alternative));
	    final Set<Category> assignedCategories = assignments.getCategories(alternative);
	    for (Category category : allCategories) {
		if (assignedCategories.contains(category)) {
		    m_writer.write("x");
		} else {
		    m_writer.write("");
		}
	    }
	    m_writer.endRecord();
	}
    }

    static public void exportCrispAssignments(Writer writer, IAssignmentsRead assignments) throws IOException {
	final CsvExporter exporter = new CsvExporter(writer);
	exporter.exportCrispAssignments(assignments);
	exporter.flush();
    }

    public void endRecord() throws IOException {
	m_writer.endRecord();
    }

    static public void exportEvaluations(Writer writer, EvaluationsRead evaluations) throws InvalidInputException,
	    IOException {
	final CsvExporter exporter = new CsvExporter(writer);
	exporter.exportEvaluations(evaluations);
	exporter.flush();
    }

    public void exportEvaluations(EvaluationsRead evaluations) throws InvalidInputException, IOException {
	boolean exportAlternatives = false;
	boolean exportCriteria = false;
	exportEvaluations(evaluations, exportAlternatives, exportCriteria);
    }

    /**
     * Exports a set of records where each record describes one alternative evaluations. If both boolean arguments are
     * <code>false</code>, one record is, for each criterion, the corresponding evaluation value, and the number of
     * records is the number of alternatives.</p>
     * <p>
     * If exportAlternatives is <code>true</code>, each record is prefixed with the alternative id. If exportCriteria is
     * <code>true</code>, the first record is the ids of the criteria, then comes the record containing the evaluations.
     * </p>
     * 
     * @param evaluations
     *            not <code>null</code>.
     * @param exportAlternatives
     *            <code>true</code> to include the alternatives id.
     * @param exportCriteria
     *            <code>true</code> to include the criteria id.
     * @throws IOException
     *             if an error occurs while writing data to the destination stream.
     * @throws InvalidInputException
     *             iff the given set of evaluations is not complete.
     */
    public void exportEvaluations(EvaluationsRead evaluations, boolean exportAlternatives, boolean exportCriteria)
	    throws IOException, InvalidInputException {
	if (exportCriteria) {
	    if (exportAlternatives) {
		m_writer.write("");
	    }
	    exportCriteria(evaluations.getColumns());
	}
	for (Alternative alternative : interOrderAlternatives(evaluations.getRows())) {
	    writePrefix();
	    if (exportAlternatives) {
		m_writer.write(getAlternativesToString().apply(alternative));
	    }
	    for (Criterion criterion : interOrderCriteria(evaluations.getColumns())) {
		final Double entry = evaluations.getEntry(alternative, criterion);
		if (entry == null) {
		    throw new InvalidInputException("Missing evaluation at " + alternative + ", " + criterion + ".");
		}
		final double value = entry.doubleValue();
		m_writer.write(getNumberFormatter().format(value));
	    }
	    m_writer.endRecord();
	}
    }

    public void exportAlternatives(Set<Alternative> alternatives) throws IOException {
	writePrefix();
	for (Alternative alternative : interOrderAlternatives(alternatives)) {
	    m_writer.write(getAlternativesToString().apply(alternative));
	}
	m_writer.endRecord();
    }

    private void writePrefix() throws IOException {
	if (m_prefix != null) {
	    m_writer.write(m_prefix);
	}
    }

    public void exportCriteria(Set<Criterion> criteria) throws IOException {
	writePrefix();
	for (Criterion criterion : interOrderCriteria(criteria)) {
	    m_writer.write(getCriterionString(criterion));
	}
	m_writer.endRecord();
    }

    public void exportCategories(Set<Category> categories) throws IOException {
	writePrefix();
	for (Category category : categories) {
	    m_writer.write(getCategoryString(category));
	}
	m_writer.endRecord();
    }

    public void exportDms(Set<DecisionMaker> dms) throws IOException {
	writePrefix();
	for (DecisionMaker dm : interOrderDms(dms)) {
	    m_writer.write(getDmString(dm));
	}
	m_writer.endRecord();
    }

    public void exportValues(Map<Criterion, Double> values) throws IOException {
	writePrefix();
	writeValues(values);
	m_writer.endRecord();
    }

    private void writeValues(Map<Criterion, Double> values) throws IOException {
	for (Criterion criterion : interOrderCriteria(values.keySet())) {
	    m_writer.write(getNumberFormatter().format(values.get(criterion).doubleValue()));
	}
    }

    public void exportCoalitions(Coalitions coalitions) throws IOException {
	writePrefix();
	writeValues(coalitions.getWeights());
	if (coalitions.containsMajorityThreshold()) {
	    m_writer.write(getNumberString(coalitions.getMajorityThreshold()));
	}
	m_writer.endRecord();
    }

    /**
     * Sets a prefix that is exported as first entry before any line.
     * 
     * @param prefix
     *            <code>null</code> for no prefix, empty string for an empty prefix, which has the effect of skipping
     *            one column before exporting each line.
     */
    public void setPrefix(String prefix) {
	m_prefix = prefix;
    }

    public String getPrefix() {
	return m_prefix;
    }

    /**
     * Writes another column of data to this record. Does not preserve leading and trailing whitespace in this column of
     * data.
     * 
     * @param content
     *            The data for the new column.
     * @exception IOException
     *                Thrown if an error occurs while writing data to the destination stream.
     */
    public void write(String content) throws IOException {
	m_writer.write(content);
    }

    public void exportCrispAssignments(IAssignmentsRead assignments) throws IOException {
	for (Alternative alternative : interOrderAlternatives(assignments.getAlternatives())) {
	    writePrefix();
	    m_writer.write(getAlternativesToString().apply(alternative));
	    final Category category = assignments.getCategory(alternative);
	    m_writer.write(getCategoriesToString().apply(category));
	    m_writer.endRecord();
	}
    }

}
