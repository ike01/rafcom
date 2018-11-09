package uk.rgu.data.ontologyprocessor.word2vec;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.ontologyprocessor.Alignment;
import uk.rgu.data.ontologyprocessor.Alignment.AlignmentGraph;
import uk.rgu.data.ontologyprocessor.Ontology;
import uk.rgu.data.ontologyprocessor.RDFManager;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.services.LinkedConceptService;
import uk.rgu.data.utilities.FileOps;
import uk.rgu.data.utilities.StringOps;

/**
 *
 * @author aikay
 */
public class UseConceptVectors {

  private static final RDFManager rdfStore = new RDFManager();
  static DecimalFormat df = new DecimalFormat("#.0000");
  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_annotated_model300_min10_iter5_custom_token.txt";

  public static void main(String[] args) {
//    String scheme = "GEMET";
//    VectorOps vectorOps = new VectorOps("model/geo_hascontext2_model.txt");
//    // Concept access service
//    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
//    List<Concept> concepts = linkedConceptService.getAllConcept(scheme);
//    System.out.println("Concept size => " + concepts.size());
//    for (Concept concept : concepts) {
//
//    }

//    VectorOps vectorOps = new VectorOps("model/geo_wiki_model.txt");
//    vectorOps.nearestWords("hes", 10);
//    vectorOps.nearestWords("im", 10);
//    vectorOps.nearestWords("theyll", 10);
//    alignments();
//    System.out.println(VectorOps.prepareString("child's_rights (gemet)"));
    alignments("EUROVOC_GEMET", "EUROVOC", "GEMET");
//Set<String> cnpt2Lbl = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph("GEMET"), "http://www.eionet.europa.eu/gemet/concept/4030").getAllLabels();
//cnpt2Lbl.forEach(System.out::println);
  }

