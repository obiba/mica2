/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import jakarta.inject.Inject;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.obiba.mica.TestApplication;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.web.rest.TestUtil;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
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
 * Test class for the StudyResource REST controller.
 *
 * @see PublishedStudiesResource
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestApplication.class)
@WebAppConfiguration
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class })
@ActiveProfiles("dev")
public class PublishedStudiesResourceTest {

  private static final String DEFAULT_ID = "1";

  private static final LocalDate DEFAULT_SAMPLE_DATE_ATTR = new LocalDate(0L);

  private static final LocalDate UPD_SAMPLE_DATE_ATTR = new LocalDate();

  private static final String DEFAULT_SAMPLE_TEXT_ATTR = "sampleTextAttribute";

  private static final String UPD_SAMPLE_TEXT_ATTR = "sampleTextAttributeUpt";

  @Inject
  private IndividualStudyService individualStudyService;

  private MockMvc restStudyMockMvc;

  private Study study;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    PublishedStudiesResource publishedStudiesResource = new PublishedStudiesResource();
    ReflectionTestUtils.setField(publishedStudiesResource, "studyRepository", individualStudyService);

    restStudyMockMvc = MockMvcBuilders.standaloneSetup(publishedStudiesResource).build();

    study = new Study();
    study.setId(DEFAULT_ID);
//    study.setSampleDateAttribute(DEFAULT_SAMPLE_DATE_ATTR);
//    study.setSampleTextAttribute(DEFAULT_SAMPLE_TEXT_ATTR);
  }

  @Test
  public void testCRUDStudy() throws Exception {

    // Create Study
    restStudyMockMvc.perform(post("/ws/studies").contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(study))).andExpect(status().isOk());

    // Read Study
    restStudyMockMvc.perform(get("/ws/studies/{id}", DEFAULT_ID)).andExpect(status().isOk()) //
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
        .andExpect(jsonPath("$.id").value(DEFAULT_ID)) //
        .andExpect(jsonPath("$.sampleDateAttribute").value(DEFAULT_SAMPLE_DATE_ATTR.toString())) //
        .andExpect(jsonPath("$.sampleTextAttribute").value(DEFAULT_SAMPLE_TEXT_ATTR));

    // Update Study
//    study.setSampleDateAttribute(UPD_SAMPLE_DATE_ATTR);
//    study.setSampleTextAttribute(UPD_SAMPLE_TEXT_ATTR);

    restStudyMockMvc.perform(post("/ws/studies").contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(study))).andExpect(status().isOk());

    // Read updated Study
    restStudyMockMvc.perform(get("/ws/studies/{id}", DEFAULT_ID)) //
        .andExpect(status().isOk()) //
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)) //
        .andExpect(jsonPath("$.id").value(DEFAULT_ID)) //
        .andExpect(jsonPath("$.sampleDateAttribute").value(UPD_SAMPLE_DATE_ATTR.toString())) //
        .andExpect(jsonPath("$.sampleTextAttribute").value(UPD_SAMPLE_TEXT_ATTR));

    // Delete Study
    restStudyMockMvc.perform(delete("/ws/studies/{id}", DEFAULT_ID).accept(TestUtil.APPLICATION_JSON_UTF8))
        .andExpect(status().isOk());

    // Read nonexisting Study
    restStudyMockMvc.perform(get("/ws/studies/{id}", DEFAULT_ID).accept(TestUtil.APPLICATION_JSON_UTF8))
        .andExpect(status().isNotFound());

  }
}
