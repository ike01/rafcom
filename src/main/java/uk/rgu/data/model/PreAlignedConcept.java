package uk.rgu.data.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author aikay
 */
public class PreAlignedConcept {

  public AlignedConcept alignedConcept;
  public double confidence;

  public PreAlignedConcept() {
  }

  public PreAlignedConcept(AlignedConcept alignedConcept, double confidence) {
    this.alignedConcept = alignedConcept;
    this.confidence = confidence;
  }

  /**
   * Adds an alignment if deemed appropriate. Multiple alignments not allowed at
   * the moment.
   *
   * @param alignments
   * @param preAlignedConcept
   * @return
   */
  public static List<PreAlignedConcept> updateAlignments(List<PreAlignedConcept> alignments, PreAlignedConcept preAlignedConcept) {
    boolean isInList = false;
    boolean addNew = false;
    for (Iterator<PreAlignedConcept> iterator = alignments.iterator(); iterator.hasNext();) {
      PreAlignedConcept pac = iterator.next();
//      System.out.println("pac: " + pac);
      if (pac.alignedConcept.concept_1.equals(preAlignedConcept.alignedConcept.concept_1) ||
              pac.alignedConcept.concept_2.equals(preAlignedConcept.alignedConcept.concept_2)) {
        isInList = true;
        if (preAlignedConcept.confidence > pac.confidence) {
          iterator.remove(); // remove
          addNew = true; // signal to add
        } else if (preAlignedConcept.confidence == pac.confidence) {
          addNew = true; // add without removing
        }
      }
    }

    if (!isInList || addNew) {
      alignments.add(preAlignedConcept);
    }

    return alignments;
  }

  /**
   * Retrieves alignments.
   *
   * @param alignments
   * @return
   */
  public static List<AlignedConcept> getAlignments(List<PreAlignedConcept> alignments) {
    List<AlignedConcept> finalAlignments = new ArrayList<AlignedConcept>();
    for (PreAlignedConcept pac : alignments) {
      finalAlignments.add(pac.alignedConcept);
    }

    return finalAlignments;
  }

  @Override
  public String toString() {
    return "PreAlignedConcept{" + "alignedConcept=" + alignedConcept + ", confidence=" + confidence + '}';
  }

  public static void main(String[] args) {
    PreAlignedConcept p1 = new PreAlignedConcept(new AlignedConcept("a", "b", "r"), 0.3);
    PreAlignedConcept p2 = new PreAlignedConcept(new AlignedConcept("x", "y", "r"), 0.3);
    List<PreAlignedConcept> lp = new ArrayList<PreAlignedConcept>();
    lp.add(p1);
    lp.add(p2);

    AlignedConcept ac = new AlignedConcept("x", "z", "r");
    lp = PreAlignedConcept.updateAlignments(lp, new PreAlignedConcept(ac, 0.3));

    List<AlignedConcept> recommendedAlignments = PreAlignedConcept.getAlignments(lp);
    recommendedAlignments.forEach(System.out::println);
  }
}