  public static void alignments(String alignmentScheme, String scheme1, String scheme2) {
    String outFile = scheme1 + "-" + scheme2 + ".csv";
    Collection lines = new ArrayList<String>();
    String outTitle = "id," + scheme1 + "_(c1),c1_labels,nearest_terms_from_" + scheme2 + "_(c2),alignment,c2_labels,exact_string_match,exact_match_has_vectors,c1_and_c2_has_vectors,in_nearest_terms,rank";
    lines.add(outTitle);

    // Get alignments
    List<AlignedConcept> alignedConcept = rdfStore.getAlignments(alignmentScheme, Relation.Predicate.EXACT_MATCH.value);
    // Get vectors
    VectorOps vectorOps = new VectorOps(vectorModelPath);

    int n = 10; // n nearest terms in vector space

    // Concept access service
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

    // for each concept in scheme 1, find labels of scheme 2 that are closest to any of its labels
    int id = 0;
    for (Concept c1 : concepts1) { // loop through eurovoc
      // 0. conceptId
      id++;
      String outLine = id + "," + c1.getConceptId(); // output line.
      outLine += ",";

//      int current = 0; // counter for want of a method for selecting sub-map
      // 1. add concept labels
      for (String label : c1.getAllLabels()) {
        outLine += label.replaceAll(",", "") + ";";
      }
      outLine += ",";

      // 2. top n closest labels from other concept
//      System.out.print("\n" + StringOps.stripAllParentheses(c1.getLabel()).replaceAll("\\s+", "_").toLowerCase() + " :\t");
//      Map<String, Double> lhm = vectorOps.nearestWordsByHighestSimilarity(c1.getAllLabels(), c2LabelMap.keySet(), n); // Map = word, similarity: gemet terms that are nearest to eurovoc concept terms
      Map<String, Double> lhm = vectorOps.nearestWordsByCumulativeSimilarity(c1.getAllLabels(), c2LabelMap.keySet(), n); // Map = word, similarity: gemet terms that are nearest to eurovoc concept terms
      for (Map.Entry<String, Double> entry : lhm.entrySet()) {
//        current++;
        String term = entry.getKey(); // word
        double similarity = entry.getValue(); // similarity
        outLine += term.replaceAll(",", "") + "(" + c2LabelMap.get(term) + "-" + df.format(similarity) + ");";
//        System.out.print(key + "(" + df.format(value) + "), ");
//        if (current == 1) {
//          topRankedConcept = Concept.getFullConceptUri(c2LabelMap.get(key));
//        }
//        if (current == n) {
//          break;
//        }
      }
      outLine += ",";

      // 3. exact match relation count
      List<AlignedConcept> currentAlignments = AlignedConcept.containsAlignment(alignedConcept, c1.getFullConceptUri());
//      int exactRelationCount = currentAlignments.size();
//      outLine += exactRelationCount + ",";

      for (AlignedConcept ac : currentAlignments) {
        String tempStr = ""; // add for each of a concepts alignment
        Set<String> cnpt1Lbl = c1.getAllLabels(); // eurovoc concept labels
        Concept c2 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme2), ac.concept_2.toString()); // get alignend concept: gemet concept
        tempStr += c1.getConceptId() + " - " + StringOps.getLastUriValue(c2.getConceptId()) + ",";
//        tempStr += "TRUE,"; // exact relation match
        Set<String> cnpt2Lbl = c2.getAllLabels(); // get labels of aligned concept: gemet concept labels
        // add concept labels of c2
        for (String label : cnpt2Lbl) { // gemet labels
          tempStr += label.replaceAll(",", "") + ";";
        }
        tempStr += ",";
        boolean exactStringMatch = checkStringMatch(cnpt1Lbl, cnpt2Lbl, false); // check it is an exact string match
        tempStr += exactStringMatch + ","; // exact string match
        // check that exact match have vectors
        boolean exactMatchHaveVectors = checkMatchHaveVectors(vectorOps, cnpt1Lbl, cnpt2Lbl);
        tempStr += exactMatchHaveVectors + ","; // exact string match have vectors?
        // check that vectors exist for at least a pair of concept terms to be aligned
        if (vectorOps.hasVector(cnpt1Lbl) && vectorOps.hasVector(cnpt2Lbl)) {
          tempStr += "TRUE,";
        } else {
          tempStr += "FALSE,";
        }
        // 5. match is included in top n?
        boolean matchedInTopN = checkStringMatch(cnpt2Lbl, lhm.keySet(), true); // gemet & nearest_words
        tempStr += matchedInTopN + ",";

        // 6. ranking of actual match (if included)
        int rankOfMatch = matchRankPosition(lhm, cnpt2Lbl);
        tempStr += rankOfMatch;

        lines.add(outLine + tempStr);
      }
      /*
      // 4. labels are exact string match ?
      if (exactRelationCount > 0) { // if there are exact/equivalent relations
        // get concept labels that match and compare
        AlignedConcept ac = currentAlignments.get(0); // for now, assume 1 match
        Set<String> cnpt1Lbl = c1.getAllLabels();
        Concept c2 = rdfStore.getConceptUsingFullConceptId(Ontology.getGraph(scheme2), ac.concept_2);
        Set<String> cnpt2Lbl = c2.getAllLabels();
        boolean exactStringMatch = checkStringMatch(cnpt1Lbl, cnpt2Lbl, false);
        outLine += exactStringMatch + ",";

        //4.b. matches
        outLine += c1.getConceptId() + " - " + c2.getConceptId() + ",";

        // 5. match is included in top n?
        boolean matchedInTopN = checkStringMatch(cnpt1Lbl, lhm.keySet(), true);
        outLine += matchedInTopN + ",";

        // 6. ranking of actual match (if included)
        if (matchedInTopN) {
          int rankOfMatch = matchRankPosition(lhm, cnpt2Lbl);
          outLine += rankOfMatch;
        }

      } else {
        outLine += ",,"; //  for 4, 5, 6
      }

      lines.add(outLine); // add to output
    }
       */
    }
    // print output
    FileOps.printResults(lines, outFile, false);
  }

  public static void coverage() {
    VectorOps vectorOps = new VectorOps("model/geo_wiki_model.txt");
    // Concept access service
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());

    String scheme = "NEW_THESAURUS";
    List<Concept> concepts = linkedConceptService.getAllConcept(scheme);
    System.out.println("Concept size => " + concepts.size());

    int zeroCount = 0;
    int oneCount = 0;
    int twoCount = 0;

    for (Concept concept : concepts) {
      String conceptId = concept.getConceptId();
      String prefLabel = concept.getLabel();
      int repCode = 0;
      // check if there is a vector for the preferred label
      if (vectorOps.hasVector(prefLabel)) {
        repCode = 1;
        oneCount++;
      } // check if there is vector for any of the alternative labels
      else if (vectorOps.hasVector(concept.getAltLabels())) {
        repCode = 2;
        twoCount++;
      } else {
        zeroCount++;
      }
      // rep coding: 0 - none, 1 - preferred label, 2 - alternative label
      // concept_id, pref-label, rep
      System.out.println(conceptId + "," + prefLabel + "," + repCode);
    }
    System.out.println("===SUMMARY===");
    System.out.println("zero = " + zeroCount);
    System.out.println("one = " + oneCount);
    System.out.println("two = " + twoCount);

  }

  /**
   * Compares strings for exact match (case insensitive). Additional
   * pre-processing is done to string when vectorLabel is set to true.
   *
   * @param list1
   * @param list2
   * @param vectorLabel
   * @return
   */
  public static boolean checkStringMatch(Set<String> list1, Set<String> list2, boolean vectorLabel) {
    boolean match = false;
    for (String s1 : list1) {
      for (String s2 : list2) {
        if (vectorLabel) {
          s1 = VectorOps.prepareStringUnderscores(s1);
          s2 = VectorOps.prepareStringUnderscores(s2);
        }
        if (s1.equalsIgnoreCase(s2)) {
          return true;
        }
      }
    }

    return match;
  }

  /**
   * Compares strings for exact match (case insensitive) after parentheses,
   * dashes and underscores have been removed. Further pre-processing is done to
   * strings if vectorLabel is set to true.
   *
   * @param list1
   * @param list2
   * @param vectorLabel
   * @return
   */
  public static boolean checkStringMatch2(Set<String> list1, Set<String> list2, boolean vectorLabel) {
    boolean match = false;
    for (String s1 : list1) {
      s1 = s1.replaceAll("-", " "); // remove dashes
      s1 = s1.replaceAll("_", " "); // remove underscores
      s1 = StringOps.stripAllParentheses(s1); // remove parentheses
      for (String s2 : list2) {
        s2 = s2.replaceAll("-", " "); // remove dashes
        s2 = s2.replaceAll("_", " "); // remove underscores
        s2 = StringOps.stripAllParentheses(s2); // remove parentheses
        if (vectorLabel) {
          s1 = VectorOps.prepareStringUnderscores(s1);
          s2 = VectorOps.prepareStringUnderscores(s2);
        }
        if (s1.equalsIgnoreCase(s2)) {
          return true;
        }
      }
    }

    return match;
  }

  public static boolean checkMatchHaveVectors(VectorOps vectorOps, Set<String> list1, Set<String> list2) {
    boolean hasVec = false;
    for (String s1 : list1) {
      for (String s2 : list2) {
        if (s1.equalsIgnoreCase(s2)) {
          return vectorOps.hasVector(VectorOps.prepareStringUnderscores(s1));
        }
      }
    }

    return hasVec;
  }

  private static int matchRankPosition(Map<String, Double> sortedMap, Set<String> alignedConceptLabels) {
    int count = 0;
    int rank = 0;
    double lastSim = 10.0; // possible max (can accommodate up to ~10-15 alternative labels for a concept)
    for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
      count++;
      String ecl = entry.getKey().trim();
      double sim = entry.getValue();
      if (sim < lastSim) { // allows for ties by having same rank when sim does not change
        rank = count;
        lastSim = sim;
      }

      for (String acl : alignedConceptLabels) {
        System.out.println("acl : " + acl);
        System.out.println("entry : " + ecl);
        acl = VectorOps.prepareStringUnderscores(acl);
        ecl = VectorOps.prepareStringUnderscores(ecl);
        if (acl.equalsIgnoreCase(ecl)) {
          return rank;
        }
      }
    }

    return -1; // concept label is not in map
  }

}
