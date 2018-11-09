package uk.rgu.data.align.oaei;

import com.hp.hpl.jena.ontology.OntClass;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.ontowrap.OntowrapException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.PreAlignedConcept;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;
import uk.rgu.data.align.eurovoc.Evaluator;
import uk.rgu.data.utilities.StringOps;

/**
 *
 * @author 1113938
 */
public class Hybrid extends ObjectAlignment implements AlignmentProcess {

    static DecimalFormat df = new DecimalFormat("#.####");
    // path to word embedding model
  static String vectorModelPath = "C:/dev/rgu/word2vec/models/GoogleNews-vectors-negative300.bin.gz";
//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_plain_model300_min10_iter5_custom_token.txt";
//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_plain_model300_min5_iter20_custom_token.txt";

  // Get vectors
  static VectorOps vectorOps = new VectorOps(vectorModelPath);

  @Override
  public void align(Alignment a, Properties prprts) throws AlignmentException {
    try {
      // Match classes
      for (Object cl2 : ontology2().getClasses()) {
        for (Object cl1 : ontology1().getClasses()) {
          // add mapping into alignment object
          addAlignCell(cl1, cl2, "=", match(cl1, cl2));
        }
      }
      // Match dataProperties
//      for (Object p2 : ontology2().getDataProperties()) {
//        for (Object p1 : ontology1().getDataProperties()) {
//          // add mapping into alignment object
//          addAlignCell(p1, p2, "=", match(p1, p2));
//        }
//      }
      // Match objectProperties
//      for (Object p2 : ontology2().getObjectProperties()) {
//        for (Object p1 : ontology1().getObjectProperties()) {
//          // add mapping into alignment object
//          addAlignCell(p1, p2, "=", match(p1, p2));
//        }
//      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public double match(Object o1, Object o2) throws AlignmentException {
//    double t = .69; // vector similarity threshold for alignment
    try {
      String s1 = ontology1().getEntityName(o1);
      String s2 = ontology2().getEntityName(o2);

      if (s1 == null || s2 == null) {
        System.out.println("a string is null");
        return 0.;
      } else {
        // Edit distance
        Set<String> set1 = new HashSet<String>();
        set1.add(s1);
        Set<String> set2 = new HashSet<String>();
        set2.add(s2);
        double levSim = StringOps.maxSimilarityByNormalizedLevenshtein(set1, set2);

        // Vector similarity
        // convert from camelCase to human-readable
        s1 = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(s1), ' ');
        s2 = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(s2), ' ');

        s1 = s1.replaceAll("\\s-\\s", "-"); // e.g. reverts "co - author" to "co-author"
        s2 = s2.replaceAll("\\s-\\s", "-");

        // prepare for lookup in word embedding vocabulary
        s1 = VectorOps.prepareStringSpaces(s1);
        s2 = VectorOps.prepareStringSpaces(s2);

//        double vecSim = vectorOps.hybridSimilarity(s1, s2);
        double vecSim = vectorOps.sentenceSimilarity(s1, s2);

//        return vecSim;
        return levSim > vecSim ? levSim : vecSim; // return greater of the 2 similarities
      }
    } catch (OntowrapException owex) {
      throw new AlignmentException("Error getting entity name", owex);
    }
  }

  public static void dummyMatch() throws AlignmentException {
    double minSimilarity = 0.7;
    double maxSimilarity = 0.9;
    Collection evaluationResults = new ArrayList<String>();

    List<AlignTrainTest> allTestcaseData = Alignment_oaei.generateConfAlignTrainTest();

    // test alignment performance at different thresholds
    for (double ht = minSimilarity; ht <= maxSimilarity; ht += 0.01) { // try different thresholds
      List<Integer> found = new ArrayList();
      List<Integer> correct = new ArrayList();
      List<Integer> expected = new ArrayList();

      for (AlignTrainTest alignTrainTest : allTestcaseData) {
        List<OntClass> concepts1 = OntoOps.getOntoClasses(alignTrainTest.sourceOnto.getAbsolutePath());
        List<OntClass> concepts2 = OntoOps.getOntoClasses(alignTrainTest.targetOnto.getAbsolutePath());
        // ground truth
        AlignmentParser aparser = new AlignmentParser(0);
        Alignment reference = aparser.parse(alignTrainTest.referenceAlignment.toPath().toUri());
        List<AlignedConcept> groundTruth = new ArrayList();
        for (Iterator<Cell> iterator = reference.iterator(); iterator.hasNext();) {
          Cell cell = iterator.next();
          groundTruth.add(new AlignedConcept(cell.getObject1AsURI().toString(), cell.getObject2AsURI().toString(), Relation.Predicate.EXACT_MATCH.value));
        }

        List<PreAlignedConcept> preAlignedConcepts = new ArrayList<PreAlignedConcept>(); // list to contain minimum threshold and above (to avoid multiple processing)

        for (OntClass c1 : concepts1) {
          for (OntClass c2 : concepts2) {
//          double stringSim = StringOps.maxSimilarityByNormalizedLevenshtein(c1.getAllLabels(), c2.getAllLabels()); // maximum string similarity
            double hybridSim = vectorOps.maxHybridSimilarity(OntoOps.getLabels(c1), OntoOps.getLabels(c2)); // maximum cosine similarity
            // check if similarity is up to the threshold by string similarity or vector similarity
//          if (stringSim >= minSimilarity || vecSim >= minSimilarity) { // check alignment
            if (hybridSim >= minSimilarity) { // check alignment
              AlignedConcept ac = new AlignedConcept(c1.getURI(), c2.getURI(), Relation.Predicate.CLOSE_MATCH.value);
              preAlignedConcepts.add(new PreAlignedConcept(ac, hybridSim));
//              preAlignedConcepts = PreAlignedConcept.updateAlignments(preAlignedConcepts, new PreAlignedConcept(ac, hybridSim));
            }

          } // concept2 loop ends
        } // concept1 loop ends

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

        found.add(recommendedAlignments.size());
        correct.add(Evaluator.getCorrect(groundTruth, recommendedAlignments));
        expected.add(alignTrainTest.expectedClassCount);

//System.out.println("Ground truth size => " + groundTruth.size());
//System.out.println("Expected size => " + alignTrainTest.expectedClassCount);
//if (groundTruth.size() != alignTrainTest.expectedClassCount) System.exit(1);
        // write out results of using these thresholds
//        System.out.println("hybrid similarity threshold=" + ht + ",Alignments found=" + recommendedAlignments.size());
//        String result = Evaluator.evaluate(groundTruth, recommendedAlignments);
//        System.out.println(result);
//        evaluationResults.add("hybrid similarity threshold=" + ht + ",Alignments found=" + recommendedAlignments.size());
//        evaluationResults.add(result);
//        System.out.println();
      }

      double precision = HarmonicPR.hPrecision(correct, found);
      double recall = HarmonicPR.hRecall(correct, expected);
      double f1 = 2 * precision * recall / (precision + recall);

      evaluationResults.add("Threshold = " + ht + " | H(p) = " + precision + ", H(r) = " + recall + ", H(fm) = " + f1 + " | Alignments expected " + HarmonicPR.sum(expected) + ", found " + HarmonicPR.sum(found));

    }

    // print results
    evaluationResults.forEach(System.out::println);
  }



