package uk.rgu.data.align.eurovoc;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.ontologyprocessor.Ontology;
import uk.rgu.data.ontologyprocessor.RDFManager;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;
import uk.rgu.data.services.LinkedConceptService;

/**
 * Alignment by edit distance and word embedding (normalised Levenshtein
 * similarity).
 *
 * @author 1113938
 */
public class HybridOA {

  private static final RDFManager rdfStore = new RDFManager();
  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_annotated_model300_min10_iter5_custom_token.txt";
  static double similarityThreshold = 1.0;

  public static void main(String[] args) {
    Collection evaluationResults = new ArrayList<String>();

    // Get vectors
    VectorOps vectorOps = new VectorOps(vectorModelPath);

    String alignmentScheme = "EUROVOC_GEMET"; // also shows alignment order for concepts
    String scheme1 = "EUROVOC";
    String scheme2 = "GEMET";
    List<String> relationTypes = new ArrayList();
    relationTypes.add(Relation.Predicate.EXACT_MATCH.value); // exactMatch is sub-property of closeMatch
    relationTypes.add(Relation.Predicate.CLOSE_MATCH.value); // non-transitively equivalent concepts

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

    // test different similarity thresholds
//    for (double t = 0.1; t <= similarityThreshold; t += 0.1) {
    Set<AlignedConcept> recommendedAlignments_1 = new HashSet<AlignedConcept>(); // t=0.1 // using sets to avoid duplicate entries
    Set<AlignedConcept> recommendedAlignments_2 = new HashSet<AlignedConcept>(); // t=0.2
    Set<AlignedConcept> recommendedAlignments_3 = new HashSet<AlignedConcept>(); // t=0.3
    Set<AlignedConcept> recommendedAlignments_4 = new HashSet<AlignedConcept>(); // t=0.4
    Set<AlignedConcept> recommendedAlignments_5 = new HashSet<AlignedConcept>(); // t=0.5
    Set<AlignedConcept> recommendedAlignments_6 = new HashSet<AlignedConcept>(); // t=0.6
    Set<AlignedConcept> recommendedAlignments_7 = new HashSet<AlignedConcept>(); // t=0.7
    Set<AlignedConcept> recommendedAlignments_8 = new HashSet<AlignedConcept>(); // t=0.8
    Set<AlignedConcept> recommendedAlignments_9 = new HashSet<AlignedConcept>(); // t=0.9
    Set<AlignedConcept> recommendedAlignments_10 = new HashSet<AlignedConcept>(); // t=1.0

    // for each concept in scheme 1, find labels of scheme 2 that are closest to any of its labels
    for (Concept c1 : concepts1) { // loop through eurovoc
      // Closest label from other concept in vector space
      int n = 5;
      Map<String, Double> lhm = mostSimilarString(c1.getAllLabels(), c2LabelMap.keySet(), n); // Map = word, similarity: gemet terms that are nearest to eurovoc concept terms
      for (Map.Entry<String, Double> entry : lhm.entrySet()) {
        String term = entry.getKey(); // word/term
        double similarity = entry.getValue(); // cosine similarity
        if (c2LabelMap.get(term).split("-").length > 1) {
          // get corresponding concept of the term
          System.out.println(c2LabelMap.get(term));
          Concept c2 = rdfStore.getConcept(Ontology.getGraph(scheme2), c2LabelMap.get(term).split("-")[1]);
          Map<String, Double> lhm_2 = vectorOps.nearestWordsByHighestSimilarity(c1.getAllLabels(), c2.getAllLabels(), 1); // get highest cosine similarity using c2 labels
          double vecSim = 0.0;
          if (lhm_2.entrySet().iterator().hasNext()) { // test to avoid NoSuchElementException
            Map.Entry<String, Double> vec = lhm_2.entrySet().iterator().next();
            vecSim = vec.getValue();
          }
          // check if similarity is up to the threshold
          if (similarity >= 0.1 || vecSim >= 0.1) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_1.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
          if (similarity >= 0.2 || vecSim >= 0.2) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_2.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
          if (similarity >= 0.3 || vecSim >= 0.3) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_3.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
          if (similarity >= 0.4 || vecSim >= 0.4) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_4.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
          if (similarity >= 0.5 || vecSim >= 0.5) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_5.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
          if (similarity >= 0.6 || vecSim >= 0.6) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_6.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
          if (similarity >= 0.7 || vecSim >= 0.7) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_7.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
          if (similarity >= 0.8 || vecSim >= 0.8) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_8.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
          if (similarity >= 0.9 || vecSim >= 0.9) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_9.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
          if (similarity >= 1.0 || vecSim >= 1.0) {
//            System.out.println( "Alignment: " + term + " : " + similarity + " | Concepts: " + c1 + " vs " + c2 );
            recommendedAlignments_10.add(new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value));
          }
        }
      } // end inner for
    } // end outer for

    // Summary of discovered alignments
    evaluationResults.add("Alignments found for threshold: " + 0.1 + " is " + recommendedAlignments_1.size());

    // Evaluate discovered alignments
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_1));
//    }

    evaluationResults.add("Alignments found for threshold: " + 0.2 + " is " + recommendedAlignments_2.size());
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_2));

    evaluationResults.add("Alignments found for threshold: " + 0.3 + " is " + recommendedAlignments_3.size());
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_3));

    evaluationResults.add("Alignments found for threshold: " + 0.4 + " is " + recommendedAlignments_4.size());
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_4));

    evaluationResults.add("Alignments found for threshold: " + 0.5 + " is " + recommendedAlignments_5.size());
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_5));

    evaluationResults.add("Alignments found for threshold: " + 0.6 + " is " + recommendedAlignments_6.size());
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_6));

    evaluationResults.add("Alignments found for threshold: " + 0.7 + " is " + recommendedAlignments_7.size());
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_7));

    evaluationResults.add("Alignments found for threshold: " + 0.8 + " is " + recommendedAlignments_8.size());
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_8));

    evaluationResults.add("Alignments found for threshold: " + 0.9 + " is " + recommendedAlignments_9.size());
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_9));

    evaluationResults.add("Alignments found for threshold: " + 1.0 + " is " + recommendedAlignments_10.size());
    evaluationResults.add(Evaluator.evaluate(alignmentScheme, relationTypes, recommendedAlignments_10));

    // print
    evaluationResults.forEach(System.out::println);
  }

  private static Map<String, Double> mostSimilarString(Set<String> words, Set<String> wordList, int n) {
    Map<String, Double> vecList = new LinkedHashMap();
    NormalizedLevenshtein l = new NormalizedLevenshtein();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = l.similarity(word, str);
        if (sim > 0.0) {
          if (vecList.containsKey(str)) { // check if to replace existing
            if (vecList.get(str) > sim) { // update only is higher than existing
              vecList.put(str, sim);
            }
          } else { // add new
            vecList.put(str, sim);
          }
        }
      } // end inner for
    }
    // sort in descending order
    vecList = sortByValue(vecList);
    // select top n
    Map<String, Double> resList = new LinkedHashMap();
    int counter = 0;
    for (Map.Entry<String, Double> entry : vecList.entrySet()) {
      counter++;
      resList.put(entry.getKey(), entry.getValue());
      if (counter == n) { // required terms retrieved
        break;
      }
    }

    return resList;
  }

  public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
    return map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
            ));
  }

}
