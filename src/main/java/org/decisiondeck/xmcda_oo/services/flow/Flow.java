package org.decisiondeck.xmcda_oo.services.flow;

import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.scores.AlternativesScores;
import org.decision_deck.utils.matrix.SparseMatrixDRead;
import org.decisiondeck.jmcda.exc.InvalidInputException;

/**
 * TODO check if alts size is one (because alts.size() - 1 will lead to division by zero).
 * 
 * @author Olivier Cailloux
 * 
 */
public class Flow {
    public AlternativesScores getNegativeFlows(SparseMatrixDRead<Alternative, Alternative> pref)
	    throws InvalidInputException {
	if (!pref.isComplete()) {
	    throw new InvalidInputException("Given matrix is not complete.");
	}
	final Set<Alternative> alts = pref.getRows();
	if (!alts.equals(pref.getColumns())) {
	    throw new InvalidInputException("Given matrix is not square.");
	}
	final AlternativesScores flowsOut = new AlternativesScores();
	for (final Alternative alt1 : alts) {
	    float sum = 0;
	    for (final Alternative alt2 : alts) {
		sum += pref.getEntry(alt2, alt1).doubleValue();
	    }
	    sum = sum / (alts.size() - 1);
	    flowsOut.put(alt1, Double.valueOf(sum));
	}
	return flowsOut;
    }

    public AlternativesScores getNetFlowsNotDivided(SparseMatrixDRead<Alternative, Alternative> pref)
	    throws InvalidInputException {
	if (!pref.isComplete()) {
	    throw new InvalidInputException("Given matrix is not complete.");
	}
	final Set<Alternative> alts = pref.getRows();
	if (!alts.equals(pref.getColumns())) {
	    throw new InvalidInputException("Given matrix is not square.");
	}
	final AlternativesScores flows = new AlternativesScores();
	for (final Alternative alt1 : alts) {
	    float sum = 0;
	    for (final Alternative alt2 : alts) {
		sum += pref.getEntry(alt1, alt2).doubleValue() - pref.getEntry(alt2, alt1).doubleValue();
	    }
	    flows.put(alt1, Double.valueOf(sum));
	}
	return flows;
    }

    public AlternativesScores getNetFlows(SparseMatrixDRead<Alternative, Alternative> pref)
	    throws InvalidInputException {
	final AlternativesScores scores = getNetFlowsNotDivided(pref);
	final Set<Alternative> alts = pref.getRows();
	if (!alts.equals(pref.getColumns())) {
	    throw new InvalidInputException("Given matrix is not square.");
	}
	for (Alternative alternative : alts) {
	    scores.put(alternative, scores.get(alternative).doubleValue() / (alts.size() - 1));
	}
	return scores;
    }

    public AlternativesScores getFlows(FlowType type, SparseMatrixDRead<Alternative, Alternative> pref)
	    throws InvalidInputException {
	final AlternativesScores flowsOut;
	switch (type) {
	case POSITIVE:
	    flowsOut = getPositiveFlows(pref);
	    break;
	case NEGATIVE:
	    flowsOut = getNegativeFlows(pref);
	    break;
	case NET:
	    flowsOut = getNetFlows(pref);
	    break;
	default:
	    throw new IllegalStateException("Unknown flow type.");
	}
	return flowsOut;
    }

    public AlternativesScores getPositiveFlows(SparseMatrixDRead<Alternative, Alternative> pref)
	    throws InvalidInputException {
	if (!pref.isComplete()) {
	    throw new InvalidInputException("Given matrix is not complete.");
	}
	final Set<Alternative> alts = pref.getRows();
	if (!alts.equals(pref.getColumns())) {
	    throw new InvalidInputException("Given matrix is not square.");
	}
	final AlternativesScores flowsIn = new AlternativesScores();
	for (final Alternative alt1 : alts) {
	    float sum = 0;
	    for (final Alternative alt2 : alts) {
		sum += pref.getEntry(alt1, alt2).doubleValue();
	    }
	    sum = sum / (alts.size() - 1);
	    flowsIn.put(alt1, Double.valueOf(sum));
	}

	return flowsIn;
    }

}
