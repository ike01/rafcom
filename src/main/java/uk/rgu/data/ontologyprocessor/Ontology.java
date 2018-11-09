package uk.rgu.data.ontologyprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author 1113938
 */
public class Ontology {

  private static final String BASE_URL = "";

  public enum Graph {

    RCS("rcs",
            "http://data.bgs.ac.uk/id/EarthMaterialClass/RockName/",
            BASE_URL + "TDBStore/rcs",
            BASE_URL + "RDFSource/EarthMaterialClass_RockName.nt",
            "",
            "http://data.bgs.ac.uk/ref/EarthMaterialClass/subClassMain",
            "http://data.bgs.ac.uk/ref/EarthMaterialClass/superClassMain",
            7,
            false,
            false,
            false,
            new ArrayList()
    ),
    LEXICON("lexicon",
            "http://data.bgs.ac.uk/id/Lexicon/NamedRockUnit/",
            BASE_URL + "TDBStore/lexicon",
            BASE_URL + "RDFSource/Lexicon_NamedRockUnit.nt",
            "",
            "http://www.w3.org/2004/02/skos/core#narrower",
            "http://www.w3.org/2004/02/skos/core#broader",
            8,
            true,
            false,
            false,
            new ArrayList()
    ),
    CHRONOSTRAT("chronostrat",
            "http://data.bgs.ac.uk/id/Geochronology/Division/",
            BASE_URL + "TDBStore/chronostrat",
            BASE_URL + "RDFSource/Geochronology_Division.nt",
            "http://data.bgs.ac.uk/ref/Geochronology/Division/XX",
            "",
            "",
            4,
            true,
            false,
            true,
            Arrays.asList("marine")
    ),
    THESAURUS("thesaurus",
            "http://data.bgs.ac.uk/id/GeoscienceThesaurus/Concept/",
            BASE_URL + "TDBStore/thesaurus",
            BASE_URL + "RDFSource/Geoscience_Thesaurus.rdf",
            "http://data.bgs.ac.uk/id/GeoscienceThesaurus/Concept/0",
            "http://www.w3.org/2004/02/skos/core#narrower",
            "http://www.w3.org/2004/02/skos/core#broader",
            6,
            true,
            false,
            false,
            new ArrayList()
    ),
    NEW_THESAURUS("new_thesaurus",
            "http://data.bgs.ac.uk/id/GeoscienceThesaurus/Concept/",
            BASE_URL + "TDBStore/thesaurus_20151130",
            BASE_URL + "RDFSource/thesaurus_20151130.nt",
            "http://data.bgs.ac.uk/id/GeoscienceThesaurus/Concept/0",
            "http://www.w3.org/2004/02/skos/core#narrower",
            "http://www.w3.org/2004/02/skos/core#broader",
            6,
            true,
            false,
            false,
            new ArrayList()
    ),
    SWEET("sweet",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#about#",
            BASE_URL + "TDBStore/sweet",
            BASE_URL + "RDFSource/SWEET/sweetAll.owl", // <http://www.w3.org/2002/07/owl#sameAs http://www.w3.org/1999/02/22-rdf-syntax-ns#resource/>
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#about",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#resource", // <http://www.w3.org/2000/01/rdf-schema#subClassOf http://www.w3.org/1999/02/22-rdf-syntax-ns#resource/>
            "", // http://id.nlm.nih.gov/mesh/vocab#broaderConcept
            -1,
            true,
            false,
            false,
            new ArrayList()
    ),
    GEMET("gemet",
            "http://www.eionet.europa.eu/gemet/concept/",
            BASE_URL + "TDBStore/gemet",
            BASE_URL + "RDFSource/GEMET/gemet.rdf",
            "",
            "http://www.w3.org/2004/02/skos/core#narrower",
            "http://www.w3.org/2004/02/skos/core#broader",
            -1,
            true,
            false,
            false,
            new ArrayList()
    ),
    AGROVOC("agrovoc",
            "http://aims.fao.org/aos/agrovoc/",
            BASE_URL + "TDBStore/agrovoc",
            BASE_URL + "RDFSource/AGROVOC/agrovoc_2016-07-15_core.rdf",
            "",
            "http://www.w3.org/2004/02/skos/core#narrower",
            "http://www.w3.org/2004/02/skos/core#broader",
            -1,
            true,
            false,
            false,
            new ArrayList()
    ),
    EUROVOC("eurovoc",
            "http://eurovoc.europa.eu/",
            BASE_URL + "TDBStore/eurovoc",
            BASE_URL + "RDFSource/EUROVOC/eurovoc_skos.rdf",
            "",
            "",
            "http://www.w3.org/2004/02/skos/core#broader",
            -1,
            true,
            false,
            false,
            new ArrayList()
    ),
    MESH("mesh",
            "http://www.nlm.nih.gov/mesh/2006#",
            BASE_URL + "TDBStore/mesh",
            BASE_URL + "RDFSource/mesh2006.rdf",
            "",
            "", // http://id.nlm.nih.gov/mesh/vocab#narrowerConcept
            "http://www.w3.org/2004/02/skos/core#broader", // http://id.nlm.nih.gov/mesh/vocab#broaderConcept
            -1,
            true,
            false,
            false,
            new ArrayList()
    );

    public final String value;
    public final String conceptUri;
    public final String dbPath;
    public final String sourcePath;
    public final String topConcept;
    public final String subClass;
    public final String superClass;
    public final int maxNGrams; // Maximum number of grams in the ontology
    public final boolean altLabel; // True indicates that the ontology has alt labels
    public final boolean descriptionAsAlt;
    public final boolean bracketAsAlt; // Terms in bracket are considered alternative labels if true (used to only  provide context otherwise)
    public final List<String> stopwords;

    private Graph(String value,
            String conceptUri,
            String dbPath,
            String sourcePath,
            String topConcept,
            String subClass,
            String superClass,
            int maxNGrams,
            boolean altLabel,
            boolean descriptionAsAlt,
            boolean bracketAsAlt,
            List<String> stopwords
    ) {
      this.value = value;
      this.conceptUri = conceptUri;
      this.dbPath = dbPath;
      this.sourcePath = sourcePath;
      this.topConcept = topConcept;
      this.subClass = subClass;
      this.superClass = superClass;
      this.maxNGrams = maxNGrams;
      this.altLabel = altLabel;
      this.descriptionAsAlt = descriptionAsAlt;
      this.bracketAsAlt = bracketAsAlt;
      this.stopwords = stopwords;
    }
  }

  /**
   * Get all graphs.
   *
   * @return List of all graphs.
   */
  public static List<Graph> allGraphs() {
    List<Graph> graphs = new ArrayList();
    graphs.add(Graph.CHRONOSTRAT);
    graphs.add(Graph.RCS);
    graphs.add(Graph.LEXICON);
    graphs.add(Graph.THESAURUS);
    graphs.add(Graph.NEW_THESAURUS);
    graphs.add(Graph.SWEET);
    graphs.add(Graph.GEMET);
    graphs.add(Graph.AGROVOC);
    graphs.add(Graph.MESH);
    graphs.add(Graph.EUROVOC);

    return graphs;
  }

  public static Graph getGraph(String name) {
    for (Graph graph : allGraphs()) {
      if (graph.name().equalsIgnoreCase(name)) {
        return graph;
      }
    }
    return null;
  }

}
