/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.mica.web.rest.provider.protobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageLite;

@Provider
@Consumes(ProtobufNativeProvider.APPLICATION_X_PROTOBUF)
@Produces(ProtobufNativeProvider.APPLICATION_X_PROTOBUF)
public class ProtobufNativeProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

  public static final String APPLICATION_X_PROTOBUF = "application/x-protobuf";

  @Inject
  protected ProtobufProviderHelper helper;

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return Message.class.isAssignableFrom(type) || helper.isWrapped(type, genericType);
  }

  @Override
  public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
      throws IOException, WebApplicationException {
    Class<Message> messageType = helper.extractMessageType(type, genericType);
    ExtensionRegistry extensionRegistry = helper.extensions().forMessage(messageType);
    Builder builder = helper.builders().forMessage(messageType);
    if(helper.isWrapped(type, genericType)) {
      Collection<Message> msgs = new ArrayList<>();
      Builder builderClone = builder.clone();
      while(builderClone.mergeDelimitedFrom(entityStream, extensionRegistry)) {
        msgs.add(builderClone.build());
        builderClone = builder.clone();
      }
      return msgs;
    }
    return builder.mergeFrom(entityStream, extensionRegistry).build();
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return Message.class.isAssignableFrom(type) || helper.isWrapped(type, genericType);
  }

  @Override
  public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  @Override
  @SuppressWarnings({ "unchecked", "PMD.ExcessiveParameterList" })
  public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
      throws IOException, WebApplicationException {
    if(helper.isWrapped(type, genericType)) {
      for(MessageLite message : (Iterable<Message>) obj) {
        message.writeDelimitedTo(entityStream);
      }
    } else {
      ((MessageLite) obj).writeTo(entityStream);
    }
  }
}
