/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.validation.constraints.NotNull;

public class DefaultEntityBase implements EntityBase {

  private String id;
  private LocalDateTime createdDate = LocalDateTime.now();
  private LocalDateTime lastModifiedDate;

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public @NotNull Optional<LocalDateTime> getCreatedDate() {
    return Optional.ofNullable(createdDate);
  }

  @Override
  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  @Override
  public Optional<LocalDateTime> getLastModifiedDate() {
    return Optional.ofNullable(lastModifiedDate);
  }

  @Override
  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  @Override
  public boolean isNew() {
    return id == null;
  }
}
