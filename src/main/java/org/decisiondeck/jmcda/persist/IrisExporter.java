package org.decisiondeck.jmcda.persist;

import java.io.BufferedWriter;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.NavigableSet;

import org.decision_deck.jmcda.services.ConsistencyChecker;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.weights.Weights;
import org.decision_deck.utils.StringUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.utils.ExportSettings;
import org.decisiondeck.jmcda.persist.utils.ExportUtils;
import org.decisiondeck.jmcda.structure.sorting.problem.results.ISortingResultsToMultiple;

import com.google.common.base.Preconditions;
import com.google.common.io.CharSink;
import com.google.common.io.OutputSupplier;

/**
 * <p>
 * Exports a (possibly partial) preference model and assignment examples to
 * multiple categories to a file suitable for import into the IRIS software.
 * </p>
 * <p>
 * Most knowledge of the file format used in this method comes from the IRIS
 * manual, 1.0, Jan 2002, INESC Coimbra. A few informations have been discovered
 * by trial and error (e.g. the fact that at least two categories must be
 * included). Note that the IRIS software exports document in a so-called
 * "version 2" including more information than what is exported by this class,
 * e.g. the veto thresholds, but we have found no documentation about that
 * format. It is currently not used by this exporter.
 * </p>
 * <p>
 * It is suggested to use the ASCII character set as for the provided writer.
 * This class, when using the default export settings, does not attempt to write
 * anything outside of that character set. Fiddling with the export settings may
 * invalidate this.
 * </p>
 * <p>
 * When choosing a name for a destination file, consider using the .tri
 * extension which is typical.
 * </p>
 * <p>
 * Input data used and ignored are the following.
 * <ul>
 * <li>All criteria must have a preference direction;</li>
 * <li>the alternatives and profiles evaluations must be complete;</li>
 * <li>the profiles must be completely ordered (in the categories and profiles
 * object) and ordered in a way compatible with the dominance relation of their
 * evaluations;</li>
 * <li>no criteria may have a veto threshold: this is not exported, this
 * constraint permits to ensure that the user of this object is aware of this;
 * </li>
 * <li>all alternatives must be assigned (this is not enforced if this object is
 * asked to be nice);</li>
 * <li>all given weights must be between zero and one (this is not enforced if
 * this object is asked to be nice), note however that weights must not
 * necessarily be all given (or even given at all), which explains why this
 * object may not proceed to systematic weights normalization;</li>
 * <li>all alternatives must be assigned to categories that constitute an
 * interval of categories, thus the set of categories may not contain “holes”
 * (this is not enforced if this object is asked to be nice);</li>
 * <li>the set of alternatives, criteria, or profiles may not be empty (this is
 * not enforced if this object is asked to be nice), note that this implies that
 * at least two categories have to be used;</li>
 * <li>all criteria must have a preference and indifference threshold (this is
 * not enforced if this object is asked to be nice), which may be zero.</li>
 * </ul>
 * </p>
 * <p>
 * The alternatives and criteria ids are not used because the IRIS file format
 * expects consecutive integer ids, not string ids. This object will convert the
 * alternatives and criteria to ids by ordering them using their natural
 * ordering and numbering them from zero. To use compatible string ids, it is
 * suggested to name your alternatives Alt000, Alt001, etc., so that the natural
 * ordering will reflect the integer ids attribution.
 * </p>
 * <p>
 * If a writer is bound to this object, this object does not close it: it is
 * considered to be the responsibility of the user who allocated the writer. If
 * however an {@link OutputSupplier} is bound to this object, this object closes
 * any writer it allocates. The second usage strategy is recommended as it
 * provides guaranteed closing.
 * </p>
 *
 * @author Olivier Cailloux
 *
 */
public class IrisExporter implements Flushable {

	private boolean m_nice;
	private ExportSettings m_settings;
	private ISortingResultsToMultiple m_source;
	private Writer m_writer;
	private final CharSink m_writerSupplier;

	/**
	 * @param writerSupplier
	 *            not <code>null</code>.
	 */
	public IrisExporter(CharSink writerSupplier) {
		Preconditions.checkNotNull(writerSupplier);
		m_writer = null;
		m_writerSupplier = writerSupplier;
		m_nice = false;
		m_settings = null;
	}

	/**
	 * @param writer
	 *            not <code>null</code>.
	 */
	public IrisExporter(Writer writer) {
		Preconditions.checkNotNull(writer);
		if (writer instanceof BufferedWriter) {
			m_writer = writer;
		} else {
			m_writer = new BufferedWriter(writer);
		}
		m_writerSupplier = null;
		m_nice = false;
		m_settings = null;
	}

