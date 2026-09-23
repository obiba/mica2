/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.support;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

public class IdentifierGeneratorTest {

  @Test
  public void testNoPrefix() {
    String generated = IdentifierGenerator.newBuilder().size(10).generate();
    Assertions.assertEquals(10, generated.length());
    generated = IdentifierGenerator.newBuilder().size(8).generate();
    Assertions.assertEquals(8, generated.length());
    generated = IdentifierGenerator.newBuilder().size(6).generate();
    Assertions.assertEquals(6, generated.length());
    generated = IdentifierGenerator.newBuilder().size(4).generate();
    Assertions.assertEquals(4, generated.length());
    generated = IdentifierGenerator.newBuilder().size(2).generate();
    Assertions.assertEquals(2, generated.length());
  }

  @Test
  public void testPrefix() {
    String prefix = "DACO-";
    String generated = IdentifierGenerator.newBuilder().size(10).prefix(prefix).generate();
    Assertions.assertTrue(generated.startsWith(prefix));
    Assertions.assertEquals(15, generated.length());
    generated = IdentifierGenerator.newBuilder().size(8).prefix(prefix).generate();
    Assertions.assertTrue(generated.startsWith(prefix));
    Assertions.assertEquals(13, generated.length());
    generated = IdentifierGenerator.newBuilder().size(6).prefix(prefix).generate();
    Assertions.assertTrue(generated.startsWith(prefix));
    Assertions.assertEquals(11, generated.length());
    generated = IdentifierGenerator.newBuilder().size(4).prefix(prefix).generate();
    Assertions.assertTrue(generated.startsWith(prefix));
    Assertions.assertEquals(9, generated.length());
    generated = IdentifierGenerator.newBuilder().size(2).prefix(prefix).generate();
    Assertions.assertTrue(generated.startsWith(prefix));
    Assertions.assertEquals(7, generated.length());
  }

  @Test
  public void testHex() {
    String generated = IdentifierGenerator.newBuilder().size(10).hex().generate();
    Assertions.assertEquals(10, generated.length());
    generated = IdentifierGenerator.newBuilder().size(8).hex().generate();
    Assertions.assertEquals(8, generated.length());
    generated = IdentifierGenerator.newBuilder().size(6).hex().generate();
    Assertions.assertEquals(6, generated.length());
    generated = IdentifierGenerator.newBuilder().size(4).hex().generate();
    Assertions.assertEquals(4, generated.length());
    generated = IdentifierGenerator.newBuilder().size(2).hex().generate();
    Assertions.assertEquals(2, generated.length());
  }

  @Test
  public void testExclusionsNotCreated() {
    List<String> exclusions = Lists.newArrayList("0", "3", "4", "8", "9");
    IdentifierGenerator generator = IdentifierGenerator.newBuilder().zeros().size(1).exclusions(exclusions).build();
    boolean isNotInExclusions = true;

    for(int i = 0; i < 10; i++) {
      isNotInExclusions = isNotInExclusions && (exclusions.indexOf(generator.generateIdentifier()) == -1);
    }

    Assertions.assertTrue(isNotInExclusions);
  }

}
