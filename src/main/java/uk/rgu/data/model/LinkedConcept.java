package uk.rgu.data.model;

import uk.rgu.data.utilities.StringOps;

/**
 * Models a linked concept.
 *
 * @author Ikechukwu
 */
public class LinkedConcept extends Concept {
  private Related subClasslike;
  private Related superClasslike;

  public LinkedConcept() {
  }

  public LinkedConcept(String id, String label, String scheme) {
    super(id, label, scheme);
  }

  public Related getSubClasslike() {
    return subClasslike;
  }

  public void setSubClasslike(Related subClasslike) {
    this.subClasslike = subClasslike;
  }

  public Related getSuperClasslike() {
    return superClasslike;
  }

  public void setSuperClasslike(Related superClasslike) {
    this.superClasslike = superClasslike;
  }

  @Override
  public String getConceptId() {
    return super.getScheme() + "-" + StringOps.getLastUriValue(super.getId());
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
    final LinkedConcept other = (LinkedConcept) obj;
    Concept c = new Concept(other.getId(), other.getScheme());
    return super.equals(c);
  }

  @Override
  public String toString() {
    return "LinkedConcept{" + super.toString() + "subClasslike=" + subClasslike + ", superClasslike=" + superClasslike + "}";
  }

}
