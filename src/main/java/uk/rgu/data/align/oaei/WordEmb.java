package uk.rgu.data.align.oaei;

import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.ontowrap.OntowrapException;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;

/**
 *
 * @author 1113938
 */
public class WordEmb extends ObjectAlignment implements AlignmentProcess {

//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_plain_model300_min10_iter5_custom_token.txt";
  static String vectorModelPath = "C:/dev/rgu/word2vec/models/GoogleNews-vectors-negative300.bin.gz";
//  static String vectorModelPath = "data/geo_hascontext1_model.txt";
  // Get vectors
  static VectorOps vectorOps = new VectorOps(vectorModelPath);
//  List<PreAlignedConcept> preAlignedConcepts = new ArrayList<PreAlignedConcept>(); // list to contain minimum threshold and above (to avoid multiple processing)

  public static void main(String[] args) {

    String s1 = "co-author";
    s1 = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(s1), " ");
    System.out.println(s1);
    s1 = s1.replaceAll("\\s-\\s", "-");
    System.out.println(s1);
    System.out.println(vectorOps.sentenceSimilarity(s1, "author"));
  }

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
//      System.out.println(s1 + " vs " + s2);

      if (s1 == null || s2 == null) {
        System.out.println("a string is null");
        return 0.;
      } else {
        // convert from camelCase to human-readable
        s1 = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(s1), ' ');
        s2 = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(s2), ' ');

        s1 = s1.replaceAll("\\s-\\s", "-"); // e.g. reverts "co - author" to "co-author"
        s2 = s2.replaceAll("\\s-\\s", "-");

        // prepare for lookup in word embedding vocabulary
        s1 = VectorOps.prepareStringSpaces(s1);
        s2 = VectorOps.prepareStringSpaces(s2);
        double sim = vectorOps.sentenceSimilarity(s1, s2);
//        if (sim > 0.8) {
//          System.out.println(s1 + " VS " + s2 + " = " + sim);
//        }
        return sim;
      }
    } catch (OntowrapException owex) {
      throw new AlignmentException("Error getting entity name", owex);
    }
  }

}
