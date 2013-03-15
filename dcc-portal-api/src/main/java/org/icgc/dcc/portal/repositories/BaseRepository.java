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

package org.icgc.dcc.portal.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import lombok.Data;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.results.FindAllResults;
import org.icgc.dcc.portal.results.FindResults;

@Data
public abstract class BaseRepository {

  private final String index;

  private final String type;

  private final String[] fields;

  private final Client client;

  private FilterBuilder filter;

  private QueryBuilder query;

  @Inject
  public BaseRepository(Client client, String index, String type, String[] fields) {
    this.client = client;
    this.index = index;
    this.type = type;
    this.fields = fields;
  }

  public final FindResults find(String id) {
    GetRequestBuilder g = buildGetRequest(id);
    return new FindResults(g.execute().actionGet());
  }

  public final FindAllResults findAll(RequestSearchQuery requestSearchQuery) {
    setFilter(buildFilters(requestSearchQuery.getFilters()));
    setQuery(buildQuery());
    SearchRequestBuilder s = buildSearchRequest(requestSearchQuery);
    s = addFacets(s, requestSearchQuery);
    return new FindAllResults(s.execute().actionGet(), requestSearchQuery);
  }

  private GetRequestBuilder buildGetRequest(String id) {
    return getClient().prepareGet(getIndex(), getType(), id).setFields(getFields());
  }

  private SearchRequestBuilder buildSearchRequest(RequestSearchQuery requestSearchQuery) {
    return getClient().prepareSearch(getIndex()).setTypes(getType()).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(getQuery()).setFilter(getFilter()).setFrom(requestSearchQuery.getFrom())
        .setSize(requestSearchQuery.getSize())
        .addSort(requestSearchQuery.getSort(), SortOrder.valueOf(requestSearchQuery.getOrder()))
        .addFields(allowedFields(requestSearchQuery.getFields()));
  }

  private String[] allowedFields(String[] searchedFields) {
    Sets.SetView<String> intersection =
        Sets.intersection(Sets.newHashSet(getFields()), Sets.newHashSet(searchedFields));
    return intersection.toArray(new String[intersection.size()]);
  }

  FilterBuilder setFacetFilter(String name, JsonNode filter) {
    JsonNode temp = filter.deepCopy();
    ((ObjectNode) temp).remove(name);
    return buildFilters(temp);
  }

  abstract SearchRequestBuilder addFacets(SearchRequestBuilder searchRequestBuilder,
      RequestSearchQuery requestSearchQuery);

  abstract QueryBuilder buildQuery();

  abstract FilterBuilder buildFilters(JsonNode filters);
}
