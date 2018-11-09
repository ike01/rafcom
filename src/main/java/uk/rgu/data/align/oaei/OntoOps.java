package uk.rgu.data.align.oaei;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Profile;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;
import uk.rgu.data.utilities.StringOps;

/**
 *
 * @author 1113938
 */
public class OntoOps {

  private static final Logger LOG = Logger.getLogger(OntoOps.class.getName());

  public static OntModel getOntologyModel(String ontoFile) {
    OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
    try {
      InputStream in = FileManager.get().open(ontoFile);
      try {
        ontoModel.read(in, null);
      } catch (Exception e) {
        e.printStackTrace();
      }
      LOG.log(Level.INFO, "Ontology {0} loaded.", ontoFile);
    } catch (JenaException je) {
      System.err.println("ERROR" + je.getMessage());
      je.printStackTrace();
      System.exit(0);
    }

    return ontoModel;
  }

  public static List<OntClass> getOntoClasses(String ontoFile) {
    OntModel ontoModel = getOntologyModel(ontoFile);

    return ontoModel.listNamedClasses().toList();
  }

  public static List<OntClass> getSubClasses(OntModel ontModel, String uri) {
    List<OntClass> ls = new ArrayList<OntClass>();
    List<OntClass> allClasses = ontModel.listNamedClasses().toList(); // all named classes
    OntClass node = ontModel.getOntClass(uri);
    for (OntClass c : node.listSubClasses().toList()) {
      if (allClasses.contains(c)) { // select named classes only
        ls.add(c);
      }
    }

    return ls;
  }

  public static List<OntClass> getSuperClasses(OntModel ontModel, String uri) {
    List<OntClass> ls = new ArrayList<OntClass>();
    List<OntClass> allClasses = ontModel.listNamedClasses().toList(); // all named classes
    OntClass node = ontModel.getOntClass(uri);
    for (OntClass c : node.listSuperClasses().toList()) {
      if (allClasses.contains(c)) { // select named classes only
        ls.add(c);
      }
    }

    return ls;
  }

  public static List<OntClass> getSiblingClasses(OntModel ontModel, String uri) {
    List<OntClass> ls = new ArrayList<OntClass>();
    List<OntClass> allClasses = ontModel.listNamedClasses().toList();
    OntClass node = ontModel.getOntClass(uri);
    // get parent classes
    List<OntClass> parentClasses = node.listSuperClasses().toList();

    for (OntClass p : parentClasses) {
      // get children of each parent that is not self
      for (OntClass c : p.listSubClasses().toList()) {
        if (allClasses.contains(c) && !c.equals(node)) {
          ls.add(c);
        }
      }
    }

    return ls;
  }

  public static List<OntClass> getSemanticContext(OntModel ontModel, String uri) {
    List<OntClass> ls = new ArrayList<OntClass>();
    ls.addAll(getSuperClasses(ontModel, uri));
    ls.addAll(getSubClasses(ontModel, uri));
//    ls.addAll(getSiblingClasses(ontModel, uri));

    return ls;
  }

  public static String getLabel(OntClass ontClass) {
    String str = ontClass.getLocalName();
    str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(str), ' ');

    str = str.replaceAll("\\s-\\s", "-"); // e.g. reverts "co - author" to "co-author"
    str = str.replaceAll("\\s_\\s", " ");
    str = str.replaceAll("\\s+", " "); // normalise spaces

