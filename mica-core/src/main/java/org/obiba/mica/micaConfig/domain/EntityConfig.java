/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.domain;

public abstract class EntityConfig {

  public final static String DEFAULT_ID = "default";

  private String id;

  private String schema;

  private String definition;

  /** the texts of the form by locale, JSON; looked up before the Mica translations */
  private String translations;

  public EntityConfig() {
    setId(DEFAULT_ID);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public String getTranslations() {
    return translations;
  }

  public void setTranslations(String translations) {
    this.translations = translations;
  }

  public boolean hasTranslations() {
    return translations != null && !translations.isBlank();
  }
}
