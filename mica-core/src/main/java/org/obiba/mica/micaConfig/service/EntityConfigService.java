/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.Scanner;

public abstract class EntityConfigService<T extends EntityConfig> {

  private static final String EMPTY_UISCHEMA = "{\"type\":\"VerticalLayout\",\"elements\":[]}";

  protected abstract MongoRepository<T, String> getRepository();

  protected abstract String getDefaultId();

  public void createOrUpdate(T configuration) {
    validateForm(configuration);
    getRepository().save(configuration);
  }

  public Optional<T> findPartial() {
    return Optional.ofNullable(findOrCreateDefaultForm());
  }

  public Optional<T> findComplete() {

    T form = findOrCreateDefaultForm();
    form = mergedRepositoryAndMandatoryConfiguration(form);

    return Optional.ofNullable(form);
  }

  public void publish() {
    Optional<T> networkForm = findPartial();
    networkForm.ifPresent(d -> getRepository().save(d));
  }

  private void validateForm(T configuration) {
    validateSchema(configuration.getSchema());
    validateDefinition(configuration.getDefinition());
    validateTranslations(configuration.getTranslations());
  }

  private T findOrCreateDefaultForm() {

    Optional<T> form = getRepository().findById(getDefaultId());

    if (!form.isPresent()) {
      createOrUpdate(createDefaultForm());
      form = getRepository().findById(getDefaultId());
    }

    return form.get();
  }

  private void validateSchema(String json) {
    try {
      new JSONObject(json);
    } catch(JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }

  /**
   * The definition is either an angular-schema-form array or a JSON Forms UI schema object.
   */
  private void validateDefinition(String json) {
    try {
      if (isUischema(json))
        new JSONObject(json);
      else
        new JSONArray(json);
    } catch(JSONException e) {
      throw new InvalidFormDefinitionException();
    }
  }

  /**
   * The translations, when there are some, are a JSON object (the texts by locale).
   */
  private void validateTranslations(String json) {
    if (json == null || json.isBlank()) return;
    try {
      new JSONObject(json);
    } catch(JSONException e) {
      throw new InvalidFormTranslationsException(e);
    }
  }

  private static boolean isUischema(String json) {
    return json != null && json.trim().startsWith("{");
  }

  private T createDefaultForm() {
    T form = createEmptyForm();
    form.setDefinition(getResourceAsString(getDefaultDefinitionResourcePath(), "[]"));
    form.setSchema(getResourceAsString(getDefaultSchemaResourcePath(), "{}"));
    return form;
  }

  private String getResourceAsString(String path, String defaultValue) {

    if (StringUtils.isEmpty(path))
      return defaultValue;

    Resource resource = new DefaultResourceLoader().getResource(path);
    try (Scanner s = new Scanner(resource.getInputStream())) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    } catch (IOException e) {
      return defaultValue;
    }
  }

  private T mergedRepositoryAndMandatoryConfiguration(T repositoryConfiguration) {

    String repositorySchema = repositoryConfiguration.getSchema();
    String mandatorySchema = getResourceAsString(getMandatorySchemaResourcePath(), "{}");
    String mergedSchema = mergeSchema(repositorySchema, mandatorySchema);
    repositoryConfiguration.setSchema(mergedSchema);

    String repositoryDefinition = repositoryConfiguration.getDefinition();
    String mergedDefinition;
    if (isUischema(repositoryDefinition)) {
      String mandatoryUischema = getResourceAsString(getMandatoryUischemaResourcePath(), EMPTY_UISCHEMA);
      mergedDefinition = mergeUischema(repositoryDefinition, mandatoryUischema);
    } else {
      String mandatoryDefinition = getResourceAsString(getMandatoryDefinitionResourcePath(), "[]");
      mergedDefinition = mergeDefinition(repositoryDefinition, mandatoryDefinition);
    }
    repositoryConfiguration.setDefinition(mergedDefinition);

    return repositoryConfiguration;
  }

