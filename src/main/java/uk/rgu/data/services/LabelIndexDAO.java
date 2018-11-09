package uk.rgu.data.services;

import java.util.List;
import uk.rgu.data.model.LabelIndex;

/**
 *
 * @author ike
 */
public interface LabelIndexDAO {

  /**
   * Creates an entry of ontology concept label.
   *
   * @param labelIndex Concept label detail to add to database.
   */
  public void save(LabelIndex labelIndex);

  /**
   * Retrieves a list of labels with exact string match.
   *
   * @param text String to search for.
   * @return List of matches found.
   */
  public List<LabelIndex> getLabelIndex(String text);

  /**
   * Retrieves a list of labels with exact string match in specified scheme.
   * @param text String to search for.
   * @param scheme Ontology to search in.
   * @return List of matches found.
   */
  public List<LabelIndex> getLabelIndex(String text, String scheme);

  /**
   * Retrieves a list of stemmed labels with exact string match.
   * @param text String to search for.
   * @return List of matches found.
   */
  public List<LabelIndex> getStemmedLabelIndex(String text);

  /**
   * Retrieves a list of stemmed labels with exact string match in specified scheme.
   * @param text String to search for.
   * @param scheme Ontology to search in.
   * @return List of matches found.
   */
  public List<LabelIndex> getStemmedLabelIndex(String text, String scheme);

  /**
   * Retrieves a list of labels that contains supplied string.
   * @param text String to search for within labels.
   * @return List of matches found.
   */
  public List<LabelIndex> getFuzzyLabelIndex(String text);

  /**
   * Deletes a label index entry. Alternative labels of concept (if any) will remain.
   * @param id Id on entry to delete.
   */
  public void delete(int id);

  /**
   * Deletes concept index entries. All labels of concept (if any) will be deleted!
   * @param idInScheme Unique id of concept in its ontology.
   * @param scheme Ontology to delete from.
   */
  public void delete(String idInScheme, String scheme);

}
