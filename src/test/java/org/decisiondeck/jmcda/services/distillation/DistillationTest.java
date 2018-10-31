package org.decisiondeck.jmcda.services.distillation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.services.SimpleDistillation;
import org.decision_deck.utils.matrix.SparseMatrixFuzzyRead;
import org.decision_deck.utils.relation.graph.Edge;
import org.decision_deck.utils.relation.graph.Preorder;
import org.decision_deck.utils.relation.graph.mess.GraphUtils;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class DistillationTest {
    @Test
    public void testAscending() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final SimpleDistillation distillation = new SimpleDistillation(testData.getConcordance());
	final Preorder<Alternative> computed = distillation.getAscending();
	s_logger.info("Obtained {}.", computed);
	assertEquals(Sets.newHashSet(new Alternative("a02")), computed.get(6));
	assertEquals(Sets.newHashSet(new Alternative("a06")), computed.get(5));
	assertEquals(Sets.newHashSet(new Alternative("a04")), computed.get(4));
	assertEquals(Sets.newHashSet(new Alternative("a01")), computed.get(3));
	assertEquals(Sets.newHashSet(new Alternative("a05")), computed.get(2));
	assertEquals(Sets.newHashSet(new Alternative("a03")), computed.get(1));
    }

    private static final Logger s_logger = LoggerFactory.getLogger(DistillationTest.class);

    @Test
    public void testDescending() throws Exception {
	final SixRealCars testData = SixRealCars.getInstance();
	final SimpleDistillation distillation = new SimpleDistillation(testData.getConcordance());
	final Preorder<Alternative> computed = distillation.getDescending();
	assertEquals(Sets.newHashSet(new Alternative("a03")), computed.get(1));
	assertEquals(Sets.newHashSet(new Alternative("a01")), computed.get(2));
	assertEquals(Sets.newHashSet(new Alternative("a05")), computed.get(3));
	assertEquals(Sets.newHashSet(new Alternative("a06")), computed.get(4));
	assertEquals(Sets.newHashSet(new Alternative("a02")), computed.get(5));
	assertEquals(Sets.newHashSet(new Alternative("a04")), computed.get(6));
    }

    @Test
    public void testIntersection() throws Exception {
	final SimpleDirectedGraph<Alternative, Edge<Alternative>> g = new SimpleDirectedGraph<Alternative, Edge<Alternative>>(
		new GraphUtils.SimpleEdgeFactory<Alternative>());
	g.addVertex(new Alternative("a01"));
	g.addVertex(new Alternative("a02"));
	g.addVertex(new Alternative("a03"));
	g.addVertex(new Alternative("a04"));
	g.addVertex(new Alternative("a05"));
	g.addVertex(new Alternative("a06"));
	g.addEdge(new Alternative("a03"), new Alternative("a01"));
	g.addEdge(new Alternative("a03"), new Alternative("a05"));
	g.addEdge(new Alternative("a01"), new Alternative("a06"));
	g.addEdge(new Alternative("a01"), new Alternative("a04"));
	g.addEdge(new Alternative("a05"), new Alternative("a06"));
	g.addEdge(new Alternative("a05"), new Alternative("a04"));
	g.addEdge(new Alternative("a06"), new Alternative("a02"));
	TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(g);

	final DefaultDirectedGraph<Alternative, Edge<Alternative>> gLoops = new DefaultDirectedGraph<Alternative, Edge<Alternative>>(
		new GraphUtils.SimpleEdgeFactory<Alternative>());
	GraphUtils.copyTo(g, gLoops);
	GraphUtils.addLoops(gLoops);

	final SparseMatrixFuzzyRead<Alternative, Alternative> intersection = new SimpleDistillation(SixRealCars
		.getInstance().getConcordance()).getIntersection();
	s_logger.info("Got inter: {}.", intersection);
	final DefaultDirectedWeightedGraph<Alternative, Edge<Alternative>> computed = GraphUtils
		.getDiGraph(intersection);
	assertEquals(gLoops.vertexSet(), computed.vertexSet());
	final Function<Edge<Alternative>, String> toStr = Edge.getToStringFunction(Alternative.getIdFct(),
		Alternative.getIdFct());
	final List<Edge<Alternative>> expected = order(gLoops.edgeSet());
	final List<Edge<Alternative>> result = order(computed.edgeSet());
	s_logger.info("Expect: {}, got: {}.", Joiner.on(", ").join(Iterables.transform(expected, toStr)),
		Joiner.on(", ").join(Iterables.transform(result, toStr)));
	assertEquals(expected, result);
    }

    private List<Edge<Alternative>> order(final Set<Edge<Alternative>> expected) {
	final ArrayList<Edge<Alternative>> ordered = Lists.newArrayList(expected);
	Collections.sort(ordered, Edge.getLexicographicOrdering(Ordering.natural(), Ordering.natural()));
	return ordered;
    }
}
