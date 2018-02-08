/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.SecurityUtils;
import org.joda.time.DateTime;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.event.DocumentSetDeletedEvent;
import org.obiba.mica.core.event.DocumentSetUpdatedEvent;
import org.obiba.mica.core.repository.DocumentSetRepository;
import org.obiba.mica.security.SubjectUtils;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import java.util.List;

@Service
@Validated
public class DocumentSetService {

  private static final Logger log = LoggerFactory.getLogger(DocumentSetService.class);

  @Inject
  private DocumentSetRepository documentSetRepository;

  @Inject
  private EventBus eventBus;

  public DocumentSet findById(String id) {
    return documentSetRepository.findOne(id);
  }

  public List<DocumentSet> findByType(String type) {
    return documentSetRepository.findByType(type);
  }

  public List<DocumentSet> findByTypeAndContainingIdentifier(String type, String identifier) {
    return documentSetRepository.findByTypeAndIdentifiers(type, identifier);
  }

  public DocumentSet save(DocumentSet documentSet) {
    documentSet.setLastModifiedDate(DateTime.now());
    if (Strings.isNullOrEmpty(documentSet.getUsername())) {
      Object principal = SecurityUtils.getSubject().getPrincipal();
      if (principal != null) {
        documentSet.setUsername(principal.toString());
      }
    }
    documentSet = documentSetRepository.save(documentSet);
    eventBus.post(new DocumentSetUpdatedEvent(documentSet));
    return documentSet;
  }

  public void delete(DocumentSet documentSet) {
    if (!documentSet.isNew()) {
      documentSetRepository.delete(documentSet);
      eventBus.post(new DocumentSetDeletedEvent(documentSet));
    }
  }

  public DocumentSet addIdentifiers(String id, List<String> identifiers) {
    DocumentSet documentSet = findById(id);
    documentSet.getIdentifiers().addAll(identifiers);
    return save(documentSet);
  }

  public DocumentSet removeIdentifiers(String id, List<String> identifiers) {
    DocumentSet documentSet = findById(id);
    documentSet.getIdentifiers().removeAll(identifiers);
    return save(documentSet);
  }

  public DocumentSet setIdentifiers(String id, List<String> identifiers) {
    DocumentSet documentSet = findById(id);
    documentSet.setIdentifiers(identifiers);
    return save(documentSet);
  }

  public List<DocumentSet> findAllCurrentUser() {
    return documentSetRepository.findByUsername(SecurityUtils.getSubject().getPrincipal().toString());
  }
}
