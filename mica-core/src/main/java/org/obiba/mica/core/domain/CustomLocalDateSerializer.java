/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.io.IOException;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Custom Jackson serializer for displaying Joda Time dates.
 */
public class CustomLocalDateSerializer extends JsonSerializer<LocalDate> {

  private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

  @Override
  public void serialize(LocalDate value, JsonGenerator generator, SerializerProvider serializerProvider)
      throws IOException {

    generator.writeString(formatter.print(value));
  }
}
