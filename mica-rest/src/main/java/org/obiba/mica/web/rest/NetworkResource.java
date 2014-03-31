package org.obiba.mica.web.rest;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.obiba.mica.domain.Network;
import org.obiba.mica.repository.NetworkRepository;
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
 * REST controller for managing Network.
 */
@RestController
@RequestMapping("/ws/networks")
public class NetworkResource {

  private static final Logger log = LoggerFactory.getLogger(NetworkResource.class);

  @Inject
  private NetworkRepository networkRepository;

  /**
   * POST  /rest/networks -> Create a new network.
   */
  @RequestMapping(method = POST, produces = "application/json")
  @Timed
  public void create(@RequestBody Network network) {
    log.debug("REST request to save Network : {}", network);
    networkRepository.save(network);
  }

  /**
   * GET  /rest/networks -> get all the networks.
   */
  @RequestMapping(method = GET, produces = "application/json")
  @Timed
  public List<Network> getAll() {
    log.debug("REST request to get all Networks");
    return networkRepository.findAll();
  }

  /**
   * GET  /rest/networks/:id -> get the "id" network.
   */
  @RequestMapping(value = "/{id}", method = GET, produces = "application/json")
  @Timed
  public Network get(@PathVariable String id, HttpServletResponse response) {
    log.debug("REST request to get Network : {}", id);
    Network network = networkRepository.findOne(id);
    if(network == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    return network;
  }

  /**
   * DELETE  /rest/networks/:id -> delete the "id" network.
   */
  @RequestMapping(value = "/{id}", method = DELETE, produces = "application/json")
  @Timed
  public void delete(@PathVariable String id, HttpServletResponse response) {
    log.debug("REST request to delete Network : {}", id);
    networkRepository.delete(id);
  }
}
