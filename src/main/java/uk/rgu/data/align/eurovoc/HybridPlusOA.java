package uk.rgu.data.align.eurovoc;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.model.ConceptContext;
import uk.rgu.data.model.LinkedConcept;
import uk.rgu.data.model.PreAlignedConcept;
import uk.rgu.data.model.PreAlignedConcept2;
import uk.rgu.data.model.RecommendedConcept;
import uk.rgu.data.model.RecommendedConcept.RecommendedConceptComparator;
import uk.rgu.data.ontologyprocessor.Ontology;
import uk.rgu.data.ontologyprocessor.Ontology.Graph;
import uk.rgu.data.ontologyprocessor.RDFManager;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.ontologyprocessor.Relation.Predicate;
import uk.rgu.data.ontologyprocessor.word2vec.TFIDFCalculator;
import uk.rgu.data.ontologyprocessor.word2vec.UseConceptVectors;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;
import uk.rgu.data.services.LinkedConceptService;
import uk.rgu.data.utilities.FileOps;
import uk.rgu.data.utilities.StringOps;

/**
 * Alignment by edit distance and word embedding (normalised Levenshtein
 * similarity).
 *
 * @author 1113938
 */
public class HybridPlusOA {

  private static final RDFManager rdfStore = new RDFManager();
  static NormalizedLevenshtein stringSimilarity = new NormalizedLevenshtein();
//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_annotated_model300_min10_iter5_custom_token.txt";
//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_plain_model300_min10_iter5_custom_token.txt";
  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/GoogleNews-vectors-negative300.bin.gz";
//  static String vectorModelPath = "C:/dev/rgu/word2vec/models/geo_hascontext1_model.txt";
//  static String vectorModelPath = "C:/dev/rgu/word2vec/models/GoogleNews-vectors-negative300.bin.gz";
  // Get vectors
  static VectorOps vectorOps = new VectorOps();
  static DecimalFormat df = new DecimalFormat("#.####");
  static double minSimilarity = 0.75;
  static double maxSimilarity = 1.0;

  public static void main2(String[] args) {
    Collection evaluationResults = new ArrayList<String>();

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
    List<Concept> concepts1 = linkedConceptService.getAllConcept(scheme1); // get scheme 1: eurovoc
    List<Concept> concepts2 = linkedConceptService.getAllConcept(scheme2); // get scheme 2: gemet
    System.out.println("Size of " + scheme1 + " => " + concepts1.size());
    System.out.println("Size of " + scheme2 + " => " + concepts2.size());
    concepts1 = Evaluator.conceptsWithVectors(concepts1, vectorOps);
    concepts2 = Evaluator.conceptsWithVectors(concepts2, vectorOps);

    System.out.println("Concepts without vectors removed. " + scheme1 + " => " + concepts1.size());
    System.out.println("Concepts without vectors removed. " + scheme2 + " => " + concepts2.size());

    vectorOps = new VectorOps(vectorModelPath); // change word embedding model

    List<PreAlignedConcept> preAlignedConcepts = new ArrayList<PreAlignedConcept>(); // list to contain minimum threshold and above (to avoid multiple processing)

    for (Concept c1 : concepts1) {
      for (Concept c2 : concepts2) {
        if (c1.getConceptId().split("-").length > 1 && c2.getConceptId().split("-").length > 1) { // test for valid concepts (should have a concept id)
//          double stringSim = StringOps.maxSimilarityByNormalizedLevenshtein(c1.getAllLabels(), c2.getAllLabels()); // maximum string similarity
          double hybridSim = vectorOps.maxHybridSimilarity(c1.getAllLabels(), c2.getAllLabels()); // maximum cosine similarity
          // check if similarity is up to the threshold by string similarity or vector similarity
//          if (stringSim >= minSimilarity || vecSim >= minSimilarity) { // check alignment
          if (hybridSim >= minSimilarity) { // check alignment
            AlignedConcept ac = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value);
            preAlignedConcepts.add(new PreAlignedConcept(ac, hybridSim));
          }
        } // end if (test of valid concepts)

      } // concept2 loop ends
    } // concept1 loop ends

     // test alignment performance at different thresholds
    for (double ht = minSimilarity; ht <= maxSimilarity; ht += 0.01) { // try different thresholds
      System.out.println("PROCESSING: concept similarity threshold=" + ht);
      List<AlignedConcept> recommendedAlignments;
      List<PreAlignedConcept> alignmentsInThreshold = new ArrayList<PreAlignedConcept>();
      for (PreAlignedConcept pac : preAlignedConcepts) {
        if (pac.confidence >= ht) {
          PreAlignedConcept p = new PreAlignedConcept(pac.alignedConcept, pac.confidence);
//          alignmentsInThreshold.add(p);
          alignmentsInThreshold = PreAlignedConcept.updateAlignments(alignmentsInThreshold, p);
        }
      }

      recommendedAlignments = PreAlignedConcept.getAlignments(alignmentsInThreshold); // get alingments (removes confidence/similarity values)
      // write out results of using these thresholds
      System.out.println("hybrid similarity threshold=" + ht + ",Alignments found=" + recommendedAlignments.size());
      String result = Evaluator.evaluate(groundTruth, selectedGroundTruth, recommendedAlignments);
      System.out.println(result);
      evaluationResults.add("hybrid similarity threshold=" + ht + ",Alignments found=" + recommendedAlignments.size());
      evaluationResults.add(result);
      System.out.println();
    }
/*
    // test alignment performance at different thresholds
    for (double vt = minSimilarity; vt <= maxSimilarity; vt += 0.1) { // cosine similarity
      for (double st = minSimilarity; st <= maxSimilarity; st += 0.1) { // edit distance
        System.out.println("PROCESSING: string similarity threshold=" + st + ",vector similarity threshold=" + vt);
        List<AlignedConcept> recommendedAlignments;
        List<PreAlignedConcept> alignmentsInThreshold = new ArrayList<PreAlignedConcept>();
        for (PreAlignedConcept2 pac : preAlignedConcepts2) {
          if (pac.similarity_1 >= st || pac.similarity_2 >= vt) {
//            double avgSim = (pac.similarity_1 + pac.similarity_2) / 2;
            double maxSim = pac.similarity_1 > pac.similarity_2 ? pac.similarity_1 : pac.similarity_2;
            PreAlignedConcept p = new PreAlignedConcept(pac.alignedConcept, maxSim);
            alignmentsInThreshold = PreAlignedConcept.updateAlignments(alignmentsInThreshold, p);
          }
//          if (pac.similarity_1 >= st) { // check alignment
//            PreAlignedConcept p = new PreAlignedConcept(pac.alignedConcept, pac.similarity_1);
//            alignmentsInThreshold = PreAlignedConcept.updateAlignments(alignmentsInThreshold, p);
//          } else if (pac.similarity_2 >= vt) {
//            PreAlignedConcept p = new PreAlignedConcept(pac.alignedConcept, pac.similarity_2);
//            alignmentsInThreshold = PreAlignedConcept.updateAlignments(alignmentsInThreshold, p);
//          }
        }
        recommendedAlignments = PreAlignedConcept.getAlignments(alignmentsInThreshold); // get alingments (removes confidence/similarity values)
        // write out results of using these thresholds
        System.out.println("string similarity threshold=" + st + ",vector similarity threshold=" + vt + ",Alignments found=" + recommendedAlignments.size());
        evaluationResults.add("string similarity threshold=" + st + ",vector similarity threshold=" + vt + ",Alignments found=" + recommendedAlignments.size());
        String result = Evaluator.evaluate(groundTruth, selectedGroundTruth, recommendedAlignments);
        System.out.println(result);
        evaluationResults.add(result);
      } // end st loop
    } // end vt loop
*/

    // print results
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

