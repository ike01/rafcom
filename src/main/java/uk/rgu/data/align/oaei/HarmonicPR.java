package uk.rgu.data.align.oaei;

import java.util.List;

/**
 * Averages precisions and recalls as used for OAEI.
 *
 * @author 1113938
 */
public class HarmonicPR {

  public static double hPrecision(List<Integer> correct, List<Integer> found) {
    double totalCorrect = .0;
    double totalFound = .0;

    for (int c : correct) {
      totalCorrect += c;
    }

    for (int f : found) {
      totalFound += f;
    }

    return (double) totalCorrect / totalFound;
  }


  public static double hRecall(List<Integer> correct, List<Integer> expected) {
    double totalCorrect = .0;
    double totalExpected = .0;

    for (int c : correct) {
      totalCorrect += c;
    }

    for (int f : expected) {
      totalExpected += f;
    }

    return (double) totalCorrect / totalExpected;
  }

  public static int sum(List<Integer> numbers) {
    int sum = 0;
    for (int c : numbers) {
      sum += c;
    }

    return sum;
  }

}
