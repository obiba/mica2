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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

class GZipServletResponseWrapper extends HttpServletResponseWrapper {

  private GZipServletOutputStream gzipOutputStream = null;

  private PrintWriter printWriter = null;

  private boolean disableFlushBuffer = false;

  GZipServletResponseWrapper(HttpServletResponse response, GZIPOutputStream gzout) {
    super(response);
    gzipOutputStream = new GZipServletOutputStream(gzout);
  }

  public void close() throws IOException {

    //PrintWriter.close does not throw exceptions. Thus, the call does not need
    //be inside a try-catch block.
    if(printWriter != null) {
      printWriter.close();
    }

    if(gzipOutputStream != null) {
      gzipOutputStream.close();
    }
  }

  /**
   * Flush OutputStream or PrintWriter
   *
   * @throws IOException
   */
  @Override
  public void flushBuffer() throws IOException {

    //PrintWriter.flush() does not throw exception
    if(printWriter != null) {
      printWriter.flush();
    }

    if(gzipOutputStream != null) {
      gzipOutputStream.flush();
    }

    // doing this might leads to response already committed exception
    // when the PageInfo has not yet built but the buffer already flushed
    // Happens in Weblogic when a servlet forward to a JSP page and the forward
    // method trigger a flush before it forwarded to the JSP
    // disableFlushBuffer for that purpose is 'true' by default
    if(!disableFlushBuffer) {
      super.flushBuffer();
    }
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if(printWriter != null) {
      throw new IllegalStateException("PrintWriter obtained already - cannot get OutputStream");
    }

    return gzipOutputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if(printWriter == null) {
      gzipOutputStream = new GZipServletOutputStream(getResponse().getOutputStream());

      printWriter = new PrintWriter(new OutputStreamWriter(gzipOutputStream, getResponse().getCharacterEncoding()),
          true);
    }

    return printWriter;
  }

  @Override
  public void setContentLength(int length) {
    //ignore, since content length of zipped content
    //does not match content length of unzipped content.
  }

  /**
   * Flushes all the streams for this response.
   */
  public void flush() throws IOException {
    if(printWriter != null) {
      printWriter.flush();
    }

    if(gzipOutputStream != null) {
      gzipOutputStream.flush();
    }
  }

  /**
   * Set if the wrapped reponse's buffer flushing should be disabled.
   *
   * @param disableFlushBuffer true if the wrapped reponse's buffer flushing should be disabled
   */
  public void setDisableFlushBuffer(boolean disableFlushBuffer) {
    this.disableFlushBuffer = disableFlushBuffer;
  }
}
