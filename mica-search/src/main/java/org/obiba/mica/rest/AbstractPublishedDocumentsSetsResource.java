/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.rest;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.MaximumDocumentSetCreationExceededException;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.SubjectUtils;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractPublishedDocumentsSetsResource<T extends DocumentSetService> {

  private final MicaConfigService micaConfigService;

  protected final SubjectAclService subjectAclService;

  protected final Dtos dtos;

  protected AbstractPublishedDocumentsSetsResource(
    MicaConfigService micaConfigService,
    SubjectAclService subjectAclService,
    Dtos dtos) {
    this.micaConfigService = micaConfigService;
    this.subjectAclService = subjectAclService;
    this.dtos = dtos;
  }

  protected abstract T getDocumentSetService();

  protected abstract boolean isCartEnabled(MicaConfig config);

  protected List<Mica.DocumentSetDto> listDocumentsSets(List<String> ids, String anonymousUserId) {
    if (isAnonymousUser()) {
      if (isAnonymousCartAllowed()) {
        return getDocumentSetService().getAllAnonymousUser(anonymousUserId).stream().map(dtos::asDto).collect(Collectors.toList());
      }
    } else if (subjectAclService.hasMicaRole()) {
      if (ids.isEmpty())
        return getDocumentSetService().getAllCurrentUser().stream().map(dtos::asDto).collect(Collectors.toList());
      else
        return ids.stream().map(id -> dtos.asDto(getDocumentSetService().get(id))).collect(Collectors.toList());
    }
    throw new AuthorizationException();
  }

  protected Mica.DocumentSetDto createEmptyDocumentSet(String name) {
    ensureUserIsAuthorized(name);
    if (!Strings.isNullOrEmpty(name)) checkSetsNumberLimit();

    DocumentSet created = getDocumentSetService().create(name, Lists.newArrayList());
    return dtos.asDto(created);
  }

  /**
   * A cart is a set without name, associated to a user.
   *
   * @return
   */
  protected Mica.DocumentSetDto getOrCreateDocumentSetCart(HttpServletRequest request) {
    if (isAnonymousUser()) {
      if (isAnonymousCartAllowed()) {
        return dtos.asDto(getDocumentSetService().getCartAnonymousUser(getAnonymousUserId(request)));
      } else
        throw new AuthorizationException();
    } else if (!subjectAclService.hasMicaRole()) {
      throw new AuthorizationException();
    } else {
      ensureUserIsAuthorized("");
      return dtos.asDto(getDocumentSetService().getCartCurrentUser());
    }
  }

  protected Mica.DocumentSetDto importDocuments(String name, String body) {
    ensureUserIsAuthorized(name);
    if (!Strings.isNullOrEmpty(name)) checkSetsNumberLimit();
    DocumentSet created = getDocumentSetService().create(name, getDocumentSetService().extractIdentifiers(body));
    return dtos.asDto(created);
  }

  protected boolean isAnonymousCartAllowed() {
    return micaConfigService.getConfig().isAnonymousCanCreateCart();
  }

  protected boolean isAnonymousUser() {
    return !SecurityUtils.getSubject().isAuthenticated();
  }

  protected String getAnonymousUserId(HttpServletRequest request) {
    return SubjectUtils.getAnonymousUserId(request);
  }

  //
  // Private methods
  //

  private void ensureUserIsAuthorized(String name) {
    boolean enabled = isCartEnabled(micaConfigService.getConfig());
    if (!enabled && Strings.isNullOrEmpty(name)) throw new AuthorizationException(); // cart
    if (enabled && !subjectAclService.hasMicaRole() && Strings.isNullOrEmpty(name))
      throw new AuthorizationException(); // cart
    if (!Strings.isNullOrEmpty(name) && !subjectAclService.hasMicaRole()) throw new AuthorizationException();
  }

  private long numberOfNamedSets() {
    return getDocumentSetService().getAllCurrentUser().stream().filter(DocumentSet::hasName).count();
  }

  private void checkSetsNumberLimit() {
    long maxNumberOfSets = micaConfigService.getConfig().getMaxNumberOfSets();
    if (numberOfNamedSets() >= maxNumberOfSets)
      throw MaximumDocumentSetCreationExceededException.because(maxNumberOfSets, getDocumentSetService().getType());
  }
}
