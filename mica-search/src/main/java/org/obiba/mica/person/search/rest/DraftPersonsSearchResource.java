/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.search.rest;

import jakarta.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/draft/persons/_search")
@RequiresAuthentication
@RequiresPermissions({ "/draft/individual-study:VIEW", "/draft/harmonization-study:VIEW", "/draft/network:VIEW" })
@Scope("request")
@Component
public class DraftPersonsSearchResource extends AbstractPersonsSearchResource {

  @Override
  protected boolean isDraft() {
    return true;
  }
}
