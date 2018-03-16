/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.core.service.IdentifiedDocumentService;
import org.obiba.mica.spi.search.Identified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractIdentifiedDocumentService<T extends Identified> extends AbstractDocumentService<T> implements IdentifiedDocumentService<T> {

  private static final Logger log = LoggerFactory.getLogger(AbstractIdentifiedDocumentService.class);

  private Cache<String, T> documentsCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();

  @Override
  @Nullable
  public T findById(String id) {
    log.debug("findById {} {}", getClass(), id);

    if (useCache()) {
      Object principal = SecurityUtils.getSubject().getPrincipal();
      String principalString = "nouser";

      if (principal != null) {
        principalString = principal.toString();
      }

      T result = documentsCache.getIfPresent(principalString + "::" + id);
      if (result != null) return result;
    }

    List<T> results = findByIds(Collections.singletonList(id));
    return results != null && results.size() > 0 ? results.get(0) : null;
  }

  @Override
  public List<T> findByIds(List<String> ids) {
    log.debug("findByIds {} {} ids", getClass(), ids.size());

    Object securityPrincipal = SecurityUtils.getSubject().getPrincipal();
    String securityPrincipalString = "nouser";
    if (securityPrincipal != null) {
      securityPrincipalString = securityPrincipal.toString();
    }

    String principal = useCache() ? securityPrincipalString : "";
    List<T> results = Lists.newArrayList();
    List<String> notCachedIds = Lists.newArrayList();
    if (useCache()) {
      for (String id : ids) {
        T result = documentsCache.getIfPresent(principal + "::" + id);
        if (result == null) notCachedIds.add(id);
        else results.add(result);
      }
    } else {
      notCachedIds = ids;
    }

    if (notCachedIds.isEmpty()) return results;

    String idsAsRqlStringParam = String.join(",", notCachedIds);
    // TODO handle case ids size is greater than MAX_SIZE
    List<T> notCachedResults = executeRqlQuery(String.format("generic(in(id,(%s)),limit(0,%s))", idsAsRqlStringParam, MAX_SIZE));

    if (useCache()) {
      notCachedResults.forEach(result -> documentsCache.put(principal + "::" + result.getId(), result));
    }

    results.addAll(notCachedResults);

    // restore the ids order
    results.sort(Comparator.comparingInt(o -> ids.indexOf(o.getId())));
    return results;
  }


}