	/**
	 * Writes the data found in the given source as an IRIS stream to the
	 * underlying writer. The writer is flushed when this method returns. If the
	 * bound writer is an {@link OutputSupplier}, the obtained writer is closed
	 * when this method returns.
	 *
	 * @param source
	 *            not <code>null</code>.
	 * @throws InvalidInputException
	 *             if one of the following holds:
	 *             <ul>
	 *             <li>some criteria miss a preference direction;</li>
	 *             <li>the alternatives of profiles evaluations are incomplete;
	 *             </li>
	 *             <li>the profiles are not completely ordered, or ordered in a
	 *             way incompatible with the dominance relation on their
	 *             evaluations;</li>
	 *             <li>some criteria have a veto threshold;</li>
	 *             <li>some alternatives are not assigned (this is not enforced
	 *             if this object is asked to be nice);</li>
	 *             <li>some weights are not between zero and one (this is not
	 *             enforced if this object is asked to be nice);</li>
	 *             <li>some alternatives are assigned to categories that do not
	 *             form an interval of categories, thus the set of categories
	 *             contain some “holes” (this is not enforced if this object is
	 *             asked to be nice);</li>
	 *             <li>the set of alternatives, criteria, or profiles, is empty
	 *             (this is not enforced if this object is asked to be nice);
	 *             </li>
	 *             <li>some criteria have missing preference or indifference
	 *             threshold (this is not enforced if this object is asked to be
	 *             nice).</li>
	 *             </ul>
	 * @throws IOException
	 *             if an exception occurs while writing to the underlying
	 *             writer.
	 */
	public void export(ISortingResultsToMultiple source) throws InvalidInputException, IOException {
		/**
		 * To avoid creating an empty file in case something is wrong, we check
		 * first.
		 */
		assertCorrect(source);

		if (m_writerSupplier == null) {
			exportInternal(source);
		} else {
			try (Writer writer = m_writerSupplier.openBufferedStream()) {
				m_writer = writer;
				exportInternal(source);
			}
		}
	}

	@Override
	public void flush() throws IOException {
		m_writer.flush();
	}

	public boolean isNice() {
		return m_nice;
	}

	/**
	 * When this object is set to be nice, this method will try to export the
	 * file even if some data is missing, thereby rendering a file which is not
	 * valid for import but which can be examined by a user to see what data is
	 * missing. If <code>false</code> (the default), this object will throw an
	 * exception if a required data is missing. Note that when this method is
	 * nice, it can produce a file which make IRIS loop forever (e.g. try to
	 * produce a file with no profiles). And be warned that IRIS sometimes
	 * displays misleading error messages when the input file is incorrect.
	 *
	 * @param nice
	 *            <code>true</code> to kindly ask this object to be nice.
	 */
	public void setNice(boolean nice) {
		m_nice = nice;
	}

	private void assertCorrect(ISortingResultsToMultiple source) throws InvalidInputException {
		Preconditions.checkNotNull(source);

		final ConsistencyChecker check = new ConsistencyChecker();
		check.assertCompleteAlternativesEvaluations(source);
		check.assertCompleteProfilesEvaluations(source);
		check.assertCompletePreferenceDirections(source);
		check.assertCompleteProfiles(source);
		check.assertDominance(source);

		if (source.getThresholds().getVetoThresholds().size() > 0) {
			throw new InvalidInputException("Some veto thresholds are set, shouldn't be.");
		}

		if (!m_nice) {
			if (source.getAlternatives().isEmpty()) {
				throw new InvalidInputException("Need at least one alternative.");
			}
			if (source.getCriteria().isEmpty()) {
				throw new InvalidInputException("Need at least one criterion.");
			}
			if (source.getProfiles().isEmpty()) {
				throw new InvalidInputException("Need at least one profile.");
			}
			check.assertCompletePreferenceThresholds(source);
			check.assertCompleteIndifferenceThresholds(source);
			check.assertCompleteAssignments(source);
			check.assertToIntervals(source.getAssignments(), source.getCatsAndProfs().getCategories());
			check.assertWeightsBelowOne(source);
		}
	}

