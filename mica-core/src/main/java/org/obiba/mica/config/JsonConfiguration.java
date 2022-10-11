/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JsonConfiguration {

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.registerModule(new JodaModule());

    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDateTime.class, new CustomLocalDateSerializer());
    javaTimeModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());

    mapper.registerModule(javaTimeModule);

    mapper.findAndRegisterModules();
    return mapper;
  }

  public static class CustomLocalDateSerializer extends StdSerializer<LocalDateTime> {

    public CustomLocalDateSerializer() {
      super(LocalDateTime.class);
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {      ;
      gen.writeString("" + value.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
    }
  }

  public static class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    public CustomLocalDateTimeDeserializer() {
      super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
      long value = p.getValueAsLong();
      Instant instant = Instant.ofEpochMilli(value);

      return LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault());
    }
  }

}
