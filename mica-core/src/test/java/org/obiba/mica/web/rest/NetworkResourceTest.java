package org.obiba.mica.web.rest;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.obiba.mica.Application;
import org.obiba.mica.domain.Network;
import org.obiba.mica.repository.NetworkRepository;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the NetworkResource REST controller.
 *
 * @see NetworkResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class })
@ActiveProfiles("dev")
public class NetworkResourceTest {

  private static final String DEFAULT_ID = "1";

  private static final LocalDate DEFAULT_SAMPLE_DATE_ATTR = new LocalDate(0L);

  private static final LocalDate UPD_SAMPLE_DATE_ATTR = new LocalDate();

  private static final String DEFAULT_SAMPLE_TEXT_ATTR = "sampleTextAttribute";

  private static final String UPD_SAMPLE_TEXT_ATTR = "sampleTextAttributeUpt";

  @Inject
  private NetworkRepository networkRepository;

  private MockMvc restNetworkMockMvc;

  private Network network;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    NetworkResource networkResource = new NetworkResource();
    ReflectionTestUtils.setField(networkResource, "networkRepository", networkRepository);

    restNetworkMockMvc = MockMvcBuilders.standaloneSetup(networkResource).build();

    network = new Network();
    network.setId(DEFAULT_ID);
//    network.setSampleDateAttribute(DEFAULT_SAMPLE_DATE_ATTR);
//    network.setSampleTextAttribute(DEFAULT_SAMPLE_TEXT_ATTR);
  }

  @Test
  public void testCRUDNetwork() throws Exception {

    // Create Network
    restNetworkMockMvc.perform(post("/app/rest/networks").contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(network))).andExpect(status().isOk());

    // Read Network
    restNetworkMockMvc.perform(get("/app/rest/networks/{id}", DEFAULT_ID)) //
        .andExpect(status().isOk()) //
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
        .andExpect(jsonPath("$.id").value(DEFAULT_ID)) //
        .andExpect(jsonPath("$.sampleDateAttribute").value(DEFAULT_SAMPLE_DATE_ATTR.toString())) //
        .andExpect(jsonPath("$.sampleTextAttribute").value(DEFAULT_SAMPLE_TEXT_ATTR));

    // Update Network
//    network.setSampleDateAttribute(UPD_SAMPLE_DATE_ATTR);
//    network.setSampleTextAttribute(UPD_SAMPLE_TEXT_ATTR);

    restNetworkMockMvc.perform(post("/app/rest/networks").contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(network))).andExpect(status().isOk());

    // Read updated Network
    restNetworkMockMvc.perform(get("/app/rest/networks/{id}", DEFAULT_ID)) //
        .andExpect(status().isOk()) //
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
        .andExpect(jsonPath("$.id").value(DEFAULT_ID)) //
        .andExpect(jsonPath("$.sampleDateAttribute").value(UPD_SAMPLE_DATE_ATTR.toString())) //
        .andExpect(jsonPath("$.sampleTextAttribute").value(UPD_SAMPLE_TEXT_ATTR));

    // Delete Network
    restNetworkMockMvc.perform(delete("/app/rest/networks/{id}", DEFAULT_ID).accept(TestUtil.APPLICATION_JSON_UTF8))
        .andExpect(status().isOk());

    // Read nonexisting Network
    restNetworkMockMvc.perform(get("/app/rest/networks/{id}", DEFAULT_ID).accept(TestUtil.APPLICATION_JSON_UTF8))
        .andExpect(status().isNotFound());

  }
}
