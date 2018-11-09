package uk.rgu.data.utilities;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.Jaccard;
import info.debatty.java.stringsimilarity.JaroWinkler;
import info.debatty.java.stringsimilarity.Levenshtein;
import info.debatty.java.stringsimilarity.MetricLCS;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import info.debatty.java.stringsimilarity.QGram;
import info.debatty.java.stringsimilarity.SorensenDice;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.ivml.alimo.ISub;
import org.simmetrics.StringMetric;
import org.simmetrics.StringMetrics;
import org.tartarus.snowball.ext.PorterStemmer;
import uk.rgu.data.ontologyprocessor.word2vec.TFIDFCalculator;

/**
 * Implements operations on Strings.
 *
 * @author 1113938
 */
public class StringOps {

  static final Logger LOG = Logger.getLogger(StringOps.class.getName());

  /**
   * Removes parentheses and their content from string. Matches '(*)' only.
   *
   * @param str Input string.
   * @return String without parentheses..
   */
  public static String stripParentheses(String str) {
    // Remove parentheses and words in them (including square brackets)
    String string = str;
    try {
//      string = str.replaceAll("\\s\\([^\\)]*\\)", "");
      string = str.replaceAll("[\\(]*(\\(.*\\))", "");
    } catch (PatternSyntaxException ex) {
      LOG.log(Level.SEVERE, "PatternSyntaxException{0}", ex.getMessage());
    }
    return string.replaceAll("\\s+", " ").trim();
  }

  /**
   * Removes parentheses (including square brackets) and their content from
   * string.
   *
   * @param str Input string.
   * @return String without parentheses.
   */
  public static String stripAllParentheses(String str) {
    // Remove parentheses and words in them (including square brackets)
    String string = str.replaceAll("\\s?\\([^\\)]*\\)|\\s?\\[[^\\)]*\\]", "");

    return string.replaceAll("\\s+", " ").trim();
  }

  /**
   * Custom Implementation: Removes descriptive content by truncating string
   * before certain words using regular expression. (See implementation for
   * words matched!)
   *
   * @param str Input string.
   * @return String truncated at certain words.
   */
  public static String stripDescriptions(String str) {
    // Remove descriptions starting with "composed"
    String string = str.replaceAll("\\scomposed.*?$", "");
    // Remove descriptions starting with "rich in"
    string = string.replaceAll("\\srich\\sin.*?$", "");
    // Remove descriptions starting with "with*"
    string = string.replaceAll("\\swith.*?$", "");

    return string;
  }

  /**
   * Retrieves combined text in all parentheses including square brackets.
   *
   * @param str Input string.
   * @return Concatenation of texts in parentheses.
   */
  public static String getParenthesesText(String str) {
    String string = "";
    // Match text within brackets using regex
    Pattern p = Pattern.compile("[\\(\\[](.*?)[\\)\\]]", Pattern.DOTALL);
    Matcher matcher = p.matcher(str);
    while (matcher.find()) {
      string = string + matcher.group(1) + " "; // concatenate content
    }

    return string.trim(); // Return string without leading or trailing spaces
  }

  /**
   * Retrieves last part of URI. This is string after the last "/" or "#".
   *
   * @param str Input string.
   * @return Text after the last forward slash.
   */
  public static String getLastUriValue(String str) {
    String out = str.substring(str.lastIndexOf("/") + 1);
    if (out.contains("#")) {
      return out.substring(out.lastIndexOf("#") + 1);
    }
    return out;
  }

  /**
   * Removes a collection of words from supplied text (this is case
   * insensitive).
   *
   * @param stopWordList Collection of stop words.
   * @param text Text to remove stop words from.
   * @return Text with stop words removed.
   */
  public static String removeStopWords(Collection<String> stopWordList, String text) {
    String output = text.toLowerCase();
    for (String word : stopWordList) {
      output = output.replaceAll("\\b" + word + "\\b", "");
    }

    return output;
  }

  /**
   * Removes parentheses tags from supplied string.
   *
   * @param string String to remove symbols from.
   * @return String with parenthesis symbols removed.
   */
  public static String getWithoutParentheses(String string) {
    String output = string;

    output = output.replaceAll("\\[", "");
    output = output.replaceAll("\\]", "");
    output = output.replaceAll("\\(", "");
    output = output.replaceAll("\\)", "");
    output = output.replaceAll("<", "");
    output = output.replaceAll(">", "");

    return output;
  }

  /**
   * Returns the input string with last word stripped (only is string is more
   * than 1 word).
   *
   * @param string
   * @return
   */
  public static String removeLastWord(String string) {
    String output = string;
    int length = string.split("\\s+").length; // length of input
    if (length > 1) {
      output = ""; // reset output
      String[] allStrings = string.split("\\s+"); // tokens splits at whitespaces
      // form output without the last token
      for (int i = 0; i < allStrings.length - 1; i++) {
        output = output + " " + allStrings[i].trim();
      }
    }

    return output.trim();
  }

  /**
   * Returns word count of supplied string. Trims whitespaces at string ends.
   *
   * @param string
   * @return
   */
  public static int getWordLength(String string) {
    String trim = string.trim();
    if (trim.isEmpty()) {
      return 0;
    }

    return trim.split("\\s+").length; // separate string around spaces
  }

  /**
   * Checks if a string is contained in another (identifies word boundaries so
   * that only complete words are matched).
   *
   * @param inputString String to look in.
   * @param words String to look for.
   * @param noParenthesis Remove parentheses with strings contained.
   * @return True if input string contains the words.
   */
  public static boolean isContain(String inputString, String words, boolean noParenthesis) {
    if (null == words || words.length() < 1) {
      return false;
    }
    words = words.replaceAll("\\+", "\\\\+").replaceAll("\\-", "\\\\-");
    if (noParenthesis) {
      words = stripParentheses(words);
    }
    String pattern = "\\b" + words + "\\b";
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(inputString);

    return m.find();
  }

