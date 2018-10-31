package org.decisiondeck.xmcda_oo.services.promethee_profiles;

import org.decision_deck.jmcda.services.ConsistencyChecker;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.thresholds.Thresholds;
import org.decisiondeck.jmcda.exc.InputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.services.outranking.Concordance;
import org.decisiondeck.jmcda.structure.sorting.problem.data.IProblemData;

public class PrometheeProfiles {
    public Evaluations computeProfiles(IProblemData data, Thresholds thresholds)
	    throws InvalidInputException {
	InputCheck.check(data.getCriteria().containsAll(thresholds.getCriteria()));
	ConsistencyChecker checker = new ConsistencyChecker();
	checker.assertCompleteAlternativesEvaluations(data);
	checker.assertCompletePreferenceDirections(data);

	final Evaluations profiles = EvaluationsUtils.newEvaluationMatrix();
	final Concordance concordanceSvc = new Concordance();
	for (Alternative a : data.getAlternatives()) {
	    for (Criterion criterion : data.getCriteria()) {
		final PreferenceDirection direction = data.getScales().get(criterion).getPreferenceDirection();
		double sumP = 0;
		for (Alternative x : data.getAlternatives()) {
		    final double eval1 = data.getAlternativesEvaluations().getEntry(a, criterion).doubleValue();
		    final double eval2 = data.getAlternativesEvaluations().getEntry(x, criterion).doubleValue();
		    final double pValue = thresholds.containsPreferenceThreshold(criterion) ? thresholds
			    .getPreferenceThreshold(criterion) : 0;
		    final double iValue = thresholds.containsIndifferenceThreshold(criterion) ? thresholds
			    .getIndifferenceThreshold(criterion) : 0;
		    final double pax = concordanceSvc.preferencePairwize(eval1, eval2, direction,
 pValue, iValue);
		    final double pxa = concordanceSvc.preferencePairwize(eval2, eval1, direction, pValue, iValue);
		    final double pDiff = pax - pxa;
		    sumP += pDiff;
		}
		sumP = sumP / (data.getAlternatives().size() - 1);
		profiles.put(a, criterion, sumP);
	    }
	}
	return profiles;
    }
}
