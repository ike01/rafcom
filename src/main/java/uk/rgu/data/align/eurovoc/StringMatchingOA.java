package uk.rgu.data.align.eurovoc;

import java.util.ArrayList;
import java.util.List;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.ontologyprocessor.RDFManager;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.services.LinkedConceptService;
import uk.rgu.data.ontologyprocessor.word2vec.UseConceptVectors;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;

/**
 * Baseline 1: Most basic approach using lexical matching (StringEquiv).
 * Aligns concepts of ontologies only if any of the concept terms have exact
 * string match (case-insensitive).
 *
 * @author 1113938
 */
public class StringMatchingOA {

//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_annotated_model300_min10_iter5_custom_token.txt";
//  private static final RDFManager rdfStore = new RDFManager();

  public static void main(String[] args) {
    // Get vectors
//    VectorOps vectorOps = new VectorOps();

    List<AlignedConcept> recommendedAlignments = new ArrayList<AlignedConcept>();

    String alignmentScheme = "EUROVOC_GEMET"; // also shows alignment order for concepts
    String scheme1 = "EUROVOC";
    String scheme2 = "GEMET";
    List<String> relationTypes = new ArrayList();
    relationTypes.add(Relation.Predicate.EXACT_MATCH.value); // exactMatch is sub-property of closeMatch
//    relationTypes.add(Relation.Predicate.CLOSE_MATCH.value); // non-transitively equivalent concepts

    List<AlignedConcept> groundTruth = Evaluator.getGroundTruth(alignmentScheme, relationTypes);
//    List<AlignedConcept> selectedGroundTruth = Evaluator.getGroundTruthWithVectors(alignmentScheme, relationTypes, vectorOps);
    System.out.println("Total reference alignments " + groundTruth.size());
//    System.out.println("Selected reference alignments " + selectedGroundTruth.size());

    // Retrieve concepts from both ontologies
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
    List<Concept> concepts1 = linkedConceptService.getAllConcept(scheme1); // get scheme 1: eurovoc
    List<Concept> concepts2 = linkedConceptService.getAllConcept(scheme2); // get scheme 2: gemet
    System.out.println("Size of " + scheme1 + " => " + concepts1.size());
    System.out.println("Size of " + scheme2 + " => " + concepts2.size());
//    concepts1 = Evaluator.conceptsWithVectors(concepts1, vectorOps);
//    concepts2 = Evaluator.conceptsWithVectors(concepts2, vectorOps);
//
//    System.out.println("Concepts without vectors removed. " + scheme1 + " => " + concepts1.size());
//    System.out.println("Concepts without vectors removed. " + scheme2 + " => " + concepts2.size());

    // Compare concept terms
    for (Concept c1 : concepts1) {
      // Add to list of alignments if any terms match
      for (Concept c2 : concepts2) {
        if (UseConceptVectors.checkStringMatch(c1.getAllLabels(), c2.getAllLabels(), false)) {
          recommendedAlignments.add( new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value) );
        }

      } // end inner loop
    } // end outer loop

    // Summary of discovered alignments
    System.out.println("Alignments found: " + recommendedAlignments.size());

    // Evaluate discovered alignments
    String result = Evaluator.evaluate(groundTruth, recommendedAlignments);
    System.out.println(result);
  }

}
