package org.obiba.mica.web.rest;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.obiba.mica.domain.study.Study;
import org.obiba.mica.repository.StudyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * REST controller for managing Study.
 */
@RestController
@RequestMapping("/app")
public class StudyResource {

  private static final Logger log = LoggerFactory.getLogger(StudyResource.class);

  @Inject
  private StudyRepository studyRepository;

  /**
   * POST  /rest/studies -> Create a new study.
   */
  @RequestMapping(value = "/rest/studies", method = POST, produces = "application/json")
  @Timed
  public void create(@RequestBody Study study) {
    log.debug("REST request to save Study : {}", study);
    studyRepository.save(study);
  }

  /**
   * GET  /rest/studies -> get all the studies.
   */
  @RequestMapping(value = "/rest/studies", method = GET, produces = "application/json")
  @Timed
  public List<Study> getAll() {
    log.debug("REST request to get all Studies");
    return studyRepository.findAll();
  }

  /**
   * GET  /rest/studies/:id -> get the "id" study.
   */
  @RequestMapping(value = "/rest/studies/{id}", method = GET, produces = "application/json")
  @Timed
  public Study get(@PathVariable Long id, HttpServletResponse response) {
    log.debug("REST request to get Study : {}", id);
    Study study = studyRepository.findOne(id);
    if(study == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    return study;
  }

  /**
   * DELETE  /rest/studies/:id -> delete the "id" study.
   */
  @RequestMapping(value = "/rest/studies/{id}", method = DELETE, produces = "application/json")
  @Timed
  public void delete(@PathVariable Long id, HttpServletResponse response) {
    log.debug("REST request to delete Study : {}", id);
    studyRepository.delete(id);
  }
}