  /**
   * Search against index of the concept terms of ontology being aligned to
   * retrieve the top n concepts for each concept.
   *
   * @param scheme1
   * @param scheme2
   * @param relationTypes
   * @param groundTruth
   * @param n
   */
  public static void generateFeatures1(String scheme1, String scheme2, List<String> relationTypes, List<AlignedConcept> groundTruth, int n) { // AlignedConcept alignedConcept,
    Collection out = new ArrayList<String>(); // for csv output
//    Concept concepts1 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme1), alignedConcept.concept_1); // eurovoc ?
//    Concept concepts2 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme2), alignedConcept.concept_2); // gemet ?
    // Retrieve concepts from both ontologies
    System.out.println("Fetching concepts ... ");
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
    List<Concept> concepts1 = linkedConceptService.getAllConcept(scheme1); // get scheme 1: eurovoc
    List<Concept> concepts2 = linkedConceptService.getAllConcept(scheme2); // get scheme 2: gemet

    concepts1 = Evaluator.conceptsWithVectors(concepts1, vectorOps);
    concepts2 = Evaluator.conceptsWithVectors(concepts2, vectorOps);

    System.out.println("Concepts without vectors removed. " + scheme1 + " => " + concepts1.size());
    System.out.println("Concepts without vectors removed. " + scheme2 + " => " + concepts2.size());

    vectorOps = new VectorOps(vectorModelPath); // change word embedding model

    // concept contexts
    System.out.println("Fetching semantic context of concepts to align to ... ");
//    List<LinkedConcept> linkedConcepts1 = linkedConceptService.getAllLinkedConcept(scheme1);
    List<LinkedConcept> linkedConcepts2 = linkedConceptService.getAllLinkedConcept(scheme2); // get scheme 2: gemet
    List<ConceptContext> conceptContexts1 = linkedConceptService.getConceptsAndContext(scheme1, 1); // get scheme 2: gemet
    List<ConceptContext> conceptContexts2 = linkedConceptService.getConceptsAndContext(scheme2, 1); // get scheme 2: gemet

    // FEATURES
    String header = scheme1 + "," + scheme2 + ",string_match,has_vector,edit_distance,rank,max_vec_sim,offset_next_max,avg_vec_sim,offset_next_avg,context_size,context_overlap(match),context_overlap(semantic),class";
    out.add(header);
    int conceptCount = 0;
    for (Concept c1 : concepts1) {
      System.out.println(++conceptCount + ". Generating features for " + c1);
      int counter = 0;
      Map<String, Double> nearestByAvg = vectorOps.nearestConceptsByHighestSimilarity(c1, concepts2, n + 1); //  n + 1 to be able to retrieve similarity offset for nth entry
      System.out.println("Count of near concepts " + nearestByAvg.size());
      nearestByAvg.forEach((k, v) -> {
        System.out.println(k + " => " + v);
      });
      Iterator<Map.Entry<String, Double>> entries = nearestByAvg.entrySet().iterator(); // iterator to peek ahead for offsets
      if (entries.hasNext()) {
        entries.next(); // move past first entry
      }
      for (Map.Entry<String, Double> entry : nearestByAvg.entrySet()) { // generate feature set for each entry (top 5?)
        counter++; // keeps rank in list
        Concept next_c2 = null;
        if (entries.hasNext()) {
          Map.Entry<String, Double> next = entries.next();
          next_c2 = rdfStore.getConcept(Ontology.getGraph(scheme2), next.getKey().split("-")[1]);
        }

        Concept c2 = rdfStore.getConcept(Ontology.getGraph(scheme2), entry.getKey().split("-")[1]);

        String line = c1.getConceptId() + "," + c2.getConceptId() + ","; // an entry in output

        // 1. Exact string match
        String exactStringMatch = "false";
        if (UseConceptVectors.checkStringMatch(c1.getAllLabels(), c2.getAllLabels(), false)) {
          exactStringMatch = "true";
        }
        line += exactStringMatch + ",";

        // 2. Exact match have vectors
        String exactMatchHaveVectors = "false";
        if (UseConceptVectors.checkMatchHaveVectors(vectorOps, c1.getAllLabels(), c2.getAllLabels())) {
          exactMatchHaveVectors = "true";
        }
        line += exactMatchHaveVectors + ",";

        // 3. NormalizedLevenshtein
//    double maxLevDist = StringOps.maxSimilarityByNormalizedLevenshtein(c1.getAllLabels(), c2.getAllLabels());
//    if (lhm.entrySet().iterator().hasNext()) { // test to avoid NoSuchElementException
//      Map.Entry<String, Double> vec = lhm.entrySet().iterator().next();
//      normalizedLevenshtein = vec.getValue();
//    }
        double normalizedLevenshtein = StringOps.maxSimilarityByNormalizedLevenshtein(c1.getAllLabels(), c2.getAllLabels());
        line += normalizedLevenshtein + ",";

        // 4. Rank in vector list
        line += counter + ",";

        // 5. Maximum vector similarity
//    Map<String, Double> lhm_vec = vectorOps.nearestWordsByHighestSimilarity(c1.getAllLabels(), c2.getAllLabels(), 1);
//    double highestVectorSimilarity = 0.0;
//    if (lhm_vec.entrySet().iterator().hasNext()) { // test to avoid NoSuchElementException
//      Map.Entry<String, Double> vec = lhm_vec.entrySet().iterator().next();
//      highestVectorSimilarity = vec.getValue();
//    }
        double maximumVectorSimilarity = vectorOps.maxSimilarity(c1.getAllLabels(), c2.getAllLabels());
        line += maximumVectorSimilarity + ",";

        // 6. Offset to next maximum vector similarity
        if (null != next_c2) {
          double nextMaximumVectorSimilarity = vectorOps.maxSimilarity(c1.getAllLabels(), next_c2.getAllLabels());
          line += (maximumVectorSimilarity - nextMaximumVectorSimilarity) + ",";
        } else {
          line += -1.0 + ",";
        }

        // 7. Average vector similarity
        double averageVectorSimilarity = vectorOps.averageSimilarity(c1.getAllLabels(), c2.getAllLabels());
        line += averageVectorSimilarity + ","; // last entry

        // 8. Offset to next average vector similarity
        if (null != next_c2) {
          double nextAverageVectorSimilarity = vectorOps.averageSimilarity(c1.getAllLabels(), next_c2.getAllLabels());
          line += (averageVectorSimilarity - nextAverageVectorSimilarity) + ",";
        } else {
          line += -1.0 + ","; // last entry
        }

        // 9. Context size
        Set<String> contextConceptsIds = getContextIds(linkedConcepts2, c2);
        line += contextConceptsIds.size() + ",";

        // 10. Context overlap (numeric proportion)
        int denom = nearestByAvg.keySet().size() > contextConceptsIds.size() ? contextConceptsIds.size() : nearestByAvg.keySet().size() - 1; // -1 after c2 is removed
        int overlap = StringOps.countWordOverlap(contextConceptsIds, nearestByAvg.keySet());
        double contextOverlap = (double) overlap / denom;
        if (Double.isNaN(contextOverlap)) {
          contextOverlap = -1.0;
        }
        line += contextOverlap + ",";

        // 11. Context overlap (semantic)
        Set<String> c1ContextTerms = getContextTerms(conceptContexts1, c1);
        Set<String> c2ContextTerms = getContextTerms(conceptContexts2, c2);
        double contextSemanticOverlap = vectorOps.maxSimilarity(c1ContextTerms, c2ContextTerms);
        if (Double.isNaN(contextSemanticOverlap)) {
          contextSemanticOverlap = -1.0;
        }
        line += contextSemanticOverlap + ",";

        // 12. Class (label) (aligned = "Y", not aligned = "N")
        String classLabel = "N";
        AlignedConcept alignedConcept = new AlignedConcept(c1.getFullConceptUri(), c2.getFullConceptUri(), Relation.Predicate.EXACT_MATCH.value);
        if (Evaluator.trueAlignment(groundTruth, alignedConcept)) {
          classLabel = "Y";
        }
        line += classLabel;
        System.out.println(line);
        out.add(line); // add to output

      }

    }

    // Write output to file
    String fileName = "alignment_features.csv";
    FileOps.printResults(out, fileName);
  }

