/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.taxonomies;

import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.support.yaml.TaxonomyYaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public abstract class AbstractTaxonomiesProviderService implements TaxonomiesProviderService {

  protected Properties properties;

  protected boolean running;

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public void configure(Properties properties) {
    this.properties = properties;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public void start() {
    this.running = true;
  }

  @Override
  public void stop() {
    this.running = false;
  }

  /**
   * Read a taxonomy in YAML format from a URL stream.
   *
   * @param uri
   * @return
   * @throws TaxonomyImportException
   */
  protected Taxonomy readTaxonomy(URL uri) throws TaxonomyImportException {
    try (InputStream input = uri.openStream()) {
      return readTaxonomy(input);
    } catch (Exception e) {
      throw new TaxonomyImportException(e);
    }
  }

  /**
   * Read a taxonomy in YAML format from a stream.
   *
   * @param input
   * @return
   * @throws TaxonomyImportException
   */
  protected Taxonomy readTaxonomy(InputStream input) {
    TaxonomyYaml yaml = new TaxonomyYaml();
    return yaml.load(input);
  }
}
