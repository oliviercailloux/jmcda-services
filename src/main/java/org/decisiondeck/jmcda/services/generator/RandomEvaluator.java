package org.decisiondeck.jmcda.services.generator;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Random;

import org.decision_deck.jmcda.structure.interval.DiscreteInterval;
import org.decision_deck.jmcda.structure.interval.Interval;

public class RandomEvaluator {

    private Interval m_scale;
    private final Random m_rand;

    /**
     * Creates a new object associated with the given scale.
     * 
     * @param scale
     *            may be discrete, must have finite minimum and maximum. Direction is not used.
     */
    public RandomEvaluator(Interval scale) {
	this(scale, new Random());
    }

    /**
     * Creates a new object associated with the given scale.
     * 
     * @param scale
     *            may be discrete, must have finite minimum and maximum. Direction is not used.
     * @param rand
     *            the random generator used by this object.
     */
    public RandomEvaluator(Interval scale, Random rand) {
	if (scale == null || rand == null) {
	    throw new NullPointerException("" + scale + rand);
	}
	checkArgument(!Double.isInfinite(scale.getMaximum()), "Scale has infinite maximum.");
	checkArgument(!Double.isInfinite(scale.getMinimum()));
	m_rand = rand;
	m_scale = scale;
    }

    /**
     * Computes and returns a random evaluation that belongs to the scale bound to this object. This is a random number
     * chosen with a uniform distribution on the (possibly discrete) possibilities offered by this scale.
     * 
     * @return a number.
     */
    public double getRandomEvaluation() {
	if (m_scale.getStepSize() == null) {
	    return m_rand.nextDouble() * (m_scale.getMaximum() - m_scale.getMinimum()) + m_scale.getMinimum();
	}
	final DiscreteInterval discrete = m_scale.getAsDiscreteInterval();
	return m_rand.nextInt(discrete.getNbSteps()) * discrete.getNonNullStepSize() + discrete.getMinimum();
    }
}