  /**
   * Retrieves the most similar concepts that are above a threshold.
   *
   * @param scheme1
   * @param scheme2
   * @param relationTypes
   * @param groundTruth
   * @param t
   * @param n
   */
  public static void generateFeatures2(String scheme1, String scheme2, List<String> relationTypes, List<AlignedConcept> groundTruth, double t, int n) { // AlignedConcept alignedConcept,
    Collection out = new ArrayList<String>(); // for csv output
//    Concept concepts1 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme1), alignedConcept.concept_1); // eurovoc ?
//    Concept concepts2 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme2), alignedConcept.concept_2); // gemet ?
    // Retrieve concepts from both ontologies
    System.out.println("Fetching concepts ... ");
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
    List<Concept> concepts1 = linkedConceptService.getAllConcept(scheme1); // get scheme 1: eurovoc
    List<Concept> concepts2 = linkedConceptService.getAllConcept(scheme2); // get scheme 2: gemet

    concepts1 = Evaluator.conceptsWithVectors(concepts1, vectorOps);
    concepts2 = Evaluator.conceptsWithVectors(concepts2, vectorOps);

    System.out.println("Concepts without vectors removed. " + scheme1 + " => " + concepts1.size());
    System.out.println("Concepts without vectors removed. " + scheme2 + " => " + concepts2.size());

    // concept contexts
    System.out.println("Fetching semantic context of concepts to align to ... ");
//    List<LinkedConcept> linkedConcepts1 = linkedConceptService.getAllLinkedConcept(scheme1);
    List<LinkedConcept> linkedConcepts2 = linkedConceptService.getAllLinkedConcept(scheme2); // get scheme 2: gemet

    // Semantic context : labels of directly linked concepts (parents and children) of all concepts in scheme
    List<ConceptContext> conceptContexts1 = linkedConceptService.getConceptsAndContext(scheme1, 1); // get scheme 1: eurovoc
    List<ConceptContext> conceptContexts2 = linkedConceptService.getConceptsAndContext(scheme2, 1); // get scheme 2: gemet

    // Sibling context : labels of siblings (other children of parents) of all concepts in scheme
    List<ConceptContext> conceptSiblings1 = linkedConceptService.getConceptsAndContextSibling(scheme1); // get scheme 1: eurovoc
    List<ConceptContext> conceptSiblings2 = linkedConceptService.getConceptsAndContextSibling(scheme2); // get scheme 2: gemet

    vectorOps = new VectorOps(vectorModelPath); // change word embedding model

    Map<Concept, ArrayList<RecommendedConcept>> candidateAlignments = new HashMap<Concept, ArrayList<RecommendedConcept>>(); // keeps candidate alignments
    // select concepts wit similarity above chosen threshold as candidate alignment concepts
    for (Concept c1 : concepts1) {
      ArrayList<RecommendedConcept> similarConcepts = new ArrayList<RecommendedConcept>(); // Similar concepts above a threshold
//      int selected = 0; // tracks number of concepts selected up to n
      for (Concept c2 : concepts2) {
        if (c1.getConceptId().split("-").length > 1 && c2.getConceptId().split("-").length > 1) { // test for valid concepts (should have a concept id)
          double vecSim = vectorOps.maxSimilarity(c1.getAllLabels(), c2.getAllLabels()); // maximum cosine similarity
          // check if similarity is up to the threshold by string similarity or vector similarity
          if (vecSim >= t) { // check alignment
//            selected++;
//            AlignedConcept ac = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value);
            similarConcepts.add(new RecommendedConcept(c2, vecSim)); // keep similarity
          }
        } // end if (test of valid concepts)

      } // concept2 loop ends
      if (!similarConcepts.isEmpty()) {
        Collections.sort(similarConcepts, new RecommendedConceptComparator()); // sort in descending order of score
        candidateAlignments.put(c1, similarConcepts);
      }
    } // concept1 loop ends

    // Generate features for alignment concepts
    // FEATURES
    String header = scheme1 + "," + scheme2 + ",string_match,has_vector,edit_distance,rank,max_vec_sim,offset_next_max,avg_vec_sim,offset_next_avg,context_size,context_overlap(match),context_overlap(semantic),class";
    out.add(header);
    int conceptCount = 0;
    for (Map.Entry<Concept, ArrayList<RecommendedConcept>> entry : candidateAlignments.entrySet()) {
      Concept c1 = entry.getKey();
      System.out.println(++conceptCount + ". Generating features for " + c1);
      ArrayList<RecommendedConcept> selectedConcepts = entry.getValue();
      System.out.println("Count of concepts above threshold " + selectedConcepts.size());
      Set<String> similarConceptIds = new HashSet<String>();
      for (int i = 0; i < selectedConcepts.size(); i++) {
        similarConceptIds.add(selectedConcepts.get(i).getConceptId());
      }

      for (int i = 0; i < selectedConcepts.size(); i++) {
        if (i == n) {
          break; // top n max
        }
        RecommendedConcept c2 = selectedConcepts.get(i);
        RecommendedConcept next_c2 = null;
        int j = i + 1; // index of next concept
        if (j < selectedConcepts.size()) { // not exceeding index limit
          next_c2 = selectedConcepts.get(j);
        }
        String line = c1.getConceptId() + "," + c2.getConceptId() + ","; // an entry in output
        // 1. Exact string match
        String exactStringMatch = "false";
        if (UseConceptVectors.checkStringMatch(c1.getAllLabels(), c2.getAllLabels(), false)) {
          exactStringMatch = "true";
        }
        line += exactStringMatch + ",";

        // 2. Exact match have vectors
        String exactMatchHaveVectors = "false";
        if (UseConceptVectors.checkMatchHaveVectors(vectorOps, c1.getAllLabels(), c2.getAllLabels())) {
          exactMatchHaveVectors = "true";
        }
        line += exactMatchHaveVectors + ",";

        // 3. NormalizedLevenshtein
        double normalizedLevenshtein = StringOps.maxSimilarityByNormalizedLevenshtein(c1.getAllLabels(), c2.getAllLabels());
        line += normalizedLevenshtein + ",";

        // 4. Rank in recommendation list
        line += (i + 1) + ",";

        // 5. Maximum vector similarity
        double maximumVectorSimilarity = vectorOps.maxSimilarity(c1.getAllLabels(), c2.getAllLabels());
        line += maximumVectorSimilarity + ",";

        // 6. Offset to next maximum vector similarity
        if (null != next_c2) {
          double nextMaximumVectorSimilarity = vectorOps.maxSimilarity(c1.getAllLabels(), next_c2.getAllLabels());
          line += (maximumVectorSimilarity - nextMaximumVectorSimilarity) + ",";
        } else {
          line += -1.0 + ",";
        }

        // 7. Average vector similarity
        double averageVectorSimilarity = vectorOps.averageSimilarity(c1.getAllLabels(), c2.getAllLabels());
        line += averageVectorSimilarity + ","; // last entry

        // 8. Offset to next average vector similarity
        if (null != next_c2) {
          double nextAverageVectorSimilarity = vectorOps.averageSimilarity(c1.getAllLabels(), next_c2.getAllLabels());
          line += (averageVectorSimilarity - nextAverageVectorSimilarity) + ",";
        } else {
          line += -1.0 + ","; // last entry
        }

        // 9. Context size
        Set<String> contextConceptsIds = getContextIds(linkedConcepts2, c2);
        line += contextConceptsIds.size() + ",";

        // 10. Context overlap (numeric proportion)
        int denom = selectedConcepts.size() > contextConceptsIds.size() ? contextConceptsIds.size() : selectedConcepts.size() - 1; // -1 after c2 is removed
        int overlap = StringOps.countWordOverlap(contextConceptsIds, similarConceptIds);
        double contextOverlap = (double) overlap / denom;
        if (Double.isNaN(contextOverlap)) {
          contextOverlap = -1.0;
        }
        line += contextOverlap + ",";

        // 11. Context overlap (semantic)
        Set<String> c1ContextTerms = getContextTerms(conceptContexts1, c1);
//        c1ContextTerms.addAll(getContextTerms(conceptSiblings1, c1)); // add siblings
        Set<String> c2ContextTerms = getContextTerms(conceptContexts2, c2);
//        c2ContextTerms.addAll(getContextTerms(conceptSiblings2, c2)); // add siblings
        double contextSemanticOverlap = vectorOps.maxSimilarity(c1ContextTerms, c2ContextTerms);
        if (Double.isNaN(contextSemanticOverlap)) {
          contextSemanticOverlap = -1.0;
        }
        line += contextSemanticOverlap + ",";

        // 12. Class (label) (aligned = "Y", not aligned = "N")
        String classLabel = "N";
        AlignedConcept alignedConcept = new AlignedConcept(c1.getFullConceptUri(), c2.getFullConceptUri(), Relation.Predicate.EXACT_MATCH.value);
        if (Evaluator.trueAlignment(groundTruth, alignedConcept)) {
          classLabel = "Y";
        }
        line += classLabel;
        System.out.println(line);
        out.add(line); // add to output
      }
    }

    // Write output to file
    String fileName = "alignment_features_top2_(exact_match_no_siblings).csv";
    FileOps.printResults(out, fileName);
  }

