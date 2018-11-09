package uk.rgu.data.model;

/**
 * Models a linked concept.
 *
 * @author Ikechukwu
 */
public class LinkedConceptWithSiblings extends LinkedConcept {
  private Related siblinglike;

  public LinkedConceptWithSiblings() {
  }

  public LinkedConceptWithSiblings(String id, String label, String scheme) {
    super(id, label, scheme);
  }

  public Related getSiblinglike() {
    return siblinglike;
  }

  public void setSiblinglike(Related siblinglike) {
    this.siblinglike = siblinglike;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final LinkedConceptWithSiblings other = (LinkedConceptWithSiblings) obj;
    Concept c = new Concept(other.getId(), other.getScheme());
    return super.equals(c);
  }

  @Override
  public String toString() {
    return "LinkedConceptWithSiblings{" + super.toString() + "siblinglike=" + siblinglike + "}";
  }

}
