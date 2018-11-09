package uk.rgu.data.ontologyprocessor.word2vec;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.JaroWinkler;
import info.debatty.java.stringsimilarity.MetricLCS;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.text.similarity.FuzzyScore;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.ivml.alimo.ISub;
import uk.rgu.data.model.Concept;
import uk.rgu.data.model.ConceptCompare;
import uk.rgu.data.model.StringCompare;
import uk.rgu.data.model.StringCompare.StringCompareComparator;
import uk.rgu.data.utilities.StringOps;

/**
 *
 * @author aikay
 */
public class VectorOps {

  private static final Logger LOG = Logger.getLogger(Word2Vec.class.getName());
//  static String vectorModelPath = "C:/dev/rgu/word2vec/models/GoogleNews-vectors-negative300.bin.gz";
  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/GoogleNews-vectors-negative300.bin.gz";
//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_plain_model300_min10_iter5_custom_token.txt";
//  static String vectorModelPath = "/program-data/DGXWord2Vec/data/model/wikipedia_annotated_model300_min10_iter5_custom_token.txt";
//  static String vectorModelPath = "C:/dev/rgu/word2vec/models/geo_hascontext1_model.txt";

  static Word2Vec vec;

  public VectorOps() {
    vec = WordVectorSerializer.readWord2VecModel(Paths.get(vectorModelPath).toAbsolutePath().toString());
    System.out.println("Vocabulary size: " + vec.getVocab().numWords());
  }

  public VectorOps(String modelFilePath) {
    System.out.println("Loading model from: " + modelFilePath + " ...");
    vec = WordVectorSerializer.readWord2VecModel(Paths.get(modelFilePath).toAbsolutePath().toString());
    System.out.println("Vocabulary size: " + vec.getVocab().numWords());
  }

  public void changeModel(String modelFilePath) {
    System.out.println("Changing model to: " + modelFilePath + " ...");
    vec = WordVectorSerializer.readWord2VecModel(Paths.get(modelFilePath).toAbsolutePath().toString());
    System.out.println("Vocabulary size: " + vec.getVocab().numWords());
  }

  /**
   * Returns true if supplied word has a vector and false otherwise.
   *
   * @param word
   * @return
   */
  public boolean hasVector(String word) {
//    word = StringOps.stripAllParentheses(word).replaceAll("\\s+", "_").toLowerCase(); // strip parenthesis and convert to expected form
    word = prepareStringUnderscores(word); // transform to uniform format
    double[] wordVec = vec.getWordVector(word); // attempt retrieving vectors for word
    return null != wordVec;
  }

