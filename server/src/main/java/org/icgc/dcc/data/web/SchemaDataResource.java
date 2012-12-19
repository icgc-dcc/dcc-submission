/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import java.util.Iterator;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.icgc.dcc.data.schema.Schema;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class SchemaDataResource {

  @Inject
  private Mongo mongo;

  private final Schema schema;

  protected SchemaDataResource(Schema schema) {
    this.schema = schema;
  }

  @GET
  public Iterator<DBObject> list(final @QueryParam("limit") Integer limit,
      final @QueryParam("offset") @DefaultValue("0") Integer offset) {
    DBCursor cursor = collection().find();
    if(limit != null) {
      cursor.limit(limit);
    }
    cursor.skip(offset);
    return cursor.iterator();
  }

  @GET
  @Path("/_schema")
  @Produces(MediaType.APPLICATION_JSON)
  public String getSchema() {
    return schema.toString();
  }

  @GET
  @Path("/_index")
  public Iterator<DBObject> indexed(final @QueryParam("index") String index, final @QueryParam("type") String type,
      final @QueryParam("limit") Integer limit, final @QueryParam("offset") @DefaultValue("0") Integer offset) {
    return Iterators.transform(list(limit, offset), new Function<DBObject, DBObject>() {

      @Override
      public DBObject apply(DBObject input) {
        return index(input);
      }

    });
  }

  @GET
  @Path("/{id}")
  public SchemaResponse getOne(@PathParam("id") String id) {
    return new SchemaResponse(schema, collection().findOne(asId(id)));
  }

  @Path("/{id}/_index")
  @GET
  public DBObject indexDonor(@PathParam("id") String id) {
    DBObject indexable = (DBObject) getOne(id).payload;
    return index(indexable);
  }

  protected DBObject index(DBObject indexable) {
    return indexable;
  }

  protected Object asId(String id) {
    return id;
  }

  protected DBCollection collection(String name) {
    return mongo.getDB("icgc").getCollection(name);
  }

  protected DBCollection collection() {
    return collection(schema.asRecord().getCollection());
  }

}
