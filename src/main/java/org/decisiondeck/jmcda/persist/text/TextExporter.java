package org.decisiondeck.jmcda.persist.text;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedWriter;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decisiondeck.jmcda.persist.utils.ExportSettings;
import org.decisiondeck.jmcda.persist.utils.ExportUtils;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsRead;

import com.google.common.base.Preconditions;

public class TextExporter extends ExportSettings implements Flushable {

    private final BufferedWriter m_writer;
    private char m_delimiter;

    public TextExporter(Writer writer) {
	Preconditions.checkNotNull(writer);
	if (writer instanceof BufferedWriter) {
	    m_writer = (BufferedWriter) writer;
	} else {
	    m_writer = new BufferedWriter(writer);
	}
    }

    static public void exportAllCrispAssignments(Writer writer,
	    Map<DecisionMaker, ? extends IAssignmentsRead> allAssignments)
	    throws IOException {
	final TextExporter textExporter = new TextExporter(writer);
	textExporter.exportAllCrispAssignments(allAssignments);
	textExporter.flush();
    }

    @Override
    public void flush() throws IOException {
	m_writer.flush();
    }

    public void exportAllCrispAssignments(Map<DecisionMaker, ? extends IAssignmentsRead> allAssignments)
	    throws IOException {
	final CsvExporter exporter = new CsvExporter(m_writer);
	ExportUtils.copySettings(this, exporter);
	exporter.setDelimiter(m_delimiter);

	for (DecisionMaker dm : interOrderDms(allAssignments.keySet())) {
	    exporter.write(getDmsToString().apply(dm));
	    exporter.endRecord();
	    final IAssignmentsRead assignments = allAssignments.get(dm);
	    exporter.exportCrispAssignments(assignments);
	    exporter.write("-1");
	    exporter.endRecord();
	}
	exporter.flush();
    }

    public void writeLn(String string) throws IOException {
	m_writer.write(string);
	m_writer.newLine();
    }

    public void setDelimiter(final char delimiter) {
	m_delimiter = delimiter;
    }

    public void exportGroupCoalitions(Map<DecisionMaker, Coalitions> groupCoalitions) throws IOException {
	checkNotNull(groupCoalitions);
	final CsvExporter exporter = new CsvExporter(m_writer);
	for (DecisionMaker dm : groupCoalitions.keySet()) {
	    exporter.write(dm.getId());
	    exporter.endRecord();
	    exporter.setPrefix("");
	    exporter.exportCoalitions(groupCoalitions.get(dm));
	    exporter.setPrefix(null);
	}
	exporter.flush();
    }

}
