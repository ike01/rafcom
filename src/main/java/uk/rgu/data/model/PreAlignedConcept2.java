package uk.rgu.data.model;

/**
 * Two similarity values for hybrid alignment.
 * 
 * @author aikay
 */
public class PreAlignedConcept2 {

  public AlignedConcept alignedConcept;
  public double similarity_1;
  public double similarity_2;

  public PreAlignedConcept2() {
  }

  public PreAlignedConcept2(AlignedConcept alignedConcept, double similarity_1, double similarity_2) {
    this.alignedConcept = alignedConcept;
    this.similarity_1 = similarity_1;
    this.similarity_2 = similarity_2;
  }

}
