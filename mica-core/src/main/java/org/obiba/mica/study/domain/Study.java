/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.domain;

import com.google.common.base.MoreObjects;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection Study: a study that describes the context (research objectives, populations and more) of collected data.
 */
@Document
public class Study extends BaseStudy {

  private static final long serialVersionUID = 6559914069652243954L;

  @Override
  protected MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", getName());
  }

  @Override
  public String pathPrefix() {
    return "collectionStudies";
  }

  @Override
  public Map<String, Serializable> parts() {
    Study self = this;

    return new HashMap<String, Serializable>() {{
      put(self.getClass().getSimpleName(), self);
    }};
  }
}
