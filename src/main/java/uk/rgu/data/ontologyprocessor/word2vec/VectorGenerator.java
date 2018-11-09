package uk.rgu.data.ontologyprocessor.word2vec;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

/**
 *
 * @author aikay
 */
public class VectorGenerator {
  private static final Logger LOG = Logger.getLogger(VectorGenerator.class.getName());

//  private static final String TEXT_PATH = "/program-data/OntologyProcessor2/wikipedia_annotated.txt";
  private static final String TEXT_PATH = "/program-data/wikipedia.txt";
//  private static final String MODEL = "data/model/wikipedia_plain_model300_min5_iter20_custom_token.txt";
  static String MODEL = "C:/dev/rgu/word2vec/models/GoogleNews-vectors-negative300.bin.gz";
//  private static final String MODEL = "data/model/GoogleNews-vectors-negative300.bin.gz";
  static String MODEL_FILE_PATH = Paths.get(MODEL).toAbsolutePath().toString();

  static Word2Vec vec;

  public static void main(String[] args) throws Exception {
//    createVectors(TEXT_PATH, MODEL_FILE_PATH); // generate word vectors
    LOG.info("Loading vector model back into memory and performing tests ... ");
    setModel(MODEL_FILE_PATH);

    LOG.info("Closest Words:");
    Collection<String> lst = vec.wordsNearest("the", 10);
    System.out.println("10 Words closest to 'the': " + lst);

    Collection<String> a = vec.wordsNearest("biomarkers", 10);
    System.out.println("10 Words closest to 'biomarkers': " + a);

    Collection<String> b = vec.wordsNearest("gene", 10);
    System.out.println("10 Words closest to 'gene': " + b);

    Collection<String> c = vec.wordsNearest("author", 10);
    System.out.println("10 Words closest to 'author': " + c);

    Collection<String> d = vec.wordsNearest("conference", 10);
    System.out.println("10 Words closest to 'conference': " + d);

    Collection<String> e = vec.wordsNearest("New_York", 10);
    System.out.println("10 Words closest to 'New_York': " + e);

    LOG.info("Vector similarity:");
    System.out.println("Similarity between 'New_York' and 'San_Francisco': " + vec.similarity("New_York", "San_Francisco"));
    System.out.println("Similarity between 'united_kingdom' and 'uk': " + vec.similarity("united_kingdom", "uk"));
    System.out.println("Similarity between 'clay' and 'clays': " + vec.similarity("clay", "clays"));
    System.out.println("Similarity between 'cat' and 'dog': " + vec.similarity("cat", "cats"));


    LOG.info("Complete!");
  }


  public static void createVectors(String textPath, String modelPath) throws Exception {
    long startTime = System.currentTimeMillis(); // START  TIME
    // Gets Path to Text file
    SentenceIterator iter = new LineSentenceIterator(new File(textPath));
    iter.setPreProcessor(new SentencePreProcessor() {
      @Override
      public String preProcess(String sentence) {
        return sentence.toLowerCase();
      }
    });

    // Split on white spaces in the line to get words
    TokenizerFactory t = new DefaultTokenizerFactory();

    /*
            CommonPreprocessor will apply the following regex to each token: [\d\.:,"'\(\)\[\]|/?!;]+
            So, effectively all numbers, punctuation symbols and some special symbols are stripped off.
            Additionally it forces lower case for all tokens.
     */
//    t.setTokenPreProcessor(new CommonPreprocessor()); // default
    t.setTokenPreProcessor(new GeoCommonPreprocessor()); // Custom implementation of CommonPreprocessor to preserve numbers attached to strings e.g. 3D

    LOG.info("Building model....");
    vec = new Word2Vec.Builder()
//            .minWordFrequency(5)
            .iterations(5)
            .layerSize(100)
            .seed(42)
            .windowSize(5)
            .iterate(iter)
            .tokenizerFactory(t)
            .build();

    LOG.info("Fitting Word2Vec model....");
    vec.fit();

    LOG.info("Writing word vectors to text file....");

    // Write word vectors to file
    WordVectorSerializer.writeWordVectors(vec, modelPath);

    long endTime = System.currentTimeMillis(); // END TIME
    LOG.log(Level.INFO, "***Duration for vector generation: {0} seconds***\n", ((endTime - startTime) / 1000));

    // Prints out the closest 10 words to "the". An example on what to do with these Word Vectors.
    LOG.info("Closest Words:");
    Collection<String> lst = vec.wordsNearest("the", 10);
    System.out.println("10 Words closest to 'the': " + lst);
  }

  public static void setModel(String modelFilePath) {
    vec = WordVectorSerializer.readWord2VecModel(modelFilePath);
    LOG.log(Level.INFO, "Model loaded. Vocabulary size is {0}", vec.getVocab().numWords());
  }

}
