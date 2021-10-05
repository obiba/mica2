/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import sun.util.locale.LanguageTag;

import static java.util.stream.Collectors.toMap;

@Component
@Scope("request")
@Path("/config/data-access")
@RequiresAuthentication
public class DataAccessResource {

  @Inject
  DataAccessConfigService dataAccessConfigService;

  @Inject
  DataAccessFormService dataAccessFormService;

  @Inject
  Dtos dtos;

  @Inject
  FileResource fileResource;

  @GET
  @Timed
  public Mica.DataAccessConfigDto getConfig() {
    return dtos.asDto(dataAccessConfigService.getOrCreateConfig());
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessConfigDto dto) {
    dataAccessConfigService.save(dtos.fromDto(dto));
    return Response.ok().build();
  }

  @GET
  @Path("/form")
  @Timed
  public Mica.DataAccessFormDto getDataAccessForm(@QueryParam("lang") String lang) {
    Optional<DataAccessForm> d = dataAccessFormService.findDraft();

    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();

    DataAccessForm dataAccessForm = d.get();
    Mica.DataAccessFormDto.Builder builder = Mica.DataAccessFormDto.newBuilder(dtos.asDto(dataAccessForm))
      .clearProperties().clearPdfTemplates();

    String langTag = !Strings.isNullOrEmpty(lang) ? Locale.forLanguageTag(lang).toLanguageTag() : LanguageTag.UNDETERMINED;

    Map<String, LocalizedString> properties = dataAccessForm.getProperties().entrySet().stream()
      .map(e -> Maps.immutableEntry(e.getKey(), new LocalizedString().forLanguageTag(langTag, e.getValue().get(langTag))))
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    builder.addAllProperties(dtos.asDtoList(properties));

    return builder.build();
  }

  @GET
  @Path("/pdf")
  public FileResource getDataAccessPdf(@QueryParam("lang") String lang) {
    Optional<DataAccessForm> d = dataAccessFormService.findDraft();

    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();

    DataAccessForm dataAccessForm = d.get();
    Locale locale = Locale.forLanguageTag(!Strings.isNullOrEmpty(lang) ? Locale.forLanguageTag(lang).toLanguageTag() : LanguageTag.UNDETERMINED);

    if (!dataAccessForm.getPdfTemplates().containsKey(locale)) throw NoSuchDataAccessFormException.withDefaultMessage();

    fileResource.setAttachment(dataAccessForm.getPdfTemplates().get(locale));

    return fileResource;
  }
}
