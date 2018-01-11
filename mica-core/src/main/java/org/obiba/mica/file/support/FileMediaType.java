/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.support;

import java.util.HashMap;
import java.util.Map;

public enum FileMediaType {
  AVI("avi"),
  BMP("bmp"),
  BZ2("bz2"),
  CSS("css"),
  DTD("dtd"),
  DOC("doc"),
  DOCX("docx"),
  DOTX("dotx"),
  ES("es"),
  EXE("exe"),
  GIF("gif"),
  GZ("gz"),
  HQX("hqx"),
  HTML("html"),
  JAR("jar"),
  JPG("jpg"),
  JS("js"),
  MIDI("midi"),
  MP3("mp3"),
  MPEG("mpeg"),
  OGG("ogg"),
  PDF("pdf"),
  PL("pl"),
  PNG("png"),
  POTX("potx"),
  PPSX("ppsx"),
  PPT("ppt"),
  PPTX("pptx"),
  PS("ps"),
  QT("qt"),
  RA("ra"),
  RAM("ram"),
  RDF("rdf"),
  RTF("rtf"),
  SGML("sgml"),
  SIT("sit"),
  SLDX("sldx"),
  SVG("svg"),
  SWF("swf"),
  TAR_GZ("tar.gz"),
  TGZ("tgz"),
  TIFF("tiff"),
  TSV("tsv"),
  TXT("txt"),
  WAV("wav"),
  XLAM("xlam"),
  XLS("xls"),
  XLSB("xlsb"),
  XLSX("xlsx"),
  XLTX("xltx"),
  XML("xml"),
  ZIP("zip");

  public String extension(FileMediaType mt) {
    return mt.extension;
  }

  public String type() {
    return type(this);
  }

  public static String type(FileMediaType mt) {
    return type(mt.extension);
  }

  public static String type(String ext) {
    return map.get(ext.replaceAll("/^\\./", ""));
  }

  private String extension;

  FileMediaType(String ext) {
    extension = ext;
  }

  private static Map<String, String> map = new HashMap<>();
  static {
    map.put("avi", "video/avi");
    map.put("bmp", "image/bmp");
    map.put("bz2", "application/x-bzip2");
    map.put("css", "text/css");
    map.put("dtd", "application/xml-dtd");
    map.put("doc", "application/msword");
    map.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    map.put("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
    map.put("es", "application/ecmascript");
    map.put("exe", "application/octet-stream");
    map.put("gif", "image/gif");
    map.put("gz", "application/x-gzip");
    map.put("hqx", "application/mac-binhex40");
    map.put("html", "text/html");
    map.put("jar", "application/java-archive");
    map.put("jpg", "image/jpeg");
    map.put("js", "application/x-javascript");
    map.put("midi", "audio/x-midi");
    map.put("mp3", "audio/mpeg");
    map.put("mpeg", "video/mpeg");
    map.put("ogg", "application/ogg");
    map.put("pdf", "application/pdf");
    map.put("pl", "application/x-perl");
    map.put("png", "image/png");
    map.put("potx", "application/vnd.openxmlformats-officedocument.presentationml.template");
    map.put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
    map.put("ppt", "application/vnd.ms-powerpointtd>");
    map.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    map.put("ps", "application/postscript");
    map.put("qt", "video/quicktime");
    map.put("ra", "audio/x-pn-realaudio");
    map.put("ram", "audio/x-pn-realaudio");
    map.put("rdf", "application/rdf");
    map.put("rtf", "application/rtf");
    map.put("sgml", "text/sgml");
    map.put("sit", "application/x-stuffit");
    map.put("sldx", "application/vnd.openxmlformats-officedocument.presentationml.slide");
    map.put("svg", "image/svg+xml");
    map.put("swf", "application/x-shockwave-flash");
    map.put("tar.gz", "application/x-tar");
    map.put("tgz", "application/x-tar");
    map.put("tiff", "image/tiff");
    map.put("tsv", "text/tab-separated-values");
    map.put("txt", "text/plain");
    map.put("wav", "audio/wav");
    map.put("xlam", "application/vnd.ms-excel.addin.macroEnabled.12");
    map.put("xls", "application/vnd.ms-excel");
    map.put("xlsb", "application/vnd.ms-excel.sheet.binary.macroEnabled.12");
    map.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    map.put("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
    map.put("xml", "application/xml");
    map.put("zip", "application/zip");
  }

}
