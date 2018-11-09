package uk.rgu.data.ontologyprocessor;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.hp.hpl.jena.ontology.OntTools;
import com.hp.hpl.jena.ontology.OntTools.Path;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.sys.TDBInternal;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.Filter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import uk.rgu.data.model.AlignedConcept;
import uk.rgu.data.model.Concept;
import uk.rgu.data.model.Subsumer;
import uk.rgu.data.ontologyprocessor.Alignment.AlignmentGraph;
import uk.rgu.data.utilities.StringOps;
import uk.rgu.data.ontologyprocessor.Ontology.Graph;
import uk.rgu.data.ontologyprocessor.Relation.Predicate;

public class RDFManager {

  private static final Logger LOG = Logger.getLogger(RDFManager.class.getName());
  static ResultSet resultSet;
  Dataset dataset;

//  String lexicon = "RDFSource/Lexicon_NamedRockUnit.nt";
//  String geochron = "RDFSource/Geochronology_Division.nt";
//  String rcs = "RDFSource/EarthMaterialClass_RockName.nt";
  public RDFManager() {
  }

  public void closeDataset() {
    this.dataset.end();
  }

  public void setDataset(Graph graph) {
    this.dataset = createOrOpenStore(graph.dbPath);
  }

  /**
   * Creates TDB-backed Relation data store.
   *
   * @param create Create/recreate TDB-backed datastores if true.
   */
  public void createDataStore(Boolean create) {
    // Create a datastore for each graph
    Ontology.allGraphs().forEach(graph -> {
      if (create) { // Delete existing graph if recreating datastore
        // Just one Relation file per TDB store
        cleanFolder(graph.dbPath); // Remove previous content before re-creating
      }
      // Create dataset
      LOG.log(Level.INFO, "Creating graph in {0} from {1}", new Object[]{graph.dbPath, graph.sourcePath});
      TDBLoader
              .load(TDBInternal.getBaseDatasetGraphTDB(
                      TDBFactory.createDatasetGraph(graph.dbPath)), graph.sourcePath, true);
    });
  }

  /**
   * Creates TDB-backed Relation data store for named graph.
   *
   * @param graph
   */
  public void createDataStore(Graph graph) {
    // Delete existing graph
    cleanFolder(graph.dbPath); // Remove previous content before re-creating

    // Create dataset
    LOG.log(Level.INFO, "Creating graph in {0} from {1}", new Object[]{graph.dbPath, graph.sourcePath});
    TDBLoader
            .load(TDBInternal.getBaseDatasetGraphTDB(
                    TDBFactory.createDatasetGraph(graph.dbPath)), graph.sourcePath, true);

  }

  /**
   * Creates TDB-backed Relation data store for named graph.
   *
   * @param graph
   */
  public void createDataStore(AlignmentGraph graph) {
    // Delete existing graph
    cleanFolder(graph.dbPath); // Remove previous content before re-creating

    // Create dataset
    LOG.log(Level.INFO, "Creating graph in {0} from {1}", new Object[]{graph.dbPath, graph.sourcePath});
    TDBLoader
            .load(TDBInternal.getBaseDatasetGraphTDB(
                    TDBFactory.createDatasetGraph(graph.dbPath)), graph.sourcePath, true);
  }

  /**
   * Gets dataset from path.
   *
   * @param graphPath
   * @return
   */
  public Dataset createOrOpenStore(String graphPath) {
    // open TDB dataset
    dataset = TDBFactory.createDataset(graphPath);
    return dataset;
  }

  /**
   * Gets default Model from specified dataset.
   *
   * @param dataset
   * @return
   */
  public Model getDefaultModel(Dataset dataset) {
    return dataset.getDefaultModel();
  }

  /**
   * Executes supplied SPARQL query against supplied Model.
   *
   * @param sparql Query to execute.
   * @param tdb Model to execute query against.
   * @return
   */
  public ResultSet queryModel(Model tdb, String sparql) {
    Query query = QueryFactory.create(sparql);
    QueryExecution qexec = QueryExecutionFactory.create(query, tdb);
    return qexec.execSelect();
  }

  /**
   * Queries supplied graph using supplied predicate.
   *
   * @param graph
   * @param predicate
   * @return
   */
  public ResultSet queryDatabase(Graph graph, Predicate predicate) {
    ResultSet results = null;
    // open TDB dataset
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    // get the named model
    Model tdb = getDefaultModel(dataset);
    // query the database using SPARQL
    String q = "SELECT ?node ?label WHERE {"
            + "?node <" + predicate.value + "> ?label ."
            //            + "OPTIONAL {?node <http://www.w3.org/2004/02/skos/core#altLabel> ?label .}"
            + "}";
    try {
      results = queryModel(tdb, q);
    } finally {
      dataset.end();
    }
    return results;
  }

  /**
   * Checks if an alignment exists.
   *
   * @param graph
   * @param subj
   * @param pred
   * @param obje
   * @return
   */
  public boolean tripleExist(AlignmentGraph graph, String subj, String pred, String obje) {
    String queryString = "ASK  { <" + subj + "> <" + pred + "> <" + obje + "> }";
    System.out.println(queryString);
    Query query = QueryFactory.create(queryString);
    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model
    QueryExecution qexec = QueryExecutionFactory.create(query, tdb);
    boolean result = qexec.execAsk();
    qexec.close();

    return result;
  }

