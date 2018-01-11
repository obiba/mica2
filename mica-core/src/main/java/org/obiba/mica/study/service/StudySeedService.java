/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.obiba.mica.study.domain.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.io.Files;

/**
 * Import studies (save and publish) from JSON files found in MICA_HOME/seed/in directory.
 * The {@link org.obiba.mica.study.domain.Study} (or the list of Studies) should be deserializable by Jackson's
 * {@link com.fasterxml.jackson.databind.ObjectMapper}.
 */
@Service
@Validated
public class StudySeedService {

  private static final Logger log = LoggerFactory.getLogger(StudySeedService.class);

  private static final String PATH_SEED = "${MICA_HOME}/seed";

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private StudyPackageImportService studyPackageImportService;

  @Inject
  private ObjectMapper objectMapper;

  private File seedRepository;

  @PostConstruct
  public void init() {
    if(seedRepository == null && !Strings.isNullOrEmpty(System.getProperty("MICA_HOME"))) {
      seedRepository = new File(PATH_SEED.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
    }
  }

  // Every 10s
  @Scheduled(fixedDelay = 10 * 1000)
  public void importSeed() {
    if(seedRepository == null || !seedRepository.exists() || !seedRepository.isDirectory()) return;

    File seedIn = new File(seedRepository, "in");
    if(seedIn.exists() && seedIn.isDirectory()) {
      Arrays.asList(seedIn.listFiles(pathname -> {
        File lock = new File(pathname.getAbsolutePath() + ".lock");
        return !lock.exists();
      })).forEach(this::importSeed);

    }
  }

  //
  // Private methods
  //

  private void importSeed(File seed) {
    String name = seed.getName().toLowerCase();
    if(name.endsWith(".json") && name.startsWith("stud")) {
      importJsonSeed(seed);
    } else if (seed.getName().endsWith(".zip")) {
      importPackageSeed(seed);
    }
  }

  private void importPackageSeed(File zip) {
    File lock = new File(zip.getAbsolutePath() + ".lock");

    try {
      if(lock.exists() || !lock.createNewFile()) return;

      studyPackageImportService.importZip(new FileInputStream(zip), true);
    } catch (IOException e) {
      log.error("Failed importing study package seed: {}", zip.getAbsolutePath(), e);
    } finally {
      if(lock.exists()) {
        File out = new File(seedRepository, "out");
        if(!out.exists()) out.mkdirs();
        try {
          Files.move(zip, new File(out, zip.getName()));
          // release lock only if move succeeded
          lock.delete();
        } catch(IOException e) {
          log.error("Failed moving seed file to: {}", out.getAbsolutePath());
        }
      }
    }
  }

  private void importJsonSeed(File json) {
    File lock = new File(json.getAbsolutePath() + ".lock");

    try {
      if(lock.exists() || !lock.createNewFile()) return;

      if(json.getName().toLowerCase().startsWith("studies")) {
        importStudies(json);
      } else if(json.getName().toLowerCase().startsWith("study")) {
        importStudy(json);
      }

    } catch(IOException e) {
      log.error("Failed importing study seed: {}", json.getAbsolutePath(), e);
    } finally {
      if(lock.exists()) {
        File out = new File(seedRepository, "out");
        if(!out.exists()) out.mkdirs();
        try {
          Files.move(json, new File(out, json.getName()));
          // release lock only if move succeeded
          lock.delete();
        } catch(IOException e) {
          log.error("Failed moving seed file to: {}", out.getAbsolutePath());
        }
      }
    }
  }

  /**
   * Import Studies from a jackson file.
   *
   * @param json
   * @throws java.io.IOException
   */
  private void importStudies(File json) throws IOException {
    log.info("Seeding studies with file: {}", json.getAbsolutePath());
    InputStream inputStream = new FileInputStream(json);
    List<Study> studies = objectMapper.readValue(inputStream, new TypeReference<List<Study>>() {});
    for(Study study : studies) {
      individualStudyService.save(study);
      individualStudyService.publish(study.getId(), true);
    }
  }

  /**
   * Import a Study from a jackson file.
   *
   * @param json
   */
  private void importStudy(File json) throws IOException {
    log.info("Seeding study with file: {}", json.getAbsolutePath());
    InputStream inputStream = new FileInputStream(json);
    Study study = objectMapper.readValue(inputStream, Study.class);
    individualStudyService.save(study);
    individualStudyService.publish(study.getId(), true);
  }

}
