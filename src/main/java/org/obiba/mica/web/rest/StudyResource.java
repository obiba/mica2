package org.obiba.mica.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.obiba.mica.domain.Study;
import org.obiba.mica.repository.StudyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * REST controller for managing Study.
 */
@RestController
@RequestMapping("/app")
public class StudyResource {

    private final Logger log = LoggerFactory.getLogger(StudyResource.class);

    @Inject
    private StudyRepository studyRepository;

    /**
     * POST  /rest/studys -> Create a new study.
     */
    @RequestMapping(value = "/rest/studys",
            method = RequestMethod.POST,
            produces = "application/json")
    @Timed
    public void create(@RequestBody Study study) {
        log.debug("REST request to save Study : {}", study);
        studyRepository.save(study);
    }

    /**
     * GET  /rest/studys -> get all the studys.
     */
    @RequestMapping(value = "/rest/studys",
            method = RequestMethod.GET,
            produces = "application/json")
    @Timed
    public List<Study> getAll() {
        log.debug("REST request to get all Studys");
        return studyRepository.findAll();
    }

    /**
     * GET  /rest/studys/:id -> get the "id" study.
     */
    @RequestMapping(value = "/rest/studys/{id}",
            method = RequestMethod.GET,
            produces = "application/json")
    @Timed
    public Study get(@PathVariable Long id, HttpServletResponse response) {
        log.debug("REST request to get Study : {}", id);
        Study study = studyRepository.findOne(id);
        if (study == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return study;
    }

    /**
     * DELETE  /rest/studys/:id -> delete the "id" study.
     */
    @RequestMapping(value = "/rest/studys/{id}",
            method = RequestMethod.DELETE,
            produces = "application/json")
    @Timed
    public void delete(@PathVariable Long id, HttpServletResponse response) {
        log.debug("REST request to delete Study : {}", id);
        studyRepository.delete(id);
    }
}
