package uk.rgu.data.ontologyprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author 1113938
 */
public class Alignment {

  private static final String BASE_URL = "";

  public enum AlignmentGraph {

    EUROVOC_GEMET("eurovoc_gemet",
            "http://eurovoc.europa.eu/",
            "EUROVOC",
            BASE_URL + "TDBStore/eurovoc_gemet",
            BASE_URL + "RDFSource/EUROVOC-GEMET/align_EuroVoc_Gemet.rdf",
            "http://www.eionet.europa.eu/gemet/concept/",
            "GEMET"
    );

    public final String value;
    public final String conceptUri;
    public final String conceptScheme;
    public final String dbPath;
    public final String sourcePath;
    public final String alignedConceptUri;
    public final String alignedConceptScheme;

    private AlignmentGraph(String value,
            String conceptUri,
            String conceptScheme,
            String dbPath,
            String sourcePath,
            String alignedConceptUri,
            String alignedConceptScheme
    ) {
      this.value = value;
      this.conceptUri = conceptUri;
      this.conceptScheme = conceptScheme;
      this.dbPath = dbPath;
      this.sourcePath = sourcePath;
      this.alignedConceptUri = alignedConceptUri;
      this.alignedConceptScheme = alignedConceptScheme;
    }
  }

  /**
   * Get all graphs.
   *
   * @return List of all graphs.
   */
  public static List<AlignmentGraph> allAlignments() {
    List<AlignmentGraph> graphs = new ArrayList();
    graphs.add(AlignmentGraph.EUROVOC_GEMET);

    return graphs;
  }

  /**
   * Retrieves a graph using its unique name (also known as scheme).
   *
   * @param name
   * @return
   */
  public static AlignmentGraph getAlignmentGraph(String name) {
    for (AlignmentGraph graph : allAlignments()) {
      if (graph.name().equalsIgnoreCase(name)) {
        return graph;
      }
    }
    return null;
  }

}
