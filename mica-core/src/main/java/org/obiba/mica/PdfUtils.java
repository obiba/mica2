/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Throwables;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

public class PdfUtils {

  private PdfUtils() {
  }

  public static void addImage(byte[] input, OutputStream output, Image image, String placeholder)
    throws IOException, DocumentException {
    try (PdfReaderAutoclosable pdfReader = new PdfReaderAutoclosable(input);
         PdfStamperAutoclosable pdfStamper = new PdfStamperAutoclosable(pdfReader, output)) {
      AcroFields form = pdfStamper.getAcroFields();
      List<AcroFields.FieldPosition> positions = form.getFieldPositions(placeholder);

      positions.forEach(p -> {
        image.scaleToFit(p.position.getWidth(), p.position.getHeight());
        image.setAbsolutePosition(p.position.getLeft() + (p.position.getWidth() - image.getScaledWidth()) / 2,
          p.position.getBottom() + (p.position.getHeight() - image.getScaledHeight()) / 2);
        PdfContentByte cb = pdfStamper.getOverContent(p.page);

        try {
          cb.addImage(image);
        } catch(DocumentException e) {
          throw Throwables.propagate(e);
        }
      });
    }
  }

  public static Set<String> getFieldNames(byte[] input) throws IOException {
    try (PdfReaderAutoclosable pdfReader = new PdfReaderAutoclosable(input)) {
      return pdfReader.getAcroFields().getFields().keySet();
    }
  }

  public static void fillOutForm(byte[] input, OutputStream output, Map<String, Object> values)
    throws IOException, DocumentException {
    try (PdfReaderAutoclosable pdfReader = new PdfReaderAutoclosable(input);
         PdfStamperAutoclosable stamper = new PdfStamperAutoclosable(pdfReader, output)) {
      stamper.setFormFlattening(true);

      AcroFields fields = stamper.getAcroFields();

      values.forEach((k, v) -> {
        if(v instanceof Boolean) setField(fields, k, (Boolean) v);
        else setField(fields, k, v);
      });
    }
  }

  private static void setField(AcroFields fields, String key, Object value) {
    setField(fields, key, value.toString());
  }

  private static void setField(AcroFields fields, String key, Boolean value) {
    String[] states = fields.getAppearanceStates(key);
    if(states.length > 0) setField(fields, key, states.length > 1 ? states[value.booleanValue() ? 1 : 0] : states[0]);
  }

  private static void setField(AcroFields fields, String key, String value) {
    try {
      fields.setField(key, value);
    } catch(DocumentException | IOException e) {
      throw new RuntimeException("Error setting PDF field", e);
    }
  }

  private static class PdfReaderAutoclosable extends PdfReader implements AutoCloseable {
    public PdfReaderAutoclosable(byte[] pdfIn) throws IOException {
      super(pdfIn);
    }
  }

  private static class PdfStamperAutoclosable extends PdfStamper implements AutoCloseable {
    public PdfStamperAutoclosable(PdfReader reader, OutputStream os) throws IOException, DocumentException {
      super(reader, os);
    }
  }
}
