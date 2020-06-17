package org.obiba.mica;

import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.web.model.LocalizedStringDtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.Mica.EntityIndexHealthDto.ItemDto;

import javax.inject.Inject;
import javax.ws.rs.GET;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class EntityIndexHealthResource<T extends AbstractGitPersistable> {

  protected final static List<String> ES_QUERY_FIELDS = Lists.newArrayList("id");

  protected final static int MAX_VALUE = Short.MAX_VALUE;

  @Inject
  protected LocalizedStringDtos localizedStringDtos;

  @GET
  public Mica.EntityIndexHealthDto findRequireIndexing() {
    Collection<T> requireIndexing = findRequireIndexingInternal().values();
    Mica.EntityIndexHealthDto.Builder builder = Mica.EntityIndexHealthDto.newBuilder();
    requireIndexing
      .forEach(entity ->
        builder.addRequireIndexing(ItemDto.newBuilder()
          .setId(entity.getId())
          .addAllTitle(localizedStringDtos.asDto(getEntityTitle(entity)))
        )
      );

    return builder.build();
  }

  protected abstract List<T> findAllPublished();

  protected abstract List<String> findAllIndexedIds();

  protected Map<String, T> findRequireIndexingInternal() {
    List<String> esIds = findAllIndexedIds();
    return findAllPublished()
      .stream()
      .filter(entity -> !esIds.contains(entity.getId()))
      .collect(Collectors.toMap(entity -> entity.getId(), entity -> entity));
  }

  protected abstract LocalizedString getEntityTitle(T entity);

  protected String createEsQuery(Class clazz) {
    return String.format("className:%s", clazz.getSimpleName());
  }

}
