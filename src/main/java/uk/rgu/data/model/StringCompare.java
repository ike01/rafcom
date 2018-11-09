package uk.rgu.data.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author 1113938
 */
public class StringCompare {

  public String s1;
  public String s2;
  public double score;
  public double weightedScore;

  public StringCompare() {
  }

  public StringCompare(String s1, String s2, double score) {
    this.s1 = s1;
    this.s2 = s2;
    this.score = score;
  }

  public StringCompare(String s1, String s2, double score, double weightedScore) {
    this.s1 = s1;
    this.s2 = s2;
    this.score = score;
    this.weightedScore = weightedScore;
  }

  /**
   * Checks if a word has been seen.
   * @param list
   * @param sc
   * @return
   */
  public static boolean hasAnyString(List<StringCompare> list, StringCompare sc) {
    return list.stream().anyMatch((c) -> (c.s1.equalsIgnoreCase(sc.s1) || c.s2.equalsIgnoreCase(sc.s2)));
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.s1);
    hash = 17 * hash + Objects.hashCode(this.s2);
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
    final StringCompare other = (StringCompare) obj;
    if (!Objects.equals(this.s1, other.s1)) {
      return false;
    }
    return Objects.equals(this.s2, other.s2);
  }

  @Override
  public String toString() {
    return "StringCompare{" + "s1=" + s1 + ", s2=" + s2 + ", score=" + score + ", weightedScore=" + weightedScore + "}";
  }

  /**
   * Custom comparator to sort StringCompares in descending order of score.
   */
  public static class StringCompareComparator implements Comparator<StringCompare> {

    @Override
    public int compare(StringCompare sc1, StringCompare sc2) {
      double diff = sc1.score - sc2.score;
      return diff < 0 ? 1 : (diff > 0 ? -1 : 0);
    }
  }

}
