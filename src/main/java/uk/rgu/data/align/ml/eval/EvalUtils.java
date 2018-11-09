package uk.rgu.data.align.ml.eval;

import java.io.File;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

/**
 *
 * @author aikay
 */
public class EvalUtils {

  public static void convertCSVtoARFF(File csv, File arff) {
    try {
      // load the CSV file (input file)
      CSVLoader loader = new CSVLoader();
      loader.setSource(csv);
      String[] options = new String[1];
      options[0] = "-H";
      loader.setOptions(options);

      Instances data = loader.getDataSet();
      System.out.println(data);

      // save as an  ARFF (output file)
      ArffSaver saver = new ArffSaver();
      saver.setInstances(data);
      saver.setFile(arff);
      saver.writeBatch();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
