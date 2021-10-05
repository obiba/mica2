package org.obiba.mica.micaConfig.service;

import com.google.common.base.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

import static org.springframework.util.StringUtils.isEmpty;

abstract class AbstractDataAccessEntityFormService<T extends AbstractDataAccessEntityForm> {

  /**
   * Create or update the draft revision of the form.
   *
   * @param dataAccessForm
   * @return
   */
  public abstract T createOrUpdate(T dataAccessForm);

  /**
   * Copy the draft and save it with an incremented revision number.
   *
   */
  public abstract void publish();

  /**
   * Get the draft revision of the form.
   *
   * @return
   */
  public abstract Optional<T> findDraft();

  /**
   * Get the form with the max revision number, and greater than 0 (means that it makes the first draft publication if necessary).
   *
   * @return
   */
  abstract T findLatest();

  /**
   * Get the form by revision number.
   *
   * @param revision
   * @return
   */
  abstract T findByRevision(int revision);

  abstract String getDataAccessEntityFormResourceLocation();

  /**
   * Helper method to get the form by revision number or draft (revision=0) or latest (max revision) aliases.
   *
   * @param revision
   * @return
   */
  public Optional<T> findByRevision(String revision) {
    Optional<T> d;
    if (Strings.isNullOrEmpty(revision) || "draft".equals(revision))
      d = findDraft();
    else if ("latest".equals(revision))
      d = Optional.ofNullable(findLatest());
    else
      try {
        d = Optional.ofNullable(findByRevision(Integer.parseInt(revision)));
      } catch (NumberFormatException e) {
        d = Optional.empty();
      }
    return d;
  }

  private void validateJsonObject(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }

  private void validateJsonArray(String json) {
    try {
      new JSONArray(json);
    } catch (JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }

  private Resource getDefaultDataAccessFormResource(String name) {
    return new DefaultResourceLoader().getResource(getDataAccessEntityFormResourceLocation() + name);
  }

  String getDefaultDataAccessFormResourceAsString(String name) {
    try (Scanner s = new Scanner(getDefaultDataAccessFormResource(name).getInputStream())) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    } catch (IOException e) {
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
