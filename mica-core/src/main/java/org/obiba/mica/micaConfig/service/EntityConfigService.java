/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import java.util.Optional;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.springframework.data.mongodb.repository.MongoRepository;


public abstract class EntityConfigService<T extends EntityConfig> {

  @Inject
  GitService gitService;

  @Inject
  FileStoreService fileStoreService;

  protected abstract MongoRepository<T, String> getRepository();

  protected abstract String getDefaultId();

  public void createOrUpdate(T networkConfig) {
    validateForm(networkConfig);
    networkConfig.incrementRevisionsAhead();
    gitService.save(networkConfig);
    getRepository().save(networkConfig);
  }

  public Optional<T> find() {
    T form = getRepository().findOne(getDefaultId());

    if(form == null) {
      createOrUpdate(createDefaultForm());
    }

    return Optional.ofNullable(form == null ? getRepository().findOne(getDefaultId()) : form);
  }

  public void publish() {
    Optional<T> networkForm = find();
    networkForm.ifPresent(d -> {
      d.setPublishedTag(gitService.tag(d).getFirst());
      d.setRevisionsAhead(0);
      d.setRevisionStatus(RevisionStatus.DRAFT);
      getRepository().save(d);
    });
  }

  private void validateForm(T networkConfig) {
    validateSchema(networkConfig.getSchema());
    validateDefinition(networkConfig.getDefinition());
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

  protected abstract T createDefaultForm();

}
