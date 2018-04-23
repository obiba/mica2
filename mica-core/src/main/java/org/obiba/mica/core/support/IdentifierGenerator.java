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

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Generate IDs according to specifications.
 */
public class IdentifierGenerator {

  private final Random generator = new SecureRandom();

  private int keySize = 10;

  private boolean allowStartWithZero = false;

  private String prefix;

  private boolean hex = false;

  private List<String> exclusions;

  private IdentifierGenerator() {}

  public void setKeySize(int keySize) {
    this.keySize = keySize;
  }

  public void setAllowStartWithZero(boolean allowStartWithZero) {
    this.allowStartWithZero = allowStartWithZero;
  }

  public boolean isAllowStartWithZero() {
    return allowStartWithZero;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  private int getPrefixLength() {
    return Strings.isNullOrEmpty(prefix) ? 0 : prefix.length();
  }

  public List<String> getExclusions() {
    if (exclusions == null) exclusions = Lists.newArrayList();
    return exclusions;
  }

  public void setExclusions(List<String> exclusions) {
    this.exclusions = exclusions;
  }

  public String generateIdentifier() {
    if(keySize < 1) {
      throw new IllegalStateException("keySize must be at least 1: " + keySize);
    }

    StringBuilder sb = new StringBuilder(keySize);

    sb.append(nextIntModuloZero());
    for(int i = 1; i < keySize; i++) {
      sb.append(generator.nextInt(10));
    }

    String value = sb.toString();
    if(hex) {
      String valueHex = Long.toHexString(Long.valueOf(value)).toUpperCase();
      while(valueHex.length() < keySize) {
        valueHex = nextIntModuloZero() + valueHex;
      }
      value = valueHex;
    }

    String result = getPrefixLength() > 0 ? prefix + value : value;

    if (getExclusions().indexOf(result) != -1) return generateIdentifier();
    return result;
  }

  private int nextIntModuloZero() {
    return allowStartWithZero //
        ? generator.nextInt(10) //
        : generator.nextInt(9) + 1; // Generate a random number between 0 and 8, then add 1.
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private IdentifierGenerator idGenerator;

    public Builder() {
      idGenerator = new IdentifierGenerator();
    }

    public Builder size(int size) {
      idGenerator.keySize = size;
      return this;
    }

    public Builder prefix(String prefix) {
      idGenerator.prefix = prefix;
      return this;
    }

    public Builder zeros() {
      idGenerator.allowStartWithZero = true;
      return this;
    }

    public Builder hex() {
      idGenerator.hex = true;
      return this;
    }

    public Builder exclusions(List<String> exclusions) {
      idGenerator.exclusions = exclusions;
      return this;
    }

    public IdentifierGenerator build() {
      return idGenerator;
    }

    public String generate() {
      return idGenerator.generateIdentifier();
    }

  }
}