//  public static void weightedHybridSimilarity(double t, int n) throws AlignmentException { // AlignedConcept alignedConcept,
//    Collection out = new ArrayList<String>(); // for csv output
//    List<AlignTrainTest> allTestcaseData = Alignment_oaei.generateConfAlignTrainTest();
//    List<AlignedConcept> seenAlignmentList = new ArrayList<>(); // overall list of alignments returned
//
//    int total = 0;
//    int correct = 0;
//
//    for (AlignTrainTest alignTrainTest : allTestcaseData) {
//      List<Double> similarityValues = new ArrayList<>();
//      System.out.println("Fetching concepts ... ");
//      String sourceScheme = FilenameUtils.removeExtension(alignTrainTest.referenceAlignment.getName()).split("-")[0];
//      String targetScheme = FilenameUtils.removeExtension(alignTrainTest.referenceAlignment.getName()).split("-")[1];
//
//      List<OntClass> concepts1 = OntoOps.getOntoClasses(alignTrainTest.sourceOnto.getAbsolutePath());
//      List<OntClass> concepts2 = OntoOps.getOntoClasses(alignTrainTest.targetOnto.getAbsolutePath());
//
//      List<List<String>> collection1 = getCollection(concepts1);
//      List<List<String>> collection2 = getCollection(concepts2);
//
//      // collection similarity
////      double collectionSim = compareCollection(collection1, collection2);
//
//      System.out.println("Concept size for " + sourceScheme + " => " + concepts1.size());
//      System.out.println("Concept size for " + targetScheme + " => " + concepts2.size());
//
////      System.out.println("Similarity: " + collectionSim);
////      int minVocab = concepts1.size() < concepts2.size() ? concepts1.size() : concepts2.size();
////      t = findThreshold(collectionSim, similarityValues, minVocab);
////      System.out.println("Threshold: " + t);
//
//      // ground truth
//      AlignmentParser aparser = new AlignmentParser(0);
//      Alignment reference = aparser.parse(alignTrainTest.referenceAlignment.toPath().toUri());
//      List<AlignedConcept> groundTruth = new ArrayList();
//      for (Iterator<Cell> iterator = reference.iterator(); iterator.hasNext();) {
//        Cell cell = iterator.next();
//        groundTruth.add(new AlignedConcept(cell.getObject1AsURI().toString(), cell.getObject2AsURI().toString(), Relation.Predicate.EXACT_MATCH.value));
//      }
//
//      Map<Concept, ArrayList<RecommendedConcept>> candidateAlignments = new HashMap<Concept, ArrayList<RecommendedConcept>>(); // keeps candidate alignments
//      // select concepts wit similarity above chosen threshold as candidate alignment concepts
//      for (OntClass ontClass1 : concepts1) {
//        ArrayList<RecommendedConcept> similarConcepts = new ArrayList<RecommendedConcept>(); // Similar concepts above a threshold
//        for (OntClass ontClass2 : concepts2) {
//          if (ontClass1 != null && ontClass2 != null) { // test for valid concepts (should have a concept id)
//            double sim = vectorOps.maxWeightedHybridSimilarity(OntoOps.getLabels(ontClass1), collection1, OntoOps.getLabels(ontClass2), collection2);
//
////            sim = Math.max(sim, contextSemantic);
//            // check if similarity is up to the threshold by string similarity or vector similarity
//            if (sim >= t) { // check alignment (low score to preserve concepts below threshold for offsets computation)
//              Concept c2 = new Concept(ontClass2.getURI(), OntoOps.getLabel(ontClass2), targetScheme);
//              similarConcepts.add(new RecommendedConcept(c2, sim, 1)); // keep similarity
//            }
//          } // end if (test of valid concepts)
//
//        } // concept2 loop ends
//
//        if (!similarConcepts.isEmpty()) { // sort and select top n +1 (see within)
//          Collections.sort(similarConcepts, new RecommendedConcept.RecommendedConceptComparator()); // sort in descending order of score
//          int N = n < similarConcepts.size() ? n : similarConcepts.size(); // top 1
//          similarConcepts = new ArrayList<>(similarConcepts.subList(0, N));
//          Concept c1 = new Concept(ontClass1.getURI(), OntoOps.getLabel(ontClass1), sourceScheme);
//          candidateAlignments.put(c1, similarConcepts);
//          similarityValues.add(similarConcepts.get(0).getScore());
//        }
//      } // concept1 loop ends
//
//      // Evaluate
//      int conceptCount = 0;
//      for (Map.Entry<Concept, ArrayList<RecommendedConcept>> entry : candidateAlignments.entrySet()) {
//        Concept c1 = entry.getKey();
//        System.out.println(++conceptCount + ". Generating features for " + c1);
//        ArrayList<RecommendedConcept> selectedConcepts = entry.getValue();
//        System.out.println("Count of concepts above threshold " + selectedConcepts.size());
//
//        for (int i = 0; i < selectedConcepts.size(); i++) {
//          RecommendedConcept c2 = selectedConcepts.get(i);
//          AlignedConcept alignedConcept = new AlignedConcept(c1.getId(), c2.getId(), Relation.Predicate.EXACT_MATCH.value);
//          if (c2.getScore() >= t && !AlignedConcept.containsTheAlignment(seenAlignmentList, alignedConcept)) { // continue if similarity is up to threshold and alignment is not selected already
//            seenAlignmentList.add(alignedConcept); // add new to list
//
////            Set<String> c1ParentContextTerms = getContextTerms(conceptParents1, c1.getId());
////            Set<String> c2ParentContextTerms = getContextTerms(conceptParents2, c2.getId());
////            double parentContextSemanticOverlap = StringOps.maxSimilarityByStoilos(VectorOps.prepareStringSpaces(c1ParentContextTerms), VectorOps.prepareStringSpaces(c2ParentContextTerms));
////            if (c1ParentContextTerms.isEmpty() || c2ParentContextTerms.isEmpty()) {
////              parentContextSemanticOverlap = -1;
////            }
////            Set<String> c1ChildrenContextTerms = getContextTerms(conceptChildren1, c1.getId());
////            Set<String> c2ChildrenContextTerms = getContextTerms(conceptChildren2, c2.getId());
////            double childrenContextSemanticOverlap = StringOps.maxSimilarityByStoilos(VectorOps.prepareStringSpaces(c1ChildrenContextTerms), VectorOps.prepareStringSpaces(c2ChildrenContextTerms));
////            if (c1ChildrenContextTerms.isEmpty() || c2ChildrenContextTerms.isEmpty()) {
////              childrenContextSemanticOverlap = -1;
////            }
////            double contextSemantic = (parentContextSemanticOverlap + childrenContextSemanticOverlap) / 2;
//
//            String classLabel = "N";
//            total++;
//            if (Evaluator.trueAlignment(groundTruth, alignedConcept)) { // check if present in the reference alignment
//              correct++;
//              classLabel = "Y";
//            }
//            out.add(c1.getLabel() + " vs " + c2.getLabel() + " = " + c2.getScore() + " => " + classLabel);
//          } // end if unseen alignment and above similarity threshold
//        }
//      }
//
//    }
//    // summary
//    out.forEach(System.out::println);
//    System.out.println("Correct => " + correct);
//    System.out.println("Total => " + total);
//  }

  public static void main(String[] args) {
    try {
      dummyMatch();
    } catch (AlignmentException ex) {
      Logger.getLogger(Hybrid.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

//  public static void main2(String[] args) {
//    File f1 = new File("data/2016_conference/cmt.owl");
//    File f2 = new File("data/2016_conference/Conference.owl");
//    File f3 = new File("data/2016_conference/reference-alignment/cmt-Conference.rdf");
//
//    AlignTrainTest a = new AlignTrainTest(f1, f2, f3, "");
//    OntModel sourceModel = OntoOps.getOntologyModel(a.sourceOnto.getAbsolutePath());
//    String sourceScheme = FilenameUtils.removeExtension(a.referenceAlignment.getName()).split("-")[0];
//    List<OntClass> concepts1 = OntoOps.getOntoClasses(a.sourceOnto.getAbsolutePath());
//    List<LinkedConcept> linkedConcepts2 = getAllLinkedConcept(sourceModel, concepts1, sourceScheme);
//    System.out.println("size = " + linkedConcepts2.size());
//    for (OntClass oc : concepts1) {
//      Concept c1 = new Concept(oc.getURI(), OntoOps.getLabel(oc), sourceScheme);
//      System.out.println(c1);
//
//      Set<String> contextConceptsIds = getContextIds(linkedConcepts2, c1);
//      contextConceptsIds.forEach(System.out::println);
//    }
//
//  }
}