  /**
   * Retrieves all concepts in a graph (Concepts with english language labels
   * only if language is specified in scheme).
   *
   * @param graph
   * @return
   */
  public Set<Concept> getAllConcepts(Graph graph) {
    Set<Concept> allConcepts = new HashSet();
    String q = "SELECT ?node ?label WHERE {"
            + "?node <" + Relation.Predicate.PREF_LABEL.value + "> ?label ."
            //            + "FILTER(langMatches(lang(?label), \"EN\"))"
            + "}";

    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model
    try {
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
//        System.out.println("Concept URI: " + res.getResource("node").getURI()); // discover concept URI
        String node = StringOps.getLastUriValue(res.getResource("node").getURI());
        String label = res.getLiteral("label").getString();
        String labelLang = res.getLiteral("label").getLanguage();
        if (labelLang.equalsIgnoreCase("en") || labelLang.equalsIgnoreCase("")) {
//        System.out.println("Pref language: " + labelLang);
          Concept c = new Concept(node, label, graph.name());
          if (graph.altLabel) {
            c.setAltLabels(this.getAltLabels(graph, node));
            if (graph.descriptionAsAlt) {
              c.addAltLabels(this.getDescriptionStrings(graph, node));
            }
          }
          allConcepts.add(c);
        }
      }
    } finally {
      dataset.end();
    }

