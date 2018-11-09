package uk.rgu.data.align.ml.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.FastVector;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 *
 * @author aikay
 */
public class EvalOAEIBenc {

  public static void main(String[] args) throws IOException, Exception {
    int tp = 0; // true positives
    int fp = 0; // false positives

    int numOfFolds = 4;
    FastVector predictions = new FastVector();
    for (int i = 1; i <= numOfFolds; i++) {
      String trainPath = "C:\\dev\\bitbucket\\rafcom\\results\\oaei_benc_google\\" + i + "\\train.arff";
      String testPath =  "C:\\dev\\bitbucket\\rafcom\\results\\oaei_benc_google\\" + i + "\\test.arff";
      // Read in data
      BufferedReader trainfile = readDataFile(trainPath);
      Instances trainData = new Instances(trainfile);
      String[] options = new String[2];
      options[0] = "-R";                                    // "range"
      options[1] = "1";                                     // first attribute (id)
      Remove trainRemove = new Remove();                         // new instance of filter
      trainRemove.setOptions(options);                           // set options
      trainRemove.setInputFormat(trainData);                     // inform filter about dataset **AFTER** setting options
      trainData = Filter.useFilter(trainData, trainRemove);      // apply filter

      String name = trainData.attribute(0).name();
      System.out.println("First attribute => " + name);

      BufferedReader testfile = readDataFile(testPath);
      Instances testData = new Instances(testfile);
      options = new String[2];
      options[0] = "-R";                                    // "range"
      options[1] = "1";                                     // first attribute (id)
      Remove testRemove = new Remove();                         // new instance of filter
      testRemove.setOptions(options);                           // set options
      testRemove.setInputFormat(testData);                     // inform filter about dataset **AFTER** setting options
      testData = Filter.useFilter(testData, testRemove);      // apply filter

      trainData.setClassIndex(trainData.numAttributes() - 1);
      testData.setClassIndex(testData.numAttributes() - 1);
      Evaluation eval = classify(trainData, testData);
      predictions.appendElements(eval.predictions());
      System.out.println("=== START ===");
      System.out.println("Fold " + i);
      System.out.println(eval.toSummaryString());
      // Get the confusion matrix
      System.out.println("*** CONFUSION MATRIX ***");
      double[][] cmMatrix = eval.confusionMatrix();
      for (double[] arr : cmMatrix) {
        System.out.println(Arrays.toString(arr));
      }
      tp += cmMatrix[1][1];
      fp += cmMatrix[0][1];
      System.out.println("=== END ===");
    }
    // Calculate overall accuracy of current classifier on all splits
    System.out.println("---------------------------------");
    double accuracy = calculateAccuracy(predictions);

    // Print current classifier's name and accuracy in a complicated, but nice-looking way.
    System.out.println("Accuracy of model: "
            + String.format("%.2f%%", accuracy)
            + "\n---------------------------------");
    double prec = (double)tp / (tp + fp);
    double rec = (double)tp / 94;
    double f1 = ((double) 2 * prec * rec) / (prec + rec);
    System.out.println("Precision: " + prec);
    System.out.println("Recall: " + rec);
    System.out.println("F1: " + f1);
  }

  public static BufferedReader readDataFile(String filename) {
    BufferedReader inputReader = null;

    try {
      inputReader = new BufferedReader(new FileReader(filename));
    } catch (FileNotFoundException ex) {
      System.err.println("File not found: " + filename);
    }

    return inputReader;
  }

  private static int[] selectAttributes(Instances trainData) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  public static Evaluation classify(Instances trainingSet, Instances testingSet) throws Exception {
    RandomForest model = new RandomForest();;
    String[] options = weka.core.Utils.splitOptions("-P 100 -I 2000 -num-slots 1 -K 0 -M 1.0 -V 0.001 -S 1");
    model.setOptions(options);
    Evaluation evaluation = new Evaluation(trainingSet);
    model.buildClassifier(trainingSet);
    evaluation.evaluateModel(model, testingSet);
    return evaluation;
  }

  public static Evaluation classify2(Instances trainingSet, Instances testingSet) throws Exception {
    AttributeSelection as = new AttributeSelection();
    ASSearch asSearch = ASSearch.forName("weka.attributeSelection.GreedyStepwise", new String[]{"-C", "-B", "-R"});
    as.setSearch(asSearch);
    ASEvaluation asEval = ASEvaluation.forName("weka.attributeSelection.CfsSubsetEval", new String[]{});
    as.setEvaluator(asEval);
    as.SelectAttributes(trainingSet);
    trainingSet = as.reduceDimensionality(trainingSet);
    Classifier classifier = AbstractClassifier.forName("weka.classifiers.lazy.LWL", new String[]{"-U", "4", "-A", "weka.core.neighboursearch.LinearNNSearch", "-W", "weka.classifiers.trees.RandomForest", "--", "-I", "96", "-K", "5", "-depth", "18"});
//    classifier.buildClassifier(trainingSet);

    Evaluation evaluation = new Evaluation(trainingSet);
    classifier.buildClassifier(trainingSet);
    evaluation.evaluateModel(classifier, testingSet);
//System.out.println(model.toString());
    return evaluation;
  }

  public static Evaluation classify3(Instances trainingSet, Instances testingSet) throws Exception {
    Classifier classifier = AbstractClassifier.forName("weka.classifiers.meta.RandomCommittee", new String[]{"-I", "13", "-S", "1", "-W", "weka.classifiers.trees.RandomTree", "--", "-M", "5", "-K", "3", "-depth", "0", "-N", "0"});
    Evaluation evaluation = new Evaluation(trainingSet);
    classifier.buildClassifier(trainingSet);
    evaluation.evaluateModel(classifier, testingSet);
    System.out.println(classifier.toString());
    return evaluation;
  }

  public static Evaluation classify4(Instances trainingSet, Instances testingSet) throws Exception {
    AttributeSelection as = new AttributeSelection();
    ASSearch asSearch = ASSearch.forName("weka.attributeSelection.GreedyStepwise", new String[]{"-C", "-R"});
    as.setSearch(asSearch);
    ASEvaluation asEval = ASEvaluation.forName("weka.attributeSelection.CfsSubsetEval", new String[]{"-L"});
    as.setEvaluator(asEval);
    as.SelectAttributes(trainingSet);
    trainingSet = as.reduceDimensionality(trainingSet);
    Classifier classifier = AbstractClassifier.forName("weka.classifiers.trees.RandomForest", new String[]{"-I", "239", "-K", "0", "-depth", "13"});

    Evaluation evaluation = new Evaluation(trainingSet);
    classifier.buildClassifier(trainingSet);
    evaluation.evaluateModel(classifier, testingSet);
    System.out.println(classifier.toString());
    return evaluation;
  }

  public static double calculateAccuracy(FastVector predictions) {
    double correct = 0;

    for (int i = 0; i < predictions.size(); i++) {
      NominalPrediction np = (NominalPrediction) predictions.elementAt(i);
      if (np.predicted() == np.actual()) {
        correct++;
      }
    }
    System.out.println("Correct = " + correct);
    return (double) 100 * correct / predictions.size();
  }
}
