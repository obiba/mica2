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
import org.apache.shiro.authz.annotation.Logical;
import org.obiba.mica.security.Roles;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.Path;

@Path("/draft/persons/external/_search")
@RequiresAuthentication
@RequiresRoles(value = {Roles.MICA_ADMIN, Roles.MICA_EXTERNAL_EDITOR}, logical = Logical.OR)
@Scope("request")
@Component
public class DraftPersonsExternalEditorSearchResource extends AbstractPersonsSearchResource {

  @Override
  protected boolean isDraft() {
    return true;
  }
}