	private void exportInternal(ISortingResultsToMultiple source) throws IOException {
		Preconditions.checkNotNull(source);
		m_source = source;

		m_settings = ExportUtils.newExportByIndexSettings(m_source, 1);
		m_settings.getNumberFormatter().setMinimumFractionDigits(0);
		m_settings.getNumberFormatter().setMaximumFractionDigits(5);

		writeSize();
		writeDirections();
		writeProfiles();
		writeThresholds();
		writeAlternatives();
		writeWeightsBounds();

		m_writer.write("\r\nc Number of constraints on weights.\r\n");
		m_writer.write("K N\t" + 0 + "\r\n");

		m_writer.write("\r\nc Cutting levels.\r\n");
		final double minLambda;
		final double maxLambda;
		if (m_source.getCoalitions().containsMajorityThreshold()) {
			final double lambda = m_source.getCoalitions().getMajorityThreshold();
			minLambda = lambda;
			maxLambda = lambda;
		} else {
			minLambda = 0.5d;
			maxLambda = 1d;
		}
		m_writer.write("L m\t" + m_settings.getNumberString(minLambda) + "\r\n");
		m_writer.write("L M\t" + m_settings.getNumberString(maxLambda) + "\r\n");

		flush();
	}

	private String getAlternativeString(Alternative alternative) {
		final String id = m_settings.getAlternativeString(alternative);
		if (!StringUtils.isInt(id) && !m_nice) {
			throw new IllegalStateException("Expected integer id for " + alternative + ".");
		}
		return id;
	}

	private String getCategoryString(Category category) {
		final String id = m_settings.getCategoryString(category);
		if (!StringUtils.isInt(id) && !m_nice) {
			throw new IllegalStateException("Expected integer id for " + category + ".");
		}
		return id;
	}

	private String getCriterionString(Criterion criterion) {
		final String id = m_settings.getCriterionString(criterion);
		if (!StringUtils.isInt(id) && !m_nice) {
			throw new IllegalStateException("Expected integer id for " + criterion + ".");
		}
		return id;
	}

	private String getProfileString(Alternative profile) {
		final String id = m_settings.getProfileString(profile);
		if (!StringUtils.isInt(id) && !m_nice) {
			throw new IllegalStateException("Expected integer id for " + profile + ".");
		}
		return id;
	}

	private void writeAlternatives() throws IOException {
		m_writer.write("\r\nc Actions.\r\n");
		m_writer.write("c\t" + "id");
		for (final Criterion crit : m_settings.interOrderCriteria(m_source.getCriteria())) {
			m_writer.write("\t" + "g" + getCriterionString(crit));
		}
		m_writer.write("\t" + "LowC" + "\t" + "HighC" + "\r\n");

		for (final Alternative alt : m_settings.interOrderAlternatives(m_source.getAlternatives())) {
			m_writer.write("a\t" + getAlternativeString(alt));
			for (final Criterion crit : m_settings.interOrderCriteria(m_source.getCriteria())) {
				final double evaluation = m_source.getAlternativesEvaluations().getEntry(alt, crit).doubleValue();
				m_writer.write("\t" + m_settings.getNumberString(evaluation));
			}

			final String lowestCatInt;
			final String highestCatInt;
			final NavigableSet<Category> categories = m_source.getAssignments().getCategories(alt);
			if (categories == null) {
				if (!m_nice) {
					throw new IllegalStateException("Missing category information for alternative " + alt.getId()
							+ " (should not happen here).");
				}
				lowestCatInt = getCategoryString(m_source.getCatsAndProfs().getCategories().first());
				highestCatInt = getCategoryString(m_source.getCatsAndProfs().getCategories().last());
			} else {
				lowestCatInt = getCategoryString(categories.first());
				highestCatInt = getCategoryString(categories.last());
			}
			m_writer.write("\t" + lowestCatInt + "\t" + highestCatInt + "\r\n");
		}
	}

	private void writeDirections() throws IOException {
		m_writer.write("\r\nc Direction.\r\n");
		m_writer.write("d");

		final Map<Criterion, Interval> scales = m_source.getScales();

		for (final Criterion criterion : m_settings.interOrderCriteria(scales.keySet())) {
			m_writer.write("\t");
			final int pDirInt = scales.get(criterion).getDirectionAsSign();
			final String pDir = Integer.toString(pDirInt);
			m_writer.write(pDir);
		}
		m_writer.write("\r\n");
	}

