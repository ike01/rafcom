package uk.rgu.data.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author aikay
 */
public class AlignedConcept {

  public String concept_1;
  public String concept_2;
  public String relation;

  public AlignedConcept() {
  }

  public AlignedConcept(String concept_1, String concept_2, String relation) {
    this.concept_1 = concept_1;
    this.concept_2 = concept_2;
    this.relation = relation;
  }

  /**
   * Checks if a concept in present in a list of alignments.
   *
   * @param alignedConcept List of alignments.
   * @param conceptUrl Full URI of concept.
   * @return
   */
  public static List<AlignedConcept> containsAlignment(List<AlignedConcept> alignedConcept, String conceptUrl) {
    List<AlignedConcept> alignments = new ArrayList<>();
    for (AlignedConcept ac : alignedConcept) {
      if (ac.concept_1.equalsIgnoreCase(conceptUrl) || ac.concept_2.equalsIgnoreCase(conceptUrl)) {
        alignments.add(ac);
      }
    }

    return alignments;
  }

  public static boolean containsTheAlignment(List<AlignedConcept> alignedConcept, AlignedConcept alignment) {
    for (AlignedConcept ac : alignedConcept) {
      if ((ac.concept_1.equalsIgnoreCase(alignment.concept_1) && ac.concept_2.equalsIgnoreCase(alignment.concept_2))
              || (ac.concept_1.equalsIgnoreCase(alignment.concept_2) && ac.concept_2.equalsIgnoreCase(alignment.concept_1))) {
        return true;
      }
    }

    return false;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 37 * hash + Objects.hashCode(this.concept_1);
    hash = 37 * hash + Objects.hashCode(this.concept_2);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final AlignedConcept other = (AlignedConcept) obj;
    if (!Objects.equals(this.concept_1, other.concept_1)) {
      return false;
    }
    if (!Objects.equals(this.concept_2, other.concept_2)) {
      return false;
    }
    return true;
  }



  @Override
  public String toString() {
    return "AlignedConcept{" + "concept_1=" + concept_1 + ", concept_2=" + concept_2 + ", relation=" + relation + "}";
  }

}
