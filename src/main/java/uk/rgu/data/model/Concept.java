package uk.rgu.data.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import uk.rgu.data.ontologyprocessor.Ontology;
import uk.rgu.data.utilities.StringOps;

/**
 * Models a concept/node in the vocabulary.
 *
 * @author 1113938
 */
public class Concept {
  /**
   * Concept id
   */
  private String id; // id
  /**
   * Preferred label
   */
  private String label; // label (prefLabel)
  /**
   * Alternative labels
   */
  private Set<String> altLabels; // label (prefLabel)
  /**
   * Scheme concept belongs to
   */
  private String scheme; // vocabulary

  public Concept() {
    this.altLabels = new HashSet();
  }

  /**
   * Constructor.
   * @param id Serves as Id in a scheme ((id + scheme) should be unique across all schemes)
   * @param label
   * @param scheme
   */
  public Concept(String id, String label, String scheme) {
    this.id = id;
    this.label = label;
    this.scheme = scheme.toUpperCase();
    this.altLabels = new HashSet();
  }

  /**
   * Constructor.
   * @param id Serves as Id.
   * @param scheme
   */
  public Concept(String id, String scheme) {
    this.id = id;
    this.scheme = scheme.toUpperCase();
    this.label = null;
    this.altLabels = new HashSet();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Set<String> getAltLabels() {
    return altLabels;
  }

  public void setAltLabels(Set<String> altLabels) {
    this.altLabels = altLabels;
  }

  public void addAltLabels(Set<String> altLabels) {
    this.altLabels.addAll(altLabels);
  }

  public void addAltLabel(String altLabels) {
    this.altLabels.add(altLabels);
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme.toUpperCase();
  }

  public String getConceptId() {
    return scheme + "-" + id;
  }

  public String getConceptIdFromFullUri() {
    return scheme + "-" + StringOps.getLastUriValue(id);
  }

  public String getFullConceptUri() {
    return Ontology.getGraph(scheme).conceptUri + id;
  }

  public static String getFullConceptUri(String conceptId) {
    String[] str = conceptId.split("-");
    return Ontology.getGraph(str[0]).conceptUri + str[1];
  }

  /**
   * Retrieves a combination of main label and alternate labels.
   *
   * @return Set of all labels.
   */
  public Set<String> getAllLabels() {
    Set<String> allLabels = new HashSet();
    allLabels.add(label);
    if (altLabels != null && !altLabels.isEmpty())
      allLabels.addAll(altLabels);
    return allLabels;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 89 * hash + Objects.hashCode(this.id);
    hash = 89 * hash + Objects.hashCode(this.scheme);
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
    final Concept other = (Concept) obj;
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return Objects.equals(this.scheme, other.scheme);
  }

  @Override
  public String toString() {
    return "Concept{" + "id=" + id + ", label=" + label + ", scheme=" + scheme + "}";
  }

  /**
   * Custom comparator to sort Concepts alphabetically based on preferred labels.
   */
  public static class ConceptComparator implements Comparator<Concept> {

    @Override
    public int compare(Concept t, Concept t1) {
      return (t.getLabel().compareTo(t1.getLabel()));
    }

  }
//
//  public static void main(String[] args) {
//    Set<Concept> concepts = new HashSet();
//    Concept c1 = new Concept("1", "a", "a");
//    Concept c2 = new Concept("2", "a", "a");
//    Concept c3 = new Concept("1", "c", "a");
//    concepts.add(c1);
//    concepts.add(c2);
//    concepts.add(c3);
//    concepts.forEach(System.out::println);
//  }

}
