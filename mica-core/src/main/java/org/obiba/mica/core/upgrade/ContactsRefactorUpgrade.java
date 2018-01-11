/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.mica.contact.event.PersonUpdatedEvent;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

@Component
public class ContactsRefactorUpgrade implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(ContactsRefactorUpgrade.class);

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private NetworkService networkService;

  @Inject
  private EventBus eventBus;

  @Override
  public String getDescription() {
    return "Refactored attachments";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("1.0.0");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing contacts upgrade");
    studyRepository.findAll().forEach(study -> {
      study.getAllPersons().forEach(p -> p.setEmail(Strings.emptyToNull(p.getEmail())));
      StudyState studyState = individualStudyService.findStateById(study.getId());

      studyRepository.saveWithReferences(study);

      eventBus.post(new DraftStudyUpdatedEvent(study));
      study.getAllPersons().forEach(c -> eventBus.post(new PersonUpdatedEvent(c)));

      if(studyState.isPublished()) {
        eventBus.post(new StudyPublishedEvent(study, studyState.getPublishedBy(), PublishCascadingScope.ALL));
      }
    });

    networkRepository.findAll().forEach(network -> {
      network.getAllPersons().forEach(p -> p.setEmail(Strings.emptyToNull(p.getEmail())));

      networkService.save(network, "System upgrade.");

      if(network.isPublished()) {
        networkService.publish(network.getId(), true);
      }
    });
  }
}
