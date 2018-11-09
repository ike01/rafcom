package uk.rgu.data.services;

import com.hp.hpl.jena.query.ResultSet;
import uk.rgu.data.model.LinkedConcept;
import uk.rgu.data.ontologyprocessor.Ontology.Graph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import uk.rgu.data.model.Concept;
import uk.rgu.data.model.ConceptContext;
import uk.rgu.data.model.LinkedConceptWithSiblings;
import uk.rgu.data.model.Related;
import uk.rgu.data.ontologyprocessor.Ontology;
import uk.rgu.data.ontologyprocessor.RDFManager;
import uk.rgu.data.ontologyprocessor.Relation.Predicate;
import uk.rgu.data.ontologyprocessor.SWTManager;
import uk.rgu.data.utilities.StringOps;

/**
 *
 * @author 1113938
 */
public class LinkedConceptService {

  private static final Logger LOG = Logger.getLogger(LinkedConceptService.class.getName());
  private RDFManager rdfStore;

  public LinkedConceptService(RDFManager rdfStore) {
    this.rdfStore = rdfStore; // initialise Triple store
  }

  public void changeRDFStore(RDFManager rdfStore) {
    this.rdfStore = rdfStore; // change Triple store
  }

  /**
   * Retrieves a concept.
   *
   * @param conceptId
   * @return
   */
  public Concept getConcept(String conceptId) {
    Concept concept = new Concept();
    String[] str = conceptId.split("-");
    String id = str[1];
    String scheme = str[0];

    Graph graph = Ontology.getGraph(scheme);
    if (null != graph) {
      concept.setId(id);
      concept.setScheme(scheme);
      String label = rdfStore.getConceptLabel(graph, id);
      concept.setLabel(label);
      if (graph.altLabel) {
        concept.setAltLabels(rdfStore.getAltLabels(graph, id));
        if (graph.descriptionAsAlt) {
          concept.addAltLabels(rdfStore.getDescriptionStrings(graph, id));
        }
      }
    }

    return concept;
  }

  /**
   * Retrieves all concepts.
   *
   * @param scheme
   * @return List of Concept (null if scheme is not found)
   */
  public List<Concept> getAllConcept(String scheme) {
    if (null == scheme) {
      return null;
    }
    return getConceptsWithoutLinks(scheme.toUpperCase());
  }

  /**
   * Retrieves all concepts.
   *
   * @param scheme
   * @return Map of Concept with id of each concept as key for easier access
   * (null if scheme is not found)
   */
  public Map<String, Concept> getAllConceptMap(String scheme) {
    if (null == scheme) {
      return null;
    }

    Map<String, Concept> conceptMap = new HashMap();
    getConceptsWithoutLinks(scheme.toUpperCase()).forEach(concept -> {
      conceptMap.put(concept.getId(), concept);
    });

    return conceptMap;
  }

  /**
   * Retrieves a concept with hierarchical links.
   *
   * @param scheme
   * @param nodeId
   * @return List of LinkConcept (null if scheme is not found)
   */
  public LinkedConcept getLinkedConcept(String scheme, String nodeId) {
    if (Graph.CHRONOSTRAT.name().equals(scheme.toUpperCase())) {
      return getLinkedChronostrat(Graph.CHRONOSTRAT, nodeId);
    }
    if (Graph.THESAURUS.name().equals(scheme.toUpperCase()) || Graph.NEW_THESAURUS.name().equals(scheme.toUpperCase())) {
      return getLinkedThesaurus(Graph.THESAURUS, nodeId);
    }
    if (Graph.SWEET.name().equals(scheme.toUpperCase())) {
      return getLinkedSweet(Graph.SWEET, nodeId);
    }
    if (Graph.MESH.name().equals(scheme.toUpperCase()) || Graph.EUROVOC.name().equals(scheme.toUpperCase())) {
      return getLinkedMesh(Graph.MESH, nodeId);
    }

    return getLinkedThesaurus(Ontology.getGraph(scheme), nodeId); // default
  }

