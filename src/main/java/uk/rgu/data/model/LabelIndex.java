package uk.rgu.data.model;

import java.util.Objects;

/**
 * Index of ontology concepts.
 *
 * @author 1113938
 */
public class LabelIndex {

  private int id;
  private String label;
  private String stemmedLabel;
  private String lemmatisedLabel;
  private String scheme;
  private String conceptId;
  private int depth; // minimum depth

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getStemmedLabel() {
    return stemmedLabel;
  }

  public void setStemmedLabel(String stemmedLabel) {
    this.stemmedLabel = stemmedLabel;
  }

  public String getLemmatisedLabel() {
    return lemmatisedLabel;
  }

  public void setLemmatisedLabel(String lemmatisedLabel) {
    this.lemmatisedLabel = lemmatisedLabel;
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public String getConceptId() {
    return conceptId;
  }

  public void setConceptId(String conceptId) {
    this.conceptId = conceptId;
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 79 * hash + Objects.hashCode(this.conceptId);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final LabelIndex other = (LabelIndex) obj;
    if (!Objects.equals(this.conceptId, other.conceptId)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "LabelIndex{" + "id=" + id + ", label=" + label + ", stemmedLabel=" + stemmedLabel + ", scheme=" + scheme + ", conceptId=" + conceptId + ", depth=" + depth + "}";
  }

}
