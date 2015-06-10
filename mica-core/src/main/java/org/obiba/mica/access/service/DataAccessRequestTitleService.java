/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;

@Component
public class DataAccessRequestTitleService {
  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestTitleService.class);

  private static final Configuration conf = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);

  @Inject
  private DataAccessFormService dataAccessFormService;

  public String getRequestTitle(DataAccessRequest request) {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    String titleFieldPath = dataAccessForm.getTitleFieldPath();
    String rawContent = request.getContent();
    if (!Strings.isNullOrEmpty(titleFieldPath) && !Strings.isNullOrEmpty(rawContent)) {
      Object content = Configuration.defaultConfiguration().jsonProvider().parse(rawContent);
      List<Object> values = null;
      try {
        values = JsonPath.using(conf).parse(content).read(titleFieldPath);
      } catch(PathNotFoundException ex) {
        //ignore
      } catch(InvalidPathException e) {
        log.warn("Invalid jsonpath {}", titleFieldPath);
      }

      if (values != null) {
        return values.get(0).toString();
      }
    }

    return null;
  }
}
