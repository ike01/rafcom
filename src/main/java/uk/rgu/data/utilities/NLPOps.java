package uk.rgu.data.utilities;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.ClassifierCombiner;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.util.CoreMap;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

/**
 * Implements NLP operations.
 *
 * @author 1113938
 */
public class NLPOps {

  private static final Logger logger = Logger.getLogger(NLPOps.class.getName());
  private final Version LUCENE_VERSION = Version.LUCENE_4_10_2;
  StanfordCoreNLP pipeline;
  Properties props;

  private static Map<Integer, String> sentences;
  private static Annotation document;

  String serializedClassifier = "C:\\dev\\projects\\annotation\\classifiers\\english.all.3class.distsim.crf.ser.gz";
  AbstractSequenceClassifier<CoreLabel> classifier;

  public NLPOps() {
    props = new Properties();
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
    pipeline = new StanfordCoreNLP(props);
    try {
      classifier = new ClassifierCombiner(serializedClassifier);
    } catch (IOException | ClassCastException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
  }

  public AbstractSequenceClassifier<CoreLabel> nerClassifier() {
    return classifier;
  }

  /**
   * Retrieves sentences in text that contain the supplied term.
   *
   * @param paragraph String containing all sentences.
   * @param term String whose sentences should be returned.
   * @param lemmatised Returns lemmatised content if true.
   * @return Concatenation of strings (unique sentences).
   */
  public String getSentencesWithTerm(String paragraph, String term, boolean lemmatised) {
    String result = "";

    // Create an empty Annotation just with the given text
    document = new Annotation(paragraph);
    // Run Annotators on this text
    pipeline.annotate(document);

    // Use map to track sentences so that a sentence is not returned twice
    sentences = new HashMap();
    int key = 0;
    for (CoreMap coreMap : document.get(CoreAnnotations.SentencesAnnotation.class)) {
      sentences.put(key, coreMap.toString());
      key++;
    }

    Set keySet = sentences.keySet();
    Set<Integer> returnedSet = new HashSet();
    Iterator keyIterator = keySet.iterator();
    // These are all the sentences in this document
    while (keyIterator.hasNext()) {
      int id = (int) keyIterator.next();
      String content = sentences.get(id);
      // This is the current sentence
      String thisSentence = content;
      // Select sentence if it contains term and is not already returned
      if (!returnedSet.contains(id)) { // ensure sentence not already used
        String label = lemmatise(term);
        if (StringUtils.contains(lemmatise(thisSentence.toLowerCase()), label)
                || StringUtils.contains(lemmatise(StringOps.stripAllParentheses(thisSentence.toLowerCase())), label)) { // lookup lemmatised strings
          result = result + " " + thisSentence.trim(); // Concatenate new sentence
          returnedSet.add(id);
        }
      }
    }

    if (null != result && lemmatised) {
      result = lemmatise(result);
    }

    return result;
  }

  /**
   * Retrieves sentences in text that contain the supplied terms. Lemmatises
   * texts before comparison.
   *
   * @param paragraph String containing all sentences.
   * @param terms Collection of strings whose sentences should be returned.
   * @param lemmatised Returns lemmatised content if true.
   * @return Concatenation of strings (unique sentences).
   */
  public String getSentencesWithTerms(String paragraph, Collection<String> terms, boolean lemmatised) {
    String result = "";

    // Create an empty Annotation just with the given text
    document = new Annotation(paragraph);
    // Run Annotators on this text
    pipeline.annotate(document);

    // Use map to track sentences so that a sentence is not returned twice
    sentences = new HashMap();
    int key = 0;
    for (CoreMap coreMap : document.get(CoreAnnotations.SentencesAnnotation.class)) {
      sentences.put(key, coreMap.toString());
      key++;
    }

    Set keySet = sentences.keySet();
    Set<Integer> returnedSet = new HashSet();
    Iterator keyIterator = keySet.iterator();
    // These are all the sentences in this document
    while (keyIterator.hasNext()) {
      int id = (int) keyIterator.next();
      String content = sentences.get(id);
      // This is the current sentence
      String thisSentence = content;
      // Select sentence if it contains any of the terms and is not already returned
      for (String t : terms) {
        if (!returnedSet.contains(id)) { // ensure sentence not already used
          String label = lemmatise(t.toLowerCase().replaceAll("\\s+", " ").trim());
          if (StringUtils.contains(lemmatise(thisSentence.toLowerCase()).replaceAll("\\s+", " "), label)
                  || StringUtils.contains(lemmatise(StringOps.stripAllParentheses(thisSentence.toLowerCase())).replaceAll("\\s+", " "), label)) { // lookup lemmatised strings
            result = result + " " + thisSentence.trim(); // Concatenate new sentence
            returnedSet.add(id);
          }
        }
      }
    }

    if (null != result && lemmatised) {
      result = lemmatise(result);
    }

    return result;
  }

  /**
   * Retrieves sentences in text that contain the supplied terms. Stems
   * texts before comparison.
   *
   * @param paragraph String containing all sentences.
   * @param terms Collection of strings whose sentences should be returned.
   * @param lemmatised Returns lemmatised content if true.
   * @param maxSentenceLength Maximum length of sentences to consider. Use -1 to skip check for sentence length.
   * @return Concatenation of strings (unique sentences).
   */
  public String extractRelevantSentences(String paragraph, Collection<String> terms, boolean lemmatised, int maxSentenceLength) {
    String result = "";
    boolean checkSentenceLength = (maxSentenceLength != -1);

    // Create an empty Annotation just with the given text
    document = new Annotation(paragraph);
    // Run Annotators on this text
    pipeline.annotate(document);

    // Use map to track sentences so that a sentence is not returned twice
    sentences = new HashMap();
    int key = 0;
    for (CoreMap coreMap : document.get(CoreAnnotations.SentencesAnnotation.class)) {
      String string = coreMap.toString();
      if (checkSentenceLength)// if checking sentences, skip is sentence is long
        if (StringOps.getWordLength(string) > maxSentenceLength)
          continue;
      sentences.put(key, coreMap.toString());
      key++;
    }

    Set keySet = sentences.keySet();
    Set<Integer> returnedSet = new HashSet();
    Iterator keyIterator = keySet.iterator();
    // These are all the sentences in this document
    while (keyIterator.hasNext()) {
      int id = (int) keyIterator.next();
      String content = sentences.get(id);
      // This is the current sentence
      String thisSentence = content;
      // Select sentence if it contains any of the terms and is not already returned
      for (String t : terms) {
        if (!returnedSet.contains(id)) { // ensure sentence not already used
          String label = StringOps.stemSentence(t, true);
          if (StringUtils.contains(StringOps.stemSentence(thisSentence, false), label)
                  || StringUtils.contains(StringOps.stemSentence(StringOps.stripAllParentheses(thisSentence), false), label)) { // lookup stemmed strings
            result = result + " " + thisSentence.trim(); // Concatenate new sentence
            returnedSet.add(id);
          }
        }
      }
    }

    if (lemmatised && null != result) {
      result = lemmatise(result);
    }

    return result;
  }

  /**
   * Retrieves collection of string common between two collections. Lemmatises
   * strings before comparison.
   *
   * @param main
   * @param searcher
   * @param stopwords
   * @param lemmatised
   * @return Concatenation of strings (unique sentences).
   */
  public Set<String> getCommonTerms(Collection<String> main, Collection<String> searcher, Collection<String> stopwords, boolean lemmatised) {
    Set<String> result = new HashSet();

    List<String> newMain = new ArrayList();
    // Remove stopwords in new list
    for (String word : main) {
      if (!stopwords.contains(word.toLowerCase()))
        newMain.add(lemmatise(word.trim()));
    }
    // Get common words
    for (String word : searcher) {
      String nextWord = lemmatise(word.trim());
      if (newMain.contains(nextWord)) {
        if (lemmatised) result.add(nextWord.trim());
        else result.add(word.trim());
      }
    }

    return result;
  }

  /**
   * Retrieves sentences contained in a string.
   *
   * @param input
   * @return Set of sentences in input string.
   */
  public Set<String> getSentences(String input) {
    Set<String> output = new HashSet();
    // create an empty Annotation just with the given text
    document = new Annotation(input);
    // run all Annotators on this text
    pipeline.annotate(document);
    // All the sentences in this document
    List<CoreMap> docSentences = document.get(CoreAnnotations.SentencesAnnotation.class);
    for (CoreMap sentence : docSentences) {
      // traverse words in the current sentence
      output.add(sentence.toString());
    }

    return output;
  }

  /**
   * Retrieves sentences contained in a string.
   *
   * @param input
   * @return Set of sentences in input string.
   */
  public List<String> getAllSentences(String input) {
    List<String> output = new ArrayList();
    // create an empty Annotation just with the given text
    document = new Annotation(input);
    // run all Annotators on this text
    pipeline.annotate(document);
    // All the sentences in this document
    List<CoreMap> docSentences = document.get(CoreAnnotations.SentencesAnnotation.class);
    for (CoreMap sentence : docSentences) {
      // traverse words in the current sentence
      output.add(sentence.toString());
    }

    return output;
  }

  /**
   * Lemmatises words in a string.
   *
   * @param input String to lemmatise.
   * @return String with words lemmatised.
   */
  public String lemmatise(String input) {
    String output = "";
    // create an empty Annotation just with the given text
    document = new Annotation(input);
    // run all Annotators on this text
    pipeline.annotate(document);
    // traverse words in the document
    for (CoreLabel token : document.get(CoreAnnotations.TokensAnnotation.class)) {
      output = output + token.lemma() + " ";
    }

    return output.trim();
  }

  /**
   * Stems words in a string.
   *
   * @param input String to stem.
   * @return String with words stemmed.
   */
  public String stem(String input) {
    String output = "";
    // create an empty Annotation just with the given text
    document = new Annotation(input);
    // run all Annotators on this text
    pipeline.annotate(document);
    // traverse words in the document
    for (CoreLabel token : document.get(CoreAnnotations.TokensAnnotation.class)) {
      output = output + token.lemma() + " ";
    }

    return output.trim();
  }

  /**
   * Retrieves the parts-of-speech of the words in a string.
   *
   * @param input Input string.
   * @return Map of input string's words and parts-of-speech as Map<word, POS>.
   */
  public Map<String, String> partsOfSpeech(String input) {
    Map<String, String> output = new HashMap();
    // create an empty Annotation just with the given text
    document = new Annotation(input);
    // run all Annotators on this text
    pipeline.annotate(document);
    // traverse words in the document
    document.get(CoreAnnotations.TokensAnnotation.class).stream().forEach((token) -> {
      // this is the text of the token
      String word = token.get(CoreAnnotations.TextAnnotation.class);
      // this is the POS tag of the token
      String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
      output.put(word, pos);
    });

    return output;
  }

  public String phraseWords(String input) {
    // create an empty Annotation just with the given text
    document = new Annotation(input);
    // run all Annotators on this text
    pipeline.annotate(document);
    String pos = document.get(CoreAnnotations.PhraseWordsTagAnnotation.class);

    return pos;
  }

  /**
   * Generates n-grams from minimum to maximum n-grams specified using Apache
   * Lucene's ShingleFilter. Make 4th parameter true to generate unigrams as
   * well.
   *
   * @param sentence String to generate n-grams from.
   * @param minGram Minimum n-grams to generate. Cannot be less than 2 (See
   * @param unigrams to generate unigrams too).
   * @param maxGram Maximum n-grams to generate.
   * @return Set of n-gram strings.
   */
  public Set<String> nGramGenerator(String sentence, int minGram, int maxGram, boolean unigrams) {
    Set<String> out = new HashSet();
    try {
      StringReader reader = new StringReader(sentence);
      StandardTokenizer source = new StandardTokenizer(LUCENE_VERSION, reader);
      TokenStream tokenStream = new StandardFilter(LUCENE_VERSION, source);
      try (ShingleFilter sf = new ShingleFilter(tokenStream, minGram, maxGram)) {
        sf.setOutputUnigrams(unigrams);

        CharTermAttribute charTermAttribute = sf.addAttribute(CharTermAttribute.class);
        sf.reset();

        while (sf.incrementToken()) {
          out.add(charTermAttribute.toString());
        }

        sf.end();
      }

    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }

    return out;
  }

//  public void testSentence() {
//    String paragraph = "My 1st sentence. “Does it work for questions?” My third sentence.";
//    Reader reader = new StringReader(paragraph);
//    DocumentPreprocessor dp = new DocumentPreprocessor(reader);
//    List<String> sentenceList = new ArrayList();
//
//    for (List<HasWord> sentence : dp) {
//      String sentenceString = Sentence.listToString(sentence);
//      sentenceList.add(sentenceString);
//    }
//
//    sentenceList.stream().forEach((sentence) -> {
//      System.out.println(sentence);
//    });
//  }

  public static void main(String[] args) {

    String string = "What viral genes affect membrane fusion during HIV infection?";
    NLPOps nlpOps = new NLPOps();
//    Set<String> sentencs = nlpOps.getSentences(string);
//    Set<String> grams = nlpOps.nGramGenerator(string, 2, 3, true);
//    grams.forEach(s -> System.out.println(s));
//    System.out.println("Gram: " + nlpOps.lemmatise(string));
  //  Set<String> n = nlpOps.nGramGenerator(nlpOps.lemmatise(string), 2, nlpOps.lemmatise(string).length(), true);
    //  n.forEach(l -> System.out.println(l));
//    Map<String, String> pos = nlpOps.partsOfSpeech(string);
////    String pos = nlpOps.phraseWords(string);
////    System.out.println(pos);
//    pos.forEach((k, v) -> {
//      System.out.println(k + " => " + v);
//    });
    String text = "West of Auchtitench Hill [NS 669 178], the formation is unconformably overlain by the basal Carboniferous Kinnesswood Formation (Inverclyde Group).";
//    System.out
    System.out.println(nlpOps.nerClassifier().classifyWithInlineXML(text));
  }

}
