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
  }

  private T findOrCreateDefaultForm() {

    T form = getRepository().findOne(getDefaultId());

    if (form == null) {
      createOrUpdate(createDefaultForm());
      form = getRepository().findOne(getDefaultId());
    }

    return form;
  }

  private void validateSchema(String json) {
    try {
      new JSONObject(json);
    } catch(JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }

  private void validateDefinition(String json) {
    try {
      new JSONArray(json);
    } catch(JSONException e) {
      throw new InvalidFormDefinitionException();
    }
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
    String mandatoryDefinition = getResourceAsString(getMandatoryDefinitionResourcePath(), "[]");
    String mergedDefinition = mergeDefinition(repositoryDefinition, mandatoryDefinition);
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

  private void mergeRequiredFields(JsonNode baseNode, JsonNode overrideNode) {
    ArrayList<JsonNode> baseRequiredItems = Lists.newArrayList(baseNode.get("required"));
    for (JsonNode overrideRequiredItem : overrideNode.get("required")) {
      if (!baseRequiredItems.contains(overrideRequiredItem)) {
        ((ArrayNode) baseNode.get("required")).add(overrideRequiredItem);
      }
    }
  }

  private void mergeProperties(JsonNode baseNode, JsonNode overrideNode) {

    JsonNode overrideProperties = overrideNode.get("properties");
    JsonNode baseProperties = baseNode.get("properties");
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
}
