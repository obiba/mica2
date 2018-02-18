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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.SecurityUtils;
import org.joda.time.DateTime;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.InvalidDocumentSetTypeException;
import org.obiba.mica.core.event.DocumentSetDeletedEvent;
import org.obiba.mica.core.event.DocumentSetUpdatedEvent;
import org.obiba.mica.core.repository.DocumentSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class DocumentSetService {

  private static final Logger log = LoggerFactory.getLogger(DocumentSetService.class);

  @Inject
  private DocumentSetRepository documentSetRepository;

  @Inject
  private EventBus eventBus;

  /**
   * Get document with ID and ensure the type is valid.
   *
   * @param id
   * @return
   */
  public DocumentSet get(String id) {
    DocumentSet documentSet = findOne(id);
    ensureType(documentSet);
    return documentSet;
  }

  /**
   * Find document with ID.
   *
   * @param id
   * @return
   */
  public DocumentSet findOne(String id) {
    DocumentSet documentSet = documentSetRepository.findOne(id);
    if (documentSet == null) throw new NoSuchElementException("No '" + getType() + "' set with id: " + id);
    return documentSet;
  }

  /**
   * Get all documents of type.
   *
   * @return
   */
  public List<DocumentSet> getAll() {
    return documentSetRepository.findByType(getType());
  }

  /**
   * Get all documents of type associated with current user.
   *
   * @return
   */
  public List<DocumentSet> getAllCurrentUser() {
    return documentSetRepository.findByTypeAndUsername(getType(), SecurityUtils.getSubject().getPrincipal().toString());
  }

  public abstract String getType();


  public DocumentSet create(String name, List<String> identifiers) {
    DocumentSet documentSet = new DocumentSet();
    if (!Strings.isNullOrEmpty(name)) documentSet.setName(name);
    documentSet.setIdentifiers(identifiers);
    documentSet.setType(getType());
    return save(documentSet, null);
  }

  public void delete(DocumentSet documentSet) {
    ensureType(documentSet);
    if (!documentSet.isNew()) {
      documentSetRepository.delete(documentSet);
      eventBus.post(new DocumentSetDeletedEvent(documentSet));
    }
  }

  public List<String> extractIdentifiers(String importedIdentifiers) {
    if (Strings.isNullOrEmpty(importedIdentifiers)) return Lists.newArrayList();
    return Splitter.on("\n").splitToList(importedIdentifiers).stream()
      .filter(id -> !Strings.isNullOrEmpty(id))
      .collect(Collectors.toList());
  }

  protected List<String> extractIdentifiers(String importedIdentifiers, Predicate<String> predicate) {
    if (Strings.isNullOrEmpty(importedIdentifiers)) return Lists.newArrayList();
    return Splitter.on("\n").splitToList(importedIdentifiers).stream()
      .filter(id -> !Strings.isNullOrEmpty(id))
      .filter(predicate)
      .collect(Collectors.toList());
  }

  public DocumentSet addIdentifiers(String id, List<String> identifiers) {
    DocumentSet documentSet = get(id);
    if (identifiers.isEmpty()) return documentSet;
    documentSet.getIdentifiers().addAll(identifiers);
    return save(documentSet, null);
  }

  public DocumentSet removeIdentifiers(String id, List<String> identifiers) {
    DocumentSet documentSet = get(id);
    if (identifiers.isEmpty()) return documentSet;

    documentSet.getIdentifiers().removeAll(identifiers);
    return save(documentSet, Sets.newLinkedHashSet(identifiers));
  }

  public DocumentSet setIdentifiers(String id, List<String> identifiers) {
    DocumentSet documentSet = get(id);
    Set<String> removedIdentifiers =  Sets.difference(documentSet.getIdentifiers(),Sets.newLinkedHashSet(identifiers));
    documentSet.setIdentifiers(identifiers);
    return save(documentSet, removedIdentifiers);
  }

  public boolean isForType(DocumentSet documentSet) {
    return documentSet != null && getType().equals(documentSet.getType());
  }

  //
  // Private methods
  //

  protected void ensureType(@NotNull DocumentSet documentSet) throws InvalidDocumentSetTypeException {
    if (!getType().equals(documentSet.getType())) throw InvalidDocumentSetTypeException.forSet(documentSet, getType());
  }

  private DocumentSet save(DocumentSet documentSet, Set<String> removedIdentifiers) {
    ensureType(documentSet);
    documentSet.setLastModifiedDate(DateTime.now());
    if (Strings.isNullOrEmpty(documentSet.getUsername())) {
      Object principal = SecurityUtils.getSubject().getPrincipal();
      if (principal != null) {
        documentSet.setUsername(principal.toString());
      }
    }
    documentSet = documentSetRepository.save(documentSet);
    eventBus.post(new DocumentSetUpdatedEvent(documentSet, removedIdentifiers));
    return documentSet;
  }
}
