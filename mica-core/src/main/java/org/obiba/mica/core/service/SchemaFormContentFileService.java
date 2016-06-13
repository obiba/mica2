/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import net.minidev.json.JSONArray;

import org.obiba.mica.core.domain.SchemaFormContentAware;
import org.obiba.mica.file.FileStoreService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.internal.JsonReader;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;

@Service
public class SchemaFormContentFileService {

  @Inject
  private FileStoreService fileStoreService;

  public void save(@NotNull SchemaFormContentAware newEntity, SchemaFormContentAware oldEntity, String entityPath) {
    Assert.notNull(newEntity, "New content cannot be null");

    Object json = defaultConfiguration().jsonProvider().parse(newEntity.getContent());
    DocumentContext newContext = JsonPath.using(defaultConfiguration().addOptions(Option.AS_PATH_LIST)).parse(json);
    Map<String, JSONArray> newPaths = getPathFilesMap(newContext, json);

    if (oldEntity != null) {
      Object oldJson = defaultConfiguration().jsonProvider().parse(oldEntity.getContent());
      DocumentContext oldContext = JsonPath.using(defaultConfiguration().addOptions(Option.AS_PATH_LIST)).parse(oldJson);
      Map<String, JSONArray> oldPaths = getPathFilesMap(oldContext, oldJson);
      saveAndDelete(oldPaths, newPaths, entityPath);
    } else {
      newPaths.values().stream().forEach(v -> saveFiles(v, entityPath));
    }

    newEntity.setContent(newContext.jsonString());
  }

  private void saveAndDelete(Map<String, JSONArray> oldPaths, Map<String, JSONArray> newPaths, String entityPath) {
    newPaths.keySet().forEach(p -> {
      if (oldPaths.containsKey(p)) {
        saveAndDeleteFiles(oldPaths.get(p), newPaths.get(p), entityPath);
      } else {
        newPaths.values().stream().forEach(v -> saveFiles(v, entityPath));
      }
    });
  }

  private Map<String, JSONArray> getPathFilesMap(DocumentContext context, Object json) {
    DocumentContext reader =
        new JsonReader(defaultConfiguration().addOptions(Option.REQUIRE_PROPERTIES)).parse(json);
    JSONArray paths = context.read("$..obibaFiles");

    return paths.stream().collect(Collectors.toMap(Object::toString, p -> (JSONArray) reader.read(p.toString())));
  }

  private Iterable<Object> saveAndDeleteFiles(JSONArray oldFiles, JSONArray newFiles, String entityPath) {
    Iterable<Object> toDelete = Sets.difference(Sets.newHashSet(oldFiles), Sets.newHashSet(newFiles));
    Iterable<Object> toSave = Sets.difference(Sets.newHashSet(newFiles), Sets.newHashSet(oldFiles));

    toDelete.forEach(file -> fileStoreService.delete(((LinkedHashMap)file).get("id").toString()));
    saveFiles(toSave, entityPath);
    return toDelete;
  }

  private void saveFiles(Iterable files, String entityPath) {
    if(files != null) files.forEach(file -> {
      LinkedHashMap map = (LinkedHashMap)file;
      map.put("path", entityPath);
      fileStoreService.save(map.get("id").toString());
    });
  }
}
