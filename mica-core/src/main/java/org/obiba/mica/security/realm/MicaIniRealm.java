/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.mica.security.realm;

import org.apache.shiro.realm.text.IniRealm;

import java.util.LinkedHashMap;
import java.util.Map;

public class MicaIniRealm extends IniRealm {

  private static final String SHIRO2_PREFIX = "$shiro2$";

  public MicaIniRealm() {
    super("classpath:shiro.ini");
  }

  /**
   * Overrides the default user definition processing to handle Shiro 2's Argon2 hash format.
   * <p>
   * The Argon2 crypt format contains commas in the parameters section
   * (e.g. {@code $shiro2$argon2id$v=19$t=1,m=65536,p=4$salt$hash}). The parent implementation
   * splits the entire value by comma to separate the password from roles, which incorrectly
   * truncates the hash at the first comma in the Argon2 parameters.
   * </p>
   * <p>
   * This override wraps {@code $shiro2$} hashed passwords in double quotes before delegating
   * to the parent, leveraging the quote-aware splitting in {@code StringUtils.split()}.
   * </p>
   */
  @Override
  protected void processUserDefinitions(Map<String, String> userDefs) {
    if (userDefs == null || userDefs.isEmpty()) {
      super.processUserDefinitions(userDefs);
      return;
    }
    Map<String, String> fixedDefs = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : userDefs.entrySet()) {
      fixedDefs.put(entry.getKey(), quoteShiro2CryptPassword(entry.getValue()));
    }
    super.processUserDefinitions(fixedDefs);
  }

  /**
   * Wraps a {@code $shiro2$} hashed password in double quotes to protect internal commas
   * from being interpreted as role delimiters.
   * <p>
   * Rather than assuming a fixed number of {@code $}-delimited sections, this locates the
   * password/roles boundary by searching for a comma only after the last {@code $} in the
   * crypt value. This preserves the current Argon2 behavior while avoiding brittle parsing
   * if the {@code $shiro2$} crypt format changes.
   * </p>
   */
  private String quoteShiro2CryptPassword(String value) {
    if (value == null || !value.startsWith(SHIRO2_PREFIX)) {
      return value;
    }
    int lastDollarIdx = value.lastIndexOf('$');
    if (lastDollarIdx == -1 || lastDollarIdx == value.length() - 1) {
      return value;
    }
    int commaIdx = value.indexOf(',', lastDollarIdx + 1);
    if (commaIdx == -1) {
      // No roles after the hash, no quoting needed
      return value;
    }
    return "\"" + value.substring(0, commaIdx) + "\"" + value.substring(commaIdx);
  }

}