  /**
   * Retrieves all concepts with hierarchical links. (Very expensive task)
   *
   * @param scheme
   * @return List of LinkConcept (null if scheme is not found)
   */
  public List<LinkedConcept> getAllLinkedConcept(String scheme) {
    if (Graph.CHRONOSTRAT.name().equals(scheme.toUpperCase())) {
      return getAllChronostrat(Graph.CHRONOSTRAT);
    }
    if (Graph.THESAURUS.name().equals(scheme.toUpperCase())) {
      return getAllThesaurus(Graph.THESAURUS);
    }
    if (Graph.NEW_THESAURUS.name().equals(scheme.toUpperCase())) {
      return getAllThesaurus(Graph.NEW_THESAURUS);
    }
    if (Graph.SWEET.name().equals(scheme.toUpperCase())) {
      return getAllSweet(Graph.SWEET);
    }
    if (Graph.MESH.name().equals(scheme.toUpperCase()) || Graph.EUROVOC.name().equals(scheme.toUpperCase())) {
      return getAllMesh(Graph.MESH);
    }

    return getAllThesaurus(Ontology.getGraph(scheme)); // default
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. Semantic
   * context is every other concept within n-hops of each concept. (Works for
   * skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  public List<ConceptContext> getConceptsAndContext(String scheme, int n) {
    if (Graph.MESH.name().equals(scheme.toUpperCase()) || Graph.EUROVOC.name().equals(scheme.toUpperCase())) {
      return conceptsContextVariant2(scheme, n);
    } else {
      return conceptsContextVariant1(scheme, n);
    }
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. Semantic
   * context is every other concept within n-hops of each concept. (Works for
   * skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  private List<ConceptContext> conceptsContextVariant1(String scheme, int n) {
    List<ConceptContext> conceptContext = new ArrayList();
    // get all concepts in scheme
    Graph graph = Graph.valueOf(scheme);
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      // get alternative labels
      String startNode = graph.conceptUri + concept.getId();
      ConceptContext l = new ConceptContext(concept.getId(), StringOps.stripAllParentheses(concept.getLabel()).replaceAll("\\s+", "_").toLowerCase(), concept.getScheme());
      concept.getAltLabels().forEach(t -> {
        if (t != null) {
          l.addAltLabel(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
        }
      });
      // get node that are n hops away
      // superclass up to n
      Set<String> sup = new HashSet();
      Set<String> lookupList = new HashSet();
      lookupList.add(startNode);
      for (int i = 0; i < n; i++) { // for n hops
        for (String node : lookupList) { // get all directly linked concepts
          ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
          while (res.hasNext()) {
            String val = res.nextSolution().getResource("obj").toString(); // concept id
            if (!val.equals(Predicate.NIL.value)) {
              sup.add(val);
            }
          }
        } // end for lookup list
        lookupList.clear();
        lookupList.addAll(sup);
      } // end for n hops of superclass
      // add all labels of sup list to concept context
      for (String concept_id : sup) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sup context

      // subclass up to n
      Set<String> sub = new HashSet();
      lookupList.clear();
      lookupList.add(startNode);
      for (int i = 0; i < n; i++) { // for n hops
        for (String node : lookupList) { // get all directly linked concepts
          ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.NARROWER.value);
          while (res.hasNext()) {
            String val = res.nextSolution().getResource("obj").toString(); // concept id
            if (!val.equals(Predicate.NIL.value)) {
              sub.add(val);
            }
          }
        } // end for lookup list
        lookupList.clear();
        lookupList.addAll(sub);
      } // end for n hops of subclass
      // add all labels of sub list to concept context
      for (String concept_id : sub) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sub context

      conceptContext.add(l); // add concept with its context to list
    } // end for all concepts

    return conceptContext;
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. This is
   * for scheme where broader relations are specified but narrower are not.
   * Semantic context is every other concept within n-hops of each concept.
   * (Works for skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  private List<ConceptContext> conceptsContextVariant2(String scheme, int n) {
    List<ConceptContext> conceptContext = new ArrayList();
    // get all concepts in scheme
    Graph graph = Graph.valueOf(scheme);
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      // get alternative labels
      String startNode = graph.conceptUri + concept.getId();
      ConceptContext l = new ConceptContext(concept.getId(), StringOps.stripAllParentheses(concept.getLabel()).replaceAll("\\s+", "_").toLowerCase(), concept.getScheme());
      concept.getAltLabels().forEach(t -> {
        if (t != null) {
          l.addAltLabel(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
        }
      });
      // get node that are n hops away
      // superclass up to n
      Set<String> sup = new HashSet();
      Set<String> lookupList = new HashSet();
      lookupList.add(startNode);
      for (int i = 0; i < n; i++) { // for n hops
        for (String node : lookupList) { // get all directly linked concepts
          ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
          while (res.hasNext()) {
            String val = res.nextSolution().getResource("obj").toString(); // concept id
            if (!val.equals(Predicate.NIL.value)) {
              sup.add(val);
            }
          }
        } // end for lookup list
        lookupList.clear();
        lookupList.addAll(sup);
      } // end for n hops of superclass
      // add all labels of sup list to concept context
      for (String concept_id : sup) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sup context

      // subclass up to n
      Set<String> sub = new HashSet();
      lookupList.clear();
      lookupList.add(startNode);
      for (int i = 0; i < n; i++) { // for n hops
        for (String node : lookupList) { // get all directly linked concepts
          ResultSet res = rdfStore.getCustomResultSetInv(graph, node, Predicate.BROADER.value);
          while (res.hasNext()) {
            String val = res.nextSolution().getResource("obj").toString();
            if (!val.equals(Predicate.NIL.value)) {
              sub.add(val);
            }
          }

        } // end for lookup list
        lookupList.clear();
        lookupList.addAll(sub);
      } // end for n hops of subclass
      // add all labels of sub list to concept context
      for (String concept_id : sub) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sub context

      conceptContext.add(l); // add concept with its context to list
    } // end for all concepts

    return conceptContext;
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. Semantic
   * context is every other concept within n-hops of each concept. (Works for
   * skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  public List<ConceptContext> getConceptsAndContextParents(String scheme, int n) {
    if (Graph.MESH.name().equals(scheme.toUpperCase()) || Graph.EUROVOC.name().equals(scheme.toUpperCase())) {
      return conceptsContextVariantParent2(scheme, n);
    } else {
      return conceptsContextVariantParent1(scheme, n);
    }
  }

  /**
   * Retrieves all the concepts in a scheme with its parent semantic context.
   * Semantic context is every other concept within n-hops of each concept.
   * (Works for skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  private List<ConceptContext> conceptsContextVariantParent1(String scheme, int n) {
    List<ConceptContext> conceptContext = new ArrayList();
    // get all concepts in scheme
    Graph graph = Graph.valueOf(scheme);
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      // get alternative labels
      String startNode = graph.conceptUri + concept.getId();
      ConceptContext l = new ConceptContext(concept.getId(), StringOps.stripAllParentheses(concept.getLabel()).replaceAll("\\s+", "_").toLowerCase(), concept.getScheme());
      concept.getAltLabels().forEach(t -> {
        if (t != null) {
          l.addAltLabel(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
        }
      });
      // get node that are n hops away
      // superclass up to n
      Set<String> sup = new HashSet();
      Set<String> lookupList = new HashSet();
      lookupList.add(startNode);
      for (int i = 0; i < n; i++) { // for n hops
        for (String node : lookupList) { // get all directly linked concepts
          ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
          while (res.hasNext()) {
            String val = res.nextSolution().getResource("obj").toString(); // concept id
            if (!val.equals(Predicate.NIL.value)) {
              sup.add(val);
            }
          }
        } // end for lookup list
        lookupList.clear();
        lookupList.addAll(sup);
      } // end for n hops of superclass
      // add all labels of sup list to concept context
      for (String concept_id : sup) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sup context

      conceptContext.add(l); // add concept with its context to list
    } // end for all concepts

    return conceptContext;
  }

  /**
   * Retrieves all the concepts in a scheme with its parent semantic context.
   * This is for scheme where broader relations are specified but narrower are
   * not. Semantic context is every other concept within n-hops of each concept.
   * (Works for skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  private List<ConceptContext> conceptsContextVariantParent2(String scheme, int n) {
    List<ConceptContext> conceptContext = new ArrayList();
    // get all concepts in scheme
    Graph graph = Graph.valueOf(scheme);
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      // get alternative labels
      String startNode = graph.conceptUri + concept.getId();
      ConceptContext l = new ConceptContext(concept.getId(), StringOps.stripAllParentheses(concept.getLabel()).replaceAll("\\s+", "_").toLowerCase(), concept.getScheme());
      concept.getAltLabels().forEach(t -> {
        if (t != null) {
          l.addAltLabel(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
        }
      });
      // get node that are n hops away
      // superclass up to n
      Set<String> sup = new HashSet();
      Set<String> lookupList = new HashSet();
      lookupList.add(startNode);
      for (int i = 0; i < n; i++) { // for n hops
        for (String node : lookupList) { // get all directly linked concepts
          ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
          while (res.hasNext()) {
            String val = res.nextSolution().getResource("obj").toString(); // concept id
            if (!val.equals(Predicate.NIL.value)) {
              sup.add(val);
            }
          }
        } // end for lookup list
        lookupList.clear();
        lookupList.addAll(sup);
      } // end for n hops of superclass
      // add all labels of sup list to concept context
      for (String concept_id : sup) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sup context

      conceptContext.add(l); // add concept with its context to list
    } // end for all concepts

    return conceptContext;
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. Semantic
   * context is every other concept within n-hops of each concept. (Works for
   * skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  public List<ConceptContext> getConceptsAndContextChildren(String scheme, int n) {
    if (Graph.MESH.name().equals(scheme.toUpperCase()) || Graph.EUROVOC.name().equals(scheme.toUpperCase())) {
      return conceptsContextVariantChildren2(scheme, n);
    } else {
      return conceptsContextVariantChildren1(scheme, n);
    }
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. Semantic
   * context is every other concept within n-hops of each concept. (Works for
   * skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  private List<ConceptContext> conceptsContextVariantChildren1(String scheme, int n) {
    List<ConceptContext> conceptContext = new ArrayList();
    // get all concepts in scheme
    Graph graph = Graph.valueOf(scheme);
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      // get alternative labels
      String startNode = graph.conceptUri + concept.getId();
      ConceptContext l = new ConceptContext(concept.getId(), StringOps.stripAllParentheses(concept.getLabel()).replaceAll("\\s+", "_").toLowerCase(), concept.getScheme());
      concept.getAltLabels().forEach(t -> {
        if (t != null) {
          l.addAltLabel(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
        }
      });
      // get node that are n hops away

      Set<String> lookupList = new HashSet();
      // subclass up to n
      Set<String> sub = new HashSet();
      lookupList.add(startNode);
      for (int i = 0; i < n; i++) { // for n hops
        for (String node : lookupList) { // get all directly linked concepts
          ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.NARROWER.value);
          while (res.hasNext()) {
            String val = res.nextSolution().getResource("obj").toString(); // concept id
            if (!val.equals(Predicate.NIL.value)) {
              sub.add(val);
            }
          }
        } // end for lookup list
        lookupList.clear();
        lookupList.addAll(sub);
      } // end for n hops of subclass
      // add all labels of sub list to concept context
      for (String concept_id : sub) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sub context

      conceptContext.add(l); // add concept with its context to list
    } // end for all concepts

    return conceptContext;
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. This is
   * for scheme where broader relations are specified but narrower are not.
   * Semantic context is every other concept within n-hops of each concept.
   * (Works for skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  private List<ConceptContext> conceptsContextVariantChildren2(String scheme, int n) {
    List<ConceptContext> conceptContext = new ArrayList();
    // get all concepts in scheme
    Graph graph = Graph.valueOf(scheme);
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      // get alternative labels
      String startNode = graph.conceptUri + concept.getId();
      ConceptContext l = new ConceptContext(concept.getId(), StringOps.stripAllParentheses(concept.getLabel()).replaceAll("\\s+", "_").toLowerCase(), concept.getScheme());
      concept.getAltLabels().forEach(t -> {
        if (t != null) {
          l.addAltLabel(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
        }
      });
      // get node that are n hops away
      Set<String> lookupList = new HashSet();

      // subclass up to n
      Set<String> sub = new HashSet();
      lookupList.add(startNode);
      for (int i = 0; i < n; i++) { // for n hops
        for (String node : lookupList) { // get all directly linked concepts
          ResultSet res = rdfStore.getCustomResultSetInv(graph, node, Predicate.BROADER.value);
          while (res.hasNext()) {
            String val = res.nextSolution().getResource("obj").toString();
            if (!val.equals(Predicate.NIL.value)) {
              sub.add(val);
            }
          }

        } // end for lookup list
        lookupList.clear();
        lookupList.addAll(sub);
      } // end for n hops of subclass
      // add all labels of sub list to concept context
      for (String concept_id : sub) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sub context

      conceptContext.add(l); // add concept with its context to list
    } // end for all concepts

    return conceptContext;
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. Semantic
   * context is every other concept within n-hops of each concept. (Works for
   * skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  public List<ConceptContext> getConceptsAndContextSibling(String scheme) {
    if (Graph.MESH.name().equals(scheme.toUpperCase()) || Graph.EUROVOC.name().equals(scheme.toUpperCase())) {
      return conceptsContextVariantSibling2(scheme);
    } else {
      return conceptsContextVariantSibling1(scheme);
    }
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. Semantic
   * context is every other concept within n-hops of each concept. (Works for
   * skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  private List<ConceptContext> conceptsContextVariantSibling1(String scheme) {
    List<ConceptContext> conceptContext = new ArrayList();
    // get all concepts in scheme
    Graph graph = Graph.valueOf(scheme);
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      // get alternative labels
      String startNode = graph.conceptUri + concept.getId();
      ConceptContext l = new ConceptContext(concept.getId(), StringOps.stripAllParentheses(concept.getLabel()).replaceAll("\\s+", "_").toLowerCase(), concept.getScheme());
      concept.getAltLabels().forEach(t -> {
        if (t != null) {
          l.addAltLabel(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
        }
      });
      // get node that are n hops away

      // superclass up to n
      Set<String> sup = new HashSet(); // parent concept uris
      ResultSet res = rdfStore.getCustomResultSet(graph, startNode, Predicate.BROADER.value);
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString(); // concept id
        if (!val.equals(Predicate.NIL.value)) {
          sup.add(val);
        }
      }

      // Siblings : subclass of parents except self
      Set<String> sib = new HashSet();
      for (String node : sup) {
        res = rdfStore.getCustomResultSet(graph, node, Predicate.NARROWER.value);
        while (res.hasNext()) {
          String val = res.nextSolution().getResource("obj").toString(); // concept id
          if (!val.equals(Predicate.NIL.value) && !val.equals(startNode)) {
            sib.add(val);
          }
        }
      }
      // labels of siblings
      for (String concept_id : sib) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sup context

      conceptContext.add(l); // add concept with its context to list
    } // end for all concepts

    return conceptContext;
  }

  /**
   * Retrieves all the concepts in a scheme with its semantic context. This is
   * for scheme where broader relations are specified but narrower are not.
   * Semantic context is every other concept within n-hops of each concept.
   * (Works for skos:broader and skos:narrower relations only!)
   *
   * @param scheme
   * @param n
   * @return
   */
  private List<ConceptContext> conceptsContextVariantSibling2(String scheme) {
    List<ConceptContext> conceptContext = new ArrayList();
    // get all concepts in scheme
    Graph graph = Graph.valueOf(scheme);
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      // get alternative labels
      String startNode = graph.conceptUri + concept.getId();
      ConceptContext l = new ConceptContext(concept.getId(), StringOps.stripAllParentheses(concept.getLabel()).replaceAll("\\s+", "_").toLowerCase(), concept.getScheme());
      concept.getAltLabels().forEach(t -> {
        if (t != null) {
          l.addAltLabel(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
        }
      });
      // get node that are n hops away

      // superclass up to n
      Set<String> sup = new HashSet(); // parent concept uris
      ResultSet res = rdfStore.getCustomResultSet(graph, startNode, Predicate.BROADER.value);
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString(); // concept id
        if (!val.equals(Predicate.NIL.value)) {
          sup.add(val);
        }
      }

      // Siblings : subclass of parents except self
      Set<String> sib = new HashSet();
      for (String node : sup) {
        res = rdfStore.getCustomResultSetInv(graph, node, Predicate.BROADER.value);
        while (res.hasNext()) {
          String val = res.nextSolution().getResource("obj").toString();
          if (!val.equals(Predicate.NIL.value) && !val.equals(startNode)) {
            sib.add(val);
          }
        }
      }
      // labels of siblings
      for (String concept_id : sib) {
        Concept c = rdfStore.getConceptUsingFullConceptId(graph, concept_id);
        c.getAllLabels().forEach(t -> {
          if (t != null) {
            l.context.add(StringOps.stripAllParentheses(t).replaceAll("\\s+", "_").toLowerCase());
          }
        });
      } // end for : read labels of sup context

      conceptContext.add(l); // add concept with its context to list
    } // end for all concepts

    return conceptContext;
  }

  /**
   * Retrieves the concepts in a scheme.
   *
   * @param scheme
   * @return
   */
  private List<Concept> getConceptsWithoutLinks(String scheme) {
    Graph graph = Ontology.getGraph(scheme); // Get graph
    List<Concept> concepts = new ArrayList();
    concepts.addAll(rdfStore.getAllConcepts(graph));
    return concepts;
  }

  private List<LinkedConcept> getAllMesh(Graph graph) {
    List<LinkedConcept> linkedConcepts = new ArrayList();
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      String node = graph.conceptUri + concept.getId();
      // Initialise a linked concept
      LinkedConcept l = new LinkedConcept(node, concept.getLabel(), concept.getScheme());
      l.setAltLabels(concept.getAltLabels());
      // Get hierarchical relations
      // superclass
      ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
      List<String> sup = new ArrayList();
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString();
        if (!val.equals(Predicate.NIL.value)) {
          sup.add(val);
        }
      }
      l.setSuperClasslike(new Related(Predicate.BROADER.value, sup));
      // subclass
      res = rdfStore.getCustomResultSetInv(graph, node, Predicate.BROADER.value);
      List<String> sub = new ArrayList();
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString();
        if (!val.equals(Predicate.NIL.value)) {
          sub.add(val);
        }
      }
      l.setSubClasslike(new Related(Predicate.NARROWER.value, sub));
      // add to result list
      linkedConcepts.add(l);
    }

    return linkedConcepts;
  }

  private List<LinkedConcept> getAllSweet(Graph graph) {
    List<LinkedConcept> linkedConcepts = new ArrayList();
//    changeRDFStore(new SWTManager());
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      String node = graph.conceptUri + concept.getId();
      // Initialise a linked concept
      LinkedConcept l = new LinkedConcept(node, concept.getLabel(), concept.getScheme());
      l.setAltLabels(concept.getAltLabels());
      // Get hierarchical relations
      // superclass
      ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
      List<String> sup = new ArrayList();
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString();
        if (!val.equals(Predicate.NIL.value)) {
          sup.add(val);
        }
      }
      l.setSuperClasslike(new Related(Predicate.BROADER.value, sup));
      // subclass
      res = rdfStore.getCustomResultSetInv(graph, node, Predicate.BROADER.value);
      List<String> sub = new ArrayList();
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString();
        if (!val.equals(Predicate.NIL.value)) {
          sub.add(val);
        }
      }
      l.setSubClasslike(new Related(Predicate.NARROWER.value, sub));
      // add to result list
      linkedConcepts.add(l);
    }

    return linkedConcepts;
  }

