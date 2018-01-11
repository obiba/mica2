/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import org.obiba.opal.core.domain.taxonomy.Vocabulary;

public class VocabularyDuplicateAliasException extends AbstractVocabularyException {

  private static final long serialVersionUID = 1184806332897637307L;

  public VocabularyDuplicateAliasException() {
    super();
  }

  public VocabularyDuplicateAliasException(Vocabulary v) {
    super("Duplicate vocabulary alias.", v);
  }
}
