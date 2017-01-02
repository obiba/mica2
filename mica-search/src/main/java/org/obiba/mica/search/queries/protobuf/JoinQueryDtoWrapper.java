/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries.protobuf;

import org.obiba.mica.search.queries.JoinQueryWrapper;
import org.obiba.mica.search.queries.QueryWrapper;
import org.obiba.mica.web.model.MicaSearch;

/**
 *
 */
public class JoinQueryDtoWrapper implements JoinQueryWrapper {

  private final MicaSearch.JoinQueryDto joinQueryDto;

  public JoinQueryDtoWrapper(MicaSearch.JoinQueryDto joinQueryDto) {this.joinQueryDto = joinQueryDto;}

  @Override
  public boolean isWithFacets() {
    return joinQueryDto.getWithFacets();
  }

  @Override
  public String getLocale() {
    return joinQueryDto.hasLocale() ? joinQueryDto.getLocale() : DEFAULT_LOCALE;
  }

  @Override
  public QueryWrapper getVariableQueryWrapper() {
    return new QueryDtoWrapper(joinQueryDto.getVariableQueryDto());
  }

  @Override
  public QueryWrapper getDatasetQueryWrapper() {
    return new QueryDtoWrapper(joinQueryDto.getDatasetQueryDto());
  }

  @Override
  public QueryWrapper getStudyQueryWrapper() {
    return new QueryDtoWrapper(joinQueryDto.getStudyQueryDto());
  }

  @Override
  public QueryWrapper getNetworkQueryWrapper() {
    return new QueryDtoWrapper(joinQueryDto.getNetworkQueryDto());
  }
}
