package uk.rgu.data.ontologyprocessor;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.util.HashSet;
import java.util.Set;
import uk.rgu.data.model.Concept;
import uk.rgu.data.ontologyprocessor.Ontology.Graph;
import uk.rgu.data.utilities.StringOps;

/**
 *
 * @author aikay
 */
public class SWTManager extends RDFManager {

  public SWTManager() {
    super();
  }

  /**
   * Retrieves specified concept from supplied graph.
   *
   * @param graph
   * @param nodeId
   * @return
   */
  @Override
  public Concept getConcept(Ontology.Graph graph, String nodeId) {
    Concept concept = new Concept();
    String q = "SELECT ?label WHERE {"
            + "<" + graph.conceptUri + nodeId + "> <" + Relation.Predicate.LABEL.value + "> ?label ."
            + "}";

    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model
    try {
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        String label = res.getLiteral("label").getString();
        concept = new Concept(nodeId, label, graph.name());
        if (graph.altLabel) {
          concept.setAltLabels(this.getAltLabels(graph, nodeId));
          if (graph.descriptionAsAlt) {
            concept.addAltLabels(this.getDescriptionStrings(graph, nodeId));
          }
        }
      }
    } finally {
      dataset.end();
    }

    return concept;
  }

  /**
   * Retrieves all concepts in a graph.
   *
   * @param graph
   * @return
   */
  @Override
  public Set<Concept> getAllConcepts(Ontology.Graph graph) {
    Set<Concept> allConcepts = new HashSet();
    String q = "SELECT ?node ?label WHERE {"
            + "?node <" + Relation.Predicate.LABEL.value + "> ?label ."
            + "}";

    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model
    try {
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        String node = StringOps.getLastUriValue(res.getResource("node").getURI());
        String label = res.getLiteral("label").getString();
        Concept c = new Concept(node, label, graph.name());
        if (graph.altLabel) {
          c.setAltLabels(this.getAltLabels(graph, node));
          if (graph.descriptionAsAlt) {
            c.addAltLabels(this.getDescriptionStrings(graph, node));
          }
        }
        allConcepts.add(c);
      }
    } finally {
      dataset.end();
    }

    return allConcepts;
  }

  /**
   * Retrieves alternate labels (owl:sameAs) of a concept.
   *
   * @param graph Ontology graph to retrieve altLabels from.
   * @param nodeId Concept node (ID) in the graph.
   * @return Set of alternate labels.
   */
  @Override
  public Set<String> getAltLabels(Ontology.Graph graph, String nodeId) {
    Set<String> altLabel = new HashSet();
    String q = "SELECT ?altLabel WHERE {"
            + "<" + graph.conceptUri + nodeId + "> <" + Relation.Predicate.SAME_AS.value + "> ?altLabel ."
            + "}";

    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model
    try {
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        altLabel.add(res.getLiteral("altLabel").getString());
      }
    } finally {
      dataset.end();
    }
    return altLabel;
  }

  public void allStatmts(Ontology.Graph graph) {
    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model

    // list the statements in the Model
    StmtIterator iter = tdb.listStatements();

// print out the predicate, subject and object of each statement
    while (iter.hasNext()) {
      Statement stmt = iter.nextStatement();  // get next statement
      Resource subject = stmt.getSubject();     // get the subject
      Property predicate = stmt.getPredicate();   // get the predicate
      RDFNode object = stmt.getObject();      // get the object

      System.out.print(subject.toString());
      System.out.print(" " + predicate.toString() + " ");
      if (object instanceof Resource) {
        System.out.print(object.toString());
      } else {
        // object is a literal
        System.out.print(" \"" + object.toString() + "\"");
      }

      System.out.println(" .");
    }
  }

  public static void main(String[] args) {
//    SWTManager rdfStore = new SWTManager();
//    rdfStore.allStatmts(Graph.SWEET);
//
//    String base = Graph.SWEET.dbPath; // the base URI of the ontology
//    OntModel m = ModelFactory.createOntologyModel();  // the model containing the ontology statements
//    com.hp.hpl.jena.ontology.Ontology ont = m.getOntology( base );
//
//    // now list the ontology imports
//    for (String imp : ont.listImportedOntologyURIs()) {
//      System.out.println( "Ontology " + base + " imports " + imp );
//    }
  }
}
