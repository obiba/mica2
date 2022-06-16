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
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.InvalidDocumentSetTypeException;
import org.obiba.mica.core.event.DocumentSetDeletedEvent;
import org.obiba.mica.core.event.DocumentSetUpdatedEvent;
import org.obiba.mica.core.repository.DocumentSetRepository;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DocumentSetService {

  private static final Logger log = LoggerFactory.getLogger(DocumentSetService.class);

  @Inject
  private DocumentSetRepository documentSetRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private MicaConfigService micaConfigService;

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
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated())
      return documentSetRepository.findByTypeAndUsername(getType(), subject.getPrincipal().toString());
    return Lists.newArrayList();
  }

  /**
   * Get or create a set without name.
   *
   * @return
   */
  public DocumentSet getCartCurrentUser() {
    Optional<DocumentSet> cartOpt = getAllCurrentUser().stream().filter(set -> !set.hasName() && set.getType().equals(getType())).findFirst();
    return cartOpt.orElseGet(() -> create("", Lists.newArrayList()));
  }

  public List<DocumentSet> getAllAnonymousUser(String userId) {
    return documentSetRepository.findByTypeAndUsername(getType(), userId);
  }

  public DocumentSet getCartAnonymousUser(String userId) {
    Optional<DocumentSet> cartOpt = getAllAnonymousUser(userId).stream().filter(set -> !set.hasName() && set.getType().equals(getType())).findFirst();
    return cartOpt.orElseGet(() -> create("", Lists.newArrayList(), userId));
  }

  /**
   * Get document type.
   *
   * @return
   */
  public abstract String getType();

  /**
   * Create a set of documents associated to the current user.
   *
   * @param name
   * @param identifiers
   * @return
   */
  public DocumentSet create(@Nullable String name, List<String> identifiers) {
    DocumentSet documentSet = new DocumentSet();
    if (!Strings.isNullOrEmpty(name)) documentSet.setName(name);
    documentSet.setIdentifiers(identifiers);
    documentSet.setType(getType());
    return save(documentSet, null);
  }

  public DocumentSet create(@Nullable String name, List<String> identifiers, String userId) {
    DocumentSet documentSet = create(name, identifiers);
    if (Strings.isNullOrEmpty(documentSet.getUsername())) {
      documentSet.setUsername(userId);
      documentSet = save(documentSet, null);
    }
    return documentSet;
  }

  /**
   * Delete definitely a document set.
   *
   * @param documentSet
   */
  public void delete(DocumentSet documentSet) {
    ensureType(documentSet);
    if (!documentSet.isNew()) {
      documentSetRepository.delete(documentSet);
      eventBus.post(new DocumentSetDeletedEvent(documentSet));
    }
  }

  /**
   * Extract a list of identifiers separated by new lines.
   *
   * @param importedIdentifiers
   * @return
   */
  public List<String> extractIdentifiers(String importedIdentifiers) {
    if (Strings.isNullOrEmpty(importedIdentifiers)) return Lists.newArrayList();
    return Splitter.on("\n").splitToList(importedIdentifiers).stream()
      .filter(id -> !Strings.isNullOrEmpty(id))
      .collect(Collectors.toList());
  }

  /**
   * Add identifiers to a set.
   *
   * @param id
   * @param identifiers
   * @return
   */
  public DocumentSet addIdentifiers(String id, List<String> identifiers) {
    DocumentSet documentSet = get(id);
    if (identifiers.isEmpty()) return documentSet;

    List<String> concatenatedIdentifiers = Stream.concat(documentSet.getIdentifiers().stream(), identifiers.stream())
      .distinct()
      .limit(micaConfigService.getConfig().getMaxItemsPerSet())
      .collect(Collectors.toList());

    documentSet.setIdentifiers(concatenatedIdentifiers);
    return save(documentSet, null);
  }

  /**
   * Remove the given identifiers from the document set and notify about the removal.
   *
   * @param id
   * @param identifiers
   * @return
   */
  public DocumentSet removeIdentifiers(String id, List<String> identifiers) {
    DocumentSet documentSet = get(id);
    if (identifiers.isEmpty()) return documentSet;

    documentSet.getIdentifiers().removeAll(identifiers);
    return save(documentSet, Sets.newLinkedHashSet(identifiers));
  }

  /**
   * Set the new list of identifiers to a document set and notifies that some of them have been removed (if any).
   *
   * @param id
   * @param identifiers
   * @return
   */
  public DocumentSet setIdentifiers(String id, List<String> identifiers) {
    DocumentSet documentSet = get(id);
    Set<String> removedIdentifiers = Sets.difference(documentSet.getIdentifiers(), Sets.newLinkedHashSet(identifiers));
    documentSet.setIdentifiers(identifiers);
    return save(documentSet, removedIdentifiers);
  }

  /**
   * Verifies that a document set applies to the current service.
   *
   * @param documentSet
   * @return
   */
  public boolean isForType(DocumentSet documentSet) {
    return documentSet != null && getType().equals(documentSet.getType());
  }

  public void touch(DocumentSet documentSet) {
    saveInternal(documentSet);
  }

  /**
   * Lock/unlock the document set.
   *
   * @param documentSet
   * @param locked
   */
  public void setLock(DocumentSet documentSet, boolean locked) {
    documentSet.setLocked(locked);
    saveInternal(documentSet);
  }

  @Async
  @Subscribe
  public void datasetUnpublished(DatasetUnpublishedEvent event) {
    // FIXME ignore for now as the dataset could be republished?
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    // TODO find sets containing documents
    String datasetId = event.getPersistable().getId();
    List<DocumentSet> sets = documentSetRepository.findByIdentifiers("^" + datasetId + ":");
    // query fails: bug in spring data?
  }

  @Async
  @Scheduled(cron = "${sets.cleanup.cron:0 0 * * * ?}")
  public void cleanupOldSets() {
    MicaConfig config = micaConfigService.getConfig();
    documentSetRepository.findAll().stream()
      .filter(set -> getType().equals(set.getType()))
      .forEach(set -> {
        int timeToLive = set.hasName() ? config.getSetTimeToLive() : config.getCartTimeToLive();
        DateTime deadline = DateTime.now().minusDays(timeToLive);
        if (set.getLastModifiedDate().isBefore(deadline) && !set.isLocked()) {
          log.debug("Last updated {} - expiration {}", set.getLastModifiedDate(), deadline);
          log.info("{} {} has expired, deleting...", (set.hasName() ? "Set" : "Cart"), set.getId());
          delete(set);
        }
      });
  }

  protected List<String> extractIdentifiers(String importedIdentifiers, Predicate<String> predicate) {
    if (Strings.isNullOrEmpty(importedIdentifiers)) return Lists.newArrayList();
    return Splitter.on("\n").splitToList(importedIdentifiers).stream()
      .filter(id -> !Strings.isNullOrEmpty(id))
      .filter(predicate)
      .collect(Collectors.toList());
  }

  protected void ensureType(@NotNull DocumentSet documentSet) throws InvalidDocumentSetTypeException {
    if (!getType().equals(documentSet.getType())) throw InvalidDocumentSetTypeException.forSet(documentSet, getType());
  }

  private DocumentSet save(DocumentSet documentSet, Set<String> removedIdentifiers) {
    DocumentSet saved = saveInternal(documentSet);
    eventBus.post(new DocumentSetUpdatedEvent(saved, removedIdentifiers));
    return saved;
  }

  private DocumentSet saveInternal(DocumentSet documentSet) {
    ensureType(documentSet);
    documentSet.setLastModifiedDate(DateTime.now());
    if (Strings.isNullOrEmpty(documentSet.getUsername())) {
      Object principal = SecurityUtils.getSubject().getPrincipal();
      if (principal != null) {
        documentSet.setUsername(principal.toString());
      }
    }
    documentSet = documentSetRepository.save(documentSet);
    return documentSet;
  }

}