  String mergeSchema(String baseNode, String overrideNode) {
    try {
      return mergeSchema(new ObjectMapper().readTree(baseNode), new ObjectMapper().readTree(overrideNode)).toString();
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }

  private JsonNode mergeSchema(JsonNode baseNode, JsonNode overrideNode) {

    if (overrideNode.get("type") == null)
      return baseNode;

    mergeProperties(baseNode, overrideNode);
    mergeRequiredFields(baseNode, overrideNode);

    return baseNode;
  }

  String mergeDefinition(String baseNode, String overrideNode) {
    try {
      return mergeDefinition(new ObjectMapper().readTree(baseNode), new ObjectMapper().readTree(overrideNode)).toString();
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }

  private JsonNode mergeDefinition(JsonNode customDefinition, JsonNode mandatoryDefinition) {

    customDefinition.forEach(((ArrayNode) mandatoryDefinition)::add);

    return mandatoryDefinition;
  }

  /**
   * A JSON Forms custom UI schema is appended to the elements of the mandatory one: the mandatory
   * layout comes first, then the custom layout as a whole.
   */
  String mergeUischema(String customNode, String mandatoryNode) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mergeUischema(mapper.readTree(customNode), mapper.readTree(mandatoryNode)).toString();
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }

  private JsonNode mergeUischema(JsonNode customUischema, JsonNode mandatoryUischema) {
    ObjectNode merged = mandatoryUischema.isObject() ? (ObjectNode) mandatoryUischema : new ObjectMapper().createObjectNode();
    if (!merged.has("type")) merged.put("type", "VerticalLayout");
    JsonNode elements = merged.get("elements");
    if (elements == null || !elements.isArray()) {
      elements = merged.putArray("elements");
    }
    if (customUischema.isObject() && !customUischema.isEmpty()) {
      ((ArrayNode) elements).add(customUischema);
    }
    return merged;
  }

  private void mergeRequiredFields(JsonNode baseNode, JsonNode overrideNode) {
    JsonNode overrideRequired = overrideNode.get("required");
    if (overrideRequired == null || !overrideRequired.isArray()) return;
    // a schema without required fields may omit the array (the form builder does)
    ArrayNode baseRequired = arrayIn(baseNode, "required");
    ArrayList<JsonNode> baseRequiredItems = Lists.newArrayList(baseRequired);
    for (JsonNode overrideRequiredItem : overrideRequired) {
      if (!baseRequiredItems.contains(overrideRequiredItem)) {
        baseRequired.add(overrideRequiredItem);
      }
    }
  }

  private ArrayNode arrayIn(JsonNode node, String name) {
    JsonNode array = node.get(name);
    return array != null && array.isArray() ? (ArrayNode) array : ((ObjectNode) node).putArray(name);
  }

  private ObjectNode objectIn(JsonNode node, String name) {
    JsonNode object = node.get(name);
    return object != null && object.isObject() ? (ObjectNode) object : ((ObjectNode) node).putObject(name);
  }

  private void mergeProperties(JsonNode baseNode, JsonNode overrideNode) {

    JsonNode overrideProperties = overrideNode.get("properties");
    if (overrideProperties == null || !overrideProperties.isObject()) return;
    JsonNode baseProperties = objectIn(baseNode, "properties");
    Iterator<String> overridePropertiesNames = overrideProperties.fieldNames();

    while (overridePropertiesNames.hasNext()) {
      String overridePropertyName = overridePropertiesNames.next();
      JsonNode overridePropertyValue = overrideProperties.get(overridePropertyName);

      writePropertyInNode(baseProperties, overridePropertyName, overridePropertyValue);
    }
  }

  private void writePropertyInNode(JsonNode baseProperties, String overridePropertyName, JsonNode overridePropertyValue) {
    if (baseProperties.get(overridePropertyName) == null)
      ((ObjectNode) baseProperties).set(overridePropertyName, overridePropertyValue);
    else
      ((ObjectNode) baseProperties).replace(overridePropertyName, overridePropertyValue);
  }

  protected abstract T createEmptyForm();

  protected abstract String getDefaultSchemaResourcePath();

  protected abstract String getMandatorySchemaResourcePath();

  protected abstract String getDefaultDefinitionResourcePath();

  protected abstract String getMandatoryDefinitionResourcePath();

  /**
   * The JSON Forms counterpart of the mandatory definition, merged into a custom definition of that
   * dialect: by default the `uischema-mandatory.json` beside the `definition-mandatory.json`.
   */
  protected String getMandatoryUischemaResourcePath() {
    String path = getMandatoryDefinitionResourcePath();
    return StringUtils.isEmpty(path) ? path : path.replace("definition-mandatory.json", "uischema-mandatory.json");
  }
}
