/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.lang.Assert;
import org.obiba.mica.core.domain.DefaultEntityBase;
import org.obiba.mica.dataset.search.AbstractEsStudyService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EsPublishedStudyService extends AbstractEsStudyService<BaseStudy> implements PublishedStudyService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @Override
  public long getIndividualStudyCount() {
    return getCountByRql(String.format("in(className,%s)", Study.class.getSimpleName()));
  }

  @Override
  public long getHarmonizationStudyCount() {
    return getCountByRql(String.format("in(className,%s)", HarmonizationStudy.class.getSimpleName()));
  }

  @Override
  public List<BaseStudy> findAllByClassName(@NotNull String className) {
    Assert.notNull("ClassName query cannot be null");
    Assert.isTrue(Study.class.getSimpleName().equals(className) ||
        HarmonizationStudy.class.getSimpleName().equals(className));

    return executeRqlQuery(String.format("generic(eq(className,%s),limit(0,%s))", className, MAX_SIZE));
  }

  @Override
  protected BaseStudy processHit(Searcher.DocumentResult res) throws IOException {
    return (BaseStudy) objectMapper.readValue(res.getSourceInputStream(), getClass(res.getClassName()));
  }

  @Override
  protected String getIndexName() {
    return Indexer.PUBLISHED_STUDY_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.STUDY_TYPE;
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        List<String> ids = individualStudyService.findPublishedStates().stream().map(DefaultEntityBase::getId)
            .filter(s -> subjectAclService.isAccessible("/individual-study", s)).collect(Collectors.toList());
        ids.addAll(harmonizationStudyService.findPublishedStates().stream().map(DefaultEntityBase::getId)
            .filter(s -> subjectAclService.isAccessible("/harmonization-study", s)).collect(Collectors.toList()));
        return ids;
      }
    };
  }

  private Class getClass(String className) {
    return Study.class.getSimpleName().equals(className) ? Study.class : HarmonizationStudy.class;
  }
}
