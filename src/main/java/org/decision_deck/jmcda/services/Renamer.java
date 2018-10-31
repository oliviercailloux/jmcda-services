package org.decision_deck.jmcda.services;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsRead;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decision_deck.jmcda.structure.sorting.category.Categories;
import org.decision_deck.jmcda.structure.sorting.category.Category;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decision_deck.jmcda.structure.weights.Coalitions;
import org.decision_deck.jmcda.structure.weights.CoalitionsUtils;
import org.decision_deck.jmcda.structure.weights.WeightsUtils;
import org.decision_deck.utils.collection.CollectionUtils;
import org.decision_deck.utils.collection.extensional_order.ExtentionalTotalOrder;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.X2SimpleReader;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.XMCDASortingProblemWriter;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultipleRead;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultiple;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsFactory;
import org.decisiondeck.jmcda.structure.sorting.assignment.utils.AssignmentsUtils;
import org.decisiondeck.jmcda.structure.sorting.problem.ProblemFactory;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public class Renamer {
	public static void main(String[] args) throws IOException, XmlException, InvalidInputException {
		final XMCDADoc doc = new XMCDAReadUtils().getXMCDADoc(
				Resources.asByteSource(Renamer.class.getResource("/random_research_30_on_100_modified.xml")));
		final X2SimpleReader reader = new X2SimpleReader(doc);
		final ImmutableMap<Criterion, Criterion> renameCriteriaMap;
		{
			final Builder<Criterion, Criterion> builder = ImmutableMap.builder();
			builder.put(new Criterion("ps"), new Criterion("pr"));
			builder.put(new Criterion("ic"), new Criterion("sc"));
			builder.put(new Criterion("te"), new Criterion("i"));
			builder.put(new Criterion("sq"), new Criterion("bv"));
			builder.put(new Criterion("rq"), new Criterion("pe"));
			builder.put(new Criterion("a"), new Criterion("d"));
			renameCriteriaMap = builder.build();
		}
		final ImmutableMap<Category, Category> renameCategoriesMap = ImmutableMap.of(new Category("bad"),
				new Category("High"), new Category("average"), new Category("Medium"), new Category("good"),
				new Category("Low"));
		final ImmutableMap<Alternative, Alternative> renameProfilesMap = ImmutableMap.of(new Alternative("bad_to_avg"),
				new Alternative("b1"), new Alternative("avg_to_gd"), new Alternative("b2"));
		final Function<Alternative, Alternative> renameProfiles = Functions.forMap(renameProfilesMap);
		final Function<Criterion, Criterion> renameCriteria = Functions.forMap(renameCriteriaMap);
		final ExtentionalTotalOrder<Criterion> criteriaOrder = ExtentionalTotalOrder.create(renameCriteriaMap.values());
		final Function<Alternative, Alternative> renameAlternatives = new Function<Alternative, Alternative>() {
			@Override
			public Alternative apply(Alternative input) {
				return new Alternative("Zone" + input.getId().replace("Pr", ""));
			}
		};
		final Function<Category, Category> renameCategories = Functions.forMap(renameCategoriesMap);
		/** Rename alternatives and criteria, categories, profiles. */
		final ISortingData data = reader.readSortingData();
		final CatsAndProfs catsAndProfs = reader.readCategoriesProfilesAndCategories();
		final Map<DecisionMaker, ? extends IAssignmentsToMultipleRead> groupAssignments = reader.readGroupAssignments();
		final Map<DecisionMaker, IOrderedAssignmentsToMultiple> renamedGroupAssignments = Maps.newLinkedHashMap();
		final Map<DecisionMaker, Coalitions> groupCoalitions = reader.readGroupCoalitions();
		final Map<DecisionMaker, Coalitions> renamedGroupCoalitions = Maps.newLinkedHashMap();
		final Ordering<DecisionMaker> dmsOrdering = Ordering.natural();
		final ExtentionalTotalOrder<Criterion> ord = CollectionUtils
				.<Criterion> newExtentionalTotalOrder(renameCriteriaMap.keySet());
		for (DecisionMaker dm : dmsOrdering.sortedCopy(groupCoalitions.keySet())) {
			final Coalitions coalitions = groupCoalitions.get(dm);
			final Coalitions renamed = CoalitionsUtils.newCoalitions(
					WeightsUtils.newRenameAndReorder(coalitions.getWeights(), renameCriteria, ord.comparator()),
					coalitions.getMajorityThreshold());
			renamedGroupCoalitions.put(dm, renamed);
		}
		for (DecisionMaker dm : dmsOrdering.sortedCopy(groupAssignments.keySet())) {
			final IAssignmentsToMultipleRead assignments = groupAssignments.get(dm);
			final IOrderedAssignmentsToMultiple orderedAssignments = AssignmentsFactory
					.newOrderedAssignmentsToMultiple(assignments, catsAndProfs.getCategories());
			final IOrderedAssignmentsToMultiple renamed = AssignmentsUtils.newRenameAndReorderToMultiple(
					orderedAssignments, renameCategories, renameAlternatives, Ordering.natural());
			renamedGroupAssignments.put(dm, renamed);
		}
		final CatsAndProfs renamedCatsAndProfs = Categories.newRenamed(catsAndProfs, renameCategories, renameProfiles);
		final EvaluationsRead alternativesEvaluations = data.getAlternativesEvaluations();
		final Evaluations renamedAlternativesEvaluations = EvaluationsUtils.newRenamedAndOrdered(
				alternativesEvaluations, renameAlternatives, renameCriteria, Ordering.<Alternative> natural(),
				criteriaOrder.comparator());
		new XMCDASortingProblemWriter(Files.asByteSink(new File("group assignments.xml"))).writeGroupAssignments(
				ProblemFactory.newGroupSortingAssignmentsToMultiple(renamedAlternativesEvaluations, null,
						renamedCatsAndProfs, renamedGroupAssignments));
		new XMCDASortingProblemWriter(Files.asByteSink(new File("group coalitions.xml")))
				.writeGroupPreferences(ProblemFactory.newGroupSortingPreferences(renamedAlternativesEvaluations, null,
						renamedCatsAndProfs, null, null, renamedGroupCoalitions));
		final Evaluations profilesEvaluations = reader.readProfilesEvaluations();
		final EvaluationsRead renamedProfilesEvaluations = EvaluationsUtils.newRenamedAndOrdered(profilesEvaluations,
				renameProfiles, renameCriteria, Ordering.<Alternative> natural(), criteriaOrder.comparator());
		new XMCDASortingProblemWriter(Files.asByteSink(new File("profiles evaluations.xml")))
				.writeProblemData(ProblemFactory.newProblemData(renamedProfilesEvaluations, null));
	}
}