  /**
   * Returns true if any entry in supplied list of words has a vector and false
   * otherwise.
   *
   * @param words
   * @return
   */
  public boolean hasVector(Set<String> words) {
    for (String word : words) {
      word = prepareStringUnderscores(word); // strip parenthesis and convert to expected form
      double[] wordVec = vec.getWordVector(word);
      if (null != wordVec) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns true if supplied any of the words in string has a vector and false
   * otherwise.
   *
   * @param word
   * @return
   */
  public boolean hasVectorSentence(String word) {
    for (String w : word.split("\\s+")) {
      w = prepareStringUnderscores(w); // transform to uniform format
      double[] wordVec = vec.getWordVector(w); // attempt retrieving vectors for word
      if (null != wordVec) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns the similarity between two word vectors (-1 if any does not have a
   * vector).
   *
   * @param word1
   * @param word2
   * @return
   */
  public double similarity(String word1, String word2) {
    double lst = 0.0;
//    String filePath = Paths.get(MODEL).toAbsolutePath().toString();
//    Word2Vec vec = WordVectorSerializer.readWord2VecModel(filePath);
//    LOG.info("Cosine Similarity:");
    word1 = prepareStringUnderscores(word1); // strip parenthesis and convert to expected form
    word2 = prepareStringUnderscores(word2); // strip parenthesis and convert to expected form

    if (hasVector(word1) && hasVector(word2)) {
      lst = vec.similarity(word1, word2);
    }
//    System.out.println("Similarity betweeen " + word1 + " and " + word2 + ": " + lst);

    return lst;
  }

  /**
   * Returns the similarity between two sentences/phrases using their word
   * vectors (0 if no pair of words in sentences have vectors).
   *
   * @param s1
   * @param s2
   * @return
   */
  public double sentenceSimilarity(String s1, String s2) {
//    System.out.println("s1 : " + s1);
//    System.out.println("s2 : " + s2);
    double sum = 0.0;
    int N = s1.split("\\s+").length;
    int M = s2.split("\\s+").length;
    if (N > M) {
      for (String w1 : s1.split("\\s+")) {
        double maxSim = 0.0;
        for (String w2 : s2.split("\\s+")) {
          double sim = similarity(w1, w2);
          if (sim > maxSim) {
            maxSim = sim;
          } // end if
        } // end inner for
        sum = sum + maxSim;
      } // end outer for
//      if (sum > -1.0) {
//        sum = sum + 1.0; // at least one one has vectors (assuming it has to be greater than 0.0)
//      }
//      System.out.println("N = " + N);
      sum = sum / N;
    } else {
      for (String w1 : s2.split("\\s+")) {
        double maxSim = 0.0;
        for (String w2 : s1.split("\\s+")) {
          double sim = similarity(w1, w2);
          if (sim > maxSim) {
            maxSim = sim;
          } // end if
        } // end inner for
        sum = sum + maxSim;
      } // end outer for
//      if (sum > -1.0) {
//        sum = sum + 1.0; // at least one one has vectors (assuming it has to be greater than 0.0)
//      }
//      System.out.println("M = " + M);
      sum = sum / M;
    }

    return sum;
  }

  /**
   * Returns the similarity between two sentences/phrases using a hybrid of word
   * vectors and edit distance whichever is greater (0 if no pair of words in
   * sentences have vectors).
   *
   * @param s1
   * @param s2
   * @return
   */
  public double hybridSimilarity(String s1, String s2) {
    NormalizedLevenshtein l = new NormalizedLevenshtein();
//    System.out.println("s1 : " + s1);
//    System.out.println("s2 : " + s2);
    double sum = 0.0;
    int N = s1.split("\\s+").length;
    int M = s2.split("\\s+").length;
    if (N > M) {
      for (String w1 : s1.split("\\s+")) {
        double maxSim = 0.0;
        for (String w2 : s2.split("\\s+")) {
          double vecSim = similarity(w1, w2);
          double strSim = l.similarity(w1.trim(), w2.trim());
          double sim;
          if (strSim >= 0.8) { // string similarity only counts from 0.89 and above
            sim = vecSim > strSim ? vecSim : strSim;
          } else {
            sim = vecSim;
          }
//          double sim = vecSim > strSim ? vecSim : strSim;
          if (sim > maxSim) {
            maxSim = sim;
          } // end if
        } // end inner for
        sum = sum + maxSim;
      } // end outer for
//      if (sum > -1.0) {
//        sum = sum + 1.0; // at least one one has vectors (assuming it has to be greater than 0.0)
//      }
//      System.out.println("N = " + N);
      sum = sum / N;
    } else {
      for (String w1 : s2.split("\\s+")) {
        double maxSim = 0.0;
        for (String w2 : s1.split("\\s+")) {
          double vecSim = similarity(w1, w2);
          double strSim = l.similarity(w1.trim(), w2.trim());
          double sim;
          if (strSim >= 0.8) {
            sim = vecSim > strSim ? vecSim : strSim;
          } else {
            sim = vecSim;
          }
//          double sim = vecSim > strSim ? vecSim : strSim;
          if (sim > maxSim) {
            maxSim = sim;
          } // end if
        } // end inner for
        sum = sum + maxSim;
      } // end outer for
//      if (sum > -1.0) {
//        sum = sum + 1.0; // at least one one has vectors (assuming it has to be greater than 0.0)
//      }
//      System.out.println("M = " + M);
      sum = sum / M;
    }

    return sum;
  }

  /**
   * Returns the similarity between two sentences/phrases using a hybrid of word
   * vectors and edit distance measures whichever is greater for word-pair coupling.
   *
   * @param s1
   * @param s2
   * @return
   */
  public double hybridSimilarityV2(String s1, String s2) {
    NormalizedLevenshtein nl = new NormalizedLevenshtein();
    JaroWinkler jw = new JaroWinkler();
    Cosine c = new Cosine();
    ISub is = new ISub();
    MetricLCS mlcs = new MetricLCS();
    FuzzyScore fs = new FuzzyScore(Locale.ENGLISH);

    double sum = 0.0;
    String longer = s1.split("\\s+").length > s2.split("\\s+").length ? s1 : s2; // longer string in words
    String shorter = s1.equals(longer) ? s2 : s1; // other as shorter

    for (String w1 : longer.split("\\s+")) {
      double maxSim = 0.0;
      for (String w2 : shorter.split("\\s+")) {
        double sim;
        // compute different similarities
        double vecSim = similarity(w1, w2);
        double nlSim = nl.similarity(w1.trim(), w2.trim());
        double jwSim = jw.similarity(w1.trim(), w2.trim());
        double cSim = c.similarity(w1.trim(), w2.trim());
        double isSim = is.score(w1.trim(), w2.trim());
        double mlcsSim = (1 - mlcs.distance(w1.trim(), w2.trim())); // distance to similarity
        String longerWord = w1.trim().length() > w2.trim().length() ? w1.trim() : w2.trim();
        double fsSim = (double)fs.fuzzyScore(w1.trim(), w2.trim()) / fs.fuzzyScore(longerWord, longerWord);

        // select max similarity
        double strSim = StatUtils.max(new double[]{nlSim, jwSim, cSim, isSim, mlcsSim, fsSim}); //
        if (strSim >= 0.8) { // string similarity only counts from 0.8 and above
          sim = vecSim > strSim ? vecSim : strSim;
        } else {
          sim = vecSim;
        } // end if
        // check if better than max
        if (sim > maxSim) {
          maxSim = sim;
        } // end if
      } // end inner for
      sum = sum + maxSim;
    } // end outer for

    return sum / longer.split("\\s+").length;
  }

  public double hybridSimilarityV3(String s1, String s2) {
    NormalizedLevenshtein nl = new NormalizedLevenshtein();
//    ISub nl = new ISub();
    double sum = 0.0;
    String longer = s1.split("\\s+").length > s2.split("\\s+").length ? s1 : s2; // longer string in words
    String shorter = s1.equalsIgnoreCase(longer) ? s2 : s1; // other as shorter

    for (String w1 : longer.split("\\s+")) {
      double maxSim = 0.0;
      for (String w2 : shorter.split("\\s+")) {
        double sim = 0.0;
        // compute different similarities
        double vecSim = similarity(w1, w2);
        double strSim = nl.similarity(w1.trim(), w2.trim());
        if (vecSim > 0.0) {
          sim = vecSim;
        } else if (strSim >= 0.85) { // string similarity only counts from 0.8 and above
          sim = strSim;
        }
        // check if better than max
        if (sim > maxSim) {
          maxSim = sim;
        } // end if
      } // end inner for
      sum = sum + maxSim;
    } // end outer for

    return sum / longer.split("\\s+").length;
  }

  public double hybridSimilarityV4(String s1, String s2) {
    NormalizedLevenshtein nl = new NormalizedLevenshtein();
    double sum = 0.0;
    String longer = s1.split("\\s+").length > s2.split("\\s+").length ? s1 : s2; // longer string in words
    String shorter = s1.equalsIgnoreCase(longer) ? s2 : s1; // other as shorter

    for (String w1 : longer.split("\\s+")) {
      double maxSim = 0.0;
      for (String w2 : shorter.split("\\s+")) {
        double sim;
        // compute different similarities
        double vecSim = similarity(w1, w2);
        double strSim = nl.similarity(w1.trim(), w2.trim());
        if (strSim >= 0.85) {
            sim = vecSim > strSim ? vecSim : strSim;
          } else {
            sim = vecSim;
          }
        // check if better than max
        if (sim > maxSim) {
          maxSim = sim;
        } // end if
      } // end inner for
      sum = sum + maxSim;
    } // end outer for

    return sum / longer.split("\\s+").length;
  }

  /**
   * Soft TFIDF with hybrid similarity for comparison. (Note within for shortcoming)
   * @param s1Map First string with words (with associated tf-idf weights) as map entries.
   * @param s2Map Second string.
   * @return
   */
  public double weightedHybridSimilarity(Map<String, Double> s1Map, Map<String, Double> s2Map) {
    double sum = 0.0;
    double editDistCut = 0.05; // threshold for string-based similarity. Value commonly used in OAEI.
//    double denom = 0;
    NormalizedLevenshtein nl = new NormalizedLevenshtein(); // edit distance algo
//    ISub isub = new ISub();
    Map<String, Double> longer = s1Map.size() > s2Map.size() ? s1Map : s2Map; // longer string in number of words
    Map<String, Double> shorter = s1Map.equals(longer) ? s2Map : s1Map; // 'shorter' string

    for (Map.Entry<String, Double> entry1 : shorter.entrySet()) { // iterate over shorter
      double maxSim = 0.0; // track maximum similarity. (NOTE: without removal, a string can count multiple times in max sim)
      double w1 = 0;
      double w2 = 0;
      for (Map.Entry<String, Double> entry2 : longer.entrySet()) { // inner: iterate over longer string
        String s1 = entry1.getKey();
        String s2 = entry2.getKey();
        double sim;
        // compute different similarities
        double vecSim = similarity(s1, s2);
        double strSim = nl.similarity(s1.trim(), s2.trim());
        if (vecSim >= strSim || strSim < editDistCut) { //  || strSim < editDistCut string similarity only counts if up to threshold
          sim = vecSim;
          System.out.println("semantic: " + s1 + " vs " + s2 + "| strSim=" + strSim + " | vecSim=" + vecSim);
        } else {
          sim = strSim;
        }
        // check if better than max
        if (sim > maxSim) {
          maxSim = sim;
          w1 = entry1.getValue();
          w2 = entry2.getValue();
        } // end if
      } // end inner for
      sum += (maxSim * w1 * w2);
//      sum += maxSim;
//      denom += (w1 * w2);
    } // end outer for

    return sum;
  }

  public double weightedHybridSimilarityImproved(Map<String, Double> s1Map, Map<String, Double> s2Map) {
    List<StringCompare> seen = new ArrayList<>();
    ArrayList<StringCompare> simMatrixList = new ArrayList<>();
    double sum = 0.0;
    double editDistCut = 0.89; // threshold for string-based similarity. Value commonly used in OAEI.
//    double denom = 0;
    NormalizedLevenshtein nl = new NormalizedLevenshtein(); // edit distance algo
//    ISub isub = new ISub();
    Map<String, Double> longer = s1Map.size() > s2Map.size() ? s1Map : s2Map; // longer string in number of words
    Map<String, Double> shorter = s1Map.equals(longer) ? s2Map : s1Map; // 'shorter' string

    for (Map.Entry<String, Double> entry1 : shorter.entrySet()) { // iterate over shorter
      for (Map.Entry<String, Double> entry2 : longer.entrySet()) { // inner: iterate over longer string
        String s1 = entry1.getKey();
        String s2 = entry2.getKey();
        double sim;
        // compute different similarities
        double vecSim = similarity(s1, s2);
        double strSim = nl.similarity(s1.trim(), s2.trim());
        if (vecSim >= strSim || strSim < editDistCut) { //  || strSim < editDistCut string similarity only counts if up to threshold
          sim = vecSim;
        } else {
          sim = strSim;
        }
        // add to list
        simMatrixList.add(new StringCompare(s1, s2, sim, (sim * entry1.getValue() * entry2.getValue())));
      } // end inner for
    } // end outer for
    // sort list in descending order of score
    Collections.sort(simMatrixList, new StringCompareComparator());
    for (int i=0; i<simMatrixList.size(); i++) {
      StringCompare sc = simMatrixList.get(i);
      if (!StringCompare.hasAnyString(seen, sc)) {
        sum += sc.weightedScore;
      }
    }

    return sum;
  }

  public double maxWeightedHybridSimilarity(Set<String> wordList1, List<List<String>> coll1, Set<String> wordList2, List<List<String>> coll2, double editDistCut) {
    double lst = 0.0; // current maximum
//    for (String s1 : wordList1)
//      Map<String, Double> wordMap1 = TFIDFCalculator.weighStringTerms(s1.toLowerCase(), coll1);
    for (String s1 : wordList1) {
      for (String s2 : wordList2) { // compute similarity
        if (null == s1 || null == s2) {
          continue;
        }

        Map<String, Double> s1Map = TFIDFCalculator.normalise(TFIDFCalculator.weighStringTerms(s1.toLowerCase(), coll1));
        Map<String, Double> s2Map = TFIDFCalculator.normalise(TFIDFCalculator.weighStringTerms(s2.toLowerCase(), coll2));
//        double sim = similarity(word, str);
        double sim = weightedHybridSimilarity(s1Map, s2Map, editDistCut);
//        double sim = weightedHybridSimilarityImproved(s1Map, s2Map);
//        System.out.println(s1 + " vs " + s2 + " = " + sim);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Soft TFIDF with hybrid similarity for comparison. (Note within for shortcoming)
   * @param s1Map First string with words (with associated tf-idf weights) as map entries.
   * @param s2Map Second string.
   * @param editDistCut Threshold for string-based similarity. About 0.89 is commonly used in OAEI.
   * @return
   */
  public double weightedHybridSimilarity(Map<String, Double> s1Map, Map<String, Double> s2Map, double editDistCut) {
    boolean usedVector = false;
//    double k = 1.5;
    double sum = 0.0;
//    editDistCut = 0.05; // threshold for string-based similarity. Value commonly used in OAEI.
//    double denom = 0;
    NormalizedLevenshtein nl = new NormalizedLevenshtein(); // edit distance algo
//    ISub isub = new ISub();
    Map<String, Double> longer = s1Map.size() > s2Map.size() ? s1Map : s2Map; // longer string in number of words
    Map<String, Double> shorter = s1Map.equals(longer) ? s2Map : s1Map; // 'shorter' string

    for (Map.Entry<String, Double> entry1 : shorter.entrySet()) { // iterate over shorter
      double maxSim = 0.0; // track maximum similarity. (NOTE: without removal, a string can count multiple times in max sim)
      double w1 = 0;
      double w2 = 0;
      for (Map.Entry<String, Double> entry2 : longer.entrySet()) { // inner: iterate over longer string
        String s1 = entry1.getKey();
        String s2 = entry2.getKey();
        double sim;
        // compute different similarities
        double vecSim = similarity(s1, s2);
        double strSim = nl.similarity(s1.trim(), s2.trim());
//        double strSim = isub.score(s1.trim(), s2.trim());
        if (vecSim >= strSim || strSim < editDistCut) { //  ||  || !s1.substring(0,1).equalsIgnoreCase(s2.substring(0, 1)). string similarity only counts if up to threshold
          System.out.println("vecSim=>" + vecSim  + "(" + s1 + " v " + s2 + ")");
          System.out.println("strSim=>" + strSim  + "(" + s1 + " v " + s2 + ")");
          sim = vecSim;
          usedVector = true;
//          System.out.println("semantic: " + s1 + " vs " + s2 + "| strSim=" + strSim + " | vecSim=" + vecSim);
        } else {
          sim = strSim;
        }
        // check if better than max
        if (sim > maxSim) {
          maxSim = sim;
          w1 = entry1.getValue();
          w2 = entry2.getValue();
          System.out.println("s1="+s1+",w1="+w1+",s2="+s2+",w2="+w2);
        } // end if
      } // end inner for
      sum += (maxSim * w1 * w2);
//      sum += (maxSim * ((w1 + w2)/2));
//      sum += maxSim;
//      denom += (w1 * w2);
    } // end outer for

    if (usedVector && sum >=  0.76) {
      s1Map.keySet().forEach(System.out::print);
      System.out.print(" <=> ");
      s2Map.keySet().forEach(System.out::print);
      System.out.print(" | sim = " + sum);
      System.out.println("");
    }

    return sum;
  }

  /**
   * Returns the maximum cosine similarity between the vectors of two sets of
   * words (-1 if a set does not have any vectors for its words).
   *
   * @param words
   * @param wordList
   * @return
   */
  public double maxSimilarity(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        if (null == word || null == str) {
          continue;
        }
//        double sim = similarity(word, str);
        double sim = sentenceSimilarity(word, str);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  public double maxHybridSimilarity(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        if (null == word || null == str) {
          continue;
        }
//        double sim = similarity(word, str);
        double sim = hybridSimilarityV3(word.toLowerCase(), str.toLowerCase());
//        System.out.println(word + " vs " + str + " = " + sim);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  public double maxWeightedHybridSimilarity(Set<String> wordList1, List<List<String>> coll1, Set<String> wordList2, List<List<String>> coll2) {
    double lst = 0.0; // current maximum
//    for (String s1 : wordList1)
//      Map<String, Double> wordMap1 = TFIDFCalculator.weighStringTerms(s1.toLowerCase(), coll1);
    for (String s1 : wordList1) {
      for (String s2 : wordList2) { // compute similarity
        if (null == s1 || null == s2) {
          continue;
        }

        Map<String, Double> s1Map = TFIDFCalculator.normalise(TFIDFCalculator.weighStringTerms(s1.toLowerCase(), coll1));
        Map<String, Double> s2Map = TFIDFCalculator.normalise(TFIDFCalculator.weighStringTerms(s2.toLowerCase(), coll2));
//        double sim = similarity(word, str);
        double sim = weightedHybridSimilarity(s1Map, s2Map);
//        double sim = weightedHybridSimilarityImproved(s1Map, s2Map);
//        System.out.println(s1 + " vs " + s2 + " = " + sim);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the cumulative cosine similarity between the vectors of two sets of
   * words (-1 if a set does not have any vectors for its words).
   *
   * @param words
   * @param wordList
   * @return
   */
  public double cumulativeSimilarity(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current cumulative
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        if (null == word || null == str) {
          continue;
        }
        double sim = similarity(word, str);
//        double sim = sentenceSimilarity(word, str);
        if (sim > 0.0) {
          lst += sim; // add to cumulative
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the average cosine similarity between the vectors of two sets of
   * words (-1 if a set does not have any vectors for its words).
   *
   * @param words
   * @param wordList
   * @return
   */
  public double averageSimilarity(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current cumulative
    int count = 0; // count of similarities cumulated
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        if (null == word || null == str) {
          continue;
        }
//        double sim = similarity(word, str);
        double sim = sentenceSimilarity(word, str);
        if (sim > 0.0) {
          count++;
          lst += sim;
        }
      } // end inner for
    }

    return count > 0 ? lst / count : lst;
  }

  public double averageHybridSimilarity(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current cumulative
    int count = 0; // count of similarities cumulated
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        if (null == word || null == str) {
          continue;
        }
//        double sim = similarity(word, str);
        double sim = hybridSimilarityV3(word, str);
        if (sim > 0.0) {
          count++;
          lst += sim;
        }
      } // end inner for
    }

    return count > 0 ? lst / count : lst;
  }

  public void nearestWords(String word, int n) {
//    String filePath = Paths.get(MODEL).toAbsolutePath().toString();
//    Word2Vec vec = WordVectorSerializer.readWord2VecModel(filePath);
    LOG.info("Closest Words:");
    Collection<String> lst = vec.wordsNearest(word, n);
    System.out.println(n + " Words closest to " + word + ": " + lst);
  }

  /**
   * Finds entries in wordList that are nearest to word in the vector space
   * model.
   *
   * @param word
   * @param wordList
   * @param n
   * @return
   */
  public Map<String, Double> nearestWords(String word, Set<String> wordList, int n) {
    Map<String, Double> vecList = new LinkedHashMap();
    for (String str : wordList) { // compute similarity
      double sim = similarity(word, str);
      if (sim > 0.0) {
        vecList.put(str, sim);
      }
    }
    // sort in descending order
    vecList = sortByValue(vecList);
    // select top n
    Map<String, Double> resList = new LinkedHashMap();
    int counter = 0;
    for (Map.Entry<String, Double> entry : vecList.entrySet()) {
      counter++;
      resList.put(entry.getKey(), entry.getValue());
      if (counter == n) {
        break;
      }
    }

    return resList;
  }

  /**
   * Finds entries in wordList that are nearest to the entries in words in the
   * vector space model. Selects highest similarity score for repeat entries.
   *
   * @param words
   * @param wordList
   * @param n
   * @return
   */
  public Map<String, Double> nearestWordsByHighestSimilarity(Set<String> words, Set<String> wordList, int n) {
    Map<String, Double> vecList = new LinkedHashMap();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = sentenceSimilarity(word, str);
        if (sim > 0.0) {
          if (vecList.containsKey(str)) { // check if to replace existing
            if (vecList.get(str) < sim) { // update only is higher than existing
              vecList.put(str, sim);
            }
          } else { // add new
            vecList.put(str, sim);
          }
        }
      } // end inner for
    }
    // sort in descending order
    vecList = sortByValue(vecList);
    // select top n
    Map<String, Double> resList = new LinkedHashMap();
    int counter = 0;
    for (Map.Entry<String, Double> entry : vecList.entrySet()) {
      counter++;
      resList.put(entry.getKey(), entry.getValue());
      if (counter == n) { // required terms retrieved
        break;
      }
    }

    return resList;
  }

  /**
   * Finds entries in wordList that are nearest to the entries in words in the
   * vector space model. Cumulates similarity scores for repeat entries.
   *
   * @param words
   * @param wordList
   * @param n
   * @return
   */
  public Map<String, Double> nearestWordsByCumulativeSimilarity(Set<String> words, Set<String> wordList, int n) {
    Map<String, Double> vecList = new LinkedHashMap();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = similarity(word, str);
        if (sim > 0.0) {
          if (vecList.containsKey(str)) { // check if to add to existing similarity value
            vecList.put(str, vecList.get(str) + sim);
          } else { // add new
            vecList.put(str, sim);
          }
        }
      } // end inner for
    }
    // sort in descending order
    vecList = sortByValue(vecList);
    // select top n
    Map<String, Double> resList = new LinkedHashMap();
    int counter = 0;
    for (Map.Entry<String, Double> entry : vecList.entrySet()) {
      counter++;
      resList.put(entry.getKey(), entry.getValue());
      if (counter == n) { // required terms retrieved
        break;
      }
    }

    return resList;
  }

  /**
   * Finds entries in concepts2 that has the nearest concept term to the c1 in
   * the vector space model. Restricts to top n.
   *
   * @param c1
   * @param concepts2
   * @param n
   * @return
   */
  public Map<String, Double> nearestConceptsByHighestSimilarity(Concept c1, List<Concept> concepts2, int n) {
    Map<String, Double> vecList = new LinkedHashMap(); // <concept_id, similarity>
    // 1. compute closeness
    for (Concept c2 : concepts2) {
      if (c1.getConceptId().split("-").length > 1 && c2.getConceptId().split("-").length > 1) { // test for valid concepts (should have a concept id)
        double sim = maxSimilarity(c1.getAllLabels(), c2.getAllLabels()); // maximum cosine similarity
        if (sim > 0.0) {
          vecList.put(c2.getConceptId(), sim);
        }
      } // end if (test of valid concepts)
    }

    // 2. sort in descending order
    vecList = sortByValue(vecList);
    // 3. select top n
    Map<String, Double> resList = new LinkedHashMap();
    int counter = 0;
    for (Map.Entry<String, Double> entry : vecList.entrySet()) {
      counter++;
      resList.put(entry.getKey(), entry.getValue());
      if (counter == n) { // required terms retrieved
        break;
      }
    }

    return resList;
  }

  /**
   * Finds entries in concepts2 that has the nearest average concept term to the
   * c1 in the vector space model. Restricts to top n.
   *
   * @param c1
   * @param concepts2
   * @param n
   * @return
   */
  public Map<String, Double> nearestConceptsByAverageSimilarity(Concept c1, List<Concept> concepts2, int n) {
    Map<String, Double> vecList = new LinkedHashMap(); // <concept_id, similarity>
    // 1. compute closeness
    for (Concept c2 : concepts2) {
      if (c1.getConceptId().split("-").length > 1 && c2.getConceptId().split("-").length > 1) { // test for valid concepts (should have a concept id)
        double sim = averageSimilarity(c1.getAllLabels(), c2.getAllLabels()); // average cosine similarity
        if (sim > 0.0) {
          vecList.put(c2.getConceptId(), sim);
        }
      } // end if (test of valid concepts)
    }

    // 2. sort in descending order
    vecList = sortByValue(vecList);
    // 3. select top n
    Map<String, Double> resList = new LinkedHashMap();
    int counter = 0;
    for (Map.Entry<String, Double> entry : vecList.entrySet()) {
      counter++;
      resList.put(entry.getKey(), entry.getValue());
      if (counter == n) { // required terms retrieved
        break;
      }
    }

    return resList;
  }

  /**
   * Finds entries in concepts2 that has the nearest cumulative concept term to
   * the c1 in the vector space model. Restricts to top n.
   *
   * @param c1
   * @param concepts2
   * @param n
   * @return
   */
  public Map<String, Double> nearestConceptsByCumulativeSimilarity(Concept c1, List<Concept> concepts2, int n) {
    Map<String, Double> vecList = new LinkedHashMap(); // <concept_id, similarity>
    // 1. compute closeness
    for (Concept c2 : concepts2) {
      if (c1.getConceptId().split("-").length > 1 && c2.getConceptId().split("-").length > 1) { // test for valid concepts (should have a concept id)
        double sim = cumulativeSimilarity(c1.getAllLabels(), c2.getAllLabels()); // average cosine similarity
        if (sim > 0.0) {
          vecList.put(c2.getConceptId(), sim);
        }
      } // end if (test of valid concepts)
    }

    // 2. sort in descending order
    vecList = sortByValue(vecList);
    // 3. select top n
    Map<String, Double> resList = new LinkedHashMap();
    int counter = 0;
    for (Map.Entry<String, Double> entry : vecList.entrySet()) {
      counter++;
      resList.put(entry.getKey(), entry.getValue());
      if (counter == n) { // required terms retrieved
        break;
      }
    }

    return resList;
  }

  /**
   * Finds topN results (d's) in analogy: a is to b as c is _?.
   *
   * @param pos1 a
   * @param pos2 c
   * @param neg1 b
   * @param topN
   */
  public void analogies(String pos1, String pos2, String neg1, int topN) {
    List<String> positive = Arrays.asList(pos1, pos2);
    List<String> negative = Arrays.asList(neg1);

    LOG.info("Closest Words:");
    Collection<String> rd = vec.wordsNearest(positive, negative, topN);
    System.out.println("5 Words closest to '" + positive.get(0) + "' + '" + positive.get(1) + "' - '" + negative.get(0) + "' : " + rd);
  }

  /**
   * Finds the word whose vector is most distant from a group. Also tries to
   * explain using the words that are nearest to others but not the odd word.
   *
   * @param words List of words. Concatenate phrases using underscores.
   * @return
   */
  public String[] oddOneOut(List<String> words) {
    String[] answer = new String[2]; // 0:word, 1:
    double[] avgSims = new double[words.size()];

    answer[1] = "No obvious reason!"; // explanation
    // get pairwise similarities
    if (words.size() > 2) { // at least 3 words are required
      for (int i = 0; i < words.size(); i++) {
        avgSims[i] = 0.0;
        for (int j = 0; j < words.size(); j++) {
          if (i != j) { // cummulate similarity of a word and every other word
            avgSims[i] += vec.similarity(words.get(i), words.get(j));
          }
        }
      }
      // get the least similar
      int oddIndex = 0;
      for (int i = 1; i < avgSims.length; i++) {
        if (avgSims[i] < avgSims[i - 1]) {
          oddIndex = i;
        }
      }
      answer[0] = words.get(oddIndex); // odd word out
      // get explanation

    }

    return answer;
  }

  /**
   * Re-arranges words in a list based on their pairwise vector similarity.
   *
   * @param words
   * @param firstIndex
   * @return
   */
  public static List<String> rearrange(List<String> words, int firstIndex) {
    List<String> sortedObjs = new ArrayList(); // keeps rearranged list
//    List<Integer> seenIndices = new ArrayList(); // indices in the list (redundant?)
    int currIdx = firstIndex;
    int nextIdx = 0;
    for (int counter = 0; counter < words.size(); counter++) { // counter so that every entry is considered
      double currMax = 1.0;
      String currObj = words.get(currIdx);
      System.out.println("Current obj => " + currObj);
      sortedObjs.add(currObj);
      for (int i = 0; i < words.size(); i++) { // compare current index with every other to find closest to it
        if (i != currIdx && !sortedObjs.contains(words.get(i))) {
          double nextDiff = vec.similarity(currObj, words.get(i));
          if (nextDiff < currMax) {
            System.out.println("Current min => " + currMax);
            System.out.println("nextIdx => " + i);
            currMax = nextDiff;
            nextIdx = i;
          }
        }
      } // end inner for
      currIdx = nextIdx;
      System.out.println("New currIdx => " + currIdx);
    } // end outer for

    return sortedObjs;
  }

  /**
   * Strips parentheses (and their contents), converts to lowercase and replaces
   * whitespaces with underscores.
   *
   * @param str
   * @return
   */
  public static String prepareStringUnderscores(String str) {
    str = StringOps.stripAllParentheses(str);
    str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(str), ' ');
    str = str.replaceAll("\\b\\d+\\b|[\\.:,\"\'\\(\\)\\[\\]|/?!;]+", "");

    return str.replaceAll("\\s+", "_").toLowerCase().trim();
  }

  public static Set<String> prepareStringUnderscores(Set<String> strs) {
    Set<String> res = new HashSet();
    for (String str : strs) {
      str = StringOps.stripAllParentheses(str);
      str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(str), ' ');
      str = str.replaceAll("\\b\\d+\\b|[\\.:,\"\'\\(\\)\\[\\]|/?!;]+", "");

      str = str.replaceAll("\\s+", "_").trim();
      res.add(str);
    }

    return res;
  }

  /**
   * Strips parentheses (and their contents), converts to lowercase and replaces
   * whitespaces with underscores.
   *
   * @param str
   * @return
   */
  public static String prepareStringSpaces(String str) {
    str = StringOps.stripAllParentheses(str);
    str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(str), ' ');
    str = str.replaceAll("\\b\\d+\\b|[\\.:,\"\'\\(\\)\\[\\]|/?!;]+", "");
    str = str.replaceAll("_", " ");

    return str.replaceAll("\\s+", " ").toLowerCase().trim();
  }

  /**
   * Strips parentheses (and their contents), converts to lowercase and replaces
   * whitespaces with underscores.
   *
   * @param str
   * @return
   */
  public static Set<String> prepareStringSpaces(Set<String> strs) {
    Set<String> res = new HashSet();
    for (String str : strs) {
      str = StringOps.stripAllParentheses(str);
      str = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(str), ' ');
      str = str.replaceAll("\\b\\d+\\b|[\\.:,\"\'\\(\\)\\[\\]|/?!;]+", "");
      str = str.replaceAll("_", " ");

      str = str.replaceAll("\\s+", " ").toLowerCase().trim();
      res.add(str);
    }

    return res;
  }

  /**
   * Sorts a map in descending order of value.
   *
   * @param <K>
   * @param <V>
   * @param map
   * @return
   */
  public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
    return map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
            ));
  }

  public static void main(String[] arg) {
    VectorOps vectorOps = new VectorOps("data/geo_hascontext1_model.txt");
    System.out.println("the stone vs rock : " + vectorOps.hybridSimilarity("stone", "rock"));
//    vectorOps.hasVector("museum");
//    Map<String, Double> map = new LinkedHashMap();
//    map.put("A", 99.5);
//    map.put("B", 67.4);
//    map.put("C", 67.4);
//    map.put("D", 67.3);
//    map.put("E", 75.7);
//    System.out.println("unsorted map: " + map);
//    map = sortByValue(map);
//    System.out.println("results: " + map);

    // one
    Set<String> left = new HashSet<String>();
    left.addAll(Arrays.asList("avow", "declare"));

    Set<String> right = new HashSet<String>();
    right.addAll(Arrays.asList("avert", "impede", "affirm", "hinder", "the permit", "the declares", "edict", "order"));
//    System.out.println("sim = " + vectorOps.maxHybridSimilarity(left, right));
//    vectorOps.nearestWordsByHighestSimilarity(left, right, 3).forEach((k, v) -> {
//      System.out.println("k=>" + k + " v=>" + v);
//    });
/*
    // two
     Map<String, String> c2LabelMap = new HashMap();
     c2LabelMap.put("avert", "C-1");
     c2LabelMap.put("impede", "C-2");
     c2LabelMap.put("affirm", "C-3");
     c2LabelMap.put("hinder", "C-2");
     c2LabelMap.put("permit", "C-4");
     c2LabelMap.put("edict", "C-5");
     c2LabelMap.put("order", "C-7");

      Map<String, Double> lhm = vectorOps.nearestWordsByHighestSimilarity(left, c2LabelMap.keySet(), 5);
      for (Map.Entry<String, Double> entry : lhm.entrySet()) {
        String term = entry.getKey(); // word/term
        double vecSim = entry.getValue(); // cosine similarity
        if (vecSim >= 0.1 && c2LabelMap.get(term).split("-").length > 1) { // test for valid concepts (should have a concept id)
          System.out.println(entry);
        } // end if (test of valid concepts)
      } // concept2 loop ends
     */

//    Iterator<Map.Entry<String, Double>> entries = map.entrySet().iterator();
//    while (entries.hasNext()) {
//      Map.Entry<String, Double> current = entries.next();
//      System.out.println("current: " + current);
//    }
//    System.out.println("underscored phrases");
//    System.out.println(vectorOps.similarity("size", "dimensions"));
//    System.out.println(vectorOps.similarity("igneous petrology", "sedimentary"));
//    System.out.println("sentences");
//    System.out.println(vectorOps.sentenceSimilarity("cat", "dog"));
//    System.out.println(vectorOps.sentenceSimilarity("conference proceedings", "proceedings"));
  }
}
