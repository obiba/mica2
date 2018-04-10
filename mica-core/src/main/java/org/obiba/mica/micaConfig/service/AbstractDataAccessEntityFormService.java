package org.obiba.mica.micaConfig.service;

import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import static org.springframework.util.StringUtils.isEmpty;

abstract class AbstractDataAccessEntityFormService<T extends AbstractDataAccessEntityForm> {

  public abstract T createOrUpdate(T dataAccessForm);

  public abstract Optional<T> find();

  abstract String getDataAccessEntityFormResourceLocation();

  private void validateJsonObject(String json) {
    try {
      new JSONObject(json);
    } catch(JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }

  private void validateJsonArray(String json) {
    try {
      new JSONArray(json);
    } catch(JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }

  private Resource getDefaultDataAccessFormResource(String name) {
    return new DefaultResourceLoader().getResource(getDataAccessEntityFormResourceLocation() + name);
  }

  String getDefaultDataAccessFormResourceAsString(String name) {
    try(Scanner s = new Scanner(getDefaultDataAccessFormResource(name).getInputStream())) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    } catch(IOException e) {
      return "";
    }
  }

  void validateForm(AbstractDataAccessEntityForm dataAccessForm) {
    validateJsonObject(dataAccessForm.getSchema());
    validateJsonArray(dataAccessForm.getDefinition());
    if (!isEmpty(dataAccessForm.getCsvExportFormat())) {
      validateJsonObject(dataAccessForm.getCsvExportFormat());
    }
  }
}