  /**
   * CURRENT. Retrieves the most similar concepts that are above a threshold.
   *
   * @param scheme1
   * @param scheme2
   * @param relationTypes
   * @param groundTruth
   * @param t
   * @param n
   */
  public static void generateFeatures3(String scheme1, String scheme2, List<String> relationTypes, List<AlignedConcept> groundTruth, double t, int n) { // AlignedConcept alignedConcept,
    Collection out = new ArrayList<>(); // for csv output
    List<AlignedConcept> seenAlignmentList = new ArrayList<>(); // overall list of alignments returned
    int total = 0;
    int correct = 0;
    int t1=0;
    int t2=0;
    int t3=0;
    int t4=0;

    String header = "id,match_type,similarity,similarity_offset,vec_sim,hybrid_sim,levenshtein,stoilos,fuzzy_score,metriclcs,tfidf_cosine,dice,monge_elkan,parent_overlap,children_overlap,avg_context_overlap,prefix_overlap,suffix_overlap,context_string_overlap,context_string_overlap_offset,has_parents,has_children,depth_difference,class";
    out.add(header);
//    Concept concepts1 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme1), alignedConcept.concept_1); // eurovoc ?
//    Concept concepts2 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme2), alignedConcept.concept_2); // gemet ?
    // Retrieve concepts from both ontologies
    System.out.println("Fetching concepts ... ");
    Graph sourceGraph = Ontology.getGraph(scheme1);
    Graph targetGraph = Ontology.getGraph(scheme2);
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
    List<Concept> concepts1 = linkedConceptService.getAllConcept(scheme1); // get scheme 1: eurovoc
    List<Concept> concepts2 = linkedConceptService.getAllConcept(scheme2); // get scheme 2: gemet

    System.out.println(scheme1 + " size => " + concepts1.size());
    System.out.println(scheme2 + " size => " + concepts2.size());

//    concepts1 = concepts1.subList(1010, 1020);
//    concepts2 = concepts2.subList(1000, 1010);

    List<List<String>> collection1 = getCollection(concepts1);
    List<List<String>> collection2 = getCollection(concepts2);

//    concepts1 = Evaluator.conceptsWithVectors(concepts1, vectorOps);
//    concepts2 = Evaluator.conceptsWithVectors(concepts2, vectorOps);


    // concept contexts
//    System.out.println("Fetching semantic context of concepts to align to ... ");
//    List<LinkedConcept> linkedConcepts1 = linkedConceptService.getAllLinkedConcept(scheme1);
//    List<LinkedConcept> linkedConcepts2 = linkedConceptService.getAllLinkedConcept(scheme2); // get scheme 2: gemet

    // Semantic context : labels of directly linked concepts (parents and children) of all concepts in scheme
    System.out.println("Fetching semantic context of concepts " + scheme1);
    List<ConceptContext> conceptContexts1 = linkedConceptService.getConceptsAndContext(scheme1, 1); // get scheme 1: eurovoc
    System.out.println("Fetching semantic context of concepts " + scheme2);
    List<ConceptContext> conceptContexts2 = linkedConceptService.getConceptsAndContext(scheme2, 1); // get scheme 2: gemet

    // Parent context : labels of parents of all concepts in scheme
    System.out.println("Fetching parents of concepts... ");
    List<ConceptContext> conceptParents1 = linkedConceptService.getConceptsAndContextParents(scheme1, 1); // get scheme 1: eurovoc
    List<ConceptContext> conceptParents2 = linkedConceptService.getConceptsAndContextParents(scheme2, 1); // get scheme 2: gemet

    // Children context : labels of children of all concepts in scheme
    System.out.println("Fetching children of concepts... ");
    List<ConceptContext> conceptChildren1 = linkedConceptService.getConceptsAndContextChildren(scheme1, 1); // get scheme 1: eurovoc
    List<ConceptContext> conceptChildren2 = linkedConceptService.getConceptsAndContextChildren(scheme2, 1); // get scheme 2: gemet

    // Sibling context : labels of siblings (other children of parents) of all concepts in scheme
    System.out.println("Fetching siblings of concepts... ");
    List<ConceptContext> conceptSiblings1 = linkedConceptService.getConceptsAndContextSibling(scheme1); // get scheme 1: eurovoc
    List<ConceptContext> conceptSiblings2 = linkedConceptService.getConceptsAndContextSibling(scheme2); // get scheme 2: gemet

//    vectorOps = new VectorOps(vectorModelPath); // change word embedding model

    for (int typeId = 1; typeId <= 4; typeId++) { // try different types of similarity (switch statement within)
      System.out.println("Selecting candidate alignments using Type" + typeId);
      Map<Concept, ArrayList<RecommendedConcept>> candidateAlignments = new HashMap<Concept, ArrayList<RecommendedConcept>>(); // keeps candidate alignments
      // select concepts wit similarity above chosen threshold as candidate alignment concepts
      for (Concept c1 : concepts1) {
        System.out.println("Current: " + c1);
        ArrayList<RecommendedConcept> similarConcepts = new ArrayList<RecommendedConcept>(); // Similar concepts above a threshold
        //      int selected = 0; // tracks number of concepts selected up to n
        for (Concept c2 : concepts2) {
          if (c1 != null && c2 != null) { // test for valid concepts (should have a concept id)
            if (c1.getConceptId().split("-").length > 1 && c2.getConceptId().split("-").length > 1) { // test for valid concepts (should have a concept id)
              //            double vecSim = StringOps.maxSimilarityByJaccard(OntoOps.getLabels(ontClass1), OntoOps.getLabels(ontClass2)); // maximum cosine similarity
              double sim;
              switch (typeId) {
                case 1:
                  sim = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1.getAllLabels()), VectorOps.prepareStringSpaces(c2.getAllLabels()));
                  t = 0.4;
                  System.out.println("Type" + typeId + ": " + c1.getLabel() + " vs " + c2.getLabel() + " = " + sim);
                  break;
                case 2:
                  sim = StringOps.maxSimilarityByStoilos(VectorOps.prepareStringSpaces(c1.getAllLabels()), VectorOps.prepareStringSpaces(c2.getAllLabels()));
                  t = 0.85;
                  System.out.println("Type" + typeId + ": " + c1.getLabel() + " vs " + c2.getLabel() + " = " + sim);
                  break;
                case 3:
                  sim = StringOps.maxSimilarityByTFIDFCosine(VectorOps.prepareStringSpaces(c1.getAllLabels()), collection1, VectorOps.prepareStringSpaces(c2.getAllLabels()), collection2);
                  t = 0.7;
                  System.out.println("Type" + typeId + ": " + c1.getLabel() + " vs " + c2.getLabel() + " = " + sim);
                  break;
                //                case 4:
                //                  String c1ContextTerms = getContextTermsString(conceptContexts1, new Concept(ontClass1.getURI(), OntoOps.getLabel(ontClass1), sourceScheme));
                //                  String c2ContextTerms = getContextTermsString(conceptContexts2, new Concept(ontClass2.getURI(), OntoOps.getLabel(ontClass2), targetScheme));
                //                  sim = vectorOps.hybridSimilarityV3(c1ContextTerms, c2ContextTerms);
                //                  t = 0.25;
                //                  break;
                case 4:
                  Set<String> c1ParentContextTerms = getContextTerms(conceptParents1, c1);
                  Set<String> c2ParentContextTerms = getContextTerms(conceptParents2, c2);
                  Set<String> c1ChildrenContextTerms = getContextTerms(conceptChildren1, c1);
                  Set<String> c2ChildrenContextTerms = getContextTerms(conceptChildren2, c2);
                  double parentContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ParentContextTerms), VectorOps.prepareStringSpaces(c2ParentContextTerms)); // semantic overlap of parents
                  double childrenContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ChildrenContextTerms), VectorOps.prepareStringSpaces(c2ChildrenContextTerms)); // semantic overlap of children
                  // average of maximum parents and children overlap
                  sim = (parentContextSemanticOverlap + childrenContextSemanticOverlap) / 2;
                  t = 0.2;
                  System.out.println("Type" + typeId + ": " + c1.getLabel() + " vs " + c2.getLabel() + " = " + sim);
                  break;
                default:
                  sim = 0.0;
                  System.out.println(c1.getLabel() + " vs " + c2.getLabel() + " = ERROR!");
                  break;
              }
              // check if similarity is up to the threshold by string similarity or vector similarity
              if (sim > 0.0) { // check alignment (low score to preserve concepts below threshold for offsets computation)
                similarConcepts.add(new RecommendedConcept(c2, sim, typeId)); // keep similarity
              }
            } // end if (test of valid concepts)

