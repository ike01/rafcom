package uk.rgu.data.utilities;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import uk.rgu.data.align.oaei.OntoOps;
import uk.rgu.data.model.Concept;
import uk.rgu.data.model.ConceptContext;
import uk.rgu.data.model.LinkedConcept;
import uk.rgu.data.model.Related;
import uk.rgu.data.ontologyprocessor.Relation;
import uk.rgu.data.ontologyprocessor.word2vec.VectorOps;

/**
 *
 * @author aikay
 */
public class DocOps {

    /**
     * Experimenting dynamic determination of threshold.
     *
     * @param entitiesSimilarity
     * @param values
     * @param matrixCount
     * @return
     */
    public static double findThreshold(double entitiesSimilarity, List<Double> values, int matrixCount) {
        double minThreshold = 0.75;
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

    private static List<LinkedConcept> getAllLinkedConcept(OntModel ontModel, List<OntClass> concepts, String scheme) {
        List<LinkedConcept> linkedConcepts = new ArrayList();

        for (OntClass ontClass : concepts) {
            LinkedConcept l = new LinkedConcept(ontClass.getURI(), OntoOps.getLabel(ontClass), scheme);
            // sup
            List<String> sup = new ArrayList();
            for (OntClass contextClass : OntoOps.getSuperClasses(ontModel, ontClass.getURI())) {
                sup.add(contextClass.getURI());
            }
            l.setSuperClasslike(new Related(Relation.Predicate.BROADER.value, sup));
            // sub
            List<String> sub = new ArrayList();
            for (OntClass contextClass : OntoOps.getSubClasses(ontModel, ontClass.getURI())) {
                sub.add(contextClass.getURI());
            }
            l.setSubClasslike(new Related(Relation.Predicate.BROADER.value, sub));

            // add to result list
            linkedConcepts.add(l);
        }

        return linkedConcepts;
    }

    public static List<ConceptContext> getConceptsAndContext(OntModel ontModel, List<OntClass> concepts, String scheme) {
        List<ConceptContext> conceptContexts = new ArrayList();
        for (OntClass ontClass : concepts) {
            ConceptContext cc = new ConceptContext(ontClass.getURI(), OntoOps.getLabel(ontClass), scheme);
            Set<String> contextLabels = new HashSet();
            for (OntClass contextClass : OntoOps.getSemanticContext(ontModel, ontClass.getURI())) {
                contextLabels.addAll(OntoOps.getLabels(contextClass));
            }
            cc.context = contextLabels; // set context
            conceptContexts.add(cc); // add to list
        }

        return conceptContexts;
    }

    public static List<ConceptContext> getConceptsAndContextParents(OntModel ontModel, List<OntClass> concepts, String scheme) {
        List<ConceptContext> conceptContexts = new ArrayList();
        for (OntClass ontClass : concepts) {
            ConceptContext cc = new ConceptContext(ontClass.getURI(), OntoOps.getLabel(ontClass), scheme);
            Set<String> contextLabels = new HashSet();
            for (OntClass contextClass : OntoOps.getSuperClasses(ontModel, ontClass.getURI())) {
                contextLabels.addAll(OntoOps.getLabels(contextClass));
            }
            cc.context = contextLabels; // set context
            conceptContexts.add(cc); // add to list
        }

        return conceptContexts;
    }

    public static List<ConceptContext> getConceptsAndContextChildren(OntModel ontModel, List<OntClass> concepts, String scheme) {
        List<ConceptContext> conceptContexts = new ArrayList();
        for (OntClass ontClass : concepts) {
            ConceptContext cc = new ConceptContext(ontClass.getURI(), OntoOps.getLabel(ontClass), scheme);
            Set<String> contextLabels = new HashSet();
            for (OntClass contextClass : OntoOps.getSubClasses(ontModel, ontClass.getURI())) {
                contextLabels.addAll(OntoOps.getLabels(contextClass));
            }
            cc.context = contextLabels; // set context
            conceptContexts.add(cc); // add to list
        }

        return conceptContexts;
    }

    public static List<ConceptContext> getConceptsAndContextSibling(OntModel ontModel, List<OntClass> concepts, String scheme) {
        List<ConceptContext> conceptContexts = new ArrayList();
        for (OntClass ontClass : concepts) {
            ConceptContext cc = new ConceptContext(ontClass.getURI(), OntoOps.getLabel(ontClass), scheme);
            Set<String> contextLabels = new HashSet();
            for (OntClass contextClass : OntoOps.getSiblingClasses(ontModel, ontClass.getURI())) {
                contextLabels.addAll(OntoOps.getLabels(contextClass));
            }
            cc.context = contextLabels; // set context
            conceptContexts.add(cc); // add to list
        }

        return conceptContexts;
    }

    /**
     * Retrieves unique list of conceptIds that are directly linked to a
     * concept.
     *
     * @param linkedConcepts
     * @param c
     * @return
     */
    public static Set<String> getContextIds(List<LinkedConcept> linkedConcepts, Concept c) {
        Set<String> contextConceptIds = new HashSet<String>();
//    System.out.println("concept : " + c);
//    System.out.println("concept id : " + c.getId());
        String id = c.getId();
//    System.out.println("full uri : " + id);
        for (LinkedConcept cc : linkedConcepts) {
            if (cc.getId().equals(id)) {
//        System.out.println("Found concept context Id: " + cc);
                for (String sub : cc.getSubClasslike().getValues()) { // narrower context
                    contextConceptIds.add(sub);
                }
                for (String sup : cc.getSuperClasslike().getValues()) { // broader context
                    contextConceptIds.add(sup);
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
     * @param uri
     * @return
     */
    public static Set<String> getContextTerms(List<ConceptContext> conceptContexts, String uri) {
        Set<String> contextConceptTerms = new HashSet<String>();
//    System.out.println("concept : " + c);
//    System.out.println("concept id : " + c.getConceptId());
//    String id = Concept.getFullConceptUri(c.getConceptId());
//    System.out.println("full uri : " + id);
        for (ConceptContext cc : conceptContexts) {
            if (cc.getId().equals(uri)) {
//        System.out.println("Found concept context term: " + cc);
                for (String sub : cc.context) { // narrower context
                    String str = sub.replaceAll("_", " ");
                    str = str.replaceAll("\\s+", " "); // normalise spaces

                    // prepare for lookup in word embedding vocabulary
                    str = VectorOps.prepareStringSpaces(str);
                    contextConceptTerms.add(str);
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

//  public static Set<String> getContextTerms(List<ConceptContext> conceptContexts, String uri) {
//    Set<String> contextConceptTerms = new HashSet<String>();
//    for (ConceptContext cc : conceptContexts) {
//      if (cc.getId().equals(uri)) {
////        System.out.println("Found concept context term: " + cc);
//        for (String sub : cc.context) { // narrower context
//          String str = sub.replaceAll("_", " ");
//          str = str.replaceAll("\\s+", " "); // normalise spaces
//
//          // prepare for lookup in word embedding vocabulary
//          str = VectorOps.prepareStringSpaces(str);
//          contextConceptTerms.add(str);
//        }
//        break; // early exit: occurs once!
//      }
//    }
//
//    return contextConceptTerms;
//  }
    /**
     * Retrieves concatenated string of concept terms that are directly linked
     * to a concept.
     *
     * @param conceptContexts
     * @param uri
     * @return
     */
    public static String getContextTermsString(List<ConceptContext> conceptContexts, String uri) {
        String contextConceptTerms = "";
//    System.out.println("concept : " + c);
//    System.out.println("concept id : " + c.getConceptId());
//    String id = Concept.getFullConceptUri(c.getConceptId());
//    System.out.println("full uri : " + id);
        for (ConceptContext cc : conceptContexts) {
            if (cc.getId().equals(uri)) {
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
     *
     * @param concepts
     * @return
     */
    public static List<List<String>> getCollection(List<OntClass> concepts) {
        List<List<String>> coll = new ArrayList<>();
        for (OntClass c : concepts) {
            coll.add(getDocument(c));
        }

        return coll;
    }

    /**
     * Generates a concept document. The words in labels of a concept forms its
     * document (case insensitive).
     *
     * @param concept
     * @return
     */
    public static List<String> getDocument(OntClass concept) {
        List<String> doc = new ArrayList<>();
        for (String s : OntoOps.getLabels(concept)) {
            doc.addAll(Arrays.asList(s.toLowerCase().split("\\s+"))); // add all words in labels of a concept to document
        }

        return doc;
    }
}
