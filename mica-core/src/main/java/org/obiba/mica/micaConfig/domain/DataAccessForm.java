package org.obiba.mica.micaConfig.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.AttachmentAware;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.PersistableWithAttachments;
import org.springframework.data.mongodb.core.index.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class DataAccessForm extends AbstractGitPersistable implements PersistableWithAttachments, AttachmentAware {

  public final static String DEFAULT_ID = "default";

  private String schema;

  private String definition;

  private Map<Locale, Attachment> pdfTemplates;

  private Map<String, LocalizedString> properties;

  private String titleFieldPath;

  @Indexed
  private RevisionStatus revisionStatus = RevisionStatus.DRAFT;

  @Indexed
  private String publishedTag;

  private int revisionsAhead = 0;

  public DataAccessForm() {
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

  public Map<String, LocalizedString> getProperties() {
    return properties == null ? properties = Maps.newHashMap() : properties;
  }

  public void setProperties(Map<String, LocalizedString> properties) {
    this.properties = properties;
  }

  @Override
  public boolean hasAttachments() {
    return false;
  }

  @Override
  public List<Attachment> getAttachments() {
    return null;
  }

  @Override
  public void addAttachment(@NotNull Attachment attachment) {
    throw new NotImplementedException();
  }

  @Override
  public void setAttachments(List<Attachment> attachments) {
    throw new NotImplementedException();
  }

  @JsonIgnore
  @Override
  public Iterable<Attachment> getAllAttachments() {
    return Iterables.concat(getPdfTemplates().values());
  }

  @Override
  public Attachment findAttachmentById(String attachmentId) {
    return null;
  }

  public Map<Locale, Attachment> getPdfTemplates() {
    return pdfTemplates == null ? pdfTemplates = Maps.newHashMap() : pdfTemplates;
  }

  public void setPdfTemplates(Map<Locale, Attachment> pdfTemplates) {
    this.pdfTemplates = pdfTemplates;
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
  public String pathPrefix() {
    return "data-access-forms";
  }

  @Override
  public Map<String, Serializable> parts() {
    final DataAccessForm self = this;

    return new HashMap<String, Serializable>(){
      {
        put(self.getClass().getSimpleName(), self);
        put("definition", self.getDefinition());
        put("schema", self.getSchema());
      }
    };
  }

  public String getTitleFieldPath() {
    return titleFieldPath;
  }

  public void setTitleFieldPath(String titleFieldPath) {
    this.titleFieldPath = titleFieldPath;
  }
}