//            double vecSim = vectorOps.maxHybridSimilarity(c1.getAllLabels(), c2.getAllLabels()); // maximum cosine similarity
//            // check if similarity is up to the threshold by string similarity or vector similarity
//            if (vecSim >= 0.1) { // check alignment (low score to preserve concepts below threshold for offsets computation)
//              //            selected++;
//              //            AlignedConcept ac = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value);
//              similarConcepts.add(new RecommendedConcept(c2, vecSim)); // keep similarity
//            }
          } // end if (test of valid concepts)

        } // concept2 loop ends
        System.out.println("Selected before sorting " + similarConcepts.size());
        if (!similarConcepts.isEmpty()) {
          Collections.sort(similarConcepts, new RecommendedConcept.RecommendedConceptComparator()); // sort in descending order of score
          int N = n < similarConcepts.size() ? n + 1 : similarConcepts.size(); // +1 to allow comptuing offsets to next most similar
          similarConcepts = new ArrayList<>(similarConcepts.subList(0, N));
          candidateAlignments.put(c1, similarConcepts);
        }
      } // concept1 loop ends

      // Generate features for alignment concepts
      // FEATURES
      int conceptCount = 0;
      for (Map.Entry<Concept, ArrayList<RecommendedConcept>> entry : candidateAlignments.entrySet()) {
        Concept c1 = entry.getKey();
        System.out.println(++conceptCount + ". Generating features for " + c1);
        ArrayList<RecommendedConcept> selectedConcepts = entry.getValue();
        System.out.println("Count of initial similar concepts selected " + selectedConcepts.size());
  //      Set<String> similarConceptIds = new HashSet<String>();
  //      for (int i = 0; i < selectedConcepts.size(); i++) {
  //        similarConceptIds.add(selectedConcepts.get(i).getConceptId());
  //      }

        for (int i = 0; i < selectedConcepts.size()-1; i++) {
          RecommendedConcept c2 = selectedConcepts.get(i);
          AlignedConcept alignedConcept = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.EXACT_MATCH.value);
          if (c2.getScore() >= t && !AlignedConcept.containsTheAlignment(seenAlignmentList, alignedConcept)) { // continue if similarity is up to threshold and alignment is not selected already
            seenAlignmentList.add(alignedConcept); // add new to list
            // best selection similarity
            double hybridSimilarity = vectorOps.maxHybridSimilarity(c1.getAllLabels(), c2.getAllLabels());
            double stoilos = StringOps.maxSimilarityByStoilos(c1.getAllLabels(), c2.getAllLabels());
            double tfidfCosine = StringOps.maxSimilarityByTFIDFCosine(c1.getAllLabels(), collection1, c2.getAllLabels(), collection2);
            Set<String> c1ParentContextTerms = getContextTerms(conceptParents1, c1);
            Set<String> c2ParentContextTerms = getContextTerms(conceptParents2, c2);
            double parentContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ParentContextTerms), VectorOps.prepareStringSpaces(c2ParentContextTerms));
            if (Double.isNaN(parentContextSemanticOverlap)) {
              parentContextSemanticOverlap = 0.0;
            }
            Set<String> c1ChildrenContextTerms = getContextTerms(conceptChildren1, c1);
            Set<String> c2ChildrenContextTerms = getContextTerms(conceptChildren2, c2);
            double childrenContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ChildrenContextTerms), VectorOps.prepareStringSpaces(c2ChildrenContextTerms));
            if (Double.isNaN(childrenContextSemanticOverlap)) {
              childrenContextSemanticOverlap = 0.0;
            }
            double contextSemantic = (parentContextSemanticOverlap + childrenContextSemanticOverlap) / 2;
            double maxSim = Math.max(contextSemantic, Math.max(tfidfCosine, Math.max(hybridSimilarity, stoilos)));
            c2.setScore(maxSim);

            RecommendedConcept next_c2 = null;
            int j = i + 1; // index of next concept
            if (j < selectedConcepts.size()) { // not exceeding index limit
              next_c2 = selectedConcepts.get(j);
              double nextHybridSimilarity = vectorOps.maxHybridSimilarity(c1.getAllLabels(), next_c2.getAllLabels());
              double nextStoilos = StringOps.maxSimilarityByStoilos(c1.getAllLabels(), next_c2.getAllLabels());
              double nextTfidfCosine = StringOps.maxSimilarityByTFIDFCosine(c1.getAllLabels(), collection1, next_c2.getAllLabels(), collection2);

              Set<String> nextC2ParentContextTerms = getContextTerms(conceptParents2, next_c2);
              double nextParentContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ParentContextTerms), VectorOps.prepareStringSpaces(nextC2ParentContextTerms));
              if (Double.isNaN(nextParentContextSemanticOverlap)) {
                nextParentContextSemanticOverlap = 0.0;
              }
              Set<String> nextC2ChildrenContextTerms = getContextTerms(conceptChildren2, next_c2);
              double nextChildrenContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ChildrenContextTerms), VectorOps.prepareStringSpaces(nextC2ChildrenContextTerms));
              if (Double.isNaN(nextChildrenContextSemanticOverlap)) {
                nextChildrenContextSemanticOverlap = 0.0;
              }
              double nextContextSemantic = (nextParentContextSemanticOverlap + nextChildrenContextSemanticOverlap) / 2;
              maxSim = Math.max(nextContextSemantic, Math.max(nextTfidfCosine, Math.max(nextHybridSimilarity, nextStoilos)));
              next_c2.setScore(maxSim);
            }

            // -1. id: Concept Ids
            String line = c1.getConceptId() + "-" + c2.getConceptId() + ","; // an entry in output

            // 0. match_type: (1=hybrid, 2=stoilos, 3=average context)
            line += "Type" + c2.matchType + ","; // nominal

            // 1. similarity
            line += df.format(c2.getScore()) + ",";

            // 2. similarity_offset
            if (null != next_c2) {
              line += df.format(c2.getScore() - next_c2.getScore()) + ",";
            } else {
              line += df.format(c2.getScore()) + ","; // assumes next is 0.0 if there isn't any
            }

            // 3. vec_sim
            double maximumVectorSimilarity = vectorOps.maxSimilarity(VectorOps.prepareStringSpaces(c1.getAllLabels()), VectorOps.prepareStringSpaces(c2.getAllLabels()));
            line += df.format(maximumVectorSimilarity) + ",";

            // 4. hybrid_sim
            line += df.format(hybridSimilarity) + ",";

            // 5. levenshtein
            double normalizedLevenshtein = StringOps.maxSimilarityByNormalizedLevenshtein(c1.getAllLabels(), c2.getAllLabels());
            line += df.format(normalizedLevenshtein) + ",";

            // 6. stoilos
            line += df.format(stoilos) + ",";

            // 7. fuzzy_score
            double fuzzy = StringOps.maxSimilarityByFuzzyScore(c1.getAllLabels(), c2.getAllLabels());
            line += df.format(fuzzy) + ",";

            // 8. metriclcs
            double metriclcs = StringOps.maxSimilarityByMetricLCS(c1.getAllLabels(), c2.getAllLabels());
            line += df.format(metriclcs) + ",";

            // 9. tfidf_cosine
            line += df.format(tfidfCosine) + ",";

            // 10. dice
            double sd = StringOps.maxSimilarityBySorensenDice(c1.getAllLabels(), c2.getAllLabels());
            line += df.format(sd) + ",";

            // 11. monge_elkan
            double me = StringOps.maxSimilarityByMongeElkan(c1.getAllLabels(), c2.getAllLabels());
            line += df.format(me) + ",";

            // 12. parent_overlap
            line += df.format(parentContextSemanticOverlap) + ",";

            // 13. children_overlap
            line += df.format(childrenContextSemanticOverlap) + ",";

            Set<String> c1SiblingContextTerms = getContextTerms(conceptSiblings1, c1);
            Set<String> c2SiblingContextTerms = getContextTerms(conceptSiblings2, c2);
            double siblingContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1SiblingContextTerms), VectorOps.prepareStringSpaces(c2SiblingContextTerms));

            // 14. avg_context_overlap
            double averageMaxContextOverlap = (parentContextSemanticOverlap + childrenContextSemanticOverlap + siblingContextSemanticOverlap) / 3;
            if (Double.isNaN(averageMaxContextOverlap)) {
              averageMaxContextOverlap = 0.0;
            }
            line += df.format(averageMaxContextOverlap) + ",";

            // 15. prefix_overlap (Best string prefix overlap)
            double bestPrefixOverlap = StringOps.bestPrefixOverlap(c1.getAllLabels(), c2.getAllLabels());
            line += bestPrefixOverlap + ",";

            // 16. suffix_overlap (Best string suffix overlap)
            double bestSuffixOverlap = StringOps.bestSuffixOverlap(c1.getAllLabels(), c2.getAllLabels());
            line += bestSuffixOverlap + ",";

            // 17. context_string_overlap
            String c1ContextTerms = getContextTermsString(conceptContexts1, c1);
            String c2ContextTerms = getContextTermsString(conceptContexts2, c2);
            double contextSemanticOverlap = vectorOps.hybridSimilarityV3(c1ContextTerms, c2ContextTerms);
            if (Double.isNaN(contextSemanticOverlap)) {
              contextSemanticOverlap = 0.0;
            }
            line += df.format(contextSemanticOverlap) + ",";

            // 18. context_string_overlap_offset
            if (null != next_c2) {
              String next_c2ContextTerms = getContextTermsString(conceptContexts2, next_c2);
              double nextContextSemanticOverlap = vectorOps.hybridSimilarityV3(c1ContextTerms, next_c2ContextTerms);
              if (Double.isNaN(nextContextSemanticOverlap)) {
                nextContextSemanticOverlap = 0.0;
              }
              line += df.format(contextSemanticOverlap - nextContextSemanticOverlap) + ",";
            } else {
              line += df.format(contextSemanticOverlap) + ",";
            }

            // NEW FEATURES - indicate extent of semantic contexts for concepts being compared
            // 19. has_parents (BOTH, ONE, or NONE)
            if (!c1ParentContextTerms.isEmpty() && !c2ParentContextTerms.isEmpty())
              line += "both" + ","; // both
            else if (!c1ParentContextTerms.isEmpty() || !c2ParentContextTerms.isEmpty())
              line += "one" + ","; // one
            else
              line += "none" + ","; // none

            // 20. has_children
            if (!c1ChildrenContextTerms.isEmpty() && !c2ChildrenContextTerms.isEmpty())
              line += "both" + ",";
            else if (!c1ChildrenContextTerms.isEmpty() || !c2ChildrenContextTerms.isEmpty())
              line += "one" + ",";
            else
              line += "none" + ",";

            // 21. depth_difference (Difference in relative depth in hierarchical path)
            double c1depth = rdfStore.getRelativeDepthInPath(sourceGraph, c1.getFullConceptUri());
            double c2depth = rdfStore.getRelativeDepthInPath(targetGraph, c2.getFullConceptUri());
            line += Math.abs(c1depth - c2depth) + ",";

            // 22. class (label) (aligned = "Y", not aligned = "N")
            String classLabel = "N";
            total++;
            System.out.println("Alignment: " + alignedConcept);
            if (Evaluator.trueAlignment(groundTruth, alignedConcept)) { // check if present in the reference alignment
              correct++;
              classLabel = "Y";
              if (typeId == 1) t1++;
              if (typeId == 2) t2++;
              if (typeId == 3) {
                t3++;
                System.out.println("T3: " + c1.getLabel() + " vs " + c2.getLabel() + " = " + c2.getScore());
              }
              if (typeId == 4) t4++;
            }
            line += classLabel;
