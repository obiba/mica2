/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.micaConfig.service.InvalidFormDefinitionException;
import org.obiba.mica.micaConfig.service.InvalidFormSchemaException;
import org.obiba.mica.micaConfig.service.InvalidFormTranslationsException;

/**
 * Checks of the JSON documents of a form configuration, shared by the entity and the data access forms.
 */
public final class FormValidation {

  private FormValidation() {}

  /** The schema is a JSON object. */
  public static void validateSchema(String json) {
    if (json == null) throw new InvalidFormSchemaException(new JSONException("The schema is missing."));
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }

  /** The definition is a JSON Forms UI schema: a JSON object. */
  public static void validateUischema(String json) {
    if (!isObject(json)) throw new InvalidFormDefinitionException();
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      throw new InvalidFormDefinitionException();
    }
  }

  /** The definition is either an angular-schema-form array or a JSON Forms UI schema object. */
  public static void validateDefinition(String json) {
    try {
      if (isObject(json))
        new JSONObject(json);
      else
        new JSONArray(json);
    } catch (JSONException e) {
      throw new InvalidFormDefinitionException();
    }
  }

  /** The translations, when there are some, are a JSON object (the texts by locale). */
  public static void validateTranslations(String json) {
    if (json == null || json.isBlank()) return;
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      throw new InvalidFormTranslationsException(e);
    }
  }

  /** true for a JSON Forms UI schema (an object), false for an angular-schema-form definition (an array) or anything else */
  public static boolean isObject(String json) {
    return json != null && json.trim().startsWith("{");
  }
}
