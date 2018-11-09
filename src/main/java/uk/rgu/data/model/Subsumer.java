package uk.rgu.data.model;

import java.util.Objects;

/**
 *
 * @author 1113938
 */
public class Subsumer extends Concept {
  private int distance;

  public Subsumer() {
  }

  public Subsumer(String id, String scheme, int distance) {
    super(id, scheme);
    this.distance = distance;
  }

  public Subsumer(Concept c, int distance) {
    this.setId(c.getId());
    this.setScheme(c.getScheme());
    this.distance = distance;
  }

  public Subsumer(Concept c) {
    this.setId(c.getId());
    this.setScheme(c.getScheme());
  }

  public int getDistance() {
    return distance;
  }

  public void setDistance(int distance) {
    this.distance = distance;
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
    final Subsumer other = (Subsumer) obj;
    if (!Objects.equals(this.getId(), other.getId())) {
      return false;
    }
    return Objects.equals(this.getScheme(), other.getScheme());
  }

  @Override
  public String toString() {
    return " Subsumer{" + super.toString() + "| distance=" + distance + '}';
  }


}
