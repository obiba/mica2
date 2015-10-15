package org.obiba.mica.file;


public class FileUtils {

  public static boolean isRoot(String basePath) {
    return "/".equals(basePath);
  }

  public static String normalizePath(String path) {
    String nPath = path.startsWith("/") ? path : String.format("/%s", path);

    if (!isRoot(nPath) && nPath.endsWith("/")) nPath = nPath.replaceAll("[/]+$", "");

    return nPath;
  }
}
