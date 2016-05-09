/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import java.io.IOException;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.util.locale.LanguageTag;

public class AbstractIndexConfiguration {
  private static final Logger log = LoggerFactory.getLogger(AbstractIndexConfiguration.class);

  @Inject
  private MicaConfigService micaConfigService;

  protected void createLocalizedMappingWithAnalyzers(XContentBuilder mapping, String name) {
    try {
      mapping.startObject(name);
      mapping.startObject("properties");
      Stream.concat(micaConfigService.getConfig().getLocalesAsString().stream(), Stream.of(
        LanguageTag.UNDETERMINED)).forEach(locale -> {
        try {
          mapping.startObject(locale).field("type", "multi_field");
          createMappingWithAnalyzers(mapping, locale);
          mapping.endObject();
        } catch(IOException e) {
          log.error("Failed to create localized mappings: '{}'", e);
        }
      });
      mapping.endObject();
      mapping.endObject();
    } catch(IOException e) {
      log.error("Failed to create localized mappings: '{}'", e);
    }
  }

  protected void createMappingWithoutAnalyzer(XContentBuilder mapping, String name) {
    try {
      mapping.startObject(name).field("type", "string").field("index", "not_analyzed").endObject();
    } catch(IOException e) {
      log.error("Failed to create localized mappings: '{}'", e);
    }
  }

  protected void createMappingWithAndWithoutAnalyzer(XContentBuilder mapping, String name) {
    try {
      mapping.startObject(name).field("type", "multi_field");
      createMappingWithAnalyzers(mapping, name);
      mapping.endObject();
    } catch(IOException e) {
      log.error("Failed to create localized mappings: '{}'", e);
    }
  }

  protected void createMappingWithAnalyzers(XContentBuilder mapping, String name) throws IOException {
    mapping
      .startObject("fields")
        .field("analyzed")
        .startObject()
          .field("type", "string")
          .field("index", "analyzed")
          .field("analyzer", "mica_index_analyzer")
          .field("search_analyzer", "mica_search_analyzer")
        .endObject()
        .field(name)
        .startObject()
          .field("type", "string")
          .field("index", "not_analyzed")
        .endObject()
      .endObject();

  }

  protected void appendMembershipProperties(XContentBuilder mapping) throws IOException {
    XContentBuilder membershipsMapping = mapping.startObject("memberships").startObject("properties");
    for(String role : micaConfigService.getConfig().getRoles()) {
      XContentBuilder personMapping = membershipsMapping.startObject(role).startObject("properties") //
        .startObject("person").startObject("properties");
      createMappingWithAndWithoutAnalyzer(personMapping, "lastName");

      XContentBuilder institutionMapping = personMapping.startObject("institution").startObject("properties");
      createLocalizedMappingWithAnalyzers(institutionMapping, "name");
      institutionMapping.endObject().endObject();

      personMapping.endObject().endObject() // person
        .endObject().endObject(); // role
    }
    membershipsMapping.endObject().endObject(); // memberships
  }

}
