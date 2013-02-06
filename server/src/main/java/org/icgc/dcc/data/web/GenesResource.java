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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.icgc.dcc.data.DataService;
import org.icgc.dcc.data.index.model.DonorObservation;
import org.icgc.dcc.data.index.model.IndexedGene;
import org.icgc.dcc.data.model.Donor;
import org.icgc.dcc.data.model.Gene;
import org.icgc.dcc.data.model.Observation;
import org.icgc.dcc.data.model.QObservation;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.EntityPath;

@Path("/genes")
public class GenesResource extends IndexableResource<Gene> {

  @Inject
  private DataService s;

  @Path("/{name}/observations")
  @GET
  public Iterator<Observation> geneObservations(@PathParam("name") String name) {
    return query(QObservation.observation).where(QObservation.observation.affectedGenes.any().geneId.eq(name))
        .iterate();
  }

  @Override
  protected Gene getFromKey(String key) {
    return s.genesFromId(key);
  }

  @Override
  protected Iterator<Gene> all() {
    return s.query().iterate();
  }

  @Override
  protected IndexedGene indexed(Gene value) {
    Multimap<Donor, Observation> donorObs = donorIndex(geneObservations(value.name));
    return IndexedGene.indexed(value, asDonorObs(donorObs));
  }

  private Multimap<Donor, Observation> donorIndex(Iterator<Observation> observations) {
    return Multimaps.index(observations, new Function<Observation, Donor>() {

      @Override
      public Donor apply(Observation input) {
        for(String sample : input.samples) {
          Donor d = s.donorFromSample(sample);
          if(d != null) return d;
        }
        return null;
      }
    });
  }

  private Iterable<DonorObservation> asDonorObs(Multimap<Donor, Observation> donorObs) {
    return Iterables.transform(donorObs.asMap().entrySet(),
        new Function<Map.Entry<Donor, Collection<Observation>>, DonorObservation>() {

          @Override
          public DonorObservation apply(Map.Entry<Donor, Collection<Observation>> input) {
            return DonorObservation.indexed(input.getKey(), input.getValue());
          }
        });
  }

  private <T> MorphiaQuery<T> query(EntityPath<T> path) {
    return new MorphiaQuery<T>(s.morphia(), s.datastore(), path);
  }
}
