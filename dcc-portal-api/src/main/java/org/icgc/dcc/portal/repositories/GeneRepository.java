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
import com.sun.tools.javac.util.List;
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
  private ObjectMapper mapper = new ObjectMapper();


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
      AndFilterBuilder fbs = FilterBuilders.andFilter();

      // Gene
      if (searchQuery.getFilters().has("gene")) {
        JsonNode gene = searchQuery.getFilters().path("gene");
        for (String key : List.of("gene_type", "symbol")) {
          if (gene.has(key)) {
            fbs.add(buildTermFilter(gene, key));
          }
        }

        // Gene Location
        if (gene.has("location")) {
          fbs.add(buildChrLocationFilter(gene));
        }
      }

      // Donor
      if (searchQuery.getFilters().has("donor")) {
        AndFilterBuilder donorAnd = FilterBuilders.andFilter();
        JsonNode donor = searchQuery.getFilters().path("donor");
        for (String key : List.of("project", "primary_site", "donor_id", "gender", "tumour", "vital_status",
            "disease_status", "donor_release_type")) {
          if (donor.has(key)) {
            donorAnd.add(buildTermFilter(donor, key));
          }
        }
        for (String key : List.of("age_at_diagnosis", "survival_time", "donor_release_interval")) {
          if (donor.has(key)) {
            donorAnd.add(buildRangeFilter(donor, key));
          }
        }

        NestedFilterBuilder donorNested = FilterBuilders.nestedFilter("donor", donorAnd);
        fbs.add(donorNested);
      }

      // Mutations
      if (searchQuery.getFilters().has("mutation")) {
        AndFilterBuilder mutationAnd = FilterBuilders.andFilter();
        JsonNode mutation = searchQuery.getFilters().path("mutation");
        for (String key : List.of("project", "primary_site", "donor_id", "gender", "tumour", "vital_status",
            "disease_status", "donor_release_type")) {
          if (mutation.has(key)) {
            mutationAnd.add(buildTermFilter(mutation, key));
          }
        }
        if (mutation.has("location")) {
          mutationAnd.add(buildChrLocationFilter(mutation));
        }

        NestedFilterBuilder mutationNested = FilterBuilders.nestedFilter("mutation", mutationAnd);
        fbs.add(mutationNested);
      }

      return fbs;
    }
  }

  private FilterBuilder buildChrLocationFilter(JsonNode json) {
    FilterBuilder chrLocFilter;
    String location = "location";

    if (json.get(location).isArray()) {
      ArrayList<String> locations = mapper.convertValue(json.get(location), new TypeReference<ArrayList<String>>() {});
      OrFilterBuilder manyChrLocations = FilterBuilders.orFilter();
      for (String loc : locations) {
        manyChrLocations.add(buildChrLocation(loc));
      }
      chrLocFilter = manyChrLocations;
    } else {
      String loc = mapper.convertValue(json.get(location), String.class);
      chrLocFilter = buildChrLocation(loc);
    }

    return chrLocFilter;
  }

  private FilterBuilder buildChrLocation(String location) {
    AndFilterBuilder locationFilter = FilterBuilders.andFilter();
    String[] parts = location.split(":");
    locationFilter.add(FilterBuilders.termFilter("chromosome", Integer.parseInt(parts[0].replaceAll("[a-zA-Z]", ""))));
    if (parts.length == 2) {
      String[] range = parts[1].split("-");
      int start = range[0].equals("") ? 0 : Integer.parseInt(range[0].replaceAll(",", ""));
      locationFilter.add(FilterBuilders.rangeFilter("start").gte(start));
      if (range.length == 2) {
        int end = Integer.parseInt(range[1].replaceAll(",", ""));
        locationFilter.add(FilterBuilders.rangeFilter("end").lte(end));
      }
    }
    return locationFilter;
  }

  private FilterBuilder buildRangeFilter(JsonNode json, String key) {
    RangeFilterBuilder rangeFilter = FilterBuilders.rangeFilter(key);
    String range = mapper.convertValue(json.get(key), String.class);
    String[] parts = range.split("-");
    int from = parts[0].equals("") ? 0 : Integer.parseInt(parts[0].replaceAll(",", ""));
    rangeFilter.gte(from);
    if (parts.length == 2) {
      int to = Integer.parseInt(parts[1].replaceAll(",", ""));
      rangeFilter.lte(to);
    }
    return rangeFilter;
  }

  private FilterBuilder buildTermFilter(JsonNode json, String key) {
    if (json.get(key).isArray()) {
      ArrayList<String> terms = mapper.convertValue(json.get(key), new TypeReference<ArrayList<String>>() {});
      return FilterBuilders.termsFilter(key, terms);
    } else {
      String term = mapper.convertValue(json.get(key), String.class);
      return FilterBuilders.termFilter(key, term);
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
