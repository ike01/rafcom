package uk.rgu.data.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.rgu.data.model.LabelIndex;

/**
 *
 * @author ike
 */
public class LabelIndexService implements LabelIndexDAO {

  private static final Logger LOG = Logger.getLogger(LabelIndexService.class.getName());
  private final Connection con;

  public LabelIndexService(Connection con) throws SQLException {
    this.con = con;
  }

  /**
   * Retrieves all label indexes for a scheme.
   *
   * @param scheme
   * @return
   */
  public List<LabelIndex> getAllLabelIndexes(String scheme) {
    List<LabelIndex> allLabelIndex = new ArrayList();
    try {
      String sql = "SELECT id, label, stemmed, lemmatised, scheme, conceptId FROM labelindex WHERE scheme = ?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, scheme.toUpperCase());
      ResultSet result = statement.executeQuery();

      while (result.next()) {
        LabelIndex labelIndex = new LabelIndex();
        labelIndex.setId(result.getInt(1));
        labelIndex.setLabel(result.getString(2));
        labelIndex.setStemmedLabel(result.getString(3));
        labelIndex.setLemmatisedLabel(result.getString(4));
        labelIndex.setScheme(result.getString(5));
        labelIndex.setConceptId(result.getString(6));
        allLabelIndex.add(labelIndex);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return allLabelIndex;
  }

  @Override
  public void save(LabelIndex labelIndex) {
    try {
      String sql = "INSERT INTO labelindex (label, stemmed, scheme, conceptId) VALUES (?, ?, ?, ?)";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, labelIndex.getLabel());
      statement.setString(2, labelIndex.getStemmedLabel());
      statement.setString(3, labelIndex.getScheme());
      statement.setString(4, labelIndex.getConceptId());

      int rowsInserted = statement.executeUpdate();
      if (rowsInserted > 0) {
        LOG.info("A concept label was inserted successfully!");
      } else {
        LOG.info("Concept label was NOT inserted!");
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Persists minimum depth from root node to concepts.
   *
   * @param conceptId
   * @param depth
   */
  public void addLevel(String conceptId, int depth) {
    try {
      String sql = "UPDATE labelindex SET depth=? WHERE conceptId=?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setInt(1, depth);
      statement.setString(2, conceptId);

      int rowsInserted = statement.executeUpdate();
//      con.commit();
      if (rowsInserted > 0) {
        LOG.log(Level.INFO, "{0} rows updated. Depth for {1} was successfully updated. Depth = {2}", new Object[]{rowsInserted, conceptId, depth});
      } else {
        LOG.log(Level.INFO, "Concept depth was NOT update for {0}", conceptId);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Add lemmatised label.
   *
   * @param id
   * @param stemmedLabel
   */
  public void addLemma(int id, String stemmedLabel) {
    try {
      String sql = "UPDATE labelindex SET lemmatised=? WHERE id=?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, stemmedLabel);
      statement.setInt(2, id);

      int rowsInserted = statement.executeUpdate();
      if (rowsInserted > 0) {
        LOG.log(Level.INFO, "Lemma for {0} was successfully added. Lemma = {1}", new Object[]{id, stemmedLabel});
      } else {
        LOG.log(Level.INFO, "Concept label lemma was NOT added for {0}", id);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Retrieves a label for a concept (Selects first available label).
   *
   * @param conceptId Id of input concept.
   * @param stemmed Indicates if stemmed label should be used.
   * @return List of all labels for a concept.
   */
  public String getConceptLabel(String conceptId, boolean stemmed) {
    String conceptLabel = "";
    try {
      String sql;
      if (stemmed) {
        sql = "SELECT stemmed FROM labelindex2 WHERE conceptId = ?";
      } else {
        sql = "SELECT label FROM labelindex2 WHERE conceptId = ?";
      }

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, conceptId);
      ResultSet result = statement.executeQuery();

      if (result.next()) {
        conceptLabel = result.getString(1);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return conceptLabel;
  }

  /**
   * Retrieves all labels of a concept.
   *
   * @param conceptId Id of input concept.
   * @param stemmed Indicates if stemmed labels should be used.
   * @return List of all labels for a concept.
   */
  public Set<String> getConceptLabels(String conceptId, boolean stemmed) {
    Set<String> coceptLabels = new HashSet();
    try {
      String sql;
      if (stemmed) {
        sql = "SELECT stemmed FROM labelindex WHERE conceptId = ?";
      } else {
        sql = "SELECT label FROM labelindex WHERE conceptId = ?";
      }

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, conceptId);
      ResultSet result = statement.executeQuery();

      while (result.next()) {
        String label = result.getString(1);
        coceptLabels.add(label);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return coceptLabels;
  }

  @Override
  public List<LabelIndex> getLabelIndex(String text) {
    List<LabelIndex> labelIndexes = new ArrayList();
    try {
      String sql = "SELECT id, label, stemmed, lemmatised, scheme, conceptId, depth FROM labelindex WHERE label = ?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, text);
      ResultSet result = statement.executeQuery();

      while (result.next()) {
        LabelIndex labelIndex = new LabelIndex();
        labelIndex.setId(result.getInt(1));
        labelIndex.setLabel(result.getString(2));
        labelIndex.setStemmedLabel(result.getString(3));
        labelIndex.setLemmatisedLabel(result.getString(4));
        labelIndex.setScheme(result.getString(5));
        labelIndex.setConceptId(result.getString(6));
        labelIndex.setDepth(result.getInt(7));
        labelIndexes.add(labelIndex);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return labelIndexes;
  }

  public List<LabelIndex> getLabelById(String conceptId) {
    List<LabelIndex> labelIndexes = new ArrayList();
    try {
      String sql = "SELECT id, label, stemmed, lemmatised, scheme, conceptId, depth FROM labelindex WHERE conceptId = ?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, conceptId);
      ResultSet result = statement.executeQuery();

      while (result.next()) {
        LabelIndex labelIndex = new LabelIndex();
        labelIndex.setId(result.getInt(1));
        labelIndex.setLabel(result.getString(2));
        labelIndex.setStemmedLabel(result.getString(3));
        labelIndex.setLemmatisedLabel(result.getString(4));
        labelIndex.setScheme(result.getString(5));
        labelIndex.setConceptId(result.getString(6));
        labelIndex.setDepth(result.getInt(7));
        labelIndexes.add(labelIndex);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return labelIndexes;
  }

  @Override
  public List<LabelIndex> getLabelIndex(String text, String scheme) {
    List<LabelIndex> labelIndexes = new ArrayList();
    try {
      String sql = "SELECT id, label, stemmed, lemmatised, scheme, conceptId, depth FROM labelindex WHERE label = ? AND scheme = ?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, text);
      statement.setString(2, scheme);
      ResultSet result = statement.executeQuery();

      while (result.next()) {
        LabelIndex labelIndex = new LabelIndex();
        labelIndex.setId(result.getInt(1));
        labelIndex.setLabel(result.getString(2));
        labelIndex.setStemmedLabel(result.getString(3));
        labelIndex.setLemmatisedLabel(result.getString(4));
        labelIndex.setScheme(result.getString(5));
        labelIndex.setConceptId(result.getString(6));
        labelIndex.setDepth(result.getInt(7));
        labelIndexes.add(labelIndex);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return labelIndexes;
  }

  @Override
  public List<LabelIndex> getStemmedLabelIndex(String text) {
    List<LabelIndex> labelIndexes = new ArrayList();
    try {
      String sql = "SELECT id, label, stemmed, lemmatised, scheme, conceptId, depth FROM labelindex WHERE stemmed = ?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, text);
      ResultSet result = statement.executeQuery();

      while (result.next()) {
        LabelIndex labelIndex = new LabelIndex();
        labelIndex.setId(result.getInt(1));
        labelIndex.setLabel(result.getString(2));
        labelIndex.setStemmedLabel(result.getString(3));
        labelIndex.setLemmatisedLabel(result.getString(4));
        labelIndex.setScheme(result.getString(5));
        labelIndex.setConceptId(result.getString(6));
        labelIndex.setDepth(result.getInt(7));
        labelIndexes.add(labelIndex);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return labelIndexes;
  }

  @Override
  public List<LabelIndex> getStemmedLabelIndex(String text, String scheme) {
    List<LabelIndex> labelIndexes = new ArrayList();
    try {
      String sql = "SELECT id, label, stemmed, lemmatised, scheme, conceptId, depth FROM labelindex WHERE stemmed = ? AND scheme = ?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, text);
      statement.setString(2, scheme);
      ResultSet result = statement.executeQuery();

      while (result.next()) {
        LabelIndex labelIndex = new LabelIndex();
        labelIndex.setId(result.getInt(1));
        labelIndex.setLabel(result.getString(2));
        labelIndex.setStemmedLabel(result.getString(3));
        labelIndex.setLemmatisedLabel(result.getString(4));
        labelIndex.setScheme(result.getString(5));
        labelIndex.setConceptId(result.getString(6));
        labelIndex.setDepth(result.getInt(7));
        labelIndexes.add(labelIndex);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return labelIndexes;
  }

  public List<LabelIndex> getLemmatisedLabelIndex(String text, String scheme) {
    List<LabelIndex> labelIndexes = new ArrayList();
    try {
      String sql = "SELECT id, label, stemmed, lemmatised, scheme, conceptId, depth FROM labelindex WHERE lemmatised = ? AND scheme = ?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, text);
      statement.setString(2, scheme);
      ResultSet result = statement.executeQuery();

      while (result.next()) {
        LabelIndex labelIndex = new LabelIndex();
        labelIndex.setId(result.getInt(1));
        labelIndex.setLabel(result.getString(2));
        labelIndex.setStemmedLabel(result.getString(3));
        labelIndex.setLemmatisedLabel(result.getString(4));
        labelIndex.setScheme(result.getString(5));
        labelIndex.setConceptId(result.getString(6));
        labelIndex.setDepth(result.getInt(7));
        labelIndexes.add(labelIndex);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return labelIndexes;
  }

  @Override
  public List<LabelIndex> getFuzzyLabelIndex(String text) {
    List<LabelIndex> labelIndexes = new ArrayList();
    try {
      String sql = "SELECT id, label, stemmed, lemmatised, scheme, conceptId, depth FROM labelindex WHERE label like ?";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, "%" + text + "%");
      ResultSet result = statement.executeQuery();

      while (result.next()) {
        LabelIndex labelIndex = new LabelIndex();
        labelIndex.setId(result.getInt(1));
        labelIndex.setLabel(result.getString(2));
        labelIndex.setStemmedLabel(result.getString(3));
        labelIndex.setLemmatisedLabel(result.getString(4));
        labelIndex.setScheme(result.getString(5));
        labelIndex.setConceptId(result.getString(6));
        labelIndex.setDepth(result.getInt(7));
        labelIndexes.add(labelIndex);
      }
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    return labelIndexes;
  }

  @Override
  public void delete(int id) {
    try {
      String sql = "DELETE FROM labelindex WHERE id = ?";
      PreparedStatement statement = con.prepareStatement(sql);
      statement.setInt(1, id);
      statement.executeUpdate();
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public void delete(String conceptId, String scheme) {
    try {
      String sql = "DELETE FROM labelindex WHERE concept = ? AND scheme = ?";
      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, conceptId);
      statement.setString(2, scheme);
      statement.executeUpdate();
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    } catch (NullPointerException ex) {
      LOG.log(Level.INFO, "No deletion was made. Reason: ", ex.getMessage());
    }
  }

  public void delete(String scheme) {
    try {
      String sql = "DELETE FROM labelindex WHERE scheme = ?";
      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, scheme);
      statement.executeUpdate();
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, null, ex);
    } catch (NullPointerException ex) {
      LOG.log(Level.INFO, "No deletion was made. Reason: ", ex.getMessage());
    }
  }

  public boolean exists(String conceptId) {
    try {
      String sql = "SELECT id FROM labelindex WHERE conceptId = ? LIMIT 1";

      PreparedStatement statement = con.prepareStatement(sql);
      statement.setString(1, conceptId);

      ResultSet result = statement.executeQuery();
      return result.next();
    } catch (SQLException ex) {
      LOG.log(Level.SEVERE, "Could not check if " + conceptId + " exists. Reason: ", ex.getMessage());
    }
    return false;
  }

}
