package org.decision_deck.services;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.scores.AlternativesScores;
import org.decision_deck.utils.matrix.Matrixes;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decision_deck.utils.matrix.SparseMatrixFuzzyRead;
import org.decision_deck.utils.relation.graph.Preorder;
import org.decision_deck.utils.relation.graph.mess.GraphUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.xmcda_oo.services.flow.Flow;

/**
 * Simple distillation on the basis of the net flow computed a la Promethee. This is a simplification of the
 * distillation procedure used by Electre III, and is not equivalent to the Promethee ranking.
 * 
 * @author Olivier Cailloux
 * 
 */
public class SimpleDistillation {
    private final SparseMatrixFuzzyRead<Alternative, Alternative> m_source;

    public SimpleDistillation(SparseMatrixFuzzyRead<Alternative, Alternative> source) {
	m_source = source;
    }

    public Preorder<Alternative> getAscending() {
	final SparseMatrixFuzzy<Alternative, Alternative> source = Matrixes.newSparseFuzzy(m_source);
	assert (source.getRows().equals(source.getColumns()));
	final Preorder<Alternative> result = new Preorder<Alternative>();

	while (!source.isEmpty()) {
	    final AlternativesScores netFlowsOld;
	    try {
		netFlowsOld = new Flow().getNetFlows(source);
	    } catch (InvalidInputException exc) {
		throw new IllegalStateException(exc);
	    }
	    final AlternativesScores netFlows = new AlternativesScores(netFlowsOld);

	    final Set<Entry<Alternative, Double>> scores = netFlows.entrySet();
	    final Iterator<Entry<Alternative, Double>> iterator = scores.iterator();
	    final Entry<Alternative, Double> first = iterator.next();
	    result.putAsHighest(first.getKey());
	    source.removeRow(first.getKey());
	    source.removeColumn(first.getKey());
	    final double firstScore = first.getValue().doubleValue();
	    while (iterator.hasNext()) {
		final Entry<Alternative, Double> next = iterator.next();
		final double nextScore = next.getValue().doubleValue();
		assert (nextScore >= firstScore);
		if (nextScore > firstScore) {
		    break;
		}
		result.put(next.getKey(), 1);
		source.removeRow(next.getKey());
		source.removeColumn(next.getKey());
	    }
	}
	return result;
    }

    public Preorder<Alternative> getDescending() {
	final SparseMatrixFuzzy<Alternative, Alternative> source = Matrixes.newSparseFuzzy(m_source);
	assert (source.getRows().equals(source.getColumns()));
	final Preorder<Alternative> result = new Preorder<Alternative>();

	int currentRank = 1;
	while (!source.isEmpty()) {
	    final AlternativesScores netFlowsOld;
	    try {
		netFlowsOld = new Flow().getNetFlows(source);
	    } catch (InvalidInputException exc) {
		throw new IllegalStateException(exc);
	    }
	    final AlternativesScores netFlows = new AlternativesScores(netFlowsOld);
	    final Iterator<Alternative> alternativesFromBest = netFlows.descendingMap().keySet().iterator();
	    final Alternative first = alternativesFromBest.next();
	    result.put(first, currentRank);
	    source.removeRow(first);
	    source.removeColumn(first);
	    final double firstScore = netFlows.getScore(first);
	    while (alternativesFromBest.hasNext()) {
		final Alternative next = alternativesFromBest.next();
		final double nextScore = netFlows.getScore(next);
		assert (nextScore <= firstScore);
		if (nextScore < firstScore) {
		    break;
		}
		result.put(next, currentRank);
		source.removeRow(next);
		source.removeColumn(next);
	    }
	    ++currentRank;
	}
	return result;
    }

    public SparseMatrixFuzzyRead<Alternative, Alternative> getIntersection() {
	return GraphUtils.getIntersection(getAscending(), getDescending());
    }
}