    return str.trim();
  }

  public static Set<String> getLabels(OntClass ontClass) {
    Set<String> labels = new HashSet<String>();

    String str = ontClass.getLocalName();
    str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(str), ' ');

    str = str.replaceAll("\\s-\\s", "-"); // e.g. reverts "co - author" to "co-author"
    str = str.replaceAll("\\s_\\s", " ");
    str = str.replaceAll("\\s+", " "); // normalise spaces

    labels.add(str.trim());

    return labels;
  }

  public static Set<String> getLabels2(OntClass ontClass) {
    Set<String> labels = new HashSet<String>();

    String str = ontClass.getLocalName();
    str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(str), ' ');

    str = str.replaceAll("\\s-\\s", "-"); // e.g. reverts "co - author" to "co-author"
    str = str.replaceAll("\\s_\\s", " ");

    // prepare for lookup in word embedding vocabulary
    str = VectorOps.prepareStringSpaces(str);

    labels.add(str.trim());

    return labels;
  }

  public static Set<String> getProperties(OntClass ontClass) { // e.g. is_paid_by, is_equipped_by, write, go_through
    Set<String> props = new HashSet<String>();
    ontClass.listDeclaredProperties().forEachRemaining(c -> {
      props.add(c.getLocalName());
    });

    return props;
  }

  public static int getShortestDistanceToRoot(OntModel ontModel, String uri) {
    int dist = 1; // imaginary root node that is parent to all top level concepts
    List<OntClass> start = getSuperClasses(ontModel, uri);
    if (start.isEmpty()) {
      return dist;
    }
    List<OntClass> ancestors = start; // add first generation
    boolean rootReached = false;
    while (!rootReached && !ancestors.isEmpty()) {
      dist++;
      List<OntClass> parents = new ArrayList<>();
      for (OntClass p : ancestors) {
        List<OntClass> myp = getSuperClasses(ontModel, p.getURI());
        if (myp.isEmpty()) {
          rootReached = true;
          break;
        }
        parents.addAll(myp);
      }
      ancestors = parents; // next generation
    }

    return dist;
  }

  public static int getShortestDistanceToLeaf(OntModel ontModel, String uri) {
    int dist = 0;
    List<OntClass> start = getSubClasses(ontModel, uri);
    if (start.isEmpty()) {
      return dist;
    }
    List<OntClass> progeny = start; // add first generation
    boolean leafReached = false;
    while (!leafReached && !progeny.isEmpty()) {
      dist++;
      List<OntClass> children = new ArrayList<>();
      for (OntClass p : progeny) {
        List<OntClass> myc = getSubClasses(ontModel, p.getURI());
        if (myc.isEmpty()) {
          leafReached = true;
          break;
        }
        children.addAll(myc);
      }
      progeny = children; // next generation
    }

    return dist;
  }

  /**
   * The shortest length of the path on which a concept is located.
   * @param ontModel
   * @param uri
   * @return
   */
  public static int getShortestLengthOfPath(OntModel ontModel, String uri) {
    return getShortestDistanceToRoot(ontModel, uri) + getShortestDistanceToLeaf(ontModel, uri);
  }

  /**
   * Related depth in hierarchy which is ratio of shortest distance from root node
   * to the shortest length of the path.
   * @param ontModel
   * @param uri
   * @return
   */
  public static double getRelativeDepthInPath(OntModel ontModel, String uri) {
    int dist = getShortestDistanceToRoot(ontModel, uri);
    int toLeaf = getShortestDistanceToLeaf(ontModel, uri);
    return (double) dist / (dist + toLeaf);
  }

  public static void getProfile(OntClass ontClass) {
    Profile profile = ontClass.getProfile();
    System.out.println("===");
    System.out.println(ontClass.getLocalName());
    System.out.println("---");
    ontClass.listEquivalentClasses().forEachRemaining(c -> {
      System.out.println("=> " + c.getLocalName());
//      System.out.println("Domain: " + c.getDomain().getLocalName());
//      System.out.println("Range: " + c.getRange().getLocalName());
//      System.out.println("Equiv: " + c.getEquivalentProperty().getLocalName());
    });
//    ontClass.listSubClasses(false).forEachRemaining(c -> {
//      System.out.println(c.getLocalName());
//    });
    System.out.println("===");
  }

  /**
   * Retrieves all words in concept labels. Multi-term words are split by
   * whitespaces.
   *
   * @param ontModel
   * @return
   */
  public static List<String> getOntoTerms(OntModel ontModel) {
    List<String> ontoTerms = new ArrayList<String>();
    List<OntClass> allClasses = ontModel.listNamedClasses().toList();
    for (OntClass c : allClasses) {
      String[] str = getLabel(c).split("\\s"); // split by spaces
      for (String t : str) {
        ontoTerms.add(t);
      }
    }

    return ontoTerms;
  }

  public static void main(String[] args) {
    OntModel conf = getOntologyModel("C:/dev/bitbucket/alignment/data/2016_benchmark/303_onto.rdf");
//    conf.listClasses().toList().forEach(System.out::println);
//    List<OntClass> allClasses =  conf.listNamedClasses().toList();
    conf.listNamedClasses().forEachRemaining(t -> {
      System.out.println(StringOps.getLastUriValue(t.toString()));
      System.out.println("sameAs " + t.listSameAs().toList());
    });
//    conf.listObjectProperties().forEachRemaining(System.out::println);
//    conf.listDatatypeProperties().forEachRemaining(System.out::println);
//    OntClass oc = conf.getOntClass("http://confOf#Social_event");
//    System.out.println(getSiblingClasses(conf, "http://confOf#Social_event"));
//    List<OntClass> ls = oc.listSuperClasses().toList();
//    for (OntClass c : ls) {
//      if (allClasses.contains(c)){
//        System.out.println("c = " + c);
//      }
//    }

//    List<DatatypeProperty> allProps =  conf.listDatatypeProperties().toList();
//    for (DatatypeProperty dtp : allProps) {
//      System.out.println(dtp);
//      System.out.println(dtp.listSuperProperties().toList());
//    }
//    getOntoClasses("data/2016_conference/cmt.owl").forEach(t -> {
//      System.out.println(getLabels(t));
//    });

//    List<AlignTrainTest> allTestcaseData = Alignment_oaei.generateAlignTrainTest();
//    for (AlignTrainTest alignTrainTest : allTestcaseData) {
//      List<OntClass> concepts1 = OntoOps.getOntoClasses(alignTrainTest.sourceOnto.getAbsolutePath());
//      List<OntClass> concepts2 = OntoOps.getOntoClasses(alignTrainTest.targetOnto.getAbsolutePath());
//
//      OntModel sourceModel = OntoOps.getOntologyModel(alignTrainTest.sourceOnto.getAbsolutePath());
//      OntModel targetModel = OntoOps.getOntologyModel(alignTrainTest.targetOnto.getAbsolutePath());
//      System.out.println(alignTrainTest.sourceOnto.getAbsolutePath());
//      concepts2.forEach(c -> {
//        System.out.println(c.getURI() + " | Depth => " + getRelativeDepthInPath(targetModel, c.getURI()));
//        getProperties(c).forEach(System.out::println);
//      });
//    }
  }
}
