/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security;

import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class PermissionsUtils {

  public static final List<String> READER_ACTIONS = Lists.newArrayList("VIEW");

  public static final List<String> EDITOR_ACTIONS = Lists.newArrayList("ADD", "VIEW", "EDIT");

  public static final List<String> REVIEWER_ACTIONS = Lists.newArrayList("ADD", "VIEW", "EDIT", "DELETE", "PUBLISH");

  public static final List<String> ADMINISTRATOR_ACTIONS = Lists.newArrayList();

  private PermissionsUtils() {}

  public static String asRole(List<String> actions) {
    if(actions == null || actions.isEmpty()) return "ADMINISTRATOR";
    if(actions.containsAll(REVIEWER_ACTIONS)) return "REVIEWER";
    if(actions.containsAll(EDITOR_ACTIONS)) return "EDITOR";
    if(actions.containsAll(READER_ACTIONS)) return "READER";
    throw new NoSuchElementException("Unknown role for the set of actions: " + Joiner.on(",").join(actions));
  }

  public static String getReaderActions() {
    return asActions("READER");
  }

  public static String getEditorActions() {
    return asActions("EDITOR");
  }

  public static String getReviewerActions() {
    return asActions("REVIEWER");
  }

  public static String asActions(String role) {
    switch(role) {
      case "READER":
        return Joiner.on(",").join(READER_ACTIONS);
      case "EDITOR":
        return Joiner.on(",").join(EDITOR_ACTIONS);
      case "REVIEWER":
        return Joiner.on(",").join(REVIEWER_ACTIONS);
      case "ADMINISTRATOR":
        return Joiner.on(",").join(ADMINISTRATOR_ACTIONS);
      default:
        throw new NoSuchElementException("Invalid role: " + role);
    }
  }

}
