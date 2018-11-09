package uk.rgu.data.ontologyprocessor;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.ontologyprocessor.Alignment.AlignmentGraph;
import uk.rgu.data.ontologyprocessor.Ontology.Graph;
import uk.rgu.data.ontologyprocessor.Relation.Predicate;
import uk.rgu.data.services.LabelIndexService;
import uk.rgu.data.services.LinkedConceptService;
import uk.rgu.data.utilities.FileOps;
import uk.rgu.data.utilities.StringOps;

/**
 * Methods used to test operation of the RDF Store.
 *
 * @author 1113938
 */
public class RDFApp {

  private static final Logger LOG = Logger.getLogger(RDFApp.class.getName());

  static final String FILES_DIR = "C:/dev/rgu/DocumentSegmentAnnotation/data/xml";
  private static final RDFManager rdfStore = new RDFManager();

  public static void main(String[] args) throws IOException {
//    rdfStore.createDataStore(Ontology.Graph.MESH);
//    String uri = "http://id.nlm.nih.gov/mesh/2015/Q000293";
//    String predicate = "http://www.w3.org/2000/01/rdf-schema#label";
//    ResultSet resultSet = rdfStore.getCustomResultSet(Ontology.Graph.MESH, uri, predicate);
//    resultSet.forEachRemaining(res -> {
//      String label = res.getLiteral("?obj").getString();
//      System.out.println("Label => " + label);
//    });

    /*
    List<ResultSet> results = rdfStore.getCustomTriple(Ontology.Graph.MESH, Predicate.PREF_LABEL.value, "Zygoma");
    for (ResultSet result : results) {
      result.forEachRemaining(res -> {
        String pred = res.getResource("?predicate").toString();
        String object;
        if (res.get("object") instanceof Resource)
          object = res.getResource("?object").toString();
        else
          object = res.getLiteral("?object").getString();
        System.out.println(pred + " - " + object);
      });
      System.out.println("=======================");
    }
     */
//    rdfStore.createDataStore(Graph.GEMET); // re-create TDB store
//    getConceptLabels("GEMET").forEach(System.out::println);
//    int counter = 0;
//    for (String string : getConceptLabels("GEMET")) {
//      counter++;
//      System.out.println(string);
//    }
//    getConceptLabels("GEMET");
//    String outFilePath = "/dev/rgu/Geoword2vec/model/gemet_text.txt";
//    FileOps.writeSentencesToFile(getDefinitions("GEMET"), outFilePath);
//    getDefinitions("GEMET").forEach(System.out::println);

/*
    rdfStore.createDataStore(Graph.GEMET); // re-create TDB store
    getConceptLabels("GEMET").forEach(System.out::println);
    rdfStore.createDataStore(Graph.EUROVOC); // re-create TDB store
    getConceptLabels("EUROVOC").forEach(System.out::println);
    rdfStore.createDataStore(AlignmentGraph.EUROVOC_GEMET); // create/re-create TDB store
    rdfStore.getAlignments("EUROVOC_GEMET", Predicate.EXACT_MATCH.value).forEach(System.out::println);
    System.out.println(rdfStore.tripleExist(AlignmentGraph.EUROVOC_GEMET, "http://eurovoc.europa.eu/1456", Predicate.EXACT_MATCH.value, "http://www.eionet.europa.eu/gemet/concept/4030"));
*/
    Collection coll = new ArrayList();
    List<AlignedConcept> alignment = rdfStore.getAlignments(AlignmentGraph.EUROVOC_GEMET.value.toUpperCase(), Predicate.EXACT_MATCH.value);
    alignment.forEach(a -> {
      coll.add(rdfStore.getConcept(Ontology.getGraph(AlignmentGraph.EUROVOC_GEMET.conceptScheme), StringOps.getLastUriValue(a.concept_1)).getLabel() + " vs " +
        rdfStore.getConcept(Ontology.getGraph(AlignmentGraph.EUROVOC_GEMET.alignedConceptScheme), StringOps.getLastUriValue(a.concept_2)).getLabel());
    });
    coll.forEach(System.out::println);
    System.out.println("Size = " + coll.size());
  }

  public static List<Concept> getConceptsInOntology(String scheme) {
    LinkedConceptService linkedConceptService = new LinkedConceptService(rdfStore);
    List<Concept> concepts = linkedConceptService.getAllConcept(scheme); // get all concepts

    return concepts;
  }

  public static List<String> getConceptLabels(String scheme) {
    List<String> labels = new ArrayList();
    List<Concept> concepts = getConceptsInOntology(scheme);
    Collections.sort(concepts, new Concept.ConceptComparator()); // sort alphabetically
    int monitorCounter = 0; // runtime monitor

    for (Concept concept : concepts) {
      String conceptId = concept.getScheme().toUpperCase() + "-" + concept.getId();
//      System.out.println(conceptId);
      Set<String> allLabels = concept.getAllLabels(); // get all labels (including preferred label)
      monitorCounter++;
      LOG.log(Level.INFO, "Currently processing: {0} | Counter: {1}", new Object[]{conceptId, monitorCounter});
      for (String label : allLabels) { // persist each label in the database
        labels.add(label);
      }
    }

    return labels;
  }

  public static List<String> getDefinitions(String scheme) {
    List<String> schemeDefinitions = new ArrayList<String>();

    List<Concept> concepts = getConceptsInOntology(scheme);
//    Collections.sort(concepts, new Concept.ConceptComparator()); // sort alphabetically

    for (Concept concept : concepts) {
      String definition = rdfStore.getCustomLiteral(Graph.GEMET, concept.getId(), Predicate.DEFINITION.value);
      if (definition != null) {
        Set<String> allLabels = concept.getAllLabels(); // get all labels (including preferred label)
        for (String label : allLabels) { // persist each label in the database
          label = label.substring(0, 1).toUpperCase() + label.substring(1); // capitalise first letter
          label = StringOps.stripAllParentheses(label); // remove parenthesis
          definition = StringOps.normaliseWhitespace(definition);
          schemeDefinitions.add(StringOps.removeRepetitionAtStart(label + " " + definition, 5)); // remove any repetition at start up to 5-grams
        }
      }
    }

    return schemeDefinitions;
  }

}
