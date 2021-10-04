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

import com.google.common.collect.Maps;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.micaConfig.PdfDownloadType;

import java.util.Locale;
import java.util.Map;

public class DataAccessForm extends AbstractDataAccessEntityForm {

  private Map<Locale, Attachment> pdfTemplates;

  private PdfDownloadType pdfDownloadType = PdfDownloadType.Template;

  public DataAccessForm() {
    super();
  }

  public Map<Locale, Attachment> getPdfTemplates() {
    return pdfTemplates == null ? pdfTemplates = Maps.newHashMap() : pdfTemplates;
  }

  public void setPdfTemplates(Map<Locale, Attachment> pdfTemplates) {
    this.pdfTemplates = pdfTemplates;
  }

  public PdfDownloadType getPdfDownloadType() {
    return pdfDownloadType;
  }

  public void setPdfDownloadType(PdfDownloadType pdfDownloadType) {
    this.pdfDownloadType = pdfDownloadType;
  }

}
