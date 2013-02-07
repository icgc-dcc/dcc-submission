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
package org.icgc.dcc.data.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.data.index.model.IndexCommand;
import org.icgc.dcc.data.index.model.IndexDetails;

import com.google.common.collect.Iterators;

public abstract class IndexableResource<T extends HasKey> {

  @GET
  public Iterator<T> getAll() {
    return all();
  }

  @Path("/{key}")
  @GET
  public T fromKey(@PathParam("key") String key) {
    return getFromKey(key);
  }

  @Path("/{key}/index")
  @GET
  public Object indexed(@PathParam("key") String key) {
    return indexed(fromKey(key));
  }

  @Path("/_index")
  @GET
  public Response indexable(final @QueryParam("index") String index, final @QueryParam("type") String type,
      final @QueryParam("limit") Integer limit, final @QueryParam("offset") @DefaultValue("0") Integer offset) {

    Iterator<T> indexables = all();
    int skipped = Iterators.skip(indexables, offset);
    if(skipped < offset) {
      return Response.noContent().build();
    }
    if(limit != null) {
      indexables = Iterators.limit(indexables, limit);
    }
    final Iterator<T> iter = indexables;
    StreamingOutput output = new StreamingOutput() {

      @Override
      public void write(OutputStream output) throws IOException, WebApplicationException {
        ObjectMapper mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        IndexCommand command = new IndexCommand();
        command.index = new IndexDetails(index, type);
        while(iter.hasNext()) {
          T indexable = iter.next();
          command.index._id = indexable.getKey();
          mapper.writeValue(output, command);
          output.write((byte) '\n');
          mapper.writeValue(output, indexed(indexable));
          output.write((byte) '\n');
        }

        output.close();
      }
    };
    return Response.ok(output, MediaType.APPLICATION_JSON).build();
  }

  protected abstract T getFromKey(String key);

  protected abstract Iterator<T> all();

  protected abstract Object indexed(T value);

}
