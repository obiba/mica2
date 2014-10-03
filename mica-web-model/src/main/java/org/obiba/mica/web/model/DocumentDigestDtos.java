/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.study.domain.Study;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public class DocumentDigestDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @NotNull
  public Mica.DocumentDigestDto.Builder asDtoBuilder(@NotNull Dataset dataset) {
    return Mica.DocumentDigestDto.newBuilder().setId(dataset.getId()) //
        .addAllName(localizedStringDtos.asDto(dataset.getName()));
  }

  @NotNull
  public Mica.DocumentDigestDto asDto(@NotNull Dataset dataset) {
    return asDtoBuilder(dataset).build();
  }

  @NotNull
  public Mica.DocumentDigestDto.Builder asDtoBuilder(@NotNull Study study) {
    return Mica.DocumentDigestDto.newBuilder().setId(study.getId()) //
        .addAllName(localizedStringDtos.asDto(study.getName()));
  }

  @NotNull
  public Mica.DocumentDigestDto asDto(@NotNull Study study) {
    return asDtoBuilder(study).build();
  }

  @NotNull
  public Mica.DocumentDigestDto.Builder asDtoBuilder(@NotNull Network network) {
    return Mica.DocumentDigestDto.newBuilder().setId(network.getId()) //
        .addAllName(localizedStringDtos.asDto(network.getName()));
  }

  @NotNull
  public Mica.DocumentDigestDto asDto(@NotNull Network network) {
    return asDtoBuilder(network).build();
  }
}