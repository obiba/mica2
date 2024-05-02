/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.filter.gzip;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

class GZipServletOutputStream extends ServletOutputStream {
  private final OutputStream stream;

  GZipServletOutputStream(OutputStream output) {
    super();
    stream = output;
  }

  @Override
  public void close() throws IOException {
    stream.close();
  }

  @Override
  public void flush() throws IOException {
    stream.flush();
  }

  @Override
  public void write(byte b[]) throws IOException {
    stream.write(b);
  }

  @Override
  public void write(byte b[], int off, int len) throws IOException {
    stream.write(b, off, len);
  }

  @Override
  public void write(int b) throws IOException {
    stream.write(b);
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {
  }
}
