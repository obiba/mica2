package org.obiba.mica.domain.util;

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
