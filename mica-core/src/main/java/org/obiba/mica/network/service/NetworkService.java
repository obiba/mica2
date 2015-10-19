/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.contact.event.PersonUpdatedEvent;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.NetworkStateRepository;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.network.event.NetworkUnpublishedEvent;
import org.obiba.mica.network.event.NetworkUpdatedEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

import static java.util.stream.Collectors.toList;
import static org.obiba.mica.core.domain.RevisionStatus.DELETED;
import static org.obiba.mica.core.domain.RevisionStatus.DRAFT;

@Service
@Validated
public class NetworkService extends AbstractGitPersistableService<NetworkState, Network> {

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private NetworkStateRepository networkStateRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private FileStoreService fileStoreService;

  /**
   * Create or update provided {@link org.obiba.mica.network.domain.Network}.
   *
   * @param network
   */
  public void save(@NotNull Network network) {
    save(network, null);
  }

  @Override
  public void save(@NotNull Network network, String comment) {
    Network saved = network;
    if(network.isNew()) {
      generateId(saved);
    } else {
      saved = networkRepository.findOne(network.getId());
      if (saved != null) {
        BeanUtils.copyProperties(network, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
            "lastModifiedDate");
      } else {
        saved = network;
      }
    }

    if (saved.getLogo() != null && saved.getLogo().isJustUploaded()) {
      fileStoreService.save(saved.getLogo().getId());
      saved.getLogo().setJustUploaded(false);
    }

    NetworkState networkState = findEntityState(network, () -> {
      NetworkState defaultState = new NetworkState();
      defaultState.setName(network.getName());

      return defaultState;
    });

    if(!network.isNew()) ensureGitRepository(networkState);

    networkState.incrementRevisionsAhead();
    networkStateRepository.save(networkState);

    saved.setLastModifiedDate(DateTime.now());

    networkRepository.saveWithReferences(saved);
    eventBus.post(new NetworkUpdatedEvent(saved));

    saved.getAllPersons().forEach(c -> eventBus.post(new PersonUpdatedEvent(c.getPerson())));

    gitService.save(saved, comment);
  }

  /**
   * Find a {@link org.obiba.mica.network.domain.Network} by its ID.
   *
   * @param id
   * @return
   * @throws NoSuchNetworkException
   */
  @NotNull
  public Network findById(@NotNull String id) throws NoSuchNetworkException {
    Network network = networkRepository.findOne(id);

    if(network == null) throw NoSuchNetworkException.withId(id);

    return network;
  }

  /**
   * Find all {@link org.obiba.mica.network.domain.Network}s, optionally related to
   * a {@link org.obiba.mica.study.domain.Study} ID.
   *
   * @param studyId
   * @return
   */
  public List<Network> findAllNetworks(@Nullable String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllNetworks();
    return networkRepository.findByStudyIds(studyId);
  }

  /**
   * Get all {@link org.obiba.mica.network.domain.Network}s.
   *
   * @return
   */
  public List<Network> findAllNetworks() {
    return networkRepository.findAll();
  }

  /**
   * Get all published {@link org.obiba.mica.network.domain.Network}s.
   *
   * @return
   */
  public List<Network> findAllPublishedNetworks() {
    return findPublishedStates().stream() //
      .filter(networkState -> { //
        return gitService.hasGitRepository(networkState) && !Strings.isNullOrEmpty(networkState.getPublishedTag()); //
      }) //
      .map(networkState -> gitService.readFromTag(networkState, networkState.getPublishedTag(), Network.class)) //
      .collect(toList());
  }

  /**
   * Index all {@link org.obiba.mica.network.domain.Network}s.
   */
  public void indexAll() {
    eventBus.post(new IndexNetworksEvent());
  }

  /**
   * Set the publication flag on a {@link org.obiba.mica.network.domain.Network}.
   *
   * @param id
   * @throws NoSuchNetworkException
   */
  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", key = "'network'") })
  public void publish(@NotNull String id) throws NoSuchEntityException {
    publishState(id);
    eventBus.post(new NetworkPublishedEvent(networkRepository.findOne(id)));
  }

  /**
   * Index a specific {@link org.obiba.mica.network.domain.Network} without updating it.
   *
   * @param id
   * @throws NoSuchNetworkException
   */
  public void index(@NotNull String id) throws NoSuchNetworkException {
    NetworkState networkState = findStateById(id);
    Network network = findById(id);

    eventBus.post(new NetworkUpdatedEvent(network));

    if(networkState.isPublished()) eventBus.post(new NetworkPublishedEvent(network));
    else eventBus.post(new NetworkUnpublishedEvent(network));
  }

  /**
   * Delete a {@link org.obiba.mica.network.domain.Network}.
   *
   * @param id
   * @throws NoSuchNetworkException
   */
  public void delete(@NotNull String id) throws NoSuchNetworkException {
    Network network = findById(id);
    networkRepository.deleteWithReferences(network);

    if (network.getLogo() != null) fileStoreService.delete(network.getLogo().getId());

    networkStateRepository.delete(id);
    gitService.deleteGitRepository(network);

    eventBus.post(new NetworkDeletedEvent(network));
  }

  protected String generateId(Network network) {
    ensureAcronym(network);
    String nextId = getNextId(network.getAcronym());
    network.setId(nextId);

    return nextId;
  }

  private String getNextId(LocalizedString suggested) {
    if (suggested == null) return null;
    String prefix = suggested.asString().toLowerCase();
    if (Strings.isNullOrEmpty(prefix)) return null;
    String next = prefix;
    try {
      findById(next);
      for (int i = 1; i<=1000; i++) {
        next = prefix + "-" + i;
        findById(next);
      }
      return null;
    } catch (NoSuchNetworkException e) {
      return next;
    }
  }

  private void ensureAcronym(@NotNull Network network) {
    if (network.getAcronym() == null || network.getAcronym().isEmpty()) {
      network.setAcronym(network.getName().asAcronym());
    }
  }

  @Nullable
  @Override
  public Network unpublish(NetworkState networkState) {
    networkState.resetRevisionsAhead();
    networkState.setPublishedTag(null);

    if(networkState.getRevisionStatus() != DELETED) networkState.setRevisionStatus(DRAFT);

    entityStateRepository.save(networkState);
    Network network = networkRepository.findOne(networkState.getId());

    if(network != null) eventBus.post(new NetworkUnpublishedEvent(network));

    return network;
  }

  @Override
  protected Class<NetworkState> getType() {
    return NetworkState.class;
  }

  @NotNull
  @Override
  public Network findDraft(@NotNull String id) throws NoSuchEntityException {
    return findById(id);
  }
}
