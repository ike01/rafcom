package uk.rgu.data.ontologyprocessor.word2vec;

import java.util.regex.Pattern;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;

/**
 * Custom implementation of CommonPreprocessor that preserves numbers if they
 * are part of a string e.g. numbers in 3D, 17km or marine_isotope_5a. However, 3 in '3-d' is not preserved.
 *
 * @author aikay
 */
public class GeoCommonPreprocessor implements TokenPreProcess {

  private static final Pattern PUNCT_PATTERN = Pattern.compile("\\b\\d+\\b|[\\.:,\"\'\\(\\)\\[\\]|/?!;]+");

  @Override
  public String preProcess(String token) {
        return stripPunct(token).toLowerCase();
    }

  /**
     * Strip punctuation
     * @param base the base string
     * @return the cleaned string
     */
    private static String stripPunct(String base) {
        return PUNCT_PATTERN.matcher(base).replaceAll("");
    }
}