  private List<LinkedConcept> getAllThesaurus(Graph graph) {
    List<LinkedConcept> linkedConcepts = new ArrayList();
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      String node = graph.conceptUri + concept.getId();
      // Initialise a linked concept
      LinkedConcept l = new LinkedConcept(node, concept.getLabel(), concept.getScheme());
      l.setAltLabels(concept.getAltLabels());
//      l.setAltLabels(rdfStore.getAltLabels(graph, concept.getId())); // get alternate labels
      // Get hierarchical relations
      // superclass
      ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
      List<String> sup = new ArrayList();
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString();
        if (!val.equals(Predicate.NIL.value)) {
          sup.add(val);
        }
      }
      l.setSuperClasslike(new Related(Predicate.BROADER.value, sup));
      // subclass
      res = rdfStore.getCustomResultSet(graph, node, Predicate.NARROWER.value);
      List<String> sub = new ArrayList();
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString();
        if (!val.equals(Predicate.NIL.value)) {
          sub.add(val);
        }
      }
      l.setSubClasslike(new Related(Predicate.NARROWER.value, sub));
      // add to result list
      linkedConcepts.add(l);
    }

    return linkedConcepts;
  }

  /**
   * Retrieves all concepts with hierarchical links. (Very expensive task)
   *
   * @param scheme
   * @return List of LinkConcept (null if scheme is not found)
   */
  public List<LinkedConceptWithSiblings> getAllLinkedConceptWithSibling(String scheme) {
    if (Graph.MESH.name().equals(scheme.toUpperCase()) || Graph.EUROVOC.name().equals(scheme.toUpperCase())) {
      return getAllMeshSiblings(Ontology.getGraph(scheme));
    }

    return getAllThesaurusSiblings(Ontology.getGraph(scheme)); // default
  }

  private List<LinkedConceptWithSiblings> getAllMeshSiblings(Graph graph) {
    List<LinkedConceptWithSiblings> linkedConcepts = new ArrayList();
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      String node = graph.conceptUri + concept.getId();
      // Initialise a linked concept
      LinkedConceptWithSiblings l = new LinkedConceptWithSiblings(node, concept.getLabel(), concept.getScheme());
      l.setAltLabels(concept.getAltLabels());
      // Get hierarchical relations
      // superclass
      ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
      List<String> sup = new ArrayList();
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString();
        if (!val.equals(Predicate.NIL.value)) {
          sup.add(val);
        }
      }