//            System.out.println(line);
            out.add(line); // add to output
          }
        }
      }
    }

    out.forEach(System.out::println);
    // print summary
    System.out.println("Correct => " + correct);
    System.out.println("Total => " + total);
    System.out.println("contribution Type1 => " + t1);
    System.out.println("contribution Type2 => " + t2);
    System.out.println("contribution Type3 => " + t3);
    System.out.println("contribution Type4 => " + t4);

    // Write output to file
    String fileName = "eurovoc/eurovoc-gemet_all.csv";
    FileOps.printResults(out, fileName);
  }

  /**
   * Retrieves unique list of conceptIds that are directly linked to a concept.
   *
   * @param linkedConcepts
   * @param c
   * @return
   */
  public static Set<String> getContextIds(List<LinkedConcept> linkedConcepts, Concept c) {
    Set<String> contextConceptIds = new HashSet<String>();
    System.out.println("concept : " + c);
    System.out.println("concept id : " + c.getConceptId());
    String id = Concept.getFullConceptUri(c.getConceptId());
    System.out.println("full uri : " + id);
    for (LinkedConcept cc : linkedConcepts) {
      if (cc.getId().equals(id)) {
        System.out.println("Found concept context Id: " + cc);
        for (String sub : cc.getSubClasslike().getValues()) { // narrower context
          contextConceptIds.add(c.getScheme() + "-" + StringOps.getLastUriValue(sub));
        }
        for (String sup : cc.getSuperClasslike().getValues()) { // broader context
          contextConceptIds.add(c.getScheme() + "-" + StringOps.getLastUriValue(sup));
        }
        break; // early exit: occurs once!
      }
    }

    return contextConceptIds;
  }

  /**
   * Retrieves unique list of concept terms that are directly linked to a
   * concept.
   *
   * @param conceptContexts
   * @param c
   * @return
   */
  public static Set<String> getContextTerms(List<ConceptContext> conceptContexts, Concept c) {
    Set<String> contextConceptTerms = new HashSet<String>();
//    System.out.println("concept : " + c);
//    System.out.println("concept id : " + c.getConceptId());
//    String id = Concept.getFullConceptUri(c.getConceptId());
//    System.out.println("full uri : " + id);
    for (ConceptContext cc : conceptContexts) {
      if (cc.getId().equals(c.getId())) {
//        System.out.println("Found concept context term: " + cc);
        for (String sub : cc.context) { // narrower context
          contextConceptTerms.add(sub.replaceAll("_", " "));
        }
        break; // early exit: occurs once!
      }
    }

//    String out = "";
//    for (String str : contextConceptTerms) {
//      out = out + str + " ";
//    }
    return contextConceptTerms;
  }

  /**
   * Retrieves concatenated string of concept terms that are directly linked to
   * a concept.
   *
   * @param conceptContexts
   * @param c
   * @return
   */
  public static String getContextTermsString(List<ConceptContext> conceptContexts, Concept c) {
    String contextConceptTerms = "";
//    System.out.println("concept : " + c);
//    System.out.println("concept id : " + c.getConceptId());
//    String id = Concept.getFullConceptUri(c.getConceptId());
//    System.out.println("full uri : " + id);
    for (ConceptContext cc : conceptContexts) {
      if (cc.getId().equals(c.getId())) {
//        System.out.println("Found concept context term: " + cc);
        for (String sub : cc.context) { // narrower context
          String str = sub.replaceAll("_", " ");
          str = str.replaceAll("\\s+", " "); // normalise spaces

          // prepare for lookup in word embedding vocabulary
          str = VectorOps.prepareStringSpaces(str);
          contextConceptTerms = contextConceptTerms + " " + str;
        }
        break; // early exit: occurs once!
      }
    }

    return contextConceptTerms.trim();
  }

  /**
   * Generates collection. The words in labels of a concept forms a document.
   * Collection is the list of all documents.
   * @param concepts
   * @return
   */
  public static List<List<String>> getCollection(List<Concept> concepts) {
    List<List<String>> coll = new ArrayList<>();
    for (Concept c : concepts) {
      coll.add(getDocument(c));
    }

    return coll;
  }

  /**
   * Generates a concept document. The words in labels of a concept forms its document (case insensitive).
   * @param concept
   * @return
   */
  public static List<String> getDocument(Concept concept) {
    List<String> doc = new ArrayList<>();
    for (String s : concept.getAllLabels()) {
      doc.addAll(Arrays.asList(VectorOps.prepareStringSpaces(s).split("\\s+"))); // add all words in labels of a concept to document
    }

    return doc;
  }

  public static void addGroundTruth(List<AlignedConcept> groundTruth) {
    Collection out = new ArrayList<>(); // for csv output
    // read file by lines (skip first)
    String sourceCSV = "C:\\dev\\rgu\\weka\\alignment\\eurovoc\\eurovoc-gemet_all.csv";
    File file = new File(sourceCSV);
    BufferedReader br = null;
    String line = "";
    String cvsSplitBy = ",";

    try {
      br = new BufferedReader(new FileReader(file));
      int count = 0;
      int correct = 0;
      int t1 = 0, t2 = 0, t3 = 0, t4 = 0;
      int total = 0;
      while ((line = br.readLine()) != null) {
        count++;
        if (count == 1) out.add(line);
        if (count > 1) {
          total++;
          // use comma as separator
          String[] section = line.split(cvsSplitBy);
          String id = section[0];
          String type = section[1];
          String[] idFrags = id.split("-");
          String sourceUri = Concept.getFullConceptUri(idFrags[0] + "-" + idFrags[1]);
          String targetUri = Concept.getFullConceptUri(idFrags[2] + "-" + idFrags[3]);
          AlignedConcept a = new AlignedConcept(sourceUri, targetUri, Predicate.EXACT_MATCH.value);
          if (Evaluator.trueAlignment(groundTruth, a)) { // check if present in the reference alignment
            correct++;
//            String classLabel = "Y";
            if (type.equals("Type1")) t1++;
            if (type.equals("Type2")) t2++;
            if (type.equals("Type3")) {
              t3++;
//              System.out.println("T3: " + c1.getLabel() + " vs " + c2.getLabel() + " = " + c2.getScore());
            }
            if (type.equals("Type4")) t4++;
            line = line.substring(0, line.length() - 1) + "Y";
          }
          out.add(line);
          System.out.println(sourceUri);
          System.out.println(targetUri);
        }
      }
      out.forEach(System.out::println);
      // print summary
      System.out.println("Correct => " + correct);
      System.out.println("Total => " + total);
      System.out.println("contribution Type1 => " + t1);
      System.out.println("contribution Type2 => " + t2);
      System.out.println("contribution Type3 => " + t3);
      System.out.println("contribution Type4 => " + t4);

      // Write output to file
      String fileName = "C:\\dev\\rgu\\weka\\alignment\\eurovoc\\eurovoc-gemet_exact.csv";
      FileOps.printResults(out, fileName);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    //
  }

  public static void filterDataset() {
    Collection out = new ArrayList<>(); // for csv output
    // read alignment candidates file
    String sourceCSV = "C:\\dev\\rgu\\weka\\alignment\\eurovoc\\eurovoc-gemet_wikipedia_exact.csv";
    Map<String, Double> data = new LinkedHashMap<>();
    // iterate lines
      // split up csv line
      // filter with ground truth
      // filter thresholds
      // reconstruct line (if selected)
      // add to output
      File file = new File(sourceCSV);
    BufferedReader br = null;
    String line = "";
    String cvsSplitBy = ",";

    try {
      br = new BufferedReader(new FileReader(file));
      int count = 0;
//      int correct = 0;
//      int t1 = 0, t2 = 0, t3 = 0, t4 = 0;
//      int total = 0;
      while ((line = br.readLine()) != null) {
        count++;
        if (count == 1) out.add(line); // header
        if (count > 1) {
//          total++;
          // use comma as separator
          String[] section = line.split(cvsSplitBy);
//          String id = section[0];
//          String type = section[1];
          Double similarity = Double.valueOf(section[2]); // similarity/confidence
          data.put(line, similarity);
//          String[] idFrags = id.split("-");
//          String sourceUri = Concept.getFullConceptUri(idFrags[0] + "-" + idFrags[1]);
//          String targetUri = Concept.getFullConceptUri(idFrags[2] + "-" + idFrags[3]);
//          AlignedConcept a = new AlignedConcept(sourceUri, targetUri, Predicate.EXACT_MATCH.value);
//          if (Evaluator.trueAlignment(groundTruth, a)) { // check if present in the reference alignment
//            correct++;
////            String classLabel = "Y";
//            if (type.equals("Type1")) t1++;
//            if (type.equals("Type2")) t2++;
//            if (type.equals("Type3")) {
//              t3++;
////              System.out.println("T3: " + c1.getLabel() + " vs " + c2.getLabel() + " = " + c2.getScore());
//            }
//            if (type.equals("Type4")) t4++;
//            line = line.substring(0, line.length() - 1) + "Y";
//          }
//          out.add(line);
//          System.out.println(sourceUri);
//          System.out.println(targetUri);
        }
      }
      // sort by value and cut off at 2 * size of smaller ontology
      data = sortAndCut(data, 5220);
      // add to output
      data.forEach((k, v) -> {
        out.add(k);
      });
      out.forEach(System.out::println);
      // print summary
      System.out.println("Total (initial) => " + (count-1));
      System.out.println("Total (final) => " + data.size());
//      System.out.println("Correct => " + correct);
//      System.out.println("Total => " + total);
//      System.out.println("contribution Type1 => " + t1);
//      System.out.println("contribution Type2 => " + t2);
//      System.out.println("contribution Type3 => " + t3);
//      System.out.println("contribution Type4 => " + t4);

      // Write output to file
      String fileName = "C:\\dev\\rgu\\weka\\alignment\\eurovoc\\eurovoc-gemet_wikipedia_exact_filtered_5220.csv";
      FileOps.printResults(out, fileName);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static Map<String, Double> sortAndCut(Map<String, Double> data, int sizeOfSmallerScheme) {
    // sort in descending order
    data = StringOps.sortByValue(data);
    // select top n
    int n = sizeOfSmallerScheme;
    Map<String, Double> resList = new LinkedHashMap();
    int counter = 0;
    for (Map.Entry<String, Double> entry : data.entrySet()) {
      counter++;
      resList.put(entry.getKey(), entry.getValue());
      if (counter == n) { // required terms retrieved
        break;
      }
    }

    return resList;
  }

  public static double compareCollection(List<List<String>> collection1, List<List<String>> collection2) {
    List<String> doc1 = new ArrayList<>();
    List<String> doc2 = new ArrayList<>();
    List<List<String>> coll = new ArrayList<>();
    collection1.stream().forEach((l) -> {
      l.stream().forEach((s) -> {
        doc1.add(s);
      });
    });
//    System.out.println("collection 1 => " + doc1.size());
    coll.add(doc1);
    collection2.stream().forEach((l) -> {
      l.stream().forEach((s) -> {
        doc2.add(s);
      });
    });
//    System.out.println("collection 2 => " + doc2.size());
    coll.add(doc2);

    Map<String, Double> v1 = TFIDFCalculator.normalise(TFIDFCalculator.weighStringTerms(doc1, coll));
    Map<String, Double> v2 = TFIDFCalculator.normalise(TFIDFCalculator.weighStringTerms(doc2, coll));

//    return TFIDFCalculator.cosine_similarity(v1, v2);
    return vectorOps.weightedHybridSimilarity(v1, v2);
  }

  public static double findThreshold(double entitiesSimilarity, List<Double> values, int matrixCount) {
    double minThreshold = 0.89;
    for (double t = 0.89; t > minThreshold; t -= 0.01) {
      int count = 0;
      for (double v : values) {
        if (v >= t) {
          count++;
        }
      }
//      System.out.println("Above " + t + ": " + count);
      double propAboveThreshold = Math.sqrt((double) count) / Math.sqrt((double) matrixCount * matrixCount);
      if (propAboveThreshold >= entitiesSimilarity) {
        System.out.println("Count above threshold: " + count);
        return t;
      }
    }

    return minThreshold;
  }

  public static void weightedHybridSimilarity(String scheme1, String scheme2, List<String> relationTypes, List<AlignedConcept> groundTruth, double t, int n) { // AlignedConcept alignedConcept,
    Collection out = new ArrayList<>(); // for csv output
    List<AlignedConcept> seenAlignmentList = new ArrayList<>(); // overall list of alignments returned
    int total = 0;
    int correct = 0;

//    Concept concepts1 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme1), alignedConcept.concept_1); // eurovoc ?
//    Concept concepts2 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme2), alignedConcept.concept_2); // gemet ?
    List<Double> similarityValues = new ArrayList<>();
    // Retrieve concepts from both ontologies
    System.out.println("Fetching concepts ... ");
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
    List<Concept> concepts1 = linkedConceptService.getAllConcept(scheme1); // get scheme 1: eurovoc
    List<Concept> concepts2 = linkedConceptService.getAllConcept(scheme2); // get scheme 2: gemet

    System.out.println(scheme1 + " size => " + concepts1.size());
    System.out.println(scheme2 + " size => " + concepts2.size());

//    concepts1 = concepts1.subList(1010, 1020);
//    concepts2 = concepts2.subList(1000, 1010);
    List<List<String>> collection1 = getCollection(concepts1);
    List<List<String>> collection2 = getCollection(concepts2);

    // collection similarity
    //double collectionSim = compareCollection(collection1, collection2);

    //System.out.println("Concept size for " + scheme1 + " => " + concepts1.size());
    //System.out.println("Concept size for " + scheme2 + " => " + concepts2.size());

    //System.out.println("Similarity: " + collectionSim);
    //int minVocab = concepts1.size() < concepts2.size() ? concepts1.size() : concepts2.size();
    //t = findThreshold(collectionSim, similarityValues, minVocab);
    System.out.println("Threshold: " + t);

    Map<Concept, ArrayList<RecommendedConcept>> candidateAlignments = new HashMap<Concept, ArrayList<RecommendedConcept>>(); // keeps candidate alignments
    // select concepts wit similarity above chosen threshold as candidate alignment concepts
    for (Concept c1 : concepts1) {
      ArrayList<RecommendedConcept> similarConcepts = new ArrayList<RecommendedConcept>(); // Similar concepts above a threshold
      for (Concept c2 : concepts2) {
        if (c1 != null && c2 != null) { // test for valid concepts (should have a concept id)
          double sim = vectorOps.maxWeightedHybridSimilarity(VectorOps.prepareStringSpaces(c1.getAllLabels()), collection1, VectorOps.prepareStringSpaces(c2.getAllLabels()), collection2);

//            sim = Math.max(sim, contextSemantic);
          // check if similarity is up to the threshold by string similarity or vector similarity
          if (sim >= t) { // check alignment (low score to preserve concepts below threshold for offsets computation)
            similarConcepts.add(new RecommendedConcept(c2, sim, 1)); // keep similarity
          }
        } // end if (test of valid concepts)

      } // concept2 loop ends

      if (!similarConcepts.isEmpty()) { // sort and select top n +1 (see within)
        Collections.sort(similarConcepts, new RecommendedConcept.RecommendedConceptComparator()); // sort in descending order of score
        int N = n < similarConcepts.size() ? n : similarConcepts.size(); // top 1
        similarConcepts = new ArrayList<>(similarConcepts.subList(0, N));
        candidateAlignments.put(c1, similarConcepts);
        similarityValues.add(similarConcepts.get(0).getScore());
      }
    } // concept1 loop ends

    // Evaluate
    int conceptCount = 0;
    for (Map.Entry<Concept, ArrayList<RecommendedConcept>> entry : candidateAlignments.entrySet()) {
      Concept c1 = entry.getKey();
      System.out.println(++conceptCount + ". Generating features for " + c1);
      ArrayList<RecommendedConcept> selectedConcepts = entry.getValue();
      System.out.println("Count of concepts above threshold " + selectedConcepts.size());

      for (int i = 0; i < selectedConcepts.size(); i++) {
        RecommendedConcept c2 = selectedConcepts.get(i);
        AlignedConcept alignedConcept = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.EXACT_MATCH.value);
        if (c2.getScore() >= t && !AlignedConcept.containsTheAlignment(seenAlignmentList, alignedConcept)) { // continue if similarity is up to threshold and alignment is not selected already
          seenAlignmentList.add(alignedConcept); // add new to list
          String classLabel = "N";
          total++;
          if (Evaluator.trueAlignment(groundTruth, alignedConcept)) { // check if present in the reference alignment
            correct++;
            classLabel = "Y";
          }
          out.add(c1.getLabel() + " vs " + c2.getLabel() + " = " + c2.getScore() + " => " + classLabel);
        } // end if unseen alignment and above similarity threshold
      }
    }
    // summary
    out.forEach(System.out::println);
    System.out.println("Correct => " + correct);
    System.out.println("Total => " + total);
  }

  public static void main(String[] args) {

    String alignmentScheme = "EUROVOC_GEMET"; // also shows alignment order for concepts
    String scheme1 = "EUROVOC";
    String scheme2 = "GEMET";
    List<String> relationTypes = new ArrayList();
    relationTypes.add(Relation.Predicate.EXACT_MATCH.value); // exactMatch is sub-property of closeMatch
//    relationTypes.add(Relation.Predicate.CLOSE_MATCH.value); // non-transitively equivalent concepts

//    String relationType = Relation.Predicate.EXACT_MATCH.value;
//    List<AlignedConcept> groundTruth = Evaluator.getGroundTruth(alignmentScheme, relationTypes);
    List<AlignedConcept> selectedGroundTruth = Evaluator.getGroundTruth(alignmentScheme, relationTypes);
    System.out.println("gold standard size: " + selectedGroundTruth.size());

//    generateFeatures1(scheme1, scheme2, relationTypes, selectedGroundTruth, 5);
//    generateFeatures3(scheme1, scheme2, relationTypes, selectedGroundTruth, 0.5, 1);
//    addGroundTruth(selectedGroundTruth);
//    filterDataset();
    weightedHybridSimilarity(scheme1, scheme2, relationTypes, selectedGroundTruth, 0.5, 1);


    /*
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
    List<Concept> concepts2 = linkedConceptService.getAllConcept("EUROVOC");
//    List<ConceptContext> conceptContexts2 = linkedConceptService.getConceptsAndContext("GEMET", 1); // get scheme 2: gemet
    Map<String, String> c2LabelMap = new HashMap();
    int counter = 0;
    for (Concept c2 : concepts2) {
      for (String c2Label : c2.getAllLabels()) {
        if (c2LabelMap.containsKey(c2Label)) {
          System.out.println("Label => " + c2Label);
          System.out.println(c2LabelMap.get(c2Label));
          System.out.println(c2);
          System.out.println("===");
        }
        c2LabelMap.put(c2Label, c2.getConceptId());
        counter++;
      }
    }

    System.out.println("Size of Map = " + c2LabelMap.size());
    System.out.println("Actual terms seen = " + counter);
     */
  }

}
