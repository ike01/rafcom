package uk.rgu.data.align.oaei;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.model.ConceptContext;
import uk.rgu.data.model.RecommendedConcept;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;
import uk.rgu.data.align.eurovoc.Evaluator;
import uk.rgu.data.utilities.DocOps;
import uk.rgu.data.utilities.FileOps;
import uk.rgu.data.utilities.StringOps;

/**
 *
 * @author aikay
 */
public class RafcomOA_Benc {

  static DecimalFormat df = new DecimalFormat("#.####");
  // path to word embedding model
  static String vectorModelPath = "/dev/rgu/word2vec/models/GoogleNews-vectors-negative300.bin.gz";
//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_plain_model300_min5_iter20_custom_token.txt";
  static String RESULT_DIR = "/results/oaei_benc_google/";

  // Get vectors
  static VectorOps vectorOps = new VectorOps(vectorModelPath);

  public static void main(String[] args) {
    try {
      generateFeatures(0.5, 1);
//      weightedHybridSimilarity(0.76, 1);
    } catch (AlignmentException ex) {
      Logger.getLogger(RafcomOA_Benc.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Retrieves the most similar concepts that are above a threshold.
   *
   * @param t
   * @param n
   */
  public static void generateFeatures(double t, int n) throws AlignmentException { // AlignedConcept alignedConcept,
//    Collection out = new ArrayList<String>(); // for csv output
    List<AlignTrainTest> allTestcaseData = Alignment_oaei.generateBencAlignTrainTest();
    List<AlignedConcept> seenAlignmentList = new ArrayList<>(); // overall list of alignments returned
    ArrayList<ArrayList<String>> listODatasets = new ArrayList<ArrayList<String>>();

    int total = 0;
    int correct = 0;
    int t1 = 0;
    int t2 = 0;
    int t3 = 0;
    int t4 = 0;

    // Header
    String header = "id,match_type,similarity,similarity_offset,vec_sim,hybrid_sim,levenshtein,stoilos,fuzzy_score,metriclcs,tfidf_cosine,dice,monge_elkan,parent_overlap,children_overlap,avg_context_overlap,prefix_overlap,suffix_overlap,context_string_overlap,context_string_overlap_offset,has_parents,has_children,depth_difference,class";
//    out.add(header);

    for (AlignTrainTest alignTrainTest : allTestcaseData) {

    ArrayList<String> dataset = new ArrayList<String>(); // holds alignment of a pair of ontologies
//    File sourceOnto = new File("C:\\dev\\bitbucket\\alignment\\data\\anatomy\\mouse.owl");
//    File targetOnto = new File("C:\\dev\\bitbucket\\alignment\\data\\anatomy\\human.owl");
//    File referenceAlignment = new File("C:\\dev\\bitbucket\\alignment\\data\\anatomy\\reference.rdf");
//    File sourceOnto = new File("C:\\dev\\bitbucket\\rafcom\\data\\2016_benchmark\\101_onto.rdf");
//    File targetOnto = new File("C:\\dev\\bitbucket\\rafcom\\data\\2016_benchmark\\301_onto.rdf");
//    File referenceAlignment = new File("C:\\dev\\bitbucket\\rafcom\\data\\2016_benchmark\\301_refalign.rdf");
      System.out.println("Fetching concepts ... ");
      String sourceScheme = FilenameUtils.removeExtension(alignTrainTest.sourceOnto.getName()).split("_")[0];
      String targetScheme = FilenameUtils.removeExtension(alignTrainTest.targetOnto.getName()).split("_")[0];

      List<OntClass> concepts1 = OntoOps.getOntoClasses(alignTrainTest.sourceOnto.getAbsolutePath());
      List<OntClass> concepts2 = OntoOps.getOntoClasses(alignTrainTest.targetOnto.getAbsolutePath());

//    concepts1.forEach(a -> {
//      System.out.println(a);
//    });
//    concepts2.forEach(System.out::println);
      List<List<String>> collection1 = DocOps.getCollection(concepts1);
      List<List<String>> collection2 = DocOps.getCollection(concepts2);

      collection1.forEach(System.out::println);
      collection2.forEach(System.out::println);

      OntModel sourceModel = OntoOps.getOntologyModel(alignTrainTest.sourceOnto.getAbsolutePath());
      OntModel targetModel = OntoOps.getOntologyModel(alignTrainTest.targetOnto.getAbsolutePath());

//      List<String> doc1 = OntoOps.getOntoTerms(sourceModel); // individual terms in ontology
//      List<String> doc2 = OntoOps.getOntoTerms(targetModel);
      System.out.println("Concept size for " + sourceScheme + " => " + concepts1.size());
      System.out.println("Concept size for " + targetScheme + " => " + concepts2.size());

//      concepts1 = concepts1.subList(10, 20);
//      concepts2 = concepts2.subList(0, 10);

      // ground truth
      AlignmentParser aparser = new AlignmentParser(0);
      Alignment reference = aparser.parse(alignTrainTest.referenceAlignment.toPath().toUri());
      List<AlignedConcept> groundTruth = new ArrayList();
      for (Iterator<Cell> iterator = reference.iterator(); iterator.hasNext();) {
        Cell cell = iterator.next();
        groundTruth.add(new AlignedConcept(cell.getObject1AsURI().toString(), cell.getObject2AsURI().toString(), Relation.Predicate.EXACT_MATCH.value));
      }

  //      List<LinkedConcept> linkedConcepts2 = getAllLinkedConcept(targetModel, concepts2, targetScheme);
      List<ConceptContext> conceptContexts1 = DocOps.getConceptsAndContext(sourceModel, concepts1, sourceScheme); // source
      List<ConceptContext> conceptContexts2 = DocOps.getConceptsAndContext(targetModel, concepts2, targetScheme); // target

      // Parent context : labels of parents of all concepts in scheme
      List<ConceptContext> conceptParents1 = DocOps.getConceptsAndContextParents(sourceModel, concepts1, sourceScheme); // source
      List<ConceptContext> conceptParents2 = DocOps.getConceptsAndContextParents(targetModel, concepts2, targetScheme); // target

      // Children context : labels of children of all concepts in scheme
      List<ConceptContext> conceptChildren1 = DocOps.getConceptsAndContextChildren(sourceModel, concepts1, sourceScheme); // source
      List<ConceptContext> conceptChildren2 = DocOps.getConceptsAndContextChildren(targetModel, concepts2, targetScheme); // target

      // Sibling context : labels of siblings (other children of parents) of all concepts in scheme
      List<ConceptContext> conceptSiblings1 = DocOps.getConceptsAndContextSibling(sourceModel, concepts1, sourceScheme); // source
      List<ConceptContext> conceptSiblings2 = DocOps.getConceptsAndContextSibling(targetModel, concepts2, targetScheme); // target

      for (int typeId = 1; typeId <= 4; typeId++) { // try different types of similarity (switch statement within)
        Map<Concept, ArrayList<RecommendedConcept>> candidateAlignments = new HashMap<Concept, ArrayList<RecommendedConcept>>(); // keeps candidate alignments
        // select concepts wit similarity above chosen threshold as candidate alignment concepts
        for (OntClass ontClass1 : concepts1) {
          ArrayList<RecommendedConcept> similarConcepts = new ArrayList<RecommendedConcept>(); // Similar concepts above a threshold
  //      int selected = 0; // tracks number of concepts selected up to n
          for (OntClass ontClass2 : concepts2) {
            if (ontClass1 != null && ontClass2 != null) { // test for valid concepts (should have a concept id)
  //            double vecSim = StringOps.maxSimilarityByJaccard(OntoOps.getLabels(ontClass1), OntoOps.getLabels(ontClass2)); // maximum cosine similarity
              double sim;
              switch (typeId) {
                case 1:
                  sim = vectorOps.maxHybridSimilarity(OntoOps.getLabels(ontClass1), OntoOps.getLabels(ontClass2));
                  t = 0.4;
                  break;
                case 2:
                  sim = StringOps.maxSimilarityByStoilos(OntoOps.getLabels(ontClass1), OntoOps.getLabels(ontClass2));
                  t = 0.85;
                  break;
                case 3:
                  sim = StringOps.maxSimilarityByTFIDFCosine(OntoOps.getLabels(ontClass1), collection1, OntoOps.getLabels(ontClass2), collection2);
                  t = 0.7;
                  break;
  //                case 4:
  //                  String c1ContextTerms = getContextTermsString(conceptContexts1, new Concept(ontClass1.getURI(), OntoOps.getLabel(ontClass1), sourceScheme));
  //                  String c2ContextTerms = getContextTermsString(conceptContexts2, new Concept(ontClass2.getURI(), OntoOps.getLabel(ontClass2), targetScheme));
  //                  sim = vectorOps.hybridSimilarityV3(c1ContextTerms, c2ContextTerms);
  //                  t = 0.25;
  //                  break;
                case 4:
                  Set<String> c1ParentContextTerms = DocOps.getContextTerms(conceptParents1, ontClass1.getURI());
                  Set<String> c2ParentContextTerms = DocOps.getContextTerms(conceptParents2, ontClass2.getURI());
                  Set<String> c1ChildrenContextTerms = DocOps.getContextTerms(conceptChildren1, ontClass1.getURI());
                  Set<String> c2ChildrenContextTerms = DocOps.getContextTerms(conceptChildren2, ontClass2.getURI());
                  double parentContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ParentContextTerms), VectorOps.prepareStringSpaces(c2ParentContextTerms)); // semantic overlap of parents
                  double childrenContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ChildrenContextTerms), VectorOps.prepareStringSpaces(c2ChildrenContextTerms)); // semantic overlap of children
                  // average of maximum parents and children overlap
                  sim = (parentContextSemanticOverlap + childrenContextSemanticOverlap) / 2;
                  t = 0.2;
                  break;
                default:
                  sim = 0.0;
                  break;
              }
              // check if similarity is up to the threshold by string similarity or vector similarity
              if (sim > 0.0) { // check alignment (low score to preserve concepts below threshold for offsets computation)
                Concept c2 = new Concept(ontClass2.getURI(), OntoOps.getLabel(ontClass2), targetScheme);
  //            selected++;
  //            AlignedConcept ac = new AlignedConcept(Concept.getFullConceptUri(c1.getConceptId()), Concept.getFullConceptUri(c2.getConceptId()), Relation.Predicate.CLOSE_MATCH.value);
                similarConcepts.add(new RecommendedConcept(c2, sim, typeId)); // keep similarity
              }
            } // end if (test of valid concepts)

          } // concept2 loop ends
          if (!similarConcepts.isEmpty()) { // sort and select top n +1 (see within)
            Collections.sort(similarConcepts, new RecommendedConcept.RecommendedConceptComparator()); // sort in descending order of score
            int N = n < similarConcepts.size() ? n + 1 : similarConcepts.size(); // +1 to allow comptuing offsets to next most similar
            similarConcepts = new ArrayList<>(similarConcepts.subList(0, N));
            Concept c1 = new Concept(ontClass1.getURI(), OntoOps.getLabel(ontClass1), sourceScheme);
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
          System.out.println("Count of concepts above threshold " + selectedConcepts.size());
  //        Set<String> similarConceptIds = new HashSet<String>();
  //        for (int i = 0; i < selectedConcepts.size(); i++) {
  //          similarConceptIds.add(selectedConcepts.get(i).getId());
  //        }

          for (int i = 0; i < selectedConcepts.size() - 1; i++) {
            RecommendedConcept c2 = selectedConcepts.get(i);
            AlignedConcept alignedConcept = new AlignedConcept(c1.getId(), c2.getId(), Relation.Predicate.EXACT_MATCH.value);
            if (c2.getScore() >= t && !AlignedConcept.containsTheAlignment(seenAlignmentList, alignedConcept)) { // continue if similarity is up to threshold and alignment is not selected already
              seenAlignmentList.add(alignedConcept); // add new to list
              // best selection similarity
              double hybridSimilarity = vectorOps.maxHybridSimilarity(c1.getAllLabels(), c2.getAllLabels());
              double stoilos = StringOps.maxSimilarityByStoilos(c1.getAllLabels(), c2.getAllLabels());
              double tfidfCosine = StringOps.maxSimilarityByTFIDFCosine(c1.getAllLabels(), collection1, c2.getAllLabels(), collection2);
              Set<String> c1ParentContextTerms = DocOps.getContextTerms(conceptParents1, c1.getId());
              Set<String> c2ParentContextTerms = DocOps.getContextTerms(conceptParents2, c2.getId());
              double parentContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ParentContextTerms), VectorOps.prepareStringSpaces(c2ParentContextTerms));
              if (Double.isNaN(parentContextSemanticOverlap)) {
                parentContextSemanticOverlap = 0.0;
              }
              Set<String> c1ChildrenContextTerms = DocOps.getContextTerms(conceptChildren1, c1.getId());
              Set<String> c2ChildrenContextTerms = DocOps.getContextTerms(conceptChildren2, c2.getId());
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

                Set<String> nextC2ParentContextTerms = DocOps.getContextTerms(conceptParents2, next_c2.getId());
                double nextParentContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ParentContextTerms), VectorOps.prepareStringSpaces(nextC2ParentContextTerms));
                if (Double.isNaN(nextParentContextSemanticOverlap)) {
                  nextParentContextSemanticOverlap = 0.0;
                }
                Set<String> nextC2ChildrenContextTerms = DocOps.getContextTerms(conceptChildren2, next_c2.getId());
                double nextChildrenContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ChildrenContextTerms), VectorOps.prepareStringSpaces(nextC2ChildrenContextTerms));
                if (Double.isNaN(nextChildrenContextSemanticOverlap)) {
                  nextChildrenContextSemanticOverlap = 0.0;
                }
                double nextContextSemantic = (nextParentContextSemanticOverlap + nextChildrenContextSemanticOverlap) / 2;
                maxSim = Math.max(nextContextSemantic, Math.max(nextTfidfCosine, Math.max(nextHybridSimilarity, nextStoilos)));
                next_c2.setScore(maxSim);
              }

              // -1. id: Concept Ids
              String line = c1.getConceptIdFromFullUri() + "-" + c2.getConceptIdFromFullUri() + ","; // an entry in output

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

              // 5. vec_sim_offset
  //              if (null != next_c2) {
  //                double nextMaximumVectorSimilarity = vectorOps.maxSimilarity(VectorOps.prepareStringSpaces(c1.getAllLabels()), VectorOps.prepareStringSpaces(next_c2.getAllLabels()));
  //                line += df.format(maximumVectorSimilarity - nextMaximumVectorSimilarity) + ",";
  //              } else {
  //                line += df.format(maximumVectorSimilarity) + ","; // assumes next is 0.0 if there isn't any
  //              }
              // 4. hybrid_sim
              line += df.format(hybridSimilarity) + ",";

              // 5. levenshtein
              double normalizedLevenshtein = StringOps.maxSimilarityByNormalizedLevenshtein(c1.getAllLabels(), c2.getAllLabels());
              line += df.format(normalizedLevenshtein) + ",";

              // 6. stoilos
              line += df.format(stoilos) + ",";

  //              // 9. stoilos_offset
  //              if (null != next_c2) {
  //                double nextStoilos = StringOps.maxSimilarityByStoilos(c1.getAllLabels(), next_c2.getAllLabels());
  //                line += df.format(stoilos - nextStoilos) + ",";
  //              } else {
  //                line += df.format(stoilos) + ","; // assumes next is 0.0 if there isn't any
  //              }
              // 7. fuzzy_score
              double fuzzy = StringOps.maxSimilarityByFuzzyScore(c1.getAllLabels(), c2.getAllLabels());
              line += df.format(fuzzy) + ",";

              // 8. metriclcs
              double metriclcs = StringOps.maxSimilarityByMetricLCS(c1.getAllLabels(), c2.getAllLabels());
              line += df.format(metriclcs) + ",";

              // 9. tfidf_cosine
              line += df.format(tfidfCosine) + ",";

  //              // 13. tfidf_cosine_offset
  //              if (null != next_c2) {
  //                double nextTfidfCosine = StringOps.maxSimilarityByTFIDFCosine(c1.getAllLabels(), collection1, next_c2.getAllLabels(), collection2);
  //                line += df.format(tfidfCosine - nextTfidfCosine) + ",";
  //              } else {
  //                line += df.format(tfidfCosine) + ",";
  //              }
              // 14. qgram
  //              double qGram = StringOps.maxSimilarityByQGram(c1.getAllLabels(), c2.getAllLabels());
  //              line += df.format(qGram) + ",";
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

  //
              Set<String> c1SiblingContextTerms = DocOps.getContextTerms(conceptSiblings1, c1.getId());
              Set<String> c2SiblingContextTerms = DocOps.getContextTerms(conceptSiblings2, c2.getId());
              double siblingContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1SiblingContextTerms), VectorOps.prepareStringSpaces(c2SiblingContextTerms));

              // 14. avg_context_overlap
              double averageMaxContextOverlap = (parentContextSemanticOverlap + childrenContextSemanticOverlap + siblingContextSemanticOverlap) / 3;
              if (Double.isNaN(averageMaxContextOverlap)) {
                averageMaxContextOverlap = 0.0;
              }
              line += df.format(averageMaxContextOverlap) + ",";

  //               // 20. avg_context_overlap_offset
  //              if (null != next_c2) {
  //                Set<String> next_c2ParentContextTerms = getContextTerms(conceptParents2, next_c2);
  //                Set<String> next_c2ChildrenContextTerms = getContextTerms(conceptChildren2, next_c2);
  //                Set<String> next_c2SiblingContextTerms = getContextTerms(conceptSiblings2, next_c2);
  //                double nextParentContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ParentContextTerms), VectorOps.prepareStringSpaces(next_c2ParentContextTerms));
  //                double nextChildrenContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1ChildrenContextTerms), VectorOps.prepareStringSpaces(next_c2ChildrenContextTerms));
  //                double nextSiblingContextSemanticOverlap = vectorOps.maxHybridSimilarity(VectorOps.prepareStringSpaces(c1SiblingContextTerms), VectorOps.prepareStringSpaces(next_c2SiblingContextTerms));
  //                double nextAverageMaxContextOverlap = (nextParentContextSemanticOverlap + nextChildrenContextSemanticOverlap + nextSiblingContextSemanticOverlap) / 3;
  //                if (Double.isNaN(nextAverageMaxContextOverlap)) {
  //                  nextAverageMaxContextOverlap = 0.0;
  //                }
  //                line += df.format(averageMaxContextOverlap - nextAverageMaxContextOverlap) + ",";
  //              } else {
  //                line += df.format(averageMaxContextOverlap) + ","; // assumes next is 0.0 if there isn't any
  //              }
  //              // 21. prefix
  //              boolean prefix = StringOps.samePrefix(c1.getAllLabels(), c2.getAllLabels());
  //              line += prefix + ",";
  //
  //              // 22. suffix
  //              boolean suffix = StringOps.sameSuffix(c1.getAllLabels(), c2.getAllLabels());
  //              line += suffix + ",";
              // 15. prefix_overlap (Best string prefix overlap)
              double bestPrefixOverlap = StringOps.bestPrefixOverlap(c1.getAllLabels(), c2.getAllLabels());
              line += bestPrefixOverlap + ",";

              // 16. suffix_overlap (Best string suffix overlap)
              double bestSuffixOverlap = StringOps.bestSuffixOverlap(c1.getAllLabels(), c2.getAllLabels());
              line += bestSuffixOverlap + ",";

              // 17. context_string_overlap
              String c1ContextTerms = DocOps.getContextTermsString(conceptContexts1, c1.getId());
              String c2ContextTerms = DocOps.getContextTermsString(conceptContexts2, c2.getId());
              double contextSemanticOverlap = vectorOps.hybridSimilarityV3(c1ContextTerms, c2ContextTerms);
              if (Double.isNaN(contextSemanticOverlap)) {
                contextSemanticOverlap = 0.0;
              }
              line += df.format(contextSemanticOverlap) + ",";

              // 18. context_string_overlap_offset
              if (null != next_c2) {
                String next_c2ContextTerms = DocOps.getContextTermsString(conceptContexts2, next_c2.getId());
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
              if (!c1ParentContextTerms.isEmpty() && !c2ParentContextTerms.isEmpty()) {
                line += "both" + ","; // both
              } else if (!c1ParentContextTerms.isEmpty() || !c2ParentContextTerms.isEmpty()) {
                line += "one" + ","; // one
              } else {
                line += "none" + ","; // none
              }
              // 20. has_children
              if (!c1ChildrenContextTerms.isEmpty() && !c2ChildrenContextTerms.isEmpty()) {
                line += "both" + ",";
              } else if (!c1ChildrenContextTerms.isEmpty() || !c2ChildrenContextTerms.isEmpty()) {
                line += "one" + ",";
              } else {
                line += "none" + ",";
              }

  //              // 29. has_sibling
  //              if (!c1SiblingContextTerms.isEmpty() && !c2SiblingContextTerms.isEmpty())
  //                line += "Both" + ",";
  //              else if (!c1SiblingContextTerms.isEmpty() || !c2SiblingContextTerms.isEmpty())
  //                line += "One" + ",";
  //              else
  //                line += "None" + ",";
              // 21. depth_difference (Difference in relative depth in hierarchical path)
              double c1depth = OntoOps.getRelativeDepthInPath(sourceModel, c1.getId());
              double c2depth = OntoOps.getRelativeDepthInPath(targetModel, c2.getId());
              line += Math.abs(c1depth - c2depth) + ",";

              // 22. class (label) (aligned = "Y", not aligned = "N")
              String classLabel = "N";
              total++;
              if (Evaluator.trueAlignment(groundTruth, alignedConcept)) { // check if present in the reference alignment
                correct++;
                classLabel = "Y";
                if (typeId == 1) {
                  t1++;
                }
                if (typeId == 3) {
                  t3++;
                  System.out.println("T3: " + c1.getLabel() + " vs " + c2.getLabel() + " = " + c2.getScore());
                }
                if (typeId == 2) {
                  t2++;
                }
                if (typeId == 4) {
                  t4++;
                }
              }
              line += classLabel;
              dataset.add(line);
  //              System.out.println(line);
  //              out.add(line); // add to output
            } // end if unseen alignment and above similarity threshold
          }
        }
      } // end similarity type loop
      listODatasets.add(dataset);
    }
    // Write output to file
