package uk.rgu.data.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Models a concept/node in the vocabulary with its semantic context.
 * Semantic context here refers to other concepts that are linked to a concept
 * in the vocabulary. We specify concepts within n-hops as context.
 *
 * @author 1113938
 */
public class ConceptContext extends Concept {
  public Set<String> context;

  public ConceptContext() {
    super();
    this.context = new HashSet();
  }

  public ConceptContext(String id, String label, String scheme) {
    super(id, label, scheme);
    this.context = new HashSet();
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
    final ConceptContext other = (ConceptContext) obj;
    Concept c = new Concept(other.getId(), other.getScheme());
    return super.equals(c);
  }

  @Override
  public String toString() {
    return "ConceptContext{" + super.toString() + ", allLabels=" + this.getAllLabels() + ",semanticContext=" + context + "}";
  }

}
