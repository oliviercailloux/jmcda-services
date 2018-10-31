package org.decisiondeck.xmcda_oo.services.sorting;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ProfilesDistanceResult {

    private int m_lastDistanceInAlternatives;
    private double m_lastMaxDist;
    private double m_lastSumDist;
    /**
     * At a given approximation level l, the number of profiles evaluations which are equal to an approximation of l,
     * thus whose distance is lower or equal to l to the other profile evaluation.
     */
    private Map<Double, Integer> m_nbEqualsByApprox = new HashMap<Double, Integer>();

    public int getLastDistanceInAlternatives() {
	return m_lastDistanceInAlternatives;
    }

    public void setLastDistanceInAlternatives(int lastDistanceInAlternatives) {
	m_lastDistanceInAlternatives = lastDistanceInAlternatives;
    }

    public double getLastMaxDist() {
	return m_lastMaxDist;
    }

    public void setLastMaxDist(double lastMaxDist) {
	m_lastMaxDist = lastMaxDist;
    }

    public double getLastSumDist() {
	return m_lastSumDist;
    }

    public void setLastSumDist(double lastSumDist) {
	m_lastSumDist = lastSumDist;
    }

    public void setNbEqualsByApprox(Map<Double, Integer> nbEqualsByApprox) {
	m_nbEqualsByApprox = nbEqualsByApprox;
    }

    public int getNbEquals() {
        return getNbEquals(0);
    }

    public int getNbEquals(final double approx) {
        return m_nbEqualsByApprox.get(Double.valueOf(approx)).intValue();
    }

    /**
     * @return a copy of the current results. Will contain values of -1 iff not yet computed, otherwise, only positive
     *         or null values.
     */
    public NavigableMap<Double, Integer> getNbEqualsByApprox() {
        return new TreeMap<Double, Integer>(m_nbEqualsByApprox);
    }

}
