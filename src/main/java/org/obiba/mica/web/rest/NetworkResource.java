package org.obiba.mica.web.rest;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.obiba.mica.jpa.domain.Network;
import org.obiba.mica.jpa.repository.NetworkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Network.
 */
@RestController
@RequestMapping("/app")
public class NetworkResource {

  private static final Logger log = LoggerFactory.getLogger(NetworkResource.class);

  @Inject
  private NetworkRepository networkRepository;

  /**
   * POST  /rest/networks -> Create a new network.
   */
  @RequestMapping(value = "/rest/networks",
      method = RequestMethod.POST,
      produces = "application/json")
  @Timed
  public void create(@RequestBody Network network) {
    log.debug("REST request to save Network : {}", network);
    networkRepository.save(network);
  }

  /**
   * GET  /rest/networks -> get all the networks.
   */
  @RequestMapping(value = "/rest/networks",
      method = RequestMethod.GET,
      produces = "application/json")
  @Timed
  public List<Network> getAll() {
    log.debug("REST request to get all Networks");
    return networkRepository.findAll();
  }

  /**
   * GET  /rest/networks/:id -> get the "id" network.
   */
  @RequestMapping(value = "/rest/networks/{id}",
      method = RequestMethod.GET,
      produces = "application/json")
  @Timed
  public Network get(@PathVariable Long id, HttpServletResponse response) {
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
  @RequestMapping(value = "/rest/networks/{id}",
      method = RequestMethod.DELETE,
      produces = "application/json")
  @Timed
  public void delete(@PathVariable Long id, HttpServletResponse response) {
    log.debug("REST request to delete Network : {}", id);
    networkRepository.delete(id);
  }
}
