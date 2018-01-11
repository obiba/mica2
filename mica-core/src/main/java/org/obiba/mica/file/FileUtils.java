/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.obiba.mica.file.service.FileSystemService;
import org.springframework.data.domain.Persistable;

import com.google.common.base.CaseFormat;

public class FileUtils {

  private FileUtils() {}

  public static boolean isRoot(String basePath) {
    return "/".equals(basePath);
  }

  public static String normalizePath(String path) {
    String nPath = path.startsWith("/") ? path : String.format("/%s", path);

    if (!isRoot(nPath) && nPath.endsWith("/")) nPath = nPath.replaceAll("[/]+$", "");

    return nPath;
  }

  public static boolean isDirectory(AttachmentState state) {
    return state != null && FileSystemService.DIR_NAME.equals(state.getName());
  }

  public static String getParentPath(String path) {
    int idx = path.lastIndexOf('/');
    return idx == 0 ? "/" : path.substring(0, idx);
  }

  public static String getEntityPath(Persistable persistable) {
    return String.format("/%s/%s",
      CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, persistable.getClass().getSimpleName()),
      persistable.getId()
    );
  }

  public static String normalizeRegex(String path) {
    if(path == null) return null;
    return path.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)");
  }

  /**
   * Encode file path to be Shiro-safe.
   *
   * @param path
   * @return
   */
  public static String encode(String path) {
    if(path == null) return null;
    try {
      return URLEncoder.encode(path, "UTF-8").replaceAll("%2F", "/");
    } catch(UnsupportedEncodingException e) {
      return path;
    }
  }

  public static String decode(String path) {
    if(path == null) return null;
    try {
      return URLDecoder.decode(path, "UTF-8");
    } catch(UnsupportedEncodingException e) {
      return path;
    }
  }
}
