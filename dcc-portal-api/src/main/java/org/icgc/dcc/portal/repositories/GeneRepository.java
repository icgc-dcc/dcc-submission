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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.facet.FacetBuilders;
import org.icgc.dcc.portal.core.Types;
import org.icgc.dcc.portal.search.SearchQuery;

import java.util.ArrayList;

@Slf4j
public class GeneRepository implements IGeneRepository {

  private final static String INDEX = "icgc_test54"; // This should probably be set in a config
  private final static Types TYPE = Types.GENES;
  private static final String[] ALLOWED_FIELDS = ImmutableList.of("symbol", "description", "chromosome", "start",
      "end", "band", "gene_type").toArray(new String[7]);
  private final Client client;
  private FilterBuilder filter;

  @Inject
  public GeneRepository(Client client) {
    this.client = client;
  }

  // Returns one hit
  // @Override
  public final GetResponse getOne(final String id) {
    return client.prepareGet(INDEX, TYPE.toString(), id).execute().actionGet();
  }

  // Returns many hits
  // @Override
  public final SearchResponse getAll(final SearchQuery searchQuery) {
    this.filter = buildFilter(searchQuery);
    SearchRequestBuilder s =
        client.prepareSearch(INDEX).setTypes(TYPE.toString()).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
            .setQuery(buildQuery(searchQuery)) //
            .setFilter(this.filter) //
            .setFrom(searchQuery.getFrom()) //
            .setSize(searchQuery.getSize()) //
            .addSort(searchQuery.getSort(), searchQuery.getOrder()) //
            .addFields(ALLOWED_FIELDS) //
            .addFacet(FacetBuilders.termsFacet("Gene Type").field("gene_type").global(true)); //

    System.out.println(s);
    return s.execute().actionGet();
  }

  private FilterBuilder buildFilter(SearchQuery searchQuery) {
    if (searchQuery.getFilters() == null) {
      return FilterBuilders.matchAllFilter();
    } else {
      ObjectMapper mapper = new ObjectMapper();
      ArrayList<FilterBuilder> fbs = new ArrayList<FilterBuilder>();

      // Gene
      if (searchQuery.getFilters().has("gene")) {
        JsonNode gene = searchQuery.getFilters().path("gene");
        // Gene List
        if (gene.has("symbol")) {
          if (gene.get("symbol").isArray()) {
            ArrayList<String> symbols =
                mapper.convertValue(gene.get("symbol"), new TypeReference<ArrayList<String>>() {});
            fbs.add(FilterBuilders.termsFilter("symbol", symbols));
          } else {
            String symbol = mapper.convertValue(gene.get("symbol"), String.class);
            fbs.add(FilterBuilders.termFilter("symbol", symbol));
          }
        }

        // Gene Location
        if (gene.has("location")) {
          if (gene.get("location").isArray()) {
            // TODO add all the location using else logic in an OR filter - !!! OR not AND since AND
            // makes no sense for location
          } else {
            AndFilterBuilder locationFilter = FilterBuilders.andFilter();
            String location = mapper.convertValue(gene.get("location"), String.class);
            String[] parts = location.split(":");
            locationFilter.add(FilterBuilders.termFilter("chromosome",
                Integer.parseInt(parts[0].replaceAll("[a-zA-Z]", ""))));
            String[] range = parts[1].split("-");
            int start = range[0].equals("") ? 0 : Integer.parseInt(range[0].replaceAll(",", ""));
            locationFilter.add(FilterBuilders.rangeFilter("start").gte(start));
            if (range.length == 2) {
              int end = Integer.parseInt(range[1].replaceAll(",", ""));
              locationFilter.add(FilterBuilders.rangeFilter("end").lte(end));
            }
            fbs.add(locationFilter);
          }
        }

        // Gene Type
        if (gene.has("gene_type")) {
          if (gene.get("gene_type").isArray()) {
            ArrayList<String> geneTypes =
                mapper.convertValue(gene.get("gene_type"), new TypeReference<ArrayList<String>>() {});
            fbs.add(FilterBuilders.termsFilter("gene_type", geneTypes));
          } else {
            String geneType = mapper.convertValue(gene.get("gene_type"), String.class);
            fbs.add(FilterBuilders.termFilter("gene_type", geneType));
          }
        }
      }

      // Donor
      if (searchQuery.getFilters().has("donor")) {
        log.error("donor filters");
      }

      // Mutations
      if (searchQuery.getFilters().has("obs")) {
        log.error("obs filters");
      }

      return FilterBuilders.andFilter(fbs.toArray(new FilterBuilder[fbs.size()]));
    }
  }

  private QueryBuilder buildQuery(SearchQuery searchQuery) {
    return QueryBuilders //
        .nestedQuery("donor", //
            QueryBuilders.customScoreQuery(QueryBuilders.filteredQuery( //
                QueryBuilders.matchAllQuery(), //
                FilterBuilders.matchAllFilter()//
                )).script("doc['donor.somatic_mutation'].value") //
        ).scoreMode("total");
  }
}
