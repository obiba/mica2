/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config;

import org.obiba.mica.core.support.HtmlDecodeMethod;
import org.springframework.boot.freemarker.autoconfigure.FreeMarkerVariablesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared variables available in all templates, built-in and custom ones (MICA_HOME/conf/templates).
 * Not named FreeMarkerConfiguration: its bean name would clash with the auto-configured freeMarkerConfiguration bean.
 */
@Configuration
public class FreeMarkerVariablesConfiguration {

  @Bean
  public FreeMarkerVariablesCustomizer htmlDecodeVariable() {
    return variables -> variables.put(HtmlDecodeMethod.NAME, new HtmlDecodeMethod());
  }
}
