package uk.rgu.data.model;

import java.util.Comparator;

/**
 * Models concept recommendation to annotate a text.
 *
 * @author Ikechukwu
 */
public class RecommendedConcept extends Concept {

  private double score;
  public int matchType; // 1=hybrid, 2=stoilos, 3=context

  public RecommendedConcept() {
  }

  public RecommendedConcept(Concept concept, double score) {
    super(concept.getId(), concept.getLabel(), concept.getScheme());
    this.score = score;
  }

  public RecommendedConcept(Concept concept, double score, int matchType) {
    super(concept.getId(), concept.getLabel(), concept.getScheme());
    this.score = score;
    this.matchType = matchType;
  }

  public RecommendedConcept(String id, String label, String scheme, double score) {
    super(id, label, scheme);
    this.score = score;
  }


  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final RecommendedConcept other = (RecommendedConcept) obj;
    Concept c = new Concept(other.getId(), other.getScheme());
    return super.equals(c);
  }

  @Override
  public String toString() {
    return " RecommendedConcept{" + super.toString() + " | score=" + score + '}';
  }

  /**
   * Custom comparator to sort RecommendedConcepts in descending order of score.
   */
  public static class RecommendedConceptComparator implements Comparator<RecommendedConcept> {

    @Override
    public int compare(RecommendedConcept t, RecommendedConcept t1) {
      double diff = t.getScore() - t1.getScore();
      return diff < 0 ? 1 : (diff > 0 ? -1 : 0);
    }

  }

}
