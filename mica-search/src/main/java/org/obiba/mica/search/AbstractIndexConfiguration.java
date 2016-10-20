/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.obiba.mica.core.domain.TaxonomyTarget;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import sun.util.locale.LanguageTag;

public abstract class AbstractIndexConfiguration {
  private static final Logger log = LoggerFactory.getLogger(AbstractIndexConfiguration.class);
  private static final String TRUE = "true";
  private static final String LOCALIZED = "localized";
  private static final String TYPE = "type";
  private static final String STATIC = "static";
  private static final String FIELD = "field";

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private TaxonomyConfigService taxonomyConfigService;

  protected Taxonomy getTaxonomy() {
    return taxonomyConfigService.findByTarget(getTarget());
  }

  protected void addLocalizedVocabularies(Taxonomy taxonomy, String... fields) {
    Arrays.asList(fields).forEach(f -> {
      taxonomy.addVocabulary(newVocabularyBuilder().name(f).field(f).localized().build());
    });
  }

  protected void addStaticVocabularies(Taxonomy taxonomy, String... fields) {
    Arrays.asList(fields).forEach(f -> {
      taxonomy.addVocabulary(newVocabularyBuilder().name(f).field(f).staticField().build());
    });
  }

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
    createMappingWithoutAnalyzer(mapping, name, null);
  }

  protected void createMappingWithoutAnalyzer(XContentBuilder mapping, String name, String type) {
    try {
      mapping.startObject(name).field("type", resolveType(type)).field("index", "not_analyzed").endObject();
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
      createMappingWithAndWithoutAnalyzer(personMapping, "fullName");
      createMappingWithAndWithoutAnalyzer(personMapping, "email");

      XContentBuilder institutionMapping = personMapping.startObject("institution").startObject("properties");
      createLocalizedMappingWithAnalyzers(institutionMapping, "name");
      institutionMapping.endObject().endObject();

      personMapping.endObject().endObject() // person
        .endObject().endObject(); // role
    }
    membershipsMapping.endObject().endObject(); // memberships
  }

  private void createSchemaMapping(XContentBuilder mapping, SchemaNode schema) throws IOException {
    for(SchemaNode node : schema.getChildren()) {
      if(node.getVocabulary() != null) {
        Vocabulary v = node.getVocabulary();
        if(TRUE.equals(v.getAttributeValue(LOCALIZED))) {
          createLocalizedMappingWithAnalyzers(mapping, node.getName());
        } else if(v.hasTerms() || TRUE.equals(v.getAttributeValue(STATIC))){
          createMappingWithoutAnalyzer(mapping, node.getName(), v.getAttributeValue(TYPE));
        }
      } else {
        mapping.startObject(node.getName()).startObject("properties");
        createSchemaMapping(mapping, node);
        mapping.endObject().endObject();
      }
    }
  }

  private String resolveType(String type) {
    if (!Strings.isNullOrEmpty(type)) {
      switch(type.toLowerCase()) {
        case "integer":
          return "long";
        case "decimal":
          return "double";
      }
    }

    return "string";
  }

  private void insertInSchema(SchemaNode schema, List<String> path, final Vocabulary vocabulary) {
    final String head = path.get(0);

    if(path.size() == 1) {
      schema.addChild(new SchemaNode(head, vocabulary));
      return;
    }

    insertInSchema(schema.getChild(head).orElseGet(() -> {
        SchemaNode newChild = new SchemaNode(head);
        schema.addChild(newChild);
        return newChild;
      }),
      path.subList(1, path.size()), vocabulary);
  }

  protected void addTaxonomyFields(XContentBuilder mapping, Taxonomy taxonomy, List<String> ignore) throws IOException {
    if(getTarget() != null) {
      SchemaNode root = new SchemaNode();
      taxonomy.getVocabularies().stream().forEach(v -> {
        String fieldName = v.getAttributeValue(FIELD);
        if(!ignore.contains(fieldName)) {
          fieldName = fieldName != null ? fieldName : v.getName();
          List<String> path = Lists.newArrayList(fieldName.split("\\."));

          if(TRUE.equals(v.getAttributeValue(LOCALIZED)) && "und".equals(path.get(path.size()- 1))) {
            path.remove(path.size() - 1);
          }

          insertInSchema(root, path, v);
        }
      });

      createSchemaMapping(mapping, root);
    }
  }

  protected TaxonomyTarget getTarget() {
    return null;
  }

  protected VocabularyBuilder newVocabularyBuilder() {
    return new VocabularyBuilder();
  }

  private class SchemaNode {
    String name;
    private List<SchemaNode> children = Lists.newArrayList();
    private Vocabulary vocabulary;

    public SchemaNode() {
    }

    public SchemaNode(String name) {
      this.name = name;
    }

    public SchemaNode(String name, Vocabulary vocabulary) {
      this.name = name;
      this.vocabulary = vocabulary;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Optional<SchemaNode> getChild(String name) {
      return  children.stream().filter(c -> c.getName().equals(name)).findFirst();
    }

    public void addChild(SchemaNode node) {
      children.add(node);
    }

    public List<SchemaNode> getChildren() {
      return children;
    }

    public void setChildren(List<SchemaNode> children) {
      this.children = children;
    }

    public Vocabulary getVocabulary() {
      return vocabulary;
    }

    public void setVocabulary(Vocabulary vocabulary) {
      this.vocabulary = vocabulary;
    }
  }


  public class VocabularyBuilder {
    String name;
    Map<String, String> attributes = Maps.newHashMap();

    public VocabularyBuilder name(String name) {
      this.name = name;
      return this;
    }

    public VocabularyBuilder field(String field) {
      attributes.put(FIELD, field);
      return this;
    }

    public VocabularyBuilder staticField() {
      attributes.put(STATIC, TRUE);
      return this;
    }

    public VocabularyBuilder localized() {
      attributes.put(LOCALIZED, TRUE);
      return this;
    }

    public Vocabulary build() {
      Vocabulary v = new Vocabulary();
      v.setName(name);
      v.setAttributes(attributes);
      return v;
    }
  }
}
