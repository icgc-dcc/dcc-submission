/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.results;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.Facets;
import org.icgc.dcc.portal.request.RequestSearchQuery;

@Data
public class FindAllResults {

  private final ImmutableList<ResultsHit> hits;
  private final ImmutableMap<String, ResultsFacet> facets;
  private final ResultsPagination pagination;

  public FindAllResults(final SearchResponse response, final RequestSearchQuery requestSearchQuery) {
    // System.out.println(response);
    this.hits = buildResponseHits(response.getHits().getHits());
    this.facets = buildResponseFacets(response.getFacets());
    this.pagination = new ResultsPagination(response.getHits(), requestSearchQuery);
  }

  private ImmutableMap<String, ResultsFacet> buildResponseFacets(Facets facets) {
    ImmutableMap.Builder<String, ResultsFacet> mb = new ImmutableMap.Builder<String, ResultsFacet>();
    for (Facet facet : facets.facets()) {
      mb.put(facet.getName(), new ResultsFacet(facet));
    }
    return mb.build();
  }

  private ImmutableList<ResultsHit> buildResponseHits(SearchHit[] hits) {
    ImmutableList.Builder<ResultsHit> lb = new ImmutableList.Builder<ResultsHit>();
    for (SearchHit hit : hits) {
      lb.add(new ResultsHit(hit));
    }
    return lb.build();
  }
}
