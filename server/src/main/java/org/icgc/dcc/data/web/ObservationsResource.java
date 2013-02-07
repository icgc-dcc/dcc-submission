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

import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.bson.types.ObjectId;
import org.icgc.dcc.data.DataService;
import org.icgc.dcc.data.model.AffectsGene;
import org.icgc.dcc.data.model.Gene;
import org.icgc.dcc.data.model.Observation;
import org.icgc.dcc.data.model.QObservation;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.EntityPath;
import com.sun.jersey.api.NotFoundException;

@Path("/observations")
public class ObservationsResource {

  @Inject
  private DataService s;

  @GET
  public Iterator<Observation> observations() {
    return query(QObservation.observation).iterate();
  }

  @GET
  @Path("/{id}")
  public Observation observation(@PathParam("id") String id) {
    Observation o = s.datastore().get(Observation.class, new ObjectId(id));
    if(o == null) {
      throw new NotFoundException();
    }
    return o;
  }

  @GET
  @Path("/{id}/genes")
  public Iterable<Gene> observationGenes(@PathParam("id") String id) {
    return Iterables.transform(observation(id).affectedGenes, new Function<AffectsGene, Gene>() {

      @Override
      public Gene apply(AffectsGene input) {
        return s.datastore().get(Gene.class, input.geneId);
      }
    });
  }

  private <T> MorphiaQuery<T> query(EntityPath<T> path) {
    return new MorphiaQuery<T>(s.morphia(), s.datastore(), path);
  }
}
