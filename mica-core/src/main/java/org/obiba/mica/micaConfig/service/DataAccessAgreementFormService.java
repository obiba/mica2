/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import org.joda.time.DateTime;
import org.obiba.mica.micaConfig.domain.DataAccessAgreementForm;
import org.obiba.mica.micaConfig.repository.DataAccessAgreementFormRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class DataAccessAgreementFormService extends AbstractDataAccessEntityFormService<DataAccessAgreementForm> {

  private DataAccessAgreementFormRepository dataAccessAgreementFormRepository;

  @Inject
  public DataAccessAgreementFormService(DataAccessAgreementFormRepository dataAccessAgreementFormRepository) {
    this.dataAccessAgreementFormRepository = dataAccessAgreementFormRepository;
  }

  @Override
  public DataAccessAgreementForm createOrUpdate(DataAccessAgreementForm dataAccessForm) {
    validateForm(dataAccessForm);
    dataAccessForm.setRevision(0);
    dataAccessForm.setLastUpdateDate(DateTime.now());
    return dataAccessAgreementFormRepository.save(dataAccessForm);
  }

  @Override
  public DataAccessAgreementForm findDraft() {
    DataAccessAgreementForm form = dataAccessAgreementFormRepository.findOne(DataAccessAgreementForm.DEFAULT_ID);
    if (form == null) {
      createOrUpdate(createDefaultDataAccessAgreementForm());
      form = dataAccessAgreementFormRepository.findOne(DataAccessAgreementForm.DEFAULT_ID);
    }
    return form;
  }

  @Override
  DataAccessAgreementForm findLatest() {
    Optional<DataAccessAgreementForm> latest = findFirstSortByRevisionDesc();
    if (!latest.isPresent()) {
      publish();
      latest = findFirstSortByRevisionDesc();
    }
    return latest.get();
  }

  @Override
  DataAccessAgreementForm findByRevision(int revision) {
    return dataAccessAgreementFormRepository.findFirstByRevision(revision);
  }

  @Override
  public void publish() {
    DataAccessAgreementForm draft = findDraft();
    draft.setId(null);
    Optional<DataAccessAgreementForm> latest = findFirstSortByRevisionDesc();
    draft.setRevision(latest.isPresent() ? latest.get().getRevision() + 1 : 1);
    dataAccessAgreementFormRepository.save(draft);
  }

  @Override
  String getDataAccessEntityFormResourceLocation() {
    return "classpath:config/data-access-agreement-form/";
  }

  //
  // Private methods
  //

  private Optional<DataAccessAgreementForm> findFirstSortByRevisionDesc() {
    return dataAccessAgreementFormRepository.findAll(new Sort(Sort.Direction.DESC, "revision")).stream()
      .filter(form -> form.getRevision()>0)
      .findFirst();
  }

  private DataAccessAgreementForm createDefaultDataAccessAgreementForm() {
    DataAccessAgreementForm form = new DataAccessAgreementForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setRevision(0);
    return form;
  }
}
