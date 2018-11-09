package uk.rgu.data.align.eurovoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.model.PreAlignedConcept;
import uk.rgu.data.ontologyprocessor.Ontology;
import uk.rgu.data.ontologyprocessor.RDFManager;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;
import uk.rgu.data.services.LinkedConceptService;

/**
 * Alignment by word embedding. The nearest terms (from ontology being aligned
 * to) in vector space are recommended as alignment. Nearness is measured by
 * cosine similarity and a threshold is used to determine when to align.
 *
 * @author 1113938
 */
public class WordEmbeddingOA {

  private static final RDFManager rdfStore = new RDFManager();
//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_plain_model300_min10_iter5_custom_token.txt";
//  static String vectorModelPath = "data/geo_hascontext1_model.txt";
//  static double similarityThreshold = 1.0;
  static double minSimilarity = 0.5;
  static double maxSimilarity = 1.0;

  public static void main(String[] args) {
    Collection evaluationResults = new ArrayList<String>();

    // Get vectors
    VectorOps vectorOps = new VectorOps();

    String alignmentScheme = "EUROVOC_GEMET"; // also shows alignment order for concepts
    String scheme1 = "EUROVOC";
    String scheme2 = "GEMET";
    List<String> relationTypes = new ArrayList();
    relationTypes.add(Relation.Predicate.EXACT_MATCH.value); // exactMatch is sub-property of closeMatch
//    relationTypes.add(Relation.Predicate.CLOSE_MATCH.value); // non-transitively equivalent concepts

    List<AlignedConcept> groundTruth = Evaluator.getGroundTruth(alignmentScheme, relationTypes);

    // Retrieve concepts from both ontologies
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
    List<Concept> concepts1 = linkedConceptService.getAllConcept(scheme1); // get scheme 1: eurovoc
    List<Concept> concepts2 = linkedConceptService.getAllConcept(scheme2); // get scheme 2: gemet
    System.out.println("Size of " + scheme1 + " => " + concepts1.size());
    System.out.println("Size of " + scheme2 + " => " + concepts2.size());

    // Add concepts of scheme 2 to a map to retrieve conceptIds from concept labels
    Map<String, String> c2LabelMap = new HashMap(); // label (key), concept_id (value): c2 - gemet in map
    for (Concept c2 : concepts2) {
      for (String c2Label : c2.getAllLabels()) {
        c2LabelMap.put(c2Label, c2.getConceptId());
      }
    }

    List<PreAlignedConcept> preAlignedConcepts = new ArrayList<PreAlignedConcept>(); // list to contain minimum threshold and above (to avoid multiple processing)

    for (Concept c1 : concepts1) {
      Map<String, Double> lhm = vectorOps.nearestWordsByHighestSimilarity(c1.getAllLabels(), c2LabelMap.keySet(), 20);
      for (Map.Entry<String, Double> entry : lhm.entrySet()) {
        String term = entry.getKey(); // word/term
        double vecSim = entry.getValue(); // cosine similarity
        if (vecSim >= minSimilarity) {  // test for valid concepts (should have a concept id)
          // get corresponding concept of the term
          Concept c2 = rdfStore.getConcept(Ontology.getGraph(scheme2), c2LabelMap.get(term).split("-")[1]);
          AlignedConcept ac = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value);
          preAlignedConcepts.add(new PreAlignedConcept(ac, vecSim));
        } // end if (test of valid concepts)

      } // concept2 loop ends
    } // concept1 loop ends

    for (double vt = minSimilarity; vt <= maxSimilarity; vt += 0.1) { // try different thresholds
      System.out.println("PROCESSING: vector similarity threshold=" + vt);
      List<AlignedConcept> recommendedAlignments;
      List<PreAlignedConcept> alignmentsInThreshold = new ArrayList<PreAlignedConcept>();
      for (PreAlignedConcept pac : preAlignedConcepts) {
        if (pac.confidence >= vt) {
          PreAlignedConcept p = new PreAlignedConcept(pac.alignedConcept, pac.confidence);
          alignmentsInThreshold = PreAlignedConcept.updateAlignments(alignmentsInThreshold, p);
        }
      }
      recommendedAlignments = PreAlignedConcept.getAlignments(alignmentsInThreshold); // get alingments (removes confidence/similarity values)
      // write out results of using these thresholds
      System.out.println("vector similarity threshold=" + vt + ",Alignments found=" + recommendedAlignments.size());
      String result = Evaluator.evaluate(groundTruth, recommendedAlignments);
      System.out.println(result);
      evaluationResults.add("vector similarity threshold=" + vt + ",Alignments found=" + recommendedAlignments.size());
      evaluationResults.add(result);
      System.out.println();
    }
    // print results
    evaluationResults.forEach(System.out::println);
  }

}
