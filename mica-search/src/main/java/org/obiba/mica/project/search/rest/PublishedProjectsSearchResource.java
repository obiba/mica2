/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.search.rest;

import jakarta.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/projects/_search")
@RequiresAuthentication
@Scope("request")
@Component
public class PublishedProjectsSearchResource extends AbstractProjectsSearchResource {

  @Override
  protected boolean isDraft() {
    return false;
  }
}
