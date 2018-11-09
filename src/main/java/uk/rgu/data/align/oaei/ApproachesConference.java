package uk.rgu.data.align.oaei;

import fr.inrialpes.exmo.align.impl.BasicParameters;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.method.StringDistAlignment;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Evaluator;

/**
 *
 * @author 1113938
 */
public class ApproachesConference {

  public static void main(String[] arg) {
    try {
//      wordEmb();
      stringEquiv();
    } catch (AlignmentException ex) {
      Logger.getLogger(ApproachesConference.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public static void stringEquiv() throws AlignmentException { // string equality applied on local names of entities which were lowercased
    System.out.println("\n===StringEquiv===");
    List<AlignTrainTest> allTestcaseData = Alignment_oaei.generateConfAlignTrainTest();
    List<Integer> found = new ArrayList();
    List<Integer> correct = new ArrayList();
    List<Integer> expected = new ArrayList();

    Properties params = new BasicParameters();

    for (AlignTrainTest alignTrainTest : allTestcaseData) {
      URI onto1 = alignTrainTest.sourceOnto.toPath().toUri();
      URI onto2 = alignTrainTest.targetOnto.toPath().toUri();

      AlignmentProcess a1 = new StringEquiv();
      a1.init(onto1, onto2);
      a1.align((Alignment) null, params);
      a1.cut(1.0);

      // Load the reference alignment
      AlignmentParser aparser = new AlignmentParser(0);
      Alignment reference = aparser.parse(alignTrainTest.referenceAlignment.toPath().toUri());

      // Evaluate alignment
      Evaluator evaluator = new PRecEvaluator(reference, a1);
      evaluator.eval(new Properties());

      // Print results
      System.out.println(alignTrainTest);
      double p = ((PRecEvaluator) evaluator).getPrecision();
      double r = ((PRecEvaluator) evaluator).getRecall();

      found.add(a1.nbCells());
      correct.add(((PRecEvaluator) evaluator).getCorrect());
//      expected.add(((PRecEvaluator) evaluator).getExpected());
      expected.add(alignTrainTest.expectedClassCount);

      System.err.println("Precision: " + p + " Recall: " + r + " F-measure: " + ((PRecEvaluator) evaluator).getFmeasure() + " over " + a1.nbCells() + " cells");
    }

    System.out.println(found);
    System.out.println(correct);
    System.out.println(expected);

    double precision = HarmonicPR.hPrecision(correct, found);
    double recall = HarmonicPR.hRecall(correct, expected);
    double f1 = 2 * precision * recall / (precision + recall);

    System.out.println();
    System.out.println("H(p) = " + precision + " H(r) = " + recall + " H(fm) = " + f1);
  }

  public static void edna() throws AlignmentException { // string editing distance matcher
    System.out.println("\n===edna===");
    List<AlignTrainTest> allTestcaseData = Alignment_oaei.generateConfAlignTrainTest();

    double t = 0.88;

//    for (double t = start; t < .95; t += 0.001) {
    List<Integer> found = new ArrayList();
    List<Integer> correct = new ArrayList();
    List<Integer> expected = new ArrayList();

    Properties params = new BasicParameters();

    for (AlignTrainTest alignTrainTest : allTestcaseData) {
      URI onto1 = alignTrainTest.sourceOnto.toPath().toUri();
      URI onto2 = alignTrainTest.targetOnto.toPath().toUri();

      AlignmentProcess a1 = new EditDist();
//        AlignmentProcess a1 = new StringDistAlignment();
      a1.init(onto1, onto2);
      params = new Properties();
//        params.setProperty("stringFunction", "levenshteinDistance");
      a1.align((Alignment) null, params);
      a1.cut(t);

      // Load the reference alignment
      AlignmentParser aparser = new AlignmentParser(0);
      Alignment reference = aparser.parse(alignTrainTest.referenceAlignment.toPath().toUri());

      // Evaluate alignment
      Evaluator evaluator = new PRecEvaluator(reference, a1);
      evaluator.eval(new Properties());

      // Print results
//        System.out.println(alignTrainTest);
      double p = ((PRecEvaluator) evaluator).getPrecision();
      double r = ((PRecEvaluator) evaluator).getRecall();

      found.add(a1.nbCells());
      correct.add(((PRecEvaluator) evaluator).getCorrect());
//      expected.add(((PRecEvaluator) evaluator).getExpected());
      expected.add(alignTrainTest.expectedClassCount);

//      System.err.println("Precision: " + p + " Recall: " + r + " F-measure: " + ((PRecEvaluator) evaluator).getFmeasure() + " over " + a1.nbCells() + " cells");
    }

//    System.out.println(found);
//    System.out.println(correct);
//    System.out.println(expected);
    double precision = HarmonicPR.hPrecision(correct, found);
    double recall = HarmonicPR.hRecall(correct, expected);
    double f1 = 2 * precision * recall / (precision + recall);

//      System.out.println();
    System.out.println("Threshold = " + t + " H(p) = " + precision + " H(r) = " + recall + " H(fm) = " + f1);
//    }
  }

  public static void wordEmb() throws AlignmentException {
    System.out.println("\n===WordEmb===");
    List<AlignTrainTest> allTestcaseData = Alignment_oaei.generateConfAlignTrainTest();

    double start = .8;

    for (double t = start; t < 1.0; t += 0.01) {
      Properties params = new BasicParameters();
      List<Integer> found = new ArrayList();
      List<Integer> correct = new ArrayList();
      List<Integer> expected = new ArrayList();

      for (AlignTrainTest alignTrainTest : allTestcaseData) {
        URI onto1 = alignTrainTest.sourceOnto.toPath().toUri();
        URI onto2 = alignTrainTest.targetOnto.toPath().toUri();

        AlignmentProcess a1 = new WordEmb();
        a1.init(onto1, onto2);
        params = new Properties();
//        params.setProperty("threshold", Double.toString(t));
        a1.align((Alignment) null, params);
        a1.cut(t);

        // Load the reference alignment
        AlignmentParser aparser = new AlignmentParser(0);
        Alignment reference = aparser.parse(alignTrainTest.referenceAlignment.toPath().toUri());

        // Evaluate alignment
        Evaluator evaluator = new PRecEvaluator(reference, a1);
        evaluator.eval(new Properties());

        // Print results
//        System.out.println(alignTrainTest);
        double p = ((PRecEvaluator) evaluator).getPrecision();
        double r = ((PRecEvaluator) evaluator).getRecall();

        found.add(a1.nbCells());
        correct.add(((PRecEvaluator) evaluator).getCorrect());
//        expected.add(((PRecEvaluator) evaluator).getExpected());
        expected.add(alignTrainTest.expectedClassCount);

//      System.err.println("Precision: " + p + " Recall: " + r + " F-measure: " + ((PRecEvaluator) evaluator).getFmeasure() + " over " + a1.nbCells() + " cells");
      }

//    System.out.println(found);
//    System.out.println(correct);
//    System.out.println(expected);
      double precision = HarmonicPR.hPrecision(correct, found);
      double recall = HarmonicPR.hRecall(correct, expected);
      double f1 = 2 * precision * recall / (precision + recall);

      System.out.println("Threshold = " + t + " H(p) = " + precision + " H(r) = " + recall + " H(fm) = " + f1);
    }
  }

  public static void hybrid() throws AlignmentException {
    System.out.println("\n===Hybrid===");
    List<AlignTrainTest> allTestcaseData = Alignment_oaei.generateConfAlignTrainTest();

    double start = .7;

    for (double t = start; t < 1.0; t += 0.01) {
      List<Integer> found = new ArrayList();
      List<Integer> correct = new ArrayList();
      List<Integer> expected = new ArrayList();

      Properties params = new BasicParameters();

      for (AlignTrainTest alignTrainTest : allTestcaseData) {
        URI onto1 = alignTrainTest.sourceOnto.toPath().toUri();
        URI onto2 = alignTrainTest.targetOnto.toPath().toUri();

        AlignmentProcess a1 = new Hybrid();
        a1.init(onto1, onto2);
        params = new Properties();
        a1.align((Alignment) null, params);
        a1.cut(t);

        // Load the reference alignment
        AlignmentParser aparser = new AlignmentParser(0);
        Alignment reference = aparser.parse(alignTrainTest.referenceAlignment.toPath().toUri());

        // Evaluate alignment
        Evaluator evaluator = new PRecEvaluator(reference, a1);
        evaluator.eval(new Properties());

        // Print results
//        System.out.println(alignTrainTest);
        double p = ((PRecEvaluator) evaluator).getPrecision();
        double r = ((PRecEvaluator) evaluator).getRecall();

        found.add(a1.nbCells());
        correct.add(((PRecEvaluator) evaluator).getCorrect());
//        expected.add(((PRecEvaluator) evaluator).getExpected());
        expected.add(alignTrainTest.expectedClassCount);

//        System.err.println("Precision: " + p + " Recall: " + r + " F-measure: " + ((PRecEvaluator) evaluator).getFmeasure() + " over " + a1.nbCells() + " cells");
      }

//    System.out.println(found);
//    System.out.println(correct);
//    System.out.println(expected);
      double precision = HarmonicPR.hPrecision(correct, found);
      double recall = HarmonicPR.hRecall(correct, expected);
      double f1 = 2 * precision * recall / (precision + recall);

      System.out.println("Threshold = " + t + " | H(p) = " + precision + ", H(r) = " + recall + ", H(fm) = " + f1 + " | Alignments expected " + HarmonicPR.sum(expected) + ", found " + HarmonicPR.sum(found));
    }
  }

}