    return allConcepts;
  }

  /**
   * Retrieves specified concept from supplied graph.
   *
   * @param graph
   * @param nodeId Last fragment of full URI.
   * @return
   */
  public Concept getConcept(Graph graph, String nodeId) {
    Concept concept = new Concept();
    String q = "SELECT ?label WHERE {"
            + "<" + graph.conceptUri + nodeId + "> <" + Relation.Predicate.PREF_LABEL.value + "> ?label ."
            + "}";

    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model
    try {
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        String label = res.getLiteral("label").getString();
        String labelLang = res.getLiteral("label").getLanguage();
        if (labelLang.equalsIgnoreCase("en") || labelLang.equalsIgnoreCase("")) {
          concept = new Concept(nodeId, label, graph.name());
          if (graph.altLabel) {
            concept.setAltLabels(this.getAltLabels(graph, nodeId));
            if (graph.descriptionAsAlt) {
              concept.addAltLabels(this.getDescriptionStrings(graph, nodeId));
            }
          }
        }
      }
    } finally {
      dataset.end();
    }

    return concept;
  }

  /**
   * Retrieves specified concept from supplied graph.
   *
   * @param graph
   * @param nodeId
   * @return
   */
  public Concept getConceptUsingFullConceptId(Graph graph, String nodeId) {
    Concept concept = new Concept();
    String q = "SELECT ?label WHERE {"
            + "<" + nodeId + "> <" + Relation.Predicate.PREF_LABEL.value + "> ?label ."
            + "}";

    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model
    try {
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        String label = res.getLiteral("label").getString();
        String labelLang = res.getLiteral("label").getLanguage();
        if (labelLang.equalsIgnoreCase("en") || labelLang.equalsIgnoreCase("")) {
          concept = new Concept(StringOps.getLastUriValue(nodeId), label, graph.name());
          if (graph.altLabel) {
            concept.setAltLabels(this.getAltLabels(graph, nodeId));
            if (graph.descriptionAsAlt) {
              concept.addAltLabels(this.getDescriptionStrings(graph, nodeId));
            }
          }
        }
      }
    } finally {
      dataset.end();
    }

    return concept;
  }

  public void conceptDistance() {
    String path = "RDFSource/Lexicon_NamedRockUnit.nt";
    String s1 = "http://data.bgs.ac.uk/id/Lexicon/NamedRockUnit/ELWK";
    String s2 = "http://data.bgs.ac.uk/id/Lexicon/NamedRockUnit/MORA";

    Model model = ModelFactory.createDefaultModel();

    // use the FileManager to find the input file
    InputStream in = FileManager.get().open(path);
    if (in == null) {
      throw new IllegalArgumentException(
              "File: " + path + " not found");
    }
    // get the named model
    model.read(in, null);

    Resource r1 = model.createResource(s1);
    Resource r2 = model.createResource(s2);

    Path p = OntTools.findShortestPath(model, r1, r2, Filter.any());
//    OntTools.getLCA(model, r1, r2);
    System.out.println(p.size());
    p.forEach(c -> System.out.println(c.toString()));
  }

  /**
   * Gets label of concept in specified graph.
   *
   * @param conceptId Last part of Concept URI. This should be unique for a
   * concept in each graph.
   * @param graph Graph to search in.
   * @return Concept's preferred label (Null if concept was not found).
   */
  public String getConceptLabel(Graph graph, String conceptId) {
    String label = null;
    if (conceptId != null) { // Query database and find label
      String conceptLabel = conceptId.toUpperCase(); // Assumption: all alphabets in concept ids are uppercase
      dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
      dataset.begin(ReadWrite.READ); // Transaction mode
      Model tdb = getDefaultModel(dataset); // get the model
      // query the database using SPARQL
      String q = "SELECT ?label WHERE {"
              + "<" + graph.conceptUri + conceptLabel + "> <" + Relation.Predicate.PREF_LABEL.value + "> ?label ."
              + "}";
      try {
        ResultSet lblResults = queryModel(tdb, q); // Execute query
        while (lblResults.hasNext()) { // Should be one result only
          label = lblResults.nextSolution().getLiteral("?label").getString();
//          LOG.log(Level.INFO, label);
        }
      } finally {
        dataset.end(); // End transaction
      }
    } // end if
    return label;
  }

  /**
   * Retrieves alternate labels (skos:altLabel) of a concept.
   *
   * @param graph Ontology graph to retrieve altLabels from.
   * @param nodeId Concept node (ID) in the graph.
   * @return Set of alternate labels.
   */
  public Set<String> getAltLabels(Graph graph, String nodeId) {
    Set<String> altLabel = new HashSet();
    String q = "SELECT ?altLabel WHERE {"
            + "<" + graph.conceptUri + nodeId + "> <" + Relation.Predicate.ALT_LABEL.value + "> ?altLabel ."
            + "}";

    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model
    try {
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        String labelLang = res.getLiteral("altLabel").getLanguage();
        if (labelLang.equalsIgnoreCase("en") || labelLang.equalsIgnoreCase("")) {
          altLabel.add(res.getLiteral("altLabel").getString());
        }
      }
    } finally {
      dataset.end();
    }
    return altLabel;
  }

  /**
   * Retrieves description strings (rdf:description) of a concept.
   *
   * @param graph Ontology graph to retrieve altLabels from.
   * @param nodeId Concept node (ID) in the graph.
   * @return Set of alternate labels.
   */
  public Set<String> getDescriptionStrings(Graph graph, String nodeId) {
    Set<String> altLabel = new HashSet();
    String q = "SELECT ?description WHERE {"
            + "<" + graph.conceptUri + nodeId + "> <" + Relation.Predicate.DESCRIPTION.value + "> ?description ."
            + "}";

    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    dataset.begin(ReadWrite.READ); // Transaction mode
    Model tdb = getDefaultModel(dataset); // get the model
    try {
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        altLabel.add(WordUtils.capitalize(res.getLiteral("description").getString()));
      }
    } finally {
      dataset.end();
    }
    return altLabel;
  }

  /**
   * Retrieves the object (resource) part of a triple from a graph.
   *
   * @param graph Graph to search.
   * @param conceptId Last part of Concept URI. This should be unique for a
   * concept in each graph.
   * @param predicate Predicate uri of triple.
   * @return
   */
  public String getCustomResource(Graph graph, String conceptId, String predicate) {
    String label = null;
    if (conceptId != null) { // Query database and find label
      String conceptLabel = conceptId.toUpperCase(); // Assumption: all alphabets in concept ids are uppercase
      dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
      dataset.begin(ReadWrite.READ); // Transaction mode
      Model tdb = getDefaultModel(dataset); // get the model
      // query the database using SPARQL
      String q = "SELECT ?resource WHERE {"
              + "<" + graph.conceptUri + conceptLabel + "> <" + predicate + "> ?resource ."
              + "}";
      try {
        ResultSet lblResults = queryModel(tdb, q); // Execute query
        while (lblResults.hasNext()) { // Should be one result only
          label = lblResults.nextSolution().getResource("?resource").getLocalName();
//          logger.log(Level.INFO, label);
        }
      } finally {
        dataset.end(); // End transaction
      }
    } // end if
    return label;
  }

  /**
   * Retrieves the object (literal) part of a triple from a graph.
   *
   * @param graph Graph to search.
   * @param conceptId Last part of Concept URI. This should be unique for a
   * concept in each graph.
   * @param predicate Predicate uri of triple.
   * @return
   */
  public String getCustomLiteral(Graph graph, String conceptId, String predicate) {
    String label = null;
    if (conceptId != null) { // Query database and find label
      String conceptLabel = conceptId.toUpperCase(); // Assumption: all alphabets in concept ids are uppercase
      dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
      dataset.begin(ReadWrite.READ); // Transaction mode
      Model tdb = getDefaultModel(dataset); // get the model
      // query the database using SPARQL
      String q = "SELECT ?literal WHERE {"
              + "<" + graph.conceptUri + conceptLabel + "> <" + predicate + "> ?literal ."
              + "}";
      try {
        ResultSet lblResults = queryModel(tdb, q); // Execute query
        while (lblResults.hasNext()) { // Should be one result only
          label = lblResults.nextSolution().getLiteral("?literal").getString();
//          logger.log(Level.INFO, label);
        }
      } finally {
        dataset.end(); // End transaction
      }
    } // end if
    return label;
  }

  /**
   * Retrieves the object part of a triple from a graph.
   *
   * @param graph Graph to search.
   * @param conceptUrl Full concept URI (Subject of a triple).
   * @param predicate Predicate uri of triple.
   * @return
   */
  public ResultSet getCustomResultSet(Graph graph, String conceptUrl, String predicate) {
    ResultSet result = null;
    if (conceptUrl != null) { // Query database and find label
      dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
      dataset.begin(ReadWrite.READ); // Transaction mode
      Model tdb = getDefaultModel(dataset); // get the model
      // query the database using SPARQL
      String q = "SELECT ?obj WHERE {"
              + "<" + conceptUrl + "> <" + predicate + "> ?obj ."
              + "}";
      try {
        result = queryModel(tdb, q); // Execute query
      } finally {
        dataset.end(); // End transaction
      }
    } // end if
    return result;
  }

  /**
   * Retrieves the subject part of a triple from a graph.
   *
   * @param graph Graph to search.
   * @param conceptUrl Full concept URI (Object of a triple).
   * @param predicate Predicate uri of triple.
   * @return
   */
  public ResultSet getCustomResultSetInv(Graph graph, String conceptUrl, String predicate) {
    ResultSet result = null;
    if (conceptUrl != null) { // Query database and find label
      dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
      dataset.begin(ReadWrite.READ); // Transaction mode
      Model tdb = getDefaultModel(dataset); // get the model
      // query the database using SPARQL
      String q = "SELECT ?obj WHERE {"
              + " ?obj <" + predicate + "> <" + conceptUrl + "> ."
              + "}";
      try {
        result = queryModel(tdb, q); // Execute query
      } finally {
        dataset.end(); // End transaction
      }
    } // end if
    return result;
  }

  /**
   * Get all ResultSet triples that match supplied label.
   *
   * @param graph
   * @param predicate
   * @param label
   * @return
   */
  public List<ResultSet> getCustomTriple(Graph graph, String predicate, String label) {
    List<ResultSet> result = new ArrayList();
    if (label != null && predicate != null) { // Query database and find label
      System.out.println("Querying datastore ...");
      dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
      dataset.begin(ReadWrite.READ); // Transaction mode
      Model tdb = getDefaultModel(dataset); // get the model
      // query the database using SPARQL
      String q = "SELECT ?node WHERE {"
              + "?node <" + predicate + "> \"" + label + "\"@en ."
              + "}";
      try {
        ResultSet res = queryModel(tdb, q); // Execute query
        res.forEachRemaining(r -> {
          String node = r.getResource("node").toString();
          System.out.println(node);
          String q1 = "SELECT ?predicate ?object WHERE {"
                  + "<" + node + "> ?predicate ?object ."
                  + "}";
          result.add(queryModel(tdb, q1));
        });
      } finally {
        dataset.end(); // End transaction
      }
    } // end if
    return result;
  }

  /**
   * Retrieve all alignments in scheme that uses specified relations.
   * @param scheme
   * @param predicate
   * @return
   */
  public List<AlignedConcept> getAlignments(String scheme, String predicate) {
    List<AlignedConcept> alignments = new ArrayList<AlignedConcept>();
    if (scheme != null) { // Query database and find label
      AlignmentGraph graph = AlignmentGraph.valueOf(scheme);
      dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
      dataset.begin(ReadWrite.READ); // Transaction mode
      Model tdb = getDefaultModel(dataset); // get the model
      // query the database using SPARQL
      String q = "SELECT ?concept1 ?concept2 WHERE {"
              + "?concept1 <" + predicate + "> ?concept2 ."
              + "}";
      try {
        ResultSet results = queryModel(tdb, q); // execute query and get results
        while (results.hasNext()) {
          QuerySolution res = results.next();
          alignments.add(new AlignedConcept(res.getResource("?concept1").getURI(), res.getResource("?concept2").getURI(), predicate));
        }
      } finally {
        dataset.end(); // End transaction
      }
    } // end if

    return alignments;
  }

  /**
   * Finds every concept that has its label contained in the supplied string
   * (skos:prefLabel only).
   *
   * @param string
   * @return
   */
  public List<Concept> getConceptContained(String string) {
    List<Concept> concepts = new ArrayList();
    // Iterate through every graph and find concept
    Ontology.allGraphs().forEach(graph -> {
//System.out.println("Current graph: " + graph.name());
      String scheme = graph.name();
      dataset = createOrOpenStore(graph.dbPath);
      dataset.begin(ReadWrite.READ);
      Model tdb = getDefaultModel(dataset);
      // query the database using SPARQL
      String q = "SELECT ?node ?label WHERE {"
              + "?node <" + Relation.Predicate.PREF_LABEL.value + "> ?label ."
              + "}";
      try {
        ResultSet conceptsFound = queryModel(tdb, q);
        conceptsFound.forEachRemaining(res -> {
          String term = res.getLiteral("?label").getString();
          if (StringUtils.containsIgnoreCase(term, string)) {
//            System.out.println("Label = " + term);
            String node = res.getResource("node").toString();
//            System.out.println("NodeS = " + node);
            String value = StringOps.getLastUriValue(node);
            concepts.add(new Concept(value, term, scheme));
          }
        });

      } finally {
        dataset.end();
      }
    });

    return concepts;
  }

  /**
   * Gets the maximum distance (edges) of a concept from its topmost concept.
   *
   * @param graph
   * @param concept
   * @return
   */
  public int maxDistFromRoot(Graph graph, Concept concept) {
    int distance = 0; // initialise distance
    Set<String> startNode = new HashSet();
    startNode.add(graph.conceptUri + concept.getId()); // create start node
    Set<String> result = getImmediateParents(graph, startNode); // get set of parent labels
    while (result.size() > 0) {
      distance++;
      result = getImmediateParents(graph, result);
    }

    return distance;
  }

  /**
   * Shortest distance from a node to the root node. Assumes presence of a root
   * node if the graph does not specify any.
   * @param graph
   * @param uri
   * @return
   */
  public int getShortestDistanceToRoot(Graph graph, String uri) {
    Set<String> parentLabels = new HashSet();
    String startNode = uri; // start noode
    System.out.println("Start: " + startNode);
    int dist = -1; // distance from start node
    parentLabels.add(startNode); // add initial node

    while (parentLabels.size() > 0) {
      dist++; // new level
      if ("CHRONOSTRAT".equals(graph.name())) {
        parentLabels = getParentsTerminateAtAnyStop2(graph, parentLabels); // get all next level nodes
      } else {
        parentLabels = getParentsTerminateAtAnyStop(graph, parentLabels); // get all next level nodes
      }
      System.out.println("Number in broader = " + parentLabels.size());
    }
    return ("".equals(StringOps.getLastUriValue(graph.topConcept))) ? (dist + 1) : dist; // add 1 to count if there is no root node in graph
  }

  /**
   * Shortest distance from a node to a leaf. Returns 0 if it is a leaf node.
   * @param graph
   * @param uri
   * @return
   */
  public int getShortestDistanceToLeaf(Graph graph, String uri) {
    int dist = 0;
    Set<String> start = graph.subClass.equals("") ? getImmediateChildrenInv(graph, uri) : getImmediateChildren(graph, uri);
    if (start.isEmpty()) {
      return dist;
    }
    Set<String> progeny = start; // add first generation
    boolean leafReached = false;
    while (!leafReached && !progeny.isEmpty()) {
      dist++;
      Set<String> children = new HashSet<>();
      for (String p : progeny) {
        Set<String> myc = graph.subClass.equals("") ? getImmediateChildrenInv(graph, p) : getImmediateChildren(graph, p);
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
   * @param graph
   * @param uri
   * @return
   */
  public int getShortestLengthOfPath(Graph graph, String uri) {
    return getShortestDistanceToRoot(graph, uri) + getShortestDistanceToLeaf(graph, uri);
  }

  /**
   * Related depth in hierarchy which is ratio of shortest distance from root node
   * to the shortest length of the path.
   * @param graph
   * @param uri
   * @return
   */
  public double getRelativeDepthInPath(Graph graph, String uri) {
    int dist = getShortestDistanceToRoot(graph, uri);
    int toLeaf = getShortestDistanceToLeaf(graph, uri);
    return (double) dist / (dist + toLeaf);
  }

  /**
   * Checks if supplied child is a specialisation of supplied parent in
   * specified graph.
   *
   * @param graph
   * @param child
   * @param parent
   * @return Number of edges from child to parent node (-1 if not a parent)
   */
  public int isParentOf(Graph graph, Concept child, Concept parent) {
    String startNode = graph.conceptUri + child.getId();
    String finalNode = graph.conceptUri + parent.getId();

    Set<String> found = getImmediateParents(createOrOpenStore(graph.dbPath), startNode);
    if (found.contains(finalNode)) {
      return 1; // Is immediate parent node
    }    // If not found, check even higher nodes
    boolean isFound = false;
    int distance = 1;
    while ((found.size() > 0) && (isFound == false)) {
      distance++;
      found = getImmediateParents(graph, found);
      if (found.contains(finalNode)) {
        isFound = true;
      }
    }

    return isFound ? distance : -1;
  }

  /**
   * Checks if a concept is a leaf node.
   *
   * @param graph
   * @param concept
   * @return True if leaf node (false otherwise)
   */
  public boolean isLeaf(Graph graph, Concept concept) {
    String startNode = graph.conceptUri + concept.getId();
    Set<String> result = getImmediateChildren(graph, startNode);
    return result.isEmpty();
  }

  /**
   * Iteratively retrieves every parent concept for supplied concept.
   *
   * @param graph Graph to retrieve concepts from.
   * @param concept Concept to retrieve its parents.
   * @return List of parent concepts with indication of level in hierarchy.
   */
  public List<Concept> getAllParents(Graph graph, Concept concept) {
    List<Concept> concepts = new ArrayList();
    Set<String> cummulative = new HashSet();
    Set<String> found = new HashSet();
    String startNode = graph.conceptUri + concept.getId();

    found.add(startNode); // add initial node
    while (found.size() > 0) {
      found = getImmediateParents(graph, found);
      found.forEach(n -> cummulative.add(n));
    }
    cummulative.forEach(c -> concepts.add(new Concept(StringOps.getLastUriValue(c), graph.name())));

    return concepts;
  }

  /**
   * Retrieves all concepts that partially or fully matches supplied string in
   * graph (used skos:prefLabel).
   *
   * @param graph Graph to lookup.
   * @param string String to match.
   * @return List of concepts.
   */
  public List<Concept> getPartialMatchConcepts(Graph graph, String string) {
    List<Concept> concepts = new ArrayList();
    int count = 0;
    ResultSet results = queryDatabase(graph, Predicate.PREF_LABEL);
    while (results.hasNext()) {
      String label = results.nextSolution().getLiteral("?label").getString();
      if (label.contains(string)) {
        String node = results.nextSolution().getResource("node").toString();
        node = StringOps.getLastUriValue(node);
        concepts.add(new Concept(node, label, graph.name()));
      }
    }

    return concepts;
  }

//  /**
//   * Retrieves the maximum depth in a graph.
//   *
//   * @param graph
//   * @return Maximum depth or 0 if there is no top concept.
//   */
//  public int graphMaxDepth(Graph graph) {
//    int depth = 0;
//    if (!graph.topConcept.isEmpty()) {
//
//    }
//
//    return depth;
//  }
//----------------------- OTHER METHODS ------------------------------------//
  /**
   * Get all node ids of child nodes (excludes start node).
   *
   * @param graph
   * @param concept
   * @return
   */
  public Set<String> getAllChildren(Graph graph, Concept concept) {
    Set<String> children = new HashSet(); // result list
    Set<String> childLabels = new HashSet();

    String startLabel = graph.conceptUri + concept.getId(); // start noode
    childLabels.add(startLabel); // add initial node
    // Iteratively get parent nodes until top
    while (childLabels.size() > 0) {
      if ("CHRONOSTRAT".equals(graph.name())) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      } else {
        childLabels = getImmediateChildren(graph, childLabels); // get all next level nodes
      }
      for (String n : childLabels) {
        String s = StringOps.getLastUriValue(n);
        // add new nodes to list if not there already (set replaces existing?)
        children.add(s);
      }
    }

    return children;
  }

  /**
   * Retrieves all parent nodes until the top of the graph. It moves upwards one
   * step at a time in all paths. Only the first encounter of a node is kept to
   * preserve minimum distances and avoid loops (includes start node).
   *
   * @param graph Graph to search in.
   * @param concept Concept to find parents of.
   * @return Unique list of parent concepts with minimum distances from supplied
   * concept.
   */
  public List<Subsumer> getAllSubsumers(Graph graph, Concept concept) {
    List<Subsumer> subsumers = new ArrayList(); // result list
    Set<String> parentLabels = new HashSet();
    int dist = 0; // distance from start node
    // Add supplied concept as first in list with distance = 0
    subsumers.add(new Subsumer(concept, dist));
    String startLabel = graph.conceptUri + concept.getId(); // start noode
    parentLabels.add(startLabel); // add initial node
    // Iteratively get parent nodes until top
    while (parentLabels.size() > 0) {
      dist++; // new level
      if ("CHRONOSTRAT".equals(graph.name())) {
        parentLabels = getImmediateParents2(graph, parentLabels); // get all next level nodes
      } else {
        parentLabels = getImmediateParents(graph, parentLabels); // get all next level nodes
      }
      int added = 0; // track addition of subsumers to avoid loops
      for (String n : parentLabels) {
        Subsumer s = new Subsumer(StringOps.getLastUriValue(n), graph.name(), dist);
        // add new nodes to list if not there already (set replaces existing?)
        if (isNotInList(subsumers, s)) {
          added++;
          subsumers.add(s);
        }
      } // end for loop
      if (added == 0) { // return if nothing was added
        parentLabels.clear();
        return subsumers;
      }
    } // end while loop
//    parentLabels.clear();

    return subsumers;
  }

  /**
   * Retrieves all child nodes until the leafs of the graph. It moves downwards
   * one step at a time in all paths. Only the first encounter of a node is kept
   * in order to preserve minimum distances and avoid loops (includes start
   * node).
   *
   * @param graph Graph to search in.
   * @param concept Concept to find children of.
   * @return Unique list of child concepts with minimum distances from supplied
   * concept.
   */
  public List<Subsumer> getAllSubsumed(Graph graph, Concept concept) {
    List<Subsumer> subsumed = new ArrayList(); // result list
    Set<String> childLabels = new HashSet();
    int dist = 0; // distance from start node
    // Add supplied concept as first in list with distance = 0
    subsumed.add(new Subsumer(concept, dist));
    String startLabel = graph.conceptUri + concept.getId(); // start node
    childLabels.add(startLabel); // add initial node
    // Iteratively get child nodes until leaf
    while (childLabels.size() > 0) {
      dist++; // new level
      if ("CHRONOSTRAT".equals(graph.name())) {
//        childLabels = getImmediateChildren2(graph, childLabels); // get all next level nodes
      } else if ("MESH".equals(graph.name())) {
        childLabels = getImmediateMeSHChildren(graph, childLabels);
      } else {
        childLabels = getImmediateChildren(graph, childLabels); // get all next level nodes
      }
      int added = 0; // track addition of subsumed to avoid loops
      for (String n : childLabels) {
        Subsumer s = new Subsumer(StringOps.getLastUriValue(n), graph.name(), dist);
        // add new nodes to list if not there already (set replaces existing?)
        if (isNotInList(subsumed, s)) {
          added++;
          subsumed.add(s);
        }
      } // end for loop
      if (added == 0) { // return if nothing was added
        childLabels.clear();
        return subsumed;
      }
    } // end while loop

    return subsumed;
  }

  /**
   * Retrieves immediate parents of node in graph.
   *
   * @param db
   * @param node
   * @return
   */
  public Set<String> getImmediateParents(Dataset db, String node) {
    Set<String> parentNodes = new HashSet();
//    dataset = createOrOpenStore(graph.dbPath);
    db.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(db);

    // Query the database using SPARQL
    String q = "SELECT ?node ?label WHERE {"
            + "<" + node + "> <" + Relation.Predicate.BROADER.value + "> ?node ."
            + "}";

    ResultSet results = queryModel(tdb, q);
    results.forEachRemaining(res -> {
      String aNode = res.getResource("node").toString();
      parentNodes.add(aNode);
    });
    return parentNodes;
  }

  /**
   * Retrieves all parents of nodes in graph.
   *
   * @param graph
   * @param nodes
   * @return
   */
  public Set<String> getImmediateParents(Graph graph, Set<String> nodes) {
    Set<String> parentNodes = new HashSet();
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);

    nodes.forEach(node -> {
      // Query the database using SPARQL
      String q = "SELECT ?node WHERE {"
              + "<" + node + "> <" + Relation.Predicate.BROADER.value + "> ?node ."
              + "}";

      ResultSet results = queryModel(tdb, q);
      results.forEachRemaining(res -> {
        String aNode = res.getResource("node").toString();
        parentNodes.add(aNode);
      });
    });
    return parentNodes;
  }

  /**
   * Retrieves immediate parents of a concept in graph.
   *
   * @param concept
   * @return ConceptIds
   */
  public Set<String> getImmediateParents(Concept concept) {
    String scheme = concept.getScheme();
    Graph graph = Ontology.getGraph(scheme);
    String node = graph.conceptUri + concept.getId();
    dataset = createOrOpenStore(graph.dbPath); // open TDB dataset
    Set<String> parentNodes = new HashSet();
//    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);

    // Query the database using SPARQL
    String q = "SELECT ?node WHERE {"
            + "<" + node + "> <" + Relation.Predicate.BROADER.value + "> ?node ."
            + "}";

    ResultSet results = queryModel(tdb, q);
    results.forEachRemaining(res -> {
      String aNode = res.getResource("node").toString();
      String id = StringOps.getLastUriValue(aNode);
      parentNodes.add(scheme + "-" + id);
    });
    return parentNodes;
  }

  /**
   * Gets the sub-divisions of a CHRONOSTRAT node.
   *
   * @param graph Graph to look in.
   * @param node Full URI of node (Subject of triple).
   * @return
   */
  public Set<String> getChronostratSubDivs(Graph graph, String node) {
    Set<String> subDivs = new HashSet();
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);

    // get node with sub divisions
    String q1 = "SELECT ?subdivnode WHERE {"
            + "<" + node + "> <http://data.bgs.ac.uk/ref/Geochronology/subDivisions> ?subdivnode ."
            + "}";

    String subDivNode = null;
    ResultSet results = queryModel(tdb, q1);
    while (results.hasNext()) {
      QuerySolution res = results.next();
      subDivNode = res.getResource("subdivnode").getURI(); // expects 1 result
    }
    // get sub divisions
    if (subDivNode != null) {
      String q2 = "SELECT ?divisions WHERE {"
              + "<" + subDivNode + "> <http://data.bgs.ac.uk/ref/Geochronology/divisionList> ?divisions ."
              + "FILTER(!(isURI(?divisions)))" // exlude uris - they are Relation.NIL
              + "}";

      results = queryModel(tdb, q2);
      while (results.hasNext()) {
        QuerySolution res2 = results.next();
        String[] tempDivs = res2.getLiteral("divisions").getString().split(" ");
        subDivs.addAll(Arrays.asList(tempDivs));
      }
    }

    return subDivs;
  }

  /**
   * Retrieves the super-divisions of a CHRONOSTRAT division.
   *
   * @param graph
   * @param node
   * @return
   */
  public Set<String> getChronostratSuperDiv(Graph graph, String node) {
    Set<String> parentNodes = new HashSet();

    String tempNode = null;
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);

    String q = "SELECT ?node ?divisions WHERE {"
            + "?node <http://data.bgs.ac.uk/ref/Geochronology/divisionList> ?divisions ."
            + "FILTER(!(isURI(?divisions)))"
            + "}";
    // Get list for parent division
    ResultSet results = queryModel(tdb, q);
    while (results.hasNext()) {
      QuerySolution res = results.next();
      String[] tempDivs = res.getLiteral("divisions").getString().split(" ");
      for (String div : tempDivs) {
        if (div.equals(node)) {
          tempNode = res.getResource("node").getURI();
          break;
        }
      }
    }
    // Get node that has the division list (parent node)
    if (tempNode != null) {
      String q2 = "SELECT ?parent WHERE {"
              + "?parent <http://data.bgs.ac.uk/ref/Geochronology/subDivisions> <" + tempNode + "> ."
              + "}";
      results = queryModel(tdb, q2);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        parentNodes.add(res.getResource("parent").getURI());
      }
    }

    return parentNodes;
  }

  /**
   * Retrieves all parents of nodes in graph. (For CHRONOSTRAT only! - try
   * alternative implementation?)
   *
   * @param graph
   * @param nodes
   * @return
   */
  public Set<String> getImmediateParents2(Graph graph, Set<String> nodes) {
    Set<String> parentNodes = new HashSet();
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);
    nodes.forEach(n -> {
      String tempNode = null;

      String q = "SELECT ?node ?divisions WHERE {"
              + "?node <http://data.bgs.ac.uk/ref/Geochronology/divisionList> ?divisions ."
              + "FILTER(!(isURI(?divisions)))"
              + "}";
      // Get list for parent division
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        String[] tempDivs = res.getLiteral("divisions").getString().split(" ");
        for (String div : tempDivs) {
          if (div.equals(n)) {
            try {
              tempNode = res.getResource("node").getURI();
              break;
            } catch (Exception ex) {
              tempNode = null;
              LOG.log(Level.INFO, "No file content or error reading file content. Reason: {0}", ex.getMessage());
            }
          }
        }
      }
      // Get node that has the division list (parent node)
      if (tempNode != null) {
        String q2 = "SELECT ?parent WHERE {"
                + "?parent <http://data.bgs.ac.uk/ref/Geochronology/subDivisions> <" + tempNode + "> ."
                + "}";
        results = queryModel(tdb, q2);
        while (results.hasNext()) {
          QuerySolution res = results.next();
          parentNodes.add(res.getResource("parent").getURI());
        }

      }
    });
    return parentNodes;
  }

  /**
   * Retrieves all parents of nodes in graph. However, return an empty
   * collection if at least, one node has no parents.
   *
   * @param graph
   * @param nodes
   * @return
   */
  public Set<String> getParentsTerminateAtAnyStop(Graph graph, Set<String> nodes) {
    Set<String> parentNodes = new HashSet();
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);
    ResultSet tempResults;
    QuerySolution tempRes;

    for (String node : nodes) {
      int parentsCount = 0; // initialise parents count
      // Query the database using SPARQL
      String q = "SELECT ?node ?label WHERE {"
              + "<" + node + "> <" + Relation.Predicate.BROADER.value + "> ?node ."
              + "}";

      tempResults = queryModel(tdb, q);

      while (tempResults.hasNext()) {
        tempRes = tempResults.next();
        String aNode = tempRes.getResource("node").toString();
        parentNodes.add(aNode);
        parentsCount++; // increment parents count for this node
      } // end while
      // Return empty set once a node has no parent
      if (parentsCount == 0) {
        return new HashSet();
      }
    } // end for
    return parentNodes;
  }

  /**
   * Retrieves all parents of nodes in graph. However, return an empty
   * collection if at least, one node has no parents. (For CHRONOSTRAT only! -
   * try alternative implementation)
   *
   * @param graph
   * @param nodes
   * @return
   */
  public Set<String> getParentsTerminateAtAnyStop2(Graph graph, Set<String> nodes) {
    Set<String> parentNodes = new HashSet();
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);

    for (String n : nodes) {
      int parentsCount = 0; // initialise parents count
      String tempNode = null;

      String q = "SELECT ?node ?divisions WHERE {"
              + "?node <http://data.bgs.ac.uk/ref/Geochronology/divisionList> ?divisions ."
              + "FILTER(!(isURI(?divisions)))"
              + "}";
      // Get list for parent division
      ResultSet results = queryModel(tdb, q);
      while (results.hasNext()) {
        QuerySolution res = results.next();
        String[] tempDivs = res.getLiteral("divisions").getString().split(" ");
        for (String div : tempDivs) {
          if (div.equals(n)) {
            tempNode = res.getResource("node").getURI();
            parentsCount++; // increment parents count for this node
            break;
          }
        }
      }// end while
      // Return empty set once a node has no parent
      if (parentsCount == 0) {
        return new HashSet();
      }
      // Get node that has the division list (parent node)
      if (tempNode != null) {
        String q2 = "SELECT ?parent WHERE {"
                + "?parent <http://data.bgs.ac.uk/ref/Geochronology/subDivisions> <" + tempNode + "> ."
                + "}";
        results = queryModel(tdb, q2);
        while (results.hasNext()) {
          QuerySolution res = results.next();
          parentNodes.add(res.getResource("parent").getURI());
        }

      }
    }
    return parentNodes;
  }

  public Set<String> getImmediateChildren(Graph graph, String node) {
    Set<String> childNodes = new HashSet();
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);

    // Query the database using SPARQL
    String q = "SELECT ?node ?label WHERE {"
            + "<" + node + "> <" + Relation.Predicate.NARROWER.value + "> ?node ."
            + "}";

    ResultSet results = queryModel(tdb, q);
    results.forEachRemaining(res -> {
      String aNode = res.getResource("node").toString();
      childNodes.add(aNode);
    });
    return childNodes;
  }

  /**
   * Retrieves all child nodes in graph.
   *
   * @param graph
   * @param nodes
   * @return
   */
  private Set<String> getImmediateChildren(Graph graph, Set<String> nodes) {
    Set<String> childNodes = new HashSet();
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);
    nodes.forEach(node -> {

      // Query the database using SPARQL
      String q = "SELECT ?node ?label WHERE {"
              + "<" + node + "> <" + Relation.Predicate.NARROWER.value + "> ?node ."
              + "}";

      ResultSet results = queryModel(tdb, q);
      results.forEachRemaining(res -> {
        String aNode = res.getResource("node").toString();
        childNodes.add(aNode);
      });
    });

    return childNodes;
  }

  /**
   * Retrieves all child nodes in graph. Customised for MeSH
   *
   * @param graph
   * @param nodes
   * @return
   */
  private Set<String> getImmediateMeSHChildren(Graph graph, Set<String> nodes) {
    Set<String> childNodes = new HashSet();
    nodes.forEach(node -> {
      ResultSet res = getCustomResultSetInv(graph, node, Predicate.BROADER.value);
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString();
        if (!val.equals(Predicate.NIL.value)) {
          childNodes.add(val);
        }
      }
    });

    return childNodes;
  }

  /**
   * Retrieves all child concepts in graph. Customised for MeSH
   *
   * @param concept
   * @return Set of conceptIds
   */
  public Set<String> getImmediateMeSHChildren(Concept concept) {
    String scheme = concept.getScheme();
    Graph graph = Ontology.getGraph(scheme);
    String node = graph.conceptUri + concept.getId();
    Set<String> childNodes = new HashSet(); // result
    ResultSet res = getCustomResultSetInv(graph, node, Predicate.BROADER.value);
    while (res.hasNext()) {
      String val = res.nextSolution().getResource("obj").toString();
      if (!val.equals(Predicate.NIL.value)) {
        String id = StringOps.getLastUriValue(val);
        childNodes.add(scheme + "-" + id);
      }
    }

    return childNodes;
  }

  /**
   * Retrieves all child concepts in graph. For use when there are only
   * broader concept relations.
   *
   * @param graph
   * @param node
   * @return Set of concept Uris
   */
  public Set<String> getImmediateChildrenInv(Graph graph, String node) {

    Set<String> childNodes = new HashSet(); // result
    ResultSet res = getCustomResultSetInv(graph, node, Predicate.BROADER.value);
    while (res.hasNext()) {
      String val = res.nextSolution().getResource("obj").toString();
      if (!val.equals(Predicate.NIL.value)) {
        childNodes.add(val);
      }
    }

    return childNodes;
  }

  /**
   * Checks if an object exists in a list.
   *
   * @param entries List of objects.
   * @param newEntry Object to check for.
   * @return True if not in list (false otherwise).
   */
  private boolean isNotInList(List<Subsumer> entries, Subsumer newEntry) {
    return entries.stream().noneMatch((e) -> (e.getId().equals(newEntry.getId())));
  }

  //------- CUSTOM EVALUATION METHODS ----------------------//
  public Set<String> meshNarrowerCount(Graph graph, Set<String> nodes) {
    Set<String> childNodes = new HashSet();
    dataset = createOrOpenStore(graph.dbPath);
    dataset.begin(ReadWrite.READ);
    Model tdb = getDefaultModel(dataset);
    nodes.forEach(node -> {

      // Query the database using SPARQL
      String q = "SELECT ?node WHERE {"
              + " ?node <" + Relation.Predicate.BROADER.value + "> <" + node + "> ."
              + "}";

      ResultSet results = queryModel(tdb, q);
      results.forEachRemaining(res -> {
        String aNode = res.getResource("node").toString();
        childNodes.add(aNode);
      });
    });
    return childNodes;
  }

  //------- N-GRAM DISTRIBUTION FOR A GRAPH ----------------------//
  Table<String, String, Integer> nGramDistTable = HashBasedTable.create(); // Table<Vocab, n-gramLabel, count>
