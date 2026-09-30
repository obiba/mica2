/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file;

import org.junit.jupiter.api.Test;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;

import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilsTest {

  @Test
  public void testEncode() {
    assertThat(FileUtils.encode("/toto/tutu/some silly, file: path.pdf")).isEqualTo("/toto/tutu/some+silly%2C+file%3A+path.pdf");
    assertThat(FileUtils.encode(null)).isNull();
  }

  @Test
  public void testGetEntityPath() {
    assertThat(FileUtils.getEntityPath(withId(new Study(), "s1"))).isEqualTo("/individual-study/s1");
    assertThat(FileUtils.getEntityPath(withId(new HarmonizationStudy(), "s1"))).isEqualTo("/harmonization-study/s1");
    assertThat(FileUtils.getEntityPath(withId(new StudyDataset(), "d1"))).isEqualTo("/collected-dataset/d1");
    assertThat(FileUtils.getEntityPath(withId(new HarmonizationDataset(), "d1"))).isEqualTo("/harmonized-dataset/d1");
    assertThat(FileUtils.getEntityPath(withId(new Network(), "n1"))).isEqualTo("/network/n1");
    assertThat(FileUtils.getEntityPath(withId(new Project(), "p1"))).isEqualTo("/project/p1");
  }

  private static <T extends AbstractGitPersistable> T withId(T persistable, String id) {
    persistable.setId(id);
    return persistable;
  }

  @Test
  public void testDecode() {
    assertThat(FileUtils.decode("/toto/tutu/some+silly%2C+file%3A+path.pdf")).isEqualTo
      ("/toto/tutu/some silly, file: path.pdf");
    assertThat(FileUtils.decode(null)).isNull();
  }
}
