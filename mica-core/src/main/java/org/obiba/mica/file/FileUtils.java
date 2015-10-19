package org.obiba.mica.file;

import org.obiba.mica.file.service.FileSystemService;

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

}
