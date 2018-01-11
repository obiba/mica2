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

import org.junit.Assert;
import org.junit.Test;

public class IdentifierGeneratorTest {

  @Test
  public void testNoPrefix() {
    String generated = IdentifierGenerator.newBuilder().size(10).generate();
    Assert.assertEquals(10, generated.length());
    generated = IdentifierGenerator.newBuilder().size(8).generate();
    Assert.assertEquals(8, generated.length());
    generated = IdentifierGenerator.newBuilder().size(6).generate();
    Assert.assertEquals(6, generated.length());
    generated = IdentifierGenerator.newBuilder().size(4).generate();
    Assert.assertEquals(4, generated.length());
    generated = IdentifierGenerator.newBuilder().size(2).generate();
    Assert.assertEquals(2, generated.length());
  }

  @Test
  public void testPrefix() {
    String prefix = "DACO-";
    String generated = IdentifierGenerator.newBuilder().size(10).prefix(prefix).generate();
    Assert.assertTrue(generated.startsWith(prefix));
    Assert.assertEquals(15, generated.length());
    generated = IdentifierGenerator.newBuilder().size(8).prefix(prefix).generate();
    Assert.assertTrue(generated.startsWith(prefix));
    Assert.assertEquals(13, generated.length());
    generated = IdentifierGenerator.newBuilder().size(6).prefix(prefix).generate();
    Assert.assertTrue(generated.startsWith(prefix));
    Assert.assertEquals(11, generated.length());
    generated = IdentifierGenerator.newBuilder().size(4).prefix(prefix).generate();
    Assert.assertTrue(generated.startsWith(prefix));
    Assert.assertEquals(9, generated.length());
    generated = IdentifierGenerator.newBuilder().size(2).prefix(prefix).generate();
    Assert.assertTrue(generated.startsWith(prefix));
    Assert.assertEquals(7, generated.length());
  }

  @Test
  public void testHex() {
    String generated = IdentifierGenerator.newBuilder().size(10).hex().generate();
    Assert.assertEquals(10, generated.length());
    generated = IdentifierGenerator.newBuilder().size(8).hex().generate();
    Assert.assertEquals(8, generated.length());
    generated = IdentifierGenerator.newBuilder().size(6).hex().generate();
    Assert.assertEquals(6, generated.length());
    generated = IdentifierGenerator.newBuilder().size(4).hex().generate();
    Assert.assertEquals(4, generated.length());
    generated = IdentifierGenerator.newBuilder().size(2).hex().generate();
    Assert.assertEquals(2, generated.length());
  }

}