//      l.setSuperClasslike(new Related(Predicate.BROADER.value, sup));
      // siblings
      for (String parentNode : sup) {
        res = rdfStore.getCustomResultSetInv(graph, parentNode, Predicate.BROADER.value);
        List<String> sub = new ArrayList();
        while (res.hasNext()) {
          String val = res.nextSolution().getResource("obj").toString();
          if (!val.equals(Predicate.NIL.value) && !val.equals(node)) {
            sub.add(val);
          }
        }
        l.setSiblinglike(new Related("Sibling", sub));
      }
      // add to result list
      linkedConcepts.add(l);
    }

    return linkedConcepts;
  }

  private List<LinkedConceptWithSiblings> getAllThesaurusSiblings(Graph graph) {
    List<LinkedConceptWithSiblings> linkedConcepts = new ArrayList();
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      String node = graph.conceptUri + concept.getId();
      // Initialise a linked concept
      LinkedConceptWithSiblings l = new LinkedConceptWithSiblings(node, concept.getLabel(), concept.getScheme());
      l.setAltLabels(concept.getAltLabels());
//      l.setAltLabels(rdfStore.getAltLabels(graph, concept.getId())); // get alternate labels
      // Get hierarchical relations
      // superclass
      ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
      List<String> sup = new ArrayList();
      while (res.hasNext()) {
        String val = res.nextSolution().getResource("obj").toString();
        if (!val.equals(Predicate.NIL.value)) {
          sup.add(val);
        }
      }
//      l.setSuperClasslike(new Related(Predicate.BROADER.value, sup));
      // siblings
      for (String parentNode : sup) {
        res = rdfStore.getCustomResultSet(graph, parentNode, Predicate.NARROWER.value);
        List<String> sub = new ArrayList();
        while (res.hasNext()) {
          String val = res.nextSolution().getResource("obj").toString();
          if (!val.equals(Predicate.NIL.value) && !val.equals(node)) {
            sub.add(val);
          }
        }
        l.setSiblinglike(new Related("Sibling", sub));
      }
      // add to result list
      linkedConcepts.add(l);
    }

    return linkedConcepts;
  }

  private List<LinkedConcept> getAllChronostrat(Graph graph) {
    List<LinkedConcept> linkedConcepts = new ArrayList();
    Set<Concept> tempList = rdfStore.getAllConcepts(graph);
    for (Concept concept : tempList) {
      String node = graph.conceptUri + concept.getId();
      // Initialise a linked concept
      LinkedConcept l = new LinkedConcept(node, concept.getLabel(), concept.getScheme());
      l.setAltLabels(concept.getAltLabels());
      // Get hierarchical relations
      // superclass
      Set<String> res = rdfStore.getChronostratSuperDiv(graph, node);
      List<String> sup = new ArrayList();
      for (String str : res) {
        sup.add(str);
      }
      l.setSuperClasslike(new Related(Predicate.SUPERDIVISION.value, sup));
      // subclass
      res = rdfStore.getChronostratSubDivs(graph, node);
      List<String> sub = new ArrayList();
      for (String str : res) {
        sub.add(str);
      }
      l.setSubClasslike(new Related(Predicate.SUBDIVISIONS.value, sub));
      // add to result list
      linkedConcepts.add(l);
    }

    return linkedConcepts;
  }

  private LinkedConcept getLinkedThesaurus(Graph graph, String nodeId) {
    LinkedConcept lc;
    Concept concept = getConcept(graph.name() + "-" + nodeId);
    String node = graph.conceptUri + concept.getId();
    // Initialise a linked concept
    lc = new LinkedConcept(node, concept.getLabel(), concept.getScheme());
    lc.setAltLabels(concept.getAltLabels());
//      l.setAltLabels(rdfStore.getAltLabels(graph, concept.getId())); // get alternate labels
    // Get hierarchical relations
    // superclass
    ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
    List<String> sup = new ArrayList();
    while (res.hasNext()) {
      String val = res.nextSolution().getResource("obj").toString();
      if (!val.equals(Predicate.NIL.value)) {
        sup.add(val);
      }
    }
    lc.setSuperClasslike(new Related(Predicate.BROADER.value, sup));
    // subclass
    res = rdfStore.getCustomResultSet(graph, node, Predicate.NARROWER.value);
    List<String> sub = new ArrayList();
    while (res.hasNext()) {
      String val = res.nextSolution().getResource("obj").toString();
      if (!val.equals(Predicate.NIL.value)) {
        sub.add(val);
      }
    }
    lc.setSubClasslike(new Related(Predicate.NARROWER.value, sub));

    return lc;
  }

  private LinkedConcept getLinkedChronostrat(Graph graph, String nodeId) {
    LinkedConcept lc;
    Concept concept = this.getConcept(graph.name() + "-" + nodeId);
    String node = graph.conceptUri + concept.getId();
    // Initialise a linked concept
    lc = new LinkedConcept(node, concept.getLabel(), concept.getScheme());
    lc.setAltLabels(concept.getAltLabels());
    // Get hierarchical relations
    // superclass
    Set<String> res = rdfStore.getChronostratSuperDiv(graph, node);
    List<String> sup = new ArrayList();
    for (String str : res) {
      sup.add(str);
    }
    lc.setSuperClasslike(new Related(Predicate.SUPERDIVISION.value, sup));
    // subclass
    res = rdfStore.getChronostratSubDivs(graph, node);
    List<String> sub = new ArrayList();
    for (String str : res) {
      sub.add(str);
    }
    lc.setSubClasslike(new Related(Predicate.SUBDIVISIONS.value, sub));

    return lc;
  }

  private LinkedConcept getLinkedSweet(Graph graph, String nodeId) {
    LinkedConcept lc;
//    changeRDFStore(new SWTManager());
    Concept concept = getConcept(graph.name() + "-" + nodeId);
    String node = graph.conceptUri + concept.getId();
    // Initialise a linked concept
    lc = new LinkedConcept(node, concept.getLabel(), concept.getScheme());
    lc.setAltLabels(concept.getAltLabels());
//      l.setAltLabels(rdfStore.getAltLabels(graph, concept.getId())); // get alternate labels
    // Get hierarchical relations
    // superclass
    ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
    List<String> sup = new ArrayList();
    while (res.hasNext()) {
      String val = res.nextSolution().getResource("obj").toString();
      if (!val.equals(Predicate.NIL.value)) {
        sup.add(val);
      }
    }
    lc.setSuperClasslike(new Related(Predicate.BROADER.value, sup));
    // subclass
    res = rdfStore.getCustomResultSetInv(graph, node, Predicate.BROADER.value);
    List<String> sub = new ArrayList();
    while (res.hasNext()) {
      String val = res.nextSolution().getResource("obj").toString();
      if (!val.equals(Predicate.NIL.value)) {
        sub.add(val);
      }
    }
    lc.setSubClasslike(new Related(Predicate.NARROWER.value, sub));

    return lc;
  }

  private LinkedConcept getLinkedMesh(Graph graph, String nodeId) {
    LinkedConcept lc;
    Concept concept = getConcept(graph.name() + "-" + nodeId);
    String node = graph.conceptUri + concept.getId();
    // Initialise a linked concept
    lc = new LinkedConcept(node, concept.getLabel(), concept.getScheme());
    lc.setAltLabels(concept.getAltLabels());
//      l.setAltLabels(rdfStore.getAltLabels(graph, concept.getId())); // get alternate labels
    // Get hierarchical relations
    // superclass
    ResultSet res = rdfStore.getCustomResultSet(graph, node, Predicate.BROADER.value);
    List<String> sup = new ArrayList();
    while (res.hasNext()) {
      String val = res.nextSolution().getResource("obj").toString();
      if (!val.equals(Predicate.NIL.value)) {
        sup.add(val);
      }
    }
    lc.setSuperClasslike(new Related(Predicate.BROADER.value, sup));
    // subclass
    res = rdfStore.getCustomResultSetInv(graph, node, Predicate.BROADER.value);
    List<String> sub = new ArrayList();
    while (res.hasNext()) {
      String val = res.nextSolution().getResource("obj").toString();
      if (!val.equals(Predicate.NIL.value)) {
        sub.add(val);
      }
    }
    lc.setSubClasslike(new Related(Predicate.NARROWER.value, sub));

    return lc;
  }

  public static void main(String[] args) {
    LinkedConceptService linkedConceptService = new LinkedConceptService(new RDFManager());
//    linkedConceptService.getConceptsAndContext("EUROVOC", 1).forEach(System.out::println);
//    linkedConceptService.getConceptsAndContext("GEMET", 1).forEach(System.out::println);
//    linkedConceptService.getAllConcept("GEMET").forEach(System.out::println);
    linkedConceptService.getConceptsAndContextSibling("GEMET").forEach(System.out::println);
//    linkedConceptService.getConceptsAndContextParents("GEMET", 1).forEach(System.out::println);
//    linkedConceptService.getConceptsAndContextChildren("GEMET", 1).forEach(System.out::println);
  }

}
