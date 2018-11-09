
package uk.rgu.data.ontologyprocessor;

/**
 *
 * @author 1113938
 */
public class Relation {
  public enum Predicate {
    /* Labels */
    PREF_LABEL("http://www.w3.org/2004/02/skos/core#prefLabel"),
    LABEL("http://www.w3.org/1999/02/22-rdf-syntax-ns#about"),
    /* Alternate Labels */
    ALT_LABEL("http://www.w3.org/2004/02/skos/core#altLabel"),
    DESCRIPTION("http://www.w3.org/2000/01/rdf-schema#description"),
    MESH_ALT("http://id.nlm.nih.gov/mesh/vocab#Term"),
    SAME_AS("http://www.w3.org/1999/02/22-rdf-syntax-ns#resource"),
    /* Empty */
    NIL("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"),
    /* Hierarchical - subclass-like */
    NARROWER("http://www.w3.org/2004/02/skos/core#narrower"),
    SUBDIVISIONS("http://data.bgs.ac.uk/ref/Geochronology/subDivisions"),
    MESH_NARROWER("http://id.nlm.nih.gov/mesh/vocab#narrowerConcept"),
    /* Hierarchical - superclass-like */
    BROADER("http://www.w3.org/2004/02/skos/core#broader"),
    SUPERDIVISION("http://data.bgs.ac.uk/ref/Geochronology/superDivision"), // does not exist in any triple
    MESH_BROADER("http://id.nlm.nih.gov/mesh/vocab#broaderConcept"),
    /* Related */
    RELATED("http://www.w3.org/2004/02/skos/core#related"),
    DEFINITION("http://www.w3.org/2004/02/skos/core#definition"),
    /* Alignment relations */
    EXACT_MATCH("http://www.w3.org/2004/02/skos/core#exactMatch"),
    CLOSE_MATCH("http://www.w3.org/2004/02/skos/core#closeMatch"),
    BROAD_MATCH("http://www.w3.org/2004/02/skos/core#broadMatch"),
    NARROW_MATCH("http://www.w3.org/2004/02/skos/core#narrowMatch"),
    SEMANTIC_RELATION("http://www.w3.org/2004/02/skos/core#semanticRelation");

    public final String value;

    private Predicate(String value) {
      this.value = value;
    }
  }
}