//  ResultSet results; // Query result
  int numOfNodes = 0;
  int maxGrams = 0;
  String maxString = "";

  public void nGramDist(Graph graph) {
    ResultSet results = queryDatabase(graph, Predicate.ALT_LABEL);
    results.forEachRemaining(res -> {
      numOfNodes++; // increment count
      String node = res.getResource("?node").toString();
      String name = res.getLiteral("?label").getString();

      name = StringOps.stripAllParentheses(name); // remove content in parentheses
      name = StringOps.stripDescriptions(name); // remove descriptive texts

      int thisLength = name.split("\\s+").length; // word count, split at whitespace
      String nGramLbl = thisLength + "-gram";

      if (nGramDistTable.contains(graph.name(), nGramLbl)) { // update
        nGramDistTable.put(graph.name(), nGramLbl, nGramDistTable.get(graph.name(), nGramLbl) + 1);
      } else { // add new
        nGramDistTable.put(graph.name(), nGramLbl, 1);
      }

      if (maxGrams < thisLength) {
        maxGrams = thisLength;
        maxString = name;
      }

      System.out.println("Node = " + node + " | Name = " + name);
    });
    // Runtime monitoring
    System.out.println("Number of node = " + numOfNodes);
    System.out.println("Maximum n-grams = " + maxGrams);
    System.out.println("Longest string = " + maxString);
  }

  public void printTable() {
    nGramDistTable.rowKeySet().stream().forEach((key) -> {
      // Print term and count
      nGramDistTable.row(key).entrySet().stream().forEach((row) -> {
        System.out.println(key + "," + row.getKey() + "," + row.getValue());
      });
    });
  }
  //------- END OF N-GRAM DISTRIBUTION FOR A GRAPH ----------------------//

  /**
   * Deletes content of supplied directory.
   *
   * @param directoryPath
   */
  private void cleanFolder(String directoryPath) {
    try {
      FileUtils.cleanDirectory(new File(directoryPath));
    } catch (IOException ex) {
      String message = "Directory could not be deleted "
              + "before creating new database!";
      LOG.log(Level.SEVERE, message, ex);
    } catch (IllegalArgumentException ex) {
      LOG.log(Level.INFO, "No file or directory was deleted. Reason: ", ex.getMessage());
    }
  }

}
