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

import java.util.Collections;
import java.util.List;

import javax.ws.rs.Path;

import org.icgc.dcc.data.schema.SchemaRegistry;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@Path("/data/donors")
public class DonorDataResource extends SchemaDataResource {

  @Inject
  public DonorDataResource(SchemaRegistry registry) {
    super(registry.getSchema("donor"));
  }

  @Override
  protected DBObject index(DBObject donor) {
    donor.put("observations", ImmutableList.copyOf(donorObservations(donor.get("_id"))));
    return donor;
  }

  private Iterable<DBObject> donorObservations(Object donorId) {
    BasicDBObject query = new BasicDBObject();
    query.put("donorId", donorId);
    return collection("Observation").find(query);
  }

  @SuppressWarnings("unused")
  private Iterable<String> geneId(DBObject observation) {
    @SuppressWarnings("unchecked")
    List<DBObject> affectedGenes = (List<DBObject>) observation.get("affectedGenes");
    if(affectedGenes != null) {
      return Iterables.transform(affectedGenes, new Function<DBObject, String>() {

        @Override
        public String apply(DBObject input) {
          return (String) input.get("geneId");
        }
      });
    }
    return Collections.emptyList();
  }
}