//    String fileName = "alignment_features_oaei" + n + "_(exact_match).csv";
//    FileOps.printResults(out, fileName);
    // generate train/test dataset
    generateLeaveOneOutDatasets(header, listODatasets);
    generateDatasets(header, listODatasets);
//    System.out.println("Correct => " + correct);
//    System.out.println("Total => " + total);
//    System.out.println("contribution Type1 => " + t1);
//    System.out.println("contribution Type2 => " + t2);
//    System.out.println("contribution Type3 => " + t3);
//    System.out.println("contribution Type4 => " + t4);
  }


  /**
   * Generates leave-one-out-cross-validation dataset.
   * @param header
   * @param listODatasets
   */
  private static void generateLeaveOneOutDatasets(String header, ArrayList<ArrayList<String>> listODatasets) {
    for (int i = 0; i < listODatasets.size(); i++) {
      Collection trainData = new ArrayList<>();
      trainData.add(header);
      Collection testData = new ArrayList<>();
      testData.add(header);
      for (int j = 0; j < listODatasets.size(); j++) {
        if (i == j) {
          testData.addAll(listODatasets.get(i));
        } else {
          trainData.addAll(listODatasets.get(j));
        }
      }
      String train = RESULT_DIR + (i + 1) + "/train.csv";
      String test = RESULT_DIR + (i + 1) + "/test.csv";
      FileOps.printResults(trainData, train);
      FileOps.printResults(testData, test);
    }
  }

  public static void generateDatasets(String header, ArrayList<ArrayList<String>> listODatasets) {
    Collection trainData = new ArrayList<>();
    trainData.add(header);
    for (int i = 0; i < listODatasets.size(); i++) {
      trainData.addAll(listODatasets.get(i));
    }
    String all = RESULT_DIR + "all.csv";
    FileOps.printResults(trainData, all);
  }

}
