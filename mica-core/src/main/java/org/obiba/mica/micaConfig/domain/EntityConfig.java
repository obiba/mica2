package org.obiba.mica.micaConfig.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.RevisionStatus;
import org.springframework.data.mongodb.core.index.Indexed;


public abstract class EntityConfig extends AbstractGitPersistable {

  public final static String DEFAULT_ID = "default";

  private String schema;

  private String definition;

  @Indexed
  private RevisionStatus revisionStatus = RevisionStatus.DRAFT;

  @Indexed
  private String publishedTag;

  private int revisionsAhead = 0;

  public EntityConfig() {
    setId(DEFAULT_ID);
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  @JsonIgnore
  public RevisionStatus getRevisionStatus() {
    return revisionStatus;
  }

  public void setRevisionStatus(RevisionStatus revisionStatus) {
    this.revisionStatus = revisionStatus;
  }

  @JsonIgnore
  public String getPublishedTag() {
    return publishedTag;
  }

  public void setPublishedTag(String publishedTag) {
    this.publishedTag = publishedTag;
  }

  @JsonIgnore
  public int getRevisionsAhead() {
    return revisionsAhead;
  }

  public void setRevisionsAhead(int revisionsAhead) {
    this.revisionsAhead = revisionsAhead;
  }

  public void incrementRevisionsAhead() {
    revisionsAhead++;
  }

  @Override
  public Map<String, Serializable> parts() {
    final EntityConfig self = this;

    return new HashMap<String, Serializable>(){
      {
        put(self.getClass().getSimpleName(), self);
        put("definition", self.getDefinition());
        put("schema", self.getSchema());
      }
    };
  }
}
