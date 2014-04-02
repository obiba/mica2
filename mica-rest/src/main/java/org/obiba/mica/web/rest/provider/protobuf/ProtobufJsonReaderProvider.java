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
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.googlecode.protobuf.format.JsonFormat;

@Component
@Provider
@Consumes({ "application/json" })
public class ProtobufJsonReaderProvider implements MessageBodyReader<Object> {

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

    Readable input = new InputStreamReader(entityStream, "UTF-8");
    if(helper.isWrapped(type, genericType)) {
      // JsonFormat does not provide a mergeCollection method
      return JsonIoUtil.mergeCollection(input, extensionRegistry, builder);
    }
    JsonFormat.merge(input, extensionRegistry, builder);
    return builder.build();
  }
}
