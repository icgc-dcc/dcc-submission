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
package org.icgc.dcc.data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.data.model.Donor;
import org.icgc.dcc.data.model.Expression;
import org.icgc.dcc.data.model.Gene;
import org.icgc.dcc.data.model.QDonor;
import org.icgc.dcc.data.model.QGene;
import org.icgc.dcc.data.model.Sample;
import org.icgc.dcc.data.model.SimpleMutation;
import org.icgc.dcc.data.model.Variation;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.EntityPath;

public class DataService extends BaseMorphiaService<Gene> {

  final LoadingCache<String, Donor> donors = CacheBuilder.newBuilder().build(new CacheLoader<String, Donor>() {

    @Override
    public Donor load(String key) throws Exception {
      return query(QDonor.donor).where(QDonor.donor.specimens.any().samples.any().id.eq(key)).uniqueResult();
    }
  });

  final LoadingCache<String, Gene> genes = CacheBuilder.newBuilder().build(new CacheLoader<String, Gene>() {

    @Override
    public Gene load(String key) throws Exception {
      return datastore().get(Gene.class, key);
    }
  });

  @Inject
  public DataService(Morphia morphia, Datastore datastore) {
    super(morphia, datastore, QGene.gene);
    registerModelClasses(Gene.class, Variation.class, Sample.class, SimpleMutation.class, Expression.class);
  }

  public Donor donorFromSample(String sample) {
    try {
      return donors.get(sample);
    } catch(ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public Gene genesFromId(String key) {
    try {
      return genes.get(key);
    } catch(ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Gene> genesFromIds(Set<String> keys) {
    Map<String, Gene> result = genes.getAllPresent(keys);
    keys.removeAll(result.keySet());
    if(keys.size() > 0) {
      result = Maps.newHashMap(result);
      for(Gene g : datastore().createQuery(Gene.class).filter("_id in", keys)) {
        genes.put(g.name, g);
        result.put(g.name, g);
      }
    }
    return result;
  }

  private <T> MorphiaQuery<T> query(EntityPath<T> path) {
    return new MorphiaQuery<T>(morphia(), datastore(), path);
  }

}
