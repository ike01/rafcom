package uk.rgu.data.align.eurovoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.model.PreAlignedConcept;
import uk.rgu.data.ontologyprocessor.RDFManager;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;
import uk.rgu.data.services.LinkedConceptService;
import uk.rgu.data.utilities.StringOps;

/**
 * Alignment by edit distance (normalised Levenshtein similarity).
 *
 * @author 1113938
 */
public class EditDistanceOA {

  static double similarityThreshold = 1.0;

  public static void main(String[] args) {
    // Get vectors
    VectorOps vectorOps = new VectorOps();
    Collection evaluationResults = new ArrayList<String>();

    String alignmentScheme = "EUROVOC_GEMET"; // also shows alignment order for concepts
    String scheme1 = "EUROVOC";
    String scheme2 = "GEMET";
    List<String> relationTypes = new ArrayList();
    relationTypes.add(Relation.Predicate.EXACT_MATCH.value); // exactMatch is sub-property of closeMatch
    relationTypes.add(Relation.Predicate.CLOSE_MATCH.value); // non-transitively equivalent concepts

    List<AlignedConcept> groundTruth = Evaluator.getGroundTruth(alignmentScheme, relationTypes);
    List<AlignedConcept> selectedGroundTruth = Evaluator.getGroundTruthWithVectors(alignmentScheme, relationTypes, vectorOps);
    System.out.println("Ground truth size: " + selectedGroundTruth.size());

    // Retrieve concepts from both ontologies
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
    List<Concept> concepts1 = linkedConceptService.getAllConcept(scheme1); // get scheme 1: eurovoc
    List<Concept> concepts2 = linkedConceptService.getAllConcept(scheme2); // get scheme 2: gemet
    System.out.println("Size of " + scheme1 + " => " + concepts1.size());
    System.out.println("Size of " + scheme2 + " => " + concepts2.size());
    concepts1 = Evaluator.conceptsWithVectors(concepts1, vectorOps);
    concepts2 = Evaluator.conceptsWithVectors(concepts2, vectorOps);

    System.out.println("Concepts without vectors removed. " + scheme1 + " => " + concepts1.size());
    System.out.println("Concepts without vectors removed. " + scheme2 + " => " + concepts2.size());

    // test different similarity thresholds
    for (double st = 0.3; st <= similarityThreshold; st += 0.1) { // try different thresholds
      System.out.println("PROCESSING: string similarity threshold=" + st);
      List<AlignedConcept> recommendedAlignments;
      List<PreAlignedConcept> preAlignedConcepts = new ArrayList<PreAlignedConcept>();
      // for each concept in scheme 1, find labels of scheme 2 that are closest to any of its labels
      for (Concept c1 : concepts1) { // loop through eurovoc
        for (Concept c2 : concepts2) { // gemet
          // Closest label from other concept in vector space
          if (c1.getConceptId().split("-").length > 1 && c2.getConceptId().split("-").length > 1) { // test for valid concepts (should have a concept id)
            double similarity = StringOps.maxSimilarityByNormalizedLevenshtein(c1.getAllLabels(), c2.getAllLabels());
            if (similarity >= st) {
              AlignedConcept ac = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value);
              preAlignedConcepts = PreAlignedConcept.updateAlignments(preAlignedConcepts, new PreAlignedConcept(ac, similarity));
            }
          } // end if (test of valid concepts)
        } // end inner for
      } // end outer for
      recommendedAlignments = PreAlignedConcept.getAlignments(preAlignedConcepts); // get alingments (removes confidence/similarity values)
      // write out results of using these thresholds
      System.out.println("string similarity threshold=" + st + ",Alignments found=" + recommendedAlignments.size());
      evaluationResults.add("string similarity threshold=" + st + ",Alignments found=" + recommendedAlignments.size());
      String result = Evaluator.evaluate(groundTruth, selectedGroundTruth, recommendedAlignments);
//      String result = Evaluator.evaluate(groundTruth, recommendedAlignments);
      System.out.println(result);
      evaluationResults.add(result);
    }

    // print
    System.out.println();
    evaluationResults.forEach(System.out::println);
  }

}
