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

import org.junit.jupiter.api.Test;
import org.obiba.mica.micaConfig.service.InvalidFormDefinitionException;
import org.obiba.mica.micaConfig.service.InvalidFormSchemaException;
import org.obiba.mica.micaConfig.service.InvalidFormTranslationsException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FormValidationTest {

  @Test
  public void schema_is_an_object() {
    assertDoesNotThrow(() -> FormValidation.validateSchema("{\"type\": \"object\"}"));
    assertThrows(InvalidFormSchemaException.class, () -> FormValidation.validateSchema("[]"));
    assertThrows(InvalidFormSchemaException.class, () -> FormValidation.validateSchema("not json"));
    assertThrows(InvalidFormSchemaException.class, () -> FormValidation.validateSchema(null));
  }

  @Test
  public void uischema_is_an_object_not_an_asf_array() {
    assertDoesNotThrow(() -> FormValidation.validateUischema(" {\"type\": \"VerticalLayout\", \"elements\": []}"));
    assertThrows(InvalidFormDefinitionException.class, () -> FormValidation.validateUischema("[\"*\"]"));
    assertThrows(InvalidFormDefinitionException.class, () -> FormValidation.validateUischema("{not json"));
    assertThrows(InvalidFormDefinitionException.class, () -> FormValidation.validateUischema(null));
  }

  @Test
  public void definition_is_either_dialect() {
    assertDoesNotThrow(() -> FormValidation.validateDefinition("{\"type\": \"VerticalLayout\"}"));
    assertDoesNotThrow(() -> FormValidation.validateDefinition("[\"*\"]"));
    assertThrows(InvalidFormDefinitionException.class, () -> FormValidation.validateDefinition("[not json"));
    assertThrows(InvalidFormDefinitionException.class, () -> FormValidation.validateDefinition("{not json"));
  }

  @Test
  public void translations_are_an_object_when_present() {
    assertDoesNotThrow(() -> FormValidation.validateTranslations(null));
    assertDoesNotThrow(() -> FormValidation.validateTranslations(" "));
    assertDoesNotThrow(() -> FormValidation.validateTranslations("{\"en\": {\"title\": \"Title\"}}"));
    assertThrows(InvalidFormTranslationsException.class, () -> FormValidation.validateTranslations("[]"));
    assertThrows(InvalidFormTranslationsException.class, () -> FormValidation.validateTranslations("not json"));
  }

  @Test
  public void object_detection() {
    assertTrue(FormValidation.isObject("  {}"));
    assertFalse(FormValidation.isObject("[]"));
    assertFalse(FormValidation.isObject(null));
  }
}
