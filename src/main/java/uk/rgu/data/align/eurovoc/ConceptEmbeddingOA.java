package uk.rgu.data.align.eurovoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.model.ConceptContext;
import uk.rgu.data.model.PreAlignedConcept;
import uk.rgu.data.model.PreAlignedConcept2;
import uk.rgu.data.ontologyprocessor.RDFManager;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;
import uk.rgu.data.services.LinkedConceptService;

/**
 * Alignment by concept embedding. The nearest concept (from ontology being
 * aligned to) in vector space are recommended as alignment. Concepts are
 * embedded by there terms (label and synonyms) and nearness is measured by
 * cosine similarity of term vectors of concepts being compared.
 *
 * @author 1113938
 */
public class ConceptEmbeddingOA {

//  private static final RDFManager rdfStore = new RDFManager();
  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_plain_model300_min10_iter5_custom_token.txt";
//  static String vectorModelPath = "data/geo_hascontext1_model.txt";
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
    relationTypes.add(Relation.Predicate.CLOSE_MATCH.value); // non-transitively equivalent concepts

    List<AlignedConcept> groundTruth = Evaluator.getGroundTruth(alignmentScheme, relationTypes);
    List<AlignedConcept> selectedGroundTruth = Evaluator.getGroundTruthWithVectors(alignmentScheme, relationTypes, vectorOps);

    // Retrieve concepts from both ontologies
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
//    List<ConceptContext> conceptContexts1 = linkedConceptService.getConceptsAndContextParents(scheme1, 1); // get scheme 2: gemet
//    List<ConceptContext> conceptContexts2 = linkedConceptService.getConceptsAndContextParents(scheme2, 1); // get scheme 2: gemet
    List<Concept> concepts1 = linkedConceptService.getAllConcept(scheme1); // get scheme 1: eurovoc
    List<Concept> concepts2 = linkedConceptService.getAllConcept(scheme2); // get scheme 2: gemet
    System.out.println("Size of " + scheme1 + " => " + concepts1.size());
    System.out.println("Size of " + scheme2 + " => " + concepts2.size());
    concepts1 = Evaluator.conceptsWithVectors(concepts1, vectorOps);
    concepts2 = Evaluator.conceptsWithVectors(concepts2, vectorOps);

    System.out.println("Concepts without vectors removed. " + scheme1 + " => " + concepts1.size());
    System.out.println("Concepts without vectors removed. " + scheme2 + " => " + concepts2.size());
    /*
    List<PreAlignedConcept2> preAlignedConcepts = new ArrayList<PreAlignedConcept2>(); // list to contain minimum threshold and above (to avoid multiple processing)

    for (Concept c1 : concepts1) {
      for (Concept c2 : concepts2) {
        if (c1.getConceptId().split("-").length > 1 && c2.getConceptId().split("-").length > 1) { // test for valid concepts (should have a concept id)
          double vecSim = vectorOps.maxSimilarity(c1.getAllLabels(), c2.getAllLabels()); // maximum cosine similarity
          // check if similarity is up to the threshold by string similarity or vector similarity
          if (vecSim >= minSimilarity) { // check alignment
            double maxContextSemantic = vectorOps.maxSimilarity(getContextTerms(conceptContexts1, c1), getContextTerms(conceptContexts2, c2));
            AlignedConcept ac = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), relationType);
            preAlignedConcepts.add(new PreAlignedConcept2(ac, vecSim, maxContextSemantic)); // keep similarity and maximum context similarity
          }
        } // end if (test of valid concepts)

      } // concept2 loop ends
    } // concept1 loop ends

    for (double vt = minSimilarity; vt <= maxSimilarity; vt += 0.1) { // try different thresholds
      for (double ct = 0.1; ct <= maxSimilarity; ct += 0.1) {
        System.out.println("PROCESSING: concept similarity threshold=" + vt + ",context similarity threshold=" + ct);
        List<AlignedConcept> recommendedAlignments;
        List<PreAlignedConcept> alignmentsInThreshold = new ArrayList<PreAlignedConcept>();
        for (PreAlignedConcept2 pac : preAlignedConcepts) {
          if (pac.similarity_1 >= vt && pac.similarity_2 >= ct) {
            PreAlignedConcept p = new PreAlignedConcept(pac.alignedConcept, pac.similarity_1);
            alignmentsInThreshold = PreAlignedConcept.updateAlignments(alignmentsInThreshold, p);
          }
        }

        recommendedAlignments = PreAlignedConcept.getAlignments(alignmentsInThreshold); // get alingments (removes confidence/similarity values)
        // write out results of using these thresholds
        System.out.println("vector similarity threshold=" + vt + ",context similarity threshold=" + ct + ",Alignments found=" + recommendedAlignments.size());
        String result = Evaluator.evaluate(groundTruth, recommendedAlignments);
        System.out.println(result);
        evaluationResults.add("vector similarity threshold=" + vt + ",context similarity threshold=" + ct + ",Alignments found=" + recommendedAlignments.size());
        evaluationResults.add(result);
        System.out.println();
      }
    }
     */

    vectorOps = new VectorOps(vectorModelPath); // change word embedding model

    List<PreAlignedConcept> preAlignedConcepts = new ArrayList<PreAlignedConcept>(); // list to contain minimum threshold and above (to avoid multiple processing)

    for (Concept c1 : concepts1) {
      for (Concept c2 : concepts2) {
        if (c1.getConceptId().split("-").length > 1 && c2.getConceptId().split("-").length > 1) { // test for valid concepts (should have a concept id)
          double vecSim = vectorOps.maxSimilarity(c1.getAllLabels(), c2.getAllLabels()); // maximum cosine similarity
          // check if similarity is up to the threshold by string similarity or vector similarity
          if (vecSim >= minSimilarity) { // check alignment
            AlignedConcept ac = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value);
            preAlignedConcepts.add(new PreAlignedConcept(ac, vecSim)); // keep similarity
          }
        } // end if (test of valid concepts)

      } // concept2 loop ends
    } // concept1 loop ends

    for (double vt = minSimilarity; vt <= maxSimilarity; vt += 0.1) { // try different thresholds
      System.out.println("PROCESSING: concept similarity threshold=" + vt);
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
      String result = Evaluator.evaluate(groundTruth, selectedGroundTruth, recommendedAlignments);
      System.out.println(result);
      evaluationResults.add("vector similarity threshold=" + vt + ",Alignments found=" + recommendedAlignments.size());
      evaluationResults.add(result);
      System.out.println();
    }

    // print results
    evaluationResults.forEach(System.out::println);
  }

  private static Set<String> getContextTerms(List<ConceptContext> conceptContexts, Concept c) {
    Set<String> contextConceptTerms = new HashSet<String>();
//    System.out.println("full uri : " + id);
    for (ConceptContext cc : conceptContexts) {
      if (cc.getId().equals(c.getId())) {
//        System.out.println("Found concept context term: " + cc);
        for (String str : cc.context) { // narrower context
          contextConceptTerms.add(str.replaceAll("_", " "));
        }
        break; // early exit: occurs once!
      }
    }

    return contextConceptTerms;
  }
}
