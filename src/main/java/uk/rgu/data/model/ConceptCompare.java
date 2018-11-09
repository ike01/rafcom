package uk.rgu.data.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author 1113938
 */
public class ConceptCompare {

  public String conceptId;
  public String otherConceptId;
  public double score;

  public ConceptCompare() {
  }

  public ConceptCompare(String conceptId, String otherConceptId, double score) {
    this.conceptId = conceptId;
    this.otherConceptId = otherConceptId;
    this.score = score;
  }

  /**
   * Retrieves the similarity of two concepts from a list.
   * Assumes the symmetry property so that concepts can be supplied in any order.
   *
   * @param conceptSimilarities
   * @param conceptUri_1
   * @param conceptUri_2
   * @return
   */
  public static double getSimilarity(List<ConceptCompare> conceptSimilarities, String conceptUri_1, String conceptUri_2) {
    if (conceptUri_1.equalsIgnoreCase(conceptUri_2)) { // concepts uris should not be the same
      return -2.0;
    }

    for (ConceptCompare cc : conceptSimilarities) {
      if ((cc.conceptId.equalsIgnoreCase(conceptUri_1) || cc.otherConceptId.equalsIgnoreCase(conceptUri_1)) && (cc.conceptId.equalsIgnoreCase(conceptUri_2) || cc.otherConceptId.equalsIgnoreCase(conceptUri_2))) {
        return cc.score;
      }
    }

    return -1.0; // does not exist in list
  }

  /**
   * Retrieves the similarity of two concepts from a list.
   * Assumes the symmetry property so that concepts can be supplied in any order.
   *
   * @param conceptSimilarities
   * @param conceptUri_1
   * @param conceptUri_2
   * @return
   */
  public static double getSimilarity(Set<ConceptCompare> conceptSimilarities, String conceptUri_1, String conceptUri_2) {
    if (conceptUri_1.equalsIgnoreCase(conceptUri_2)) { // concepts uris should not be the same
      return -2.0;
    }

    for (ConceptCompare cc : conceptSimilarities) {
      if ((cc.conceptId.equalsIgnoreCase(conceptUri_1) || cc.otherConceptId.equalsIgnoreCase(conceptUri_1)) && (cc.conceptId.equalsIgnoreCase(conceptUri_2) || cc.otherConceptId.equalsIgnoreCase(conceptUri_2))) {
        return cc.score;
      }
    }

    return -1.0; // does not exist in list
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.conceptId);
    hash = 17 * hash + Objects.hashCode(this.otherConceptId);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ConceptCompare other = (ConceptCompare) obj;
    if (!Objects.equals(this.conceptId, other.conceptId)) {
      return false;
    }
    return Objects.equals(this.otherConceptId, other.otherConceptId);
  }

  @Override
  public String toString() {
    return "ConceptCompare{" + "conceptId=" + conceptId + ", otherConceptId=" + otherConceptId + ", score=" + score + '}';
  }

}
