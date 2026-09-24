/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.JsonContext;
import net.minidev.json.JSONArray;
import org.obiba.mica.core.domain.SchemaFormContentAware;
import org.obiba.mica.file.FileRuntimeException;
import org.obiba.mica.file.FileStoreService;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class SchemaFormContentFileService {

  private static final Logger log = getLogger(SchemaFormContentFileService.class);

  private static final String MISSING_ENTRY_NAME = "MISSING.txt";

  @Inject
  private FileStoreService fileStoreService;

  public void save(@NotNull SchemaFormContentAware newEntity, Optional<? extends SchemaFormContentAware> oldEntity, String entityPath) {
    Assert.notNull(newEntity, "New content cannot be null");
    if (newEntity.getContent() == null) return;

    Object json = defaultConfiguration().jsonProvider().parse(newEntity.getContent());
    DocumentContext newContext = JsonPath.using(defaultConfiguration().addOptions(Option.AS_PATH_LIST)).parse(json);
    Map<String, JSONArray> newPaths = getPathFilesMap(newContext, json);
    if (newPaths == null) return; // content does not have any file field

    if (oldEntity.isPresent()) {
      String oldContent = Strings.isNullOrEmpty(oldEntity.get().getContent()) ? "{}" : oldEntity.get().getContent();
      Object oldJson = defaultConfiguration().jsonProvider().parse(oldContent);
      DocumentContext oldContext = JsonPath.using(defaultConfiguration().addOptions(Option.AS_PATH_LIST)).parse(oldJson);
      Map<String, JSONArray> oldPaths = getPathFilesMap(oldContext, oldJson);
      if (oldPaths != null) {
        saveAndDelete(oldPaths, newPaths, entityPath);
      } else {
        // schema and definition now have files
        newPaths.values().forEach(v -> saveFiles(v, entityPath));
      }

    } else {
      newPaths.values().forEach(v -> saveFiles(v, entityPath));
    }

    cleanup(newPaths, newContext);
    newEntity.setContent(newContext.jsonString());
  }

  /**
   * Collects the ZIP entry name to file ID for each obibaFiles field of the entity's content, without
   * opening any file. Names are de-duplicated by inserting the file ID before the extension.
   *
   * @param entity
   * @return entry name -> file ID, in document order
   */
  public Map<String, String> getFileEntries(@NotNull SchemaFormContentAware entity) {
    Map<String, String> entries = new LinkedHashMap<>();

    String content = entity.getContent();
    if (Strings.isNullOrEmpty(content)) return entries;

    Object json = defaultConfiguration().jsonProvider().parse(content);
    DocumentContext context = JsonPath.using(defaultConfiguration().addOptions(Option.AS_PATH_LIST)).parse(json);
    Map<String, JSONArray> paths = getPathFilesMap(context, json);
    if (paths == null) return entries;

    paths.values().stream()
      .flatMap(Collection::stream)
      .forEach(o -> {
        LinkedHashMap<String, Object> fileMap = (LinkedHashMap<String, Object>) o;
        Object fileId = fileMap.get("id");
        if (fileId == null) return;

        String id = fileId.toString();
        Object fileName = fileMap.get("fileName");
        String name = sanitizeEntryName(fileName == null ? null : fileName.toString(), id);
        if (entries.containsKey(name)) {
          // In case of duplicates, insert the file ID before the extension
          int dot = name.lastIndexOf('.');
          name = dot > 0 ? name.substring(0, dot) + "_" + id + name.substring(dot) : name + "_" + id;
        }
        entries.put(name, id);
      });

    return entries;
  }

  /**
   * Strips any directory part and control characters from a file name, so it is safe to use as a ZIP
   * entry name. Falls back to {@code fallback} when the result would be empty, "." or "..".
   *
   * @param name
   * @param fallback
   * @return a safe, non-empty entry name
   */
  private static String sanitizeEntryName(String name, String fallback) {
    if (Strings.isNullOrEmpty(name)) return fallback;
    String base = name.replace('\\', '/');
    base = base.substring(base.lastIndexOf('/') + 1);
    base = base.replaceAll("\\p{Cntrl}", "").trim();
    return base.isEmpty() || base.equals(".") || base.equals("..") ? fallback : base;
  }

  /**
   * Streams a ZIP archive of the given file entries (see {@link #getFileEntries(SchemaFormContentAware)}).
   * Each file is opened only when its entry is written; a file that fails to open is skipped and logged,
   * the rest of the archive is still produced and the skipped files are listed in a {@value #MISSING_ENTRY_NAME}
   * entry.
   *
   * @param entries entry name -> file ID
   * @param output
   */
  public void writeZip(@NotNull Map<String, String> entries, OutputStream output) throws IOException {
    List<String> missing = new ArrayList<>();

    try (ZipOutputStream zos = new ZipOutputStream(output)) {
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        String name = entry.getKey();
        String fileId = entry.getValue();
        try (InputStream is = fileStoreService.getFile(fileId)) {
          zos.putNextEntry(new ZipEntry(name));
          is.transferTo(zos);
          zos.closeEntry();
        } catch (FileRuntimeException e) {
          log.warn("Failed to retrieve file {}: {}", fileId, e.getMessage());
          missing.add(name);
        }
      }

      if (!missing.isEmpty()) {
        String missingName = MISSING_ENTRY_NAME;
        while (entries.containsKey(missingName)) missingName = "_" + missingName;
        zos.putNextEntry(new ZipEntry(missingName));
        zos.write(("The following files could not be retrieved:\n" + String.join("\n", missing) + "\n")
          .getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
      }
    }
  }

  public void deleteFiles(SchemaFormContentAware entity) {
    String content = Strings.isNullOrEmpty(entity.getContent()) ? "{}" : entity.getContent();
    Object json = defaultConfiguration().jsonProvider().parse(content);
    DocumentContext context = JsonPath.using(defaultConfiguration().addOptions(Option.AS_PATH_LIST)).parse(json);
    DocumentContext reader =
      JsonPath.using(defaultConfiguration().addOptions(Option.REQUIRE_PROPERTIES)).parse(json);

    try {
      ((JSONArray)context.read("$..obibaFiles")).stream()
          .map(p -> (JSONArray) reader.read(p.toString()))
          .flatMap(Collection::stream)
          .forEach(file -> fileStoreService.delete(((LinkedHashMap)file).get("id").toString()));
    } catch(PathNotFoundException e) {
    }
  }

  /**
   * Removes the fields with empty obibaFiles from content.
   *
   * @param newPaths
   * @param newContext
   */
  private void cleanup(Map<String, JSONArray> newPaths, DocumentContext newContext) {
    newPaths.keySet().forEach(p -> {
      if (newPaths.get(p).isEmpty()) {
        newContext.delete(p.replace("['obibaFiles']", ""));
      }
    });
  }

  private void saveAndDelete(Map<String, JSONArray> oldPaths, Map<String, JSONArray> newPaths, String entityPath) {
    newPaths.keySet().forEach(p -> {
      if (oldPaths.containsKey(p)) {
        saveAndDeleteFiles(oldPaths.get(p), newPaths.get(p), entityPath);
      } else {
        saveFiles(newPaths.get(p), entityPath);
      }
    });
  }

  private Map<String, JSONArray> getPathFilesMap(DocumentContext context, Object json) {
    DocumentContext reader =
      JsonPath.using(defaultConfiguration().addOptions(Option.REQUIRE_PROPERTIES)).parse(json);

    JSONArray paths = null;
    try {
      paths = context.read("$..obibaFiles");
    } catch(PathNotFoundException e) {
      return null;
    }

    return paths.stream().collect(Collectors.toMap(Object::toString, p -> (JSONArray) reader.read(p.toString())));
  }

  private Iterable<Object> saveAndDeleteFiles(JSONArray oldFiles, JSONArray newFiles, String entityPath) {
    cleanFileJsonArrays(oldFiles, newFiles);
    Iterable<Object> toDelete = Sets.difference(Sets.newHashSet(oldFiles), Sets.newHashSet(newFiles));
    Iterable<Object> toSave = Sets.difference(Sets.newHashSet(newFiles), Sets.newHashSet(oldFiles));

    toDelete.forEach(file -> fileStoreService.delete(((LinkedHashMap)file).get("id").toString()));
    saveFiles(toSave, entityPath);
    return toDelete;
  }

  private void cleanFileJsonArrays(JSONArray... arrays) {
    if (arrays != null) {
      Arrays.stream(arrays).forEach(s -> s.forEach(a -> {
        if (a instanceof LinkedHashMap) {
          LinkedHashMap<String, String> jsonMap = (LinkedHashMap<String, String>) a;
          jsonMap.keySet().stream().filter(k -> k.contains("$")).collect(Collectors.toList()).forEach(jsonMap::remove);
        }
      }));
    }
  }

  private void saveFiles(Iterable files, String entityPath) {
    if(files != null) files.forEach(file -> {
      LinkedHashMap map = (LinkedHashMap)file;
      map.put("path", entityPath);
      fileStoreService.save(map.get("id").toString());
    });
  }
}
