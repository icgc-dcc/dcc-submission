/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * A Jersey provider which adds validation to the basic Jackson Json provider. Any request entity method parameters
 * annotated with {@code @Valid} are validated, and an informative {@code 422 Unprocessable Entity} response is returned
 * should the entity be invalid. Additionally, a {@code 400 Bad Request} is returned should the entity be unparseable.
 * 
 * @author codyaray
 * @since 5/23/12
 */
@Provider
@Consumes({ MediaType.APPLICATION_JSON, "text/json" })
@Produces({ MediaType.APPLICATION_JSON, "text/json" })
public class ValidatingJacksonJsonProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

  // Unfortunate that this isn't defined in Response.Status
  @VisibleForTesting
  static final Response.StatusType UNPROCESSABLE_ENTITY = new Response.StatusType() {
    @Override
    public int getStatusCode() {
      return 422;
    }

    @Override
    public Response.Status.Family getFamily() {
      return Response.Status.Family.CLIENT_ERROR;
    }

    @Override
    public String getReasonPhrase() {
      return "Unprocessable Entity";
    }
  };

  private final JacksonJsonProvider delegate;

  private final Validator validator;

  @Inject
  public ValidatingJacksonJsonProvider(JacksonJsonProvider delegate, Validator validator) {
    this.delegate = delegate;
    this.validator = validator;
  }

  @Override
  public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {

    Object value = parseEntity(type, genericType, annotations, mediaType, httpHeaders, entityStream);

    if(hasValidAnnotation(annotations)) {
      List<String> errors = validate(value);
      if(!errors.isEmpty()) {
        StringBuilder msg = new StringBuilder("The request entity had the following errors:\n");
        for(String error : errors) {
          msg.append("  * ").append(error).append('\n');
        }
        throw new WebApplicationException(unprocessableEntity(msg.toString()));
      }
    }

    return value;
  }

  @Override
  public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
      WebApplicationException {
    delegate.writeTo(t, type, genericType, annotations, mediaType, httpHeaders, entityStream);
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return delegate.isWriteable(type, genericType, annotations, mediaType);
  }

  @Override
  public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return delegate.getSize(t, type, genericType, annotations, mediaType);
  }

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return delegate.isReadable(type, genericType, annotations, mediaType);
  }

  private Object parseEntity(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
    return delegate.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
  }

  @VisibleForTesting
  boolean hasValidAnnotation(Annotation[] annotations) {
    for(Annotation annotation : annotations) {
      if(Valid.class.equals(annotation.annotationType())) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private List<String> validate(Object o) {
    if(o instanceof Collection<?>) {
      o = new CollectionWrapper((Collection<?>) o);
    }
    Set<String> errors = Sets.newHashSet();
    Set<ConstraintViolation<Object>> violations = validator.validate(o);
    for(ConstraintViolation<Object> v : violations) {
      errors.add(String.format("%s %s (was %s)", v.getPropertyPath(), v.getMessage(), v.getInvalidValue()));
    }
    return ImmutableList.copyOf(Ordering.natural().sortedCopy(errors));
  }

  private static Response unprocessableEntity(String msg) {
    return Response.status(UNPROCESSABLE_ENTITY).entity(msg).type(MediaType.TEXT_PLAIN_TYPE).build();
  }

  private static class CollectionWrapper<T> {
    @Valid
    private final Collection<T> collection;

    public CollectionWrapper(Collection<T> toWrap) {
      this.collection = toWrap;
    }
  }
}
