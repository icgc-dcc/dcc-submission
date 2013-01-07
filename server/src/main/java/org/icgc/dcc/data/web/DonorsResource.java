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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.icgc.dcc.data.DataService;
import org.icgc.dcc.data.index.model.GeneObservation;
import org.icgc.dcc.data.index.model.IndexedDonor;
import org.icgc.dcc.data.model.AffectsGene;
import org.icgc.dcc.data.model.Donor;
import org.icgc.dcc.data.model.Gene;
import org.icgc.dcc.data.model.Observation;
import org.icgc.dcc.data.model.QObservation;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.EntityPath;

@Path("/donors")
public class DonorsResource extends IndexableResource<Donor> {

  @Inject
  private DataService s;

  @Path("/{name}/observations")
  @GET
  public Iterator<Observation> donorObservations(@PathParam("name") String id) {
    return query(QObservation.observation).where(QObservation.observation.donorId.eq(id)).iterate();
  }

  @Override
  protected Donor getFromKey(String key) {
    return s.datastore().get(Donor.class, key);
  }

  @Override
  protected Iterator<Donor> all() {
    return s.datastore().find(Donor.class).iterator();
  }

  @Override
  protected IndexedDonor indexed(Donor donor) {
    Multimap<Gene, Observation> geneObs = geneIndex(donorObservations(donor.id));
    return IndexedDonor.indexed(donor, asGeneObs(geneObs));
  }

  private Multimap<Gene, Observation> geneIndex(Iterator<Observation> observations) {
    Multimap<String, Observation> geneObs = HashMultimap.create();
    while(observations.hasNext()) {
      Observation o = observations.next();
      for(AffectsGene g : o.affectedGenes) {
        geneObs.put(g.geneId, o);
      }
    }
    Multimap<Gene, Observation> geneObs2 = HashMultimap.create(geneObs.keySet().size(), 50);
    if(geneObs.size() > 0) {
      Map<String, Gene> genes = s.genesFromIds(geneObs.keySet());
      for(Gene g : genes.values()) {
        geneObs2.putAll(g, geneObs.get(g.name));
      }
    }
    return geneObs2;
  }

  private Iterable<GeneObservation> asGeneObs(Multimap<Gene, Observation> geneObs) {
    return Iterables.transform(geneObs.asMap().entrySet(),
        new Function<Map.Entry<Gene, Collection<Observation>>, GeneObservation>() {

          @Override
          public GeneObservation apply(Map.Entry<Gene, Collection<Observation>> input) {
            return GeneObservation.indexed(input.getKey(), input.getValue());
          }
        });
  }

  private <T> MorphiaQuery<T> query(EntityPath<T> path) {
    return new MorphiaQuery<T>(s.morphia(), s.datastore(), path);
  }

}
