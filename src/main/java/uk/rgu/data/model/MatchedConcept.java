package uk.rgu.data.model;

import java.util.List;

/**
 * Models a string with list of concept labels it is assumed to refer to.
 *
 * @author 1113938
 */
public class MatchedConcept {

  private String label;
  private List<LabelIndex> labelIndex;

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public List<LabelIndex> getLabelIndex() {
    return labelIndex;
  }

  public void setLabelIndex(List<LabelIndex> labelIndex) {
    this.labelIndex = labelIndex;
  }

  @Override
  public String toString() {
    return "MatchedConcept{" + "label=" + label + ", labelIndex=" + labelIndex + '}';
  }

}
