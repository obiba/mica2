/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

import com.google.protobuf.Message;
import org.glassfish.jersey.jackson.internal.DefaultJacksonJaxbJsonProvider;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Jackson JSON provider that leaves protobuf messages to {@link org.obiba.jersey.protobuf.ProtobufJsonProvider}.
 * <p>
 * Both providers declare {@code MessageBodyWriter<Object>} for {@code application/json}, so Jersey cannot tell them
 * apart by type or media type distance and picks whichever comes first in an unordered provider set. When Jackson wins,
 * writing a protobuf DTO fails with "Direct self-reference leading to cycle" on {@code UnknownFieldSet}. Declining
 * protobuf types here makes the choice deterministic, whatever the provider order happens to be.
 *
 * @see JerseyConfiguration
 */
@Provider
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class MicaJacksonJsonProvider extends DefaultJacksonJaxbJsonProvider {

  @Inject
  public MicaJacksonJsonProvider(@Context Providers providers, @Context Configuration config) {
    super(providers, config);
  }

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return !isProtobuf(type, genericType) && super.isReadable(type, genericType, annotations, mediaType);
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return !isProtobuf(type, genericType) && super.isWriteable(type, genericType, annotations, mediaType);
  }

  /**
   * A protobuf message, or a collection/array of protobuf messages, as accepted by ProtobufJsonProvider.
   */
  private static boolean isProtobuf(Class<?> type, Type genericType) {
    if (Message.class.isAssignableFrom(type)) return true;
    if (type.isArray()) return Message.class.isAssignableFrom(type.getComponentType());
    if (!Iterable.class.isAssignableFrom(type)) return false;
    if (genericType instanceof ParameterizedType) {
      Type[] arguments = ((ParameterizedType) genericType).getActualTypeArguments();
      return arguments.length == 1 && arguments[0] instanceof Class
        && Message.class.isAssignableFrom((Class<?>) arguments[0]);
    }
    return false;
  }
}
