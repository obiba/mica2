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

import com.google.common.collect.Sets;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.JsonContext;
import net.minidev.json.JSONArray;
import org.obiba.mica.core.domain.SchemaFormContentAware;
import org.obiba.mica.file.FileStoreService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;

@Service
public class SchemaFormContentFileService {

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
      Object oldJson = defaultConfiguration().jsonProvider().parse(oldEntity.get().getContent());
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

  public void deleteFiles(SchemaFormContentAware entity) {
    Object json = defaultConfiguration().jsonProvider().parse(entity.getContent());
    DocumentContext context = JsonPath.using(defaultConfiguration().addOptions(Option.AS_PATH_LIST)).parse(json);
    DocumentContext reader =
        new JsonContext(defaultConfiguration().addOptions(Option.REQUIRE_PROPERTIES)).parse(json);

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
        new JsonContext(defaultConfiguration().addOptions(Option.REQUIRE_PROPERTIES)).parse(json);

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
