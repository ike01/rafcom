package uk.rgu.data.model;

import java.util.List;

/**
 * Models a related concept.
 *
 * @author Ikechukwu
 */
public class Related {

  private String property;
  private List<String> values;

  public Related() {
  }

  public Related(String property, List<String> values) {
    this.property = property;
    this.values = values;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
    this.values = values;
  }

  @Override
  public String toString() {
    return "Related{" + "property=" + property + ", values=" + values + "}";
  }

}
