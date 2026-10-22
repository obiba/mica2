/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
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
import org.junit.jupiter.api.Test;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.domain.DataAccessAgreementForm;
import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.domain.DataAccessPreliminaryForm;
import org.obiba.mica.micaConfig.repository.DataAccessAgreementFormRepository;
import org.obiba.mica.micaConfig.repository.DataAccessAmendmentFormRepository;
import org.obiba.mica.micaConfig.repository.DataAccessFeasibilityFormRepository;
import org.obiba.mica.micaConfig.repository.DataAccessFormRepository;
import org.obiba.mica.micaConfig.repository.DataAccessPreliminaryFormRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The five data access form services: the default forms are JSON Forms pairs, and a saved form is validated as such.
 */
public class DataAccessFormServicesTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String UISCHEMA = "{\"type\": \"VerticalLayout\", \"elements\": [{\"type\": \"Control\", \"scope\": \"#/properties/name\"}]}";

  /** a repository that keeps the last saved form as the draft */
  private static <T extends AbstractDataAccessEntityForm, R extends MongoRepository<T, String>> R repository(Class<R> type) {
    R repository = mock(type);
    AtomicReference<T> draft = new AtomicReference<>();
    when(repository.save(any())).thenAnswer(invocation -> {
      T form = invocation.getArgument(0);
      draft.set(form);
      return form;
    });
    when(repository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(draft.get()));
    return repository;
  }

  private DataAccessFormService dataAccessFormService() {
    return new DataAccessFormService(mock(FileStoreService.class), repository(DataAccessFormRepository.class));
  }

  private static void assertJsonFormsPair(AbstractDataAccessEntityForm form) throws Exception {
    JsonNode schema = mapper.readTree(form.getSchema());
    assertTrue(schema.isObject() && schema.has("properties"), form.getSchema());
    JsonNode uischema = mapper.readTree(form.getDefinition());
    assertTrue(uischema.isObject(), form.getDefinition());
    assertEquals("VerticalLayout", uischema.get("type").asText());
    assertTrue(uischema.get("elements").isArray() && uischema.get("elements").size() > 0);
    // the t() tokens are kept for the server to resolve them, from the texts of the form
    assertTrue(form.getSchema().contains("\"t(") || form.getDefinition().contains("t("));
    EntityConfigServiceTest.assertTranslated(form.getClass().getSimpleName(), form);
    assertEquals(0, form.getRevision());
    assertNotNull(form.getLastUpdateDate());
  }

  @Test
  public void default_forms_are_json_forms_pairs() throws Exception {
    assertJsonFormsPair(dataAccessFormService().findDraft());
    assertJsonFormsPair(new DataAccessPreliminaryFormService(repository(DataAccessPreliminaryFormRepository.class)).findDraft());
    assertJsonFormsPair(new DataAccessFeasibilityFormService(repository(DataAccessFeasibilityFormRepository.class)).findDraft());
    assertJsonFormsPair(new DataAccessAmendmentFormService(repository(DataAccessAmendmentFormRepository.class)).findDraft());
    assertJsonFormsPair(new DataAccessAgreementFormService(repository(DataAccessAgreementFormRepository.class)).findDraft());
  }

  @Test
  public void default_main_form_has_its_field_paths() {
    DataAccessForm form = dataAccessFormService().findDraft();
    assertEquals("projectTitle", form.getTitleFieldPath());
    assertEquals("summary", form.getSummaryFieldPath());
    assertEquals("endDate", form.getEndDateFieldPath());
  }

  @Test
  public void a_saved_form_is_a_json_forms_pair_with_optional_texts() {
    DataAccessFormService service = dataAccessFormService();
    DataAccessForm form = new DataAccessForm();
    form.setSchema("{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\", \"title\": \"t(name.title)\"}}}");
    form.setDefinition(UISCHEMA);
    form.setTranslations("{\"en\": {\"name.title\": \"Name\"}}");
    DataAccessForm saved = service.createOrUpdate(form);
    assertEquals(0, saved.getRevision());
    assertEquals("{\"en\": {\"name.title\": \"Name\"}}", saved.getTranslations());

    form.setTranslations(null);
    service.createOrUpdate(form);

    form.setTranslations("not json");
    assertThrows(InvalidFormTranslationsException.class, () -> service.createOrUpdate(form));
  }

  @Test
  public void an_asf_definition_is_rejected() {
    DataAccessPreliminaryFormService service = new DataAccessPreliminaryFormService(repository(DataAccessPreliminaryFormRepository.class));
    DataAccessPreliminaryForm form = new DataAccessPreliminaryForm();
    form.setSchema("{\"type\": \"object\"}");
    form.setDefinition("[\"*\"]");
    assertThrows(InvalidFormDefinitionException.class, () -> service.createOrUpdate(form));

    form.setDefinition(UISCHEMA);
    form.setSchema("[]");
    assertThrows(InvalidFormSchemaException.class, () -> service.createOrUpdate(form));
  }
}
