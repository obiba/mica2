/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.LocalizedString;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;

@Document
public class Attachment extends AbstractAuditableDocument implements AttributeAware, Comparable<Attachment>, Serializable {

  private static final long serialVersionUID = 3L;

  @JsonIgnore
  private boolean justUploaded;

  @NotNull
  private String name;

  private String type;

  private LocalizedString description;

  private Locale lang;

  private long size;

  private String md5;

  private Attributes attributes = new Attributes();

  private String path;

  private String fileReference;

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.writeObject(getId());
    out.writeObject(getVersion());
    out.writeObject(getCreatedBy().orElse(null));
    out.writeObject(getLastModifiedBy().orElse(null));
    out.writeObject(getCreatedDate().orElse(null));
    out.writeObject(getLastModifiedDate().orElse(null));

    out.writeObject(name);
    out.writeObject(type);
    out.writeObject(description);
    out.writeObject(lang);
    out.writeLong(size);
    out.writeObject(md5);
    out.writeObject(attributes);
    out.writeObject(path);
    out.writeObject(fileReference);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    setId((String) in.readObject());
    setVersion((Long) in.readObject());
    setCreatedBy((String) in.readObject());
    setLastModifiedBy((String) in.readObject());
    setCreatedDate((LocalDateTime) in.readObject());
    setLastModifiedDate((LocalDateTime) in.readObject());

    name = (String) in.readObject();
    type = (String) in.readObject();
    description = (LocalizedString) in.readObject();
    lang = (Locale) in.readObject();
    size = in.readLong();
    md5 = (String) in.readObject();
    attributes = (Attributes) in.readObject();
    path = (String) in.readObject();
    fileReference = (String) in.readObject();
  }

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public LocalizedString getDescription() {
    return description;
  }

  public void setDescription(LocalizedString description) {
    this.description = description;
  }

  public Locale getLang() {
    return lang;
  }

  public void setLang(Locale lang) {
    this.lang = lang;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public String getFileReference() {
    return Strings.isNullOrEmpty(fileReference) ? getId() : fileReference;
  }

  public void setFileReference(String fileReference) {
    this.fileReference = fileReference;
  }

  public boolean isJustUploaded() {
    return justUploaded;
  }

  public void setJustUploaded(boolean justUploaded) {
    this.justUploaded = justUploaded;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;

    if(obj == null || getClass() != obj.getClass()) return false;

    return Objects.equals(getId(), ((Attachment) obj).getId());
  }

  @Override
  public void addAttribute(Attribute attribute) {
    attributes.addAttribute(attribute);
  }

  @Override
  public void removeAttribute(Attribute attribute) {
    attributes.removeAttribute(attribute);
  }

  @Override
  public void removeAllAttributes() {
    attributes.removeAllAttributes();
  }

  @Override
  public boolean hasAttribute(String attName, @Nullable String namespace) {
    return attributes.hasAttribute(attName, namespace);
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public void setAttributes(@NotNull Attributes attributes) {
    this.attributes = attributes;
  }

  public boolean hasPath() {
    return !Strings.isNullOrEmpty(path);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public int compareTo(Attachment o) {
    if (equals(o)) return 0;
    int cmp = 0;
    if (!Strings.isNullOrEmpty(getPath())) cmp = getPath().compareToIgnoreCase(o.getPath());
    if (cmp != 0) return cmp;
    cmp = getName().compareToIgnoreCase(o.getName());
    if (cmp != 0) return cmp;
    return getCreatedDate().orElse(null).compareTo(o.getCreatedDate().orElse(null));
  }
}