	private void writeProfiles() throws IOException {
		m_writer.write("\r\nc Profiles (worst first).\r\n");
		for (final Alternative profile : m_settings.interOrderProfiles(m_source.getProfiles())) {
			m_writer.write("p\t" + m_settings.getProfileString(profile));
			for (final Criterion crit : m_settings.interOrderCriteria(m_source.getCriteria())) {
				final double evaluation = m_source.getProfilesEvaluations().getEntry(profile, crit).doubleValue();
				m_writer.write("\t" + m_settings.getNumberString(evaluation));
			}
			m_writer.write("\r\n");
		}
	}

	private void writeSize() throws IOException {
		m_writer.write("c Size. (Criteria, Categories, Actions.)\r\n");

		m_writer.write(
				"t\t" + m_source.getCriteria().size() + "\t" + (m_source.getCatsAndProfs().getCategories().size())
						+ "\t" + m_source.getAlternatives().size() + "\r\n");
	}

	private void writeThresholds() throws IOException {
		m_writer.write("\r\nc Thresholds.\r\n");
		// final boolean useVetoes = m_vetoes.size() > 0;
		// final String vetoCol = useVetoes ? "\tunknown\tvetoT" : "";
		// m_writer.write("c\tprofId\tcritId\tindiffT\tprefT" + vetoCol +
		// "\r\n");
		m_writer.write("c\tprofId\tcritId\tindiffT\tprefT\r\n");
		for (Alternative profile : m_settings.interOrderProfiles(m_source.getProfiles())) {
			for (Criterion crit : m_settings.interOrderCriteria(m_source.getCriteria())) {
				m_writer.write("s\t" + getProfileString(profile));
				// final VetoThreshold vThresh = m_vetoes.get(crit);
				final String pThreshStr;
				final String iThreshStr;
				// final String vThreshStr;
				if (!m_source.getThresholds().containsPreferenceThreshold(crit)) {
					if (!m_nice) {
						throw new IllegalStateException(
								"Preference threshold not set for criterion " + crit.getId() + " (and is required).");
					}
					pThreshStr = "Unset";
				} else {
					pThreshStr = String.valueOf(m_source.getThresholds().getPreferenceThreshold(crit));

				}
				if (!m_source.getThresholds().containsIndifferenceThreshold(crit)) {
					if (!m_nice) {
						throw new IllegalStateException(
								"Indifference threshold not set for criterion " + crit.getId() + " (and is required).");
					}
					iThreshStr = "Unset";
				} else {
					iThreshStr = String.valueOf(m_source.getThresholds().getIndifferenceThreshold(crit));
				}
				// if (useVetoes) {
				// if (vThresh == null) {
				// if (!m_beNice) {
				// throw new IllegalStateException("Veto threshold not set for
				// criterion " + crit.getId()
				// + " (and is required).");
				// }
				// vThreshStr = "Unset";
				// } else {
				// vThreshStr = String.valueOf(vThresh.getValue());
				// }
				// } else {
				// vThreshStr = "";
				// }
				// String vThreshAndTab = useVetoes ? "\t0\t" + vThreshStr : "";
				// m_writer.write("\t" + critId++ + "\t" + iThreshStr + "\t" +
				// pThreshStr + vThreshAndTab + "\r\n");
				m_writer.write("\t" + getCriterionString(crit) + "\t" + iThreshStr + "\t" + pThreshStr + "\r\n");
			}
		}
	}

	private void writeWeightsBounds() throws IOException {
		m_writer.write("\r\nc Bounds on weights. (NB: sum of weights is one.)\r\n");
		final Weights weights = m_source.getCoalitions().getWeights();
		for (final Criterion crit : m_settings.interOrderCriteria(m_source.getCriteria())) {
			final double upperBound;
			final Double weight = weights.get(crit);
			if (weight == null) {
				upperBound = 1d;
			} else {
				upperBound = weight.doubleValue();
				if (upperBound > 1d && !m_nice) {
					throw new IllegalStateException("Shouldn't happen.");
				}
			}
			m_writer.write("K S\t" + getCriterionString(crit) + "\t" + m_settings.getNumberString(upperBound) + "\r\n");
		}
		for (final Criterion crit : m_settings.interOrderCriteria(m_source.getCriteria())) {
			final double lowerBound;
			final Double weight = weights.get(crit);
			if (weight == null) {
				lowerBound = 0;
			} else {
				lowerBound = weight.doubleValue();
				if (lowerBound > 1d && !m_nice) {
					throw new IllegalStateException("Shouldn't happen.");
				}
			}
			m_writer.write("K I\t" + getCriterionString(crit) + "\t" + m_settings.getNumberString(lowerBound) + "\r\n");
		}
	}

}
