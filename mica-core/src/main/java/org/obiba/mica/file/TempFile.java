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

import jakarta.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class TempFile extends AbstractAuditableDocument {

  private static final long serialVersionUID = -6939664239667654622L;

  @NotNull
  private String name;

  private long size;

  private String md5;

  public TempFile() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

}