  /**
   * Checks if a group of strings are contained in another (identifies word
   * boundaries so that only complete words are matched).
   *
   * @param inputString String to look in.
   * @param words Set of string to look for.
   * @param noParenthesis Remove parentheses with strings contained (True to
   * remove parenthesis).
   * @return True if input string contains any words in set.
   */
  public static boolean isContain(String inputString, Set<String> words, boolean noParenthesis) {
    for (String word : words) {
      if (isContain(inputString, word, noParenthesis)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if a group of strings are contained in another (identifies word
   * boundaries so that only complete words are matched). Input words are
   * stemmed before matching.
   *
   * @param inputString String to look in.
   * @param words Set of string to look for.
   * @param noParenthesis Remove parentheses with strings contained.
   * @return True if input string contains any words in set.
   */
  public static boolean isContainStemmed(String inputString, Set<String> words, boolean noParenthesis) {
    for (String word : words) {
      if (isContain(inputString, stemSentence(word, noParenthesis), noParenthesis)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if a group of strings are contained in another (identifies word
   * boundaries so that only complete words are matched).
   *
   * @param inputString String to look in.
   * @param words Set of string to look for.
   * @param noParenthesis Remove parentheses with strings contained.
   * @return True if input string contains any words in set.
   */
  public static boolean isContain(String inputString, List<String> words, boolean noParenthesis) {
    for (String word : words) {
      if (isContain(inputString, word, noParenthesis)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Counts number of times a string is contained in another (identifies word
   * boundaries so that only complete words are matched).
   *
   * @param inputString String to look in.
   * @param words String to look for.
   * @param noParenthesis Remove parentheses with strings contained.
   * @return count of occurrences of word.
   */
  public static int countOccurrence(String inputString, String words, boolean noParenthesis) {
    if (null == words || words.length() < 1) {
      return 0;
    }
    int count = 0;
    words = words.replaceAll("\\+", "\\\\+").replaceAll("\\-", "\\\\-");
    if (noParenthesis) {
      words = stripParentheses(words);
    }
    String pattern = "\\b" + words + "\\b";
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(inputString);
    while (m.find()) {
      count++;
    }

    return count;
  }

  /**
   * Counts number of times a string is contained in another (identifies word
   * boundaries so that only complete words are matched).
   *
   * @param inputString String to look in.
   * @param words String to look for.
   * @param noParenthesis Remove parentheses with strings contained.
   * @return count of occurrences of word.
   */
  public static int countOccurrence(String inputString, Set<String> words, boolean noParenthesis) {
    int count = 0;
    for (String word : words) {
      count += countOccurrence(inputString, word, noParenthesis);
    }

    return count;
  }

  /**
   * Counts number of times a string is contained in another (identifies word
   * boundaries so that only complete words are matched). Input word is stemmed
   * before match attempt.
   *
   * @param inputString String to look in.
   * @param words String to look for.
   * @param noParenthesis Remove parentheses with strings contained.
   * @return count of occurrences of word.
   */
  public static int countStemmedOccurrence(String inputString, Set<String> words, boolean noParenthesis) {
    int count = 0;
    for (String word : words) {
      count += countOccurrence(inputString, stemSentence(word, noParenthesis), noParenthesis);
    }

    return count;
  }

  /**
   * Counts number of times a string is contained in another (identifies word
   * boundaries so that only complete words are matched).
   *
   * @param inputString String to look in.
   * @param words String to look for.
   * @param noParenthesis
   * @return count of occurrences of word.
   */
  public static int countOccurrence(String inputString, List<String> words, boolean noParenthesis) {
    int count = 0;
    for (String word : words) {
      count += countOccurrence(inputString, word, noParenthesis);
    }

    return count;
  }

  /**
   * Returns string with matched words stripped (identifies word boundaries so
   * that only complete words are matched). Case sensitive.
   *
   * @param sentence String to look in.
   * @param words Substring to remove (removes all occurrences of @param words).
   * @return String with @param words removed (@param sentence if no matches
   * were found).
   */
  public static String withoutWords(String sentence, String words) {
    words = words.replaceAll("\\+", "\\\\+").replaceAll("\\-", "\\\\-"); // escape to allow regex
    String pattern = "\\b" + words + "\\b";
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(sentence);
    if (m.find()) {
      return m.replaceAll("");
    }
    return normaliseWhitespace(sentence.replace("\\", "").trim());
  }

  /**
   * Returns string with matched words replaced (identifies word boundaries so
   * that only complete words are matched) by markers.
   *
   * @param sentence String to look in.
   * @param words Substring to remove (removes all occurrences of @param words).
   * @param marker
   * @return String with @param words removed (@param sentence if no matches
   * were found).
   */
  public static String withoutWords(String sentence, String words, String marker) {
    words = words.replaceAll("\\+", "\\\\+").replaceAll("\\-", "\\\\-");
    String pattern = "\\b" + words + "\\b";
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(sentence);
    if (m.find()) {
      return m.replaceAll(marker);
    }
    // try separating words with dashes if not match
    pattern = "\\b" + words.replaceAll("\\s", "-") + "\\b"; // replace each space with dash (-)
    p = Pattern.compile(pattern);
    m = p.matcher(sentence);
    if (m.find()) {
      return m.replaceAll(marker);
    }

    return sentence;
  }

  /**
   * Returns string with matched words replaced (identifies word boundaries so
   * that only complete words are matched) by markers.
   *
   * @param inputString
   * @param words Substring to remove (removes all occurrences of @param words).
   * @param marker
   * @param noParenthesis
   * @return String with words removed and replaced with marker.
   */
  public static String withoutWords(String inputString, String words, String marker, boolean noParenthesis) {
    if (null == words || words.trim().length() < 1) {
      return inputString;
    }
    int count = 0;
    words = words.replaceAll("\\+", "\\\\+").replaceAll("\\-", "\\\\-");
    if (noParenthesis) {
      words = stripParentheses(words);
    }
    String pattern = "\\b" + words + "\\b";
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(inputString);
    while (m.find()) {
      count++;
      inputString = m.replaceAll(marker);
    }
    System.out.println("Match count for " + words + " => " + count);

    return inputString;
  }

  /**
   * Stems supplied string. This is for a single word.
   *
   * @param word String to stem.
   * @param noParenthesis Remove parentheses with strings contained.
   * @return Stemmed string.
   */
  public static String stemWord(String word, boolean noParenthesis) {
    if (null != word && !"".equals(word.trim())) {
      if (noParenthesis) {
        word = stripParentheses(word);
      }
      PorterStemmer stem = new PorterStemmer();
      stem.setCurrent(word.toLowerCase());
      stem.stem();
      return stem.getCurrent();
    }
    return "";
  }

  /**
   * Stems supplied string.
   *
   * @param sentence String to stem.
   * @param noParenthesis Remove parentheses with strings contained.
   * @return Stemmed string in lower case with all alphanumeric characters
   * removed.
   */
  public static String stemSentence(String sentence, boolean noParenthesis) {
    String output = "";
    if (null != sentence && !"".equals(sentence.trim())) {
      if (noParenthesis) {
        sentence = stripParentheses(sentence);
      }
      PorterStemmer stem = new PorterStemmer();
      String[] tokens = sentence.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase().split("[\\s']");
      for (String s : tokens) {
        if (null != s && !"".equals(s.trim())) {
          stem.setCurrent(s);
          stem.stem();
          output = output + " " + stem.getCurrent().replaceAll("\\s+", " ");
        }
      }
    }
    return output.trim();
  }

  public static String stem(String string) {
    StringBuilder stringBuilder = new StringBuilder();
    try {
      TokenStream tokenizer = new StandardTokenizer(new StringReader(string));
      tokenizer = new StandardFilter(tokenizer);
      tokenizer = new LowerCaseFilter(tokenizer);
      tokenizer = new PorterStemFilter(tokenizer);

      CharTermAttribute token = tokenizer.getAttribute(CharTermAttribute.class);
      tokenizer.reset();

      while (tokenizer.incrementToken()) {
        if (stringBuilder.length() > 0) {
          stringBuilder.append(" ");
        }

        stringBuilder.append(token.toString());
      }

      tokenizer.end();
      tokenizer.close();
    } catch (IOException ex) {
      Logger.getLogger(StringOps.class.getName()).log(Level.SEVERE, null, ex);
    }
    return stringBuilder.toString();
  }

  /**
   * Normalise whitespaces (i.e. change spaces between words to single-space).
   * Also trims to remove trailing spaces.
   *
   * @param string
   * @return
   */
  public static String normaliseWhitespace(String string) {
    return string.replaceAll("\\s\\s+", " ").trim();
  }

  /**
   * Counts number of common string in two sets.
   *
   * @param set1
   * @param set2
   * @return
   */
  public static int countWordOverlap(Set<String> set1, Set<String> set2) {
    int count = 0;
    for (String w1 : set1) {
      for (String w2 : set2) {
        if (w1.equals(w2)) {
          count++;
        }
      }
    }

    return count;
  }

  /**
   * Finds entries in wordList that are most similar to the entries in set of
   * words. Returns the highest similarity score after sorting in descending
   * order.
   *
   * @param words
   * @param wordList
   * @param n
   * @return
   */
  public static Map<String, Double> mostSimilarStringByNormalizedLevenshtein(Set<String> words, Set<String> wordList, int n) {
    Map<String, Double> vecList = new LinkedHashMap();
    NormalizedLevenshtein l = new NormalizedLevenshtein();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = l.similarity(word, str);
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
   * Finds entries in wordList that are most similar to the entries in set of
   * words. Returns the highest similarity score after sorting in descending
   * order.
   *
   * @param words
   * @param wordList
   * @param n
   * @return
   */
  public static Map<String, Double> mostSimilarStringByJaroWinkler(Set<String> words, Set<String> wordList, int n) {
    Map<String, Double> vecList = new LinkedHashMap();
    JaroWinkler l = new JaroWinkler();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = l.similarity(word, str);
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
   * Finds entries in wordList that are most similar to the entries in set of
   * words. Returns the highest similarity score after sorting in descending
   * order.
   *
   * @param words
   * @param wordList
   * @param n
   * @return
   */
  public static Map<String, Double> mostSimilarStringByJaccard(Set<String> words, Set<String> wordList, int n) {
    Map<String, Double> vecList = new LinkedHashMap();
    Jaccard l = new Jaccard();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = l.similarity(word, str);
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
   * Returns the maximum similarity between two sets of words using NormalizedLevenshtein.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double maxSimilarityByNormalizedLevenshtein(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    NormalizedLevenshtein l = new NormalizedLevenshtein();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        if (null == word || null == str) {
          continue;
        }
        double sim = l.similarity(word.toLowerCase().trim(), str.toLowerCase().trim());
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the maximum similarity between two sets of words.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double maxSimilarityByJaroWinkler(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    JaroWinkler l = new JaroWinkler();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = l.similarity(word, str);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the maximum similarity between two sets of words by Metric LongestCommonSubsequence.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double maxSimilarityByMetricLCS(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    MetricLCS mlcs = new MetricLCS();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = (1 - mlcs.distance(word.toLowerCase(), str.toLowerCase()));
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the maximum similarity between two sets of words by Stoilos similarity.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double maxSimilarityByStoilos(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    ISub isub = new ISub();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = isub.score(word.toLowerCase(), str.toLowerCase());
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the maximum similarity between two sets of words by FuzzyScore similarity.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double maxSimilarityByFuzzyScore(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    FuzzyScore fs = new FuzzyScore(Locale.ENGLISH);
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        word = word.toLowerCase();
        str = str.toLowerCase();
        String longerWord = word.trim().length() > str.trim().length() ? word.trim() : str.trim();
        double sim = (double)fs.fuzzyScore(word.trim(), str.trim()) / fs.fuzzyScore(longerWord, longerWord);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the maximum similarity between two sets of words by Cosine similarity.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double maxSimilarityByCosine(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    Cosine c = new Cosine();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = c.similarity(word, str);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the maximum similarity between two sets of words.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double maxSimilarityByJaccard(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    Jaccard l = new Jaccard();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = l.similarity(word, str);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the maximum similarity between two sets of words.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double maxSimilarityBySorensenDice(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    SorensenDice l = new SorensenDice();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = l.similarity(word.toLowerCase(), str.toLowerCase());
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  public static double maxSimilarityByStringMatching(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    Levenshtein l = new Levenshtein();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        int min = word.length() < str.length() ? word.length() : str.length();
        double sim = Math.max(0.0, (double) (min - l.distance(word.toLowerCase(), str.toLowerCase())) / min);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  public static double maxSimilarityByMongeElkan(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    StringMetric me = StringMetrics.mongeElkan();
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        double sim = me.compare(word.toLowerCase(), str.toLowerCase());
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Cosine similarity of the best pairing.
   *
   * @param wordList1
   * @param coll1
   * @param wordList2
   * @param coll2
   * @return
   */
  public static double maxSimilarityByTFIDFCosine(Set<String> wordList1, List<List<String>> coll1, Set<String> wordList2, List<List<String>> coll2) {
    double lst = 0.0; // current maximum
    for (String s1 : wordList1) {
      for (String s2 : wordList2) { // compute similarity
        double sim = TFIDFCalculator.cosine_similarity(TFIDFCalculator.weighStringTerms(s1.toLowerCase(), coll1),
                TFIDFCalculator.weighStringTerms(s2.toLowerCase(), coll2));
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the maximum similarity between two sets of words.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double maxSimilarityByQGram(Set<String> words, Set<String> wordList) {
    double lst = 0.0; // current maximum
    QGram q = new QGram(2);
    for (String word : words) {
      for (String str : wordList) { // compute similarity
        int max = word.length() > str.length() ? word.length() : str.length();
        double sim = 1 - q.distance(word.toLowerCase(), str.toLowerCase()) / max;
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Returns the minimum distance between two sets of words using QGram.
   *
   * @param words
   * @param wordList
   * @return
   */
  public static double minDistanceByQGram(Set<String> words, Set<String> wordList) {
    double lst = 100.0; // init minimum distance
    QGram l = new QGram(2);
    for (String word : words) {
      for (String str : wordList) { // compute distance
        double dist = l.distance(word, str);
        if (dist < lst) {
          lst = dist; // update distance
        }
      } // end inner for
    }

    return lst;
  }

  /**
   * Longest common prefix overlap as an real number [0,1] showing how much
   * the prefixes of the best pair from two sets of strings overlap. Splits
   * strings at whitespace and uses best coupling to measure overlap.
   *
   * @param wordList1
   * @param wordList2
   * @return
   */
  public static double bestPrefixOverlap(Set<String> wordList1, Set<String> wordList2) {
    double lst = 0.0; // current maximum
    for (String word : wordList1) {
      for (String str : wordList2) { // compute best overlap
        double sim = lcp(word, str);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  public static double bestSuffixOverlap(Set<String> wordList1, Set<String> wordList2) {
    double lst = 0.0; // current maximum
    for (String word : wordList1) {
      for (String str : wordList2) { // compute best overlap
        double sim = lcs(word, str);
        if (sim > lst) {
          lst = sim; // update maximum
        }
      } // end inner for
    }

    return lst;
  }

  public static boolean samePrefix(Set<String> wordList1, Set<String> wordList2) {
    for (String word : wordList1) {
      for (String str : wordList2) { // compute best overlap
        if (word.substring(0, 1).equalsIgnoreCase(str.substring(0, 1))) {
          return true;
        }
      } // end inner for
    }

    return false;
  }

  public static boolean sameSuffix(Set<String> wordList1, Set<String> wordList2) {
    for (String word : wordList1) {
      for (String str : wordList2) { // compute best overlap
        if (word.substring(word.length() - 1).equalsIgnoreCase(str.substring(str.length() - 1))) {
          return true;
        }
      } // end inner for
    }

    return false;
  }

  /**
   * Longest common prefix overlap as an real number [0,1] showing how much
   * the prefixes of two strings overlap. Divides by the shorter so 1.0 does
   * not imply exact strings.
   *
   * @param str1
   * @param str2
   * @return
   */
  private static double lcp(String str1, String str2) {
    if (str1.isEmpty() || str2.isEmpty()) {
      return 0.0;
    }
    String small, large;
    if (str1.length() > str2.length()) {
      small = str2.toLowerCase();
      large = str1.toLowerCase();
    } else {
      small = str1.toLowerCase();
      large = str2.toLowerCase();
    }
    int index = 0;
    for (char c : large.toCharArray()) {
      if (index == small.length()) {
        break;
      }
      if (c != small.charAt(index)) {
        break;
      }
      index++;
    }

    return (double)index / small.length(); // divides by the smaller length
  }

  private static double lcs(String str1, String str2) {
    if (str1.isEmpty() || str2.isEmpty()) {
      return 0.0;
    }
    String small, large;
    if (str1.length() > str2.length()) {
      small = str2.toLowerCase();
      large = str1.toLowerCase();
    } else {
      small = str1.toLowerCase();
      large = str2.toLowerCase();
    }
    int index = small.length();
//    for (char c : large.toCharArray()) {
    for (int i=large.length(); i > 0; i--) {
      if (index == 0) {
        break;
      }
      char c = large.toCharArray()[i-1];
      if (c != small.charAt(index-1)) {
        break;
      }
      index--;
    }

    return (double)(small.length() - index) / small.length(); // divides by the smaller length
  }

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

  // PRIVATE: used by removeRepetitionAtStart
  private static int ordinalIndexOf(String str, int n) { // nth whitespace
    int pos = str.indexOf(" ");
    while (--n > 0 && pos != -1) {
      pos = str.indexOf(" ", pos + 1);
    }
    return pos;
  }

  // PRIVATE: used by removeRepetitionAtStart
  private static int compareSplit(String str, int n) { // nth whitespace
    int mid = ordinalIndexOf(str, (int) n / 2);
    String left = str.substring(0, mid).trim();
    String right = str.substring(mid).trim();
    if (left.equalsIgnoreCase(right)) {
      System.out.println("Repetition detected!");
      System.out.println("Remove up to index: " + mid);
      return mid;
    }

    return 0;
  }

  /**
   * Removes repetitions at start of texts. This checks up to first n words for
   * repetition.
   *
   * @param text String to check.
   * @param n Maximum n-grams to check from start of string.
   * @return String without repetition (or original string if no repetition
   * found).
   */
  public static String removeRepetitionAtStart(String text, int n) {
    LOG.log(Level.INFO, "Processing {0}", text);

    for (int i = 2; i <= (n * 2); i += 2) { // find index of nth whitespace
      int pos = ordinalIndexOf(text, i);
      if (pos > 0) {
        String sub = text.substring(0, pos);
        int mid = compareSplit(sub, i);
        if (mid > 0) { // repetition found
          text = text.substring(mid).trim(); // Text without repetition
          break; // early exit
        }
      }
    }

    return text;
  }

//  public static String stemTerm (String term) {
//    PorterStemmer stemmer = new PorterStemmer();
//    return stemmer.stem(term);
//  }
  /**
   * NOT COMPLETE!: Retrieves word window from text around every occurrence of a
   * word.
   *
   * @param text
   * @param word
   * @param eitherSide
   * @return
   */
  public static String wordWindow(String text, String word, int eitherSide) {
    String output = "";
    int lengthOfWord = word.split("\\s+").length;
    // Split text into array
//    String[] stringArray = text.split("\\s+");
    // Get index of the start of every occurrence of word in text
    List<Integer> indexes = new ArrayList();
    int index = text.indexOf(word);
    if (index >= 0) {
      indexes.add(index);
    }
    while (index >= 0) {
      System.out.println(index);
      index = text.indexOf(word, index + 1);
      if (index >= 0) {
        indexes.add(index);
      }
    }

    Collections.sort(indexes); // ensure ascending order of indexes

    if (indexes.size() > 0) {
      // Add substring containing each occurrence of word to list such that
      // word is at the start of each list entry except the first (index=0)
      List<String> strings = new ArrayList();
      int start = 0;
      for (Integer indexe : indexes) {
        System.out.print(start);
        System.out.println(" to " + indexe);
        String s = text.substring(start, indexe);
        System.out.println(s);
        strings.add(s);
        start = indexe;
      }
      strings.add(text.substring(start));

      int outstanding = 0; // tracks extracted string extent from a list to avoid content duplication
      for (int i = 1; i < strings.size(); i++) {
        String[] prevLineArray = strings.get(i - 1).split("\\s+");
        String[] lineArray = strings.get(i).split("\\s+");

        int leftExtent = Math.min(prevLineArray.length - outstanding, eitherSide);
        int rightExtent = Math.min(lineArray.length, (eitherSide + lengthOfWord));

        String leftString = extractLeft(prevLineArray, leftExtent);
        String rightString = extractRight(lineArray, rightExtent);

        System.out.println("LEFT: " + leftString);
        System.out.println("RIGHT: " + rightString);

        outstanding = lineArray.length - rightExtent;
        output = (leftString + " " + rightString).trim() + " " + output.trim();
      }
    }

    return output;
  }

  private static String extractLeft(String[] string, int length) {
    String output = "";
    if (string.length > 0 && length > 0) {
      for (int i = string.length - 1; i >= length; i--) {
        output = (string[i] + " " + output).trim();
      }
    }

    return output;
  }

  private static String extractRight(String[] string, int length) {
    String output = "";
    if (string.length > 0 && length > 0) {
      for (int i = 0; i <= length; i++) {
        output = (output + " " + string[i]).trim();
      }
    }

    return output;
  }

  public static void main(String[] args) throws IOException {
//    System.out.println(getLastUriValue("http://data.bgs.ac.uk/id/GeoscienceThesaurus/Concept/0"));
//    try {
//    String sentence = "Some original text(+) I want to match against?^.";
////      sentence = "the first five days of winter were the last five days of -the 10the month of the fifthe year of the decade";
////      String testStr = "4-Dichloro-N-methyl-N-(2-(1-pyrrolidinyl)cyclohexyl)-benzeneacetamide Mesylate, (trans)-(^)-Isomer";
//    String testStr = "Tetraethylthioperoxydicarbonic Diamide, ((H2N)C(S))2S2";
////    testStr = "Colon Surgery (Specialty)";
////      testStr = "-(the";
////    testStr = StringEscapeUtils.escapeJava(testStr);
//    testStr = StringOps.stripParentheses(testStr);
//    System.out.println(testStr);
////      testStr = testStr.replaceAll("\\-", "\\\\-");
//    int i = StringOps.countOccurrence(sentence, testStr, false);
//    System.out.println("Count = " + i);
//    System.out.println("Original : " + testStr);
//    System.out.println("Escaped : " + StringEscapeUtils.escapeJava(testStr)); //.escapeHtml(testStr));
////    } catch(PatternSyntaxException ex) {
////      System.out.println(ex);
////    }
//    System.out.println("Done processing");
//    String string = "Which genes involved in NFkappaB signaling regulate iNOS";
//    System.out.println(string + " | count => " + StringOps.getWordLength(string));
//    string = StringOps.removeStopWords(StopWords.MYSQL_STOP_WORDS, string);
//    System.out.println("==================================");
//    System.out.println(string + " | count => " + StringOps.getWordLength(string));

//    String source = "/Project_Resource/Test_Collections/genomics/topics.csv";
//    List<String> removeWords = new ArrayList<String>();
//    removeWords.add("role");
//    removeWords.add("affect");
//    System.out.println(stem("3,4-Dichloro-N-methyl-N-(2-(1-pyrrolidinyl)cyclohexyl)-benzeneacetamide Mesylate, (trans)-(+-)-Isomer."));
//    File file = new File("C:/dev/rgu/Geoword2vec/text/genomics/genomics.001.txt");
//    System.out.println(stripAllParentheses("child's_rights (gemet)"));
    QGram l = new QGram();
    Set<String> s1 = new HashSet();
    s1.addAll(Arrays.asList("Scientific event", "Conference Event"));
    Set<String> s2 = new HashSet();
    s2.addAll(Arrays.asList("Academic Event", "Conference Participant"));
//    double dist = minDistanceByQGram( s1, s2 );
//    double sim = 1.0 / (dist + 1.0);
    System.out.println( bestPrefixOverlap(s1, s2) );
    System.out.println( bestSuffixOverlap(s1, s2) );

  }

}
