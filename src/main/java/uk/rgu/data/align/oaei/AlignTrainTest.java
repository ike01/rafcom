package uk.rgu.data.align.oaei;

import java.io.File;
import java.util.List;

/**
 *
 * @author 1113938
 */
public class AlignTrainTest {

  File sourceOnto;
  File targetOnto;
  File referenceAlignment;
  String evaluationResult;
  int expectedClassCount;

  public AlignTrainTest(File sourceOnto, File targetOnto, File referenceAlignment, String evaluationResult) {
    this.sourceOnto = sourceOnto;
    this.targetOnto = targetOnto;
    this.referenceAlignment = referenceAlignment;
    this.evaluationResult = evaluationResult;
  }

  public String getEvaluationResult() {
    return evaluationResult;
  }

  public void setEvaluationResult(String evaluationResult) {
    this.evaluationResult = evaluationResult;
  }

  public int getExpectedClassCount() {
    return expectedClassCount;
  }

  public void setExpectedClassCount(int expectedClassCount) {
    this.expectedClassCount = expectedClassCount;
  }

  @Override
  public String toString() {
    return "AlignTrainTest{" + "sourceOnto=" + sourceOnto + ", targetOnto=" + targetOnto + ", referenceAlignment=" + referenceAlignment + ", evaluationResult=" + evaluationResult + ", expectedClassCount=" + expectedClassCount + '}';
  }

}
