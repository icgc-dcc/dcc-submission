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

package org.icgc.dcc.portal.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.tools.javac.util.List;
import org.elasticsearch.index.query.*;

import java.util.ArrayList;

public class FiltersService {

  private static ObjectMapper MAPPER = new ObjectMapper();

  public static NestedFilterBuilder buildNestedFilter(String key, AndFilterBuilder andFilters) {
    return FilterBuilders.nestedFilter(key, andFilters);
  }

  public static AndFilterBuilder craftMutationFilters(JsonNode filters) {
    AndFilterBuilder mutationAnd = FilterBuilders.andFilter();
    JsonNode mutation = filters.path("mutation");
    for (String key : List.of("project", "primary_site", "donor_id", "gender", "tumour", "vital_status",
        "disease_status", "donor_release_type")) {
      if (mutation.has(key)) {
        mutationAnd.add(buildTermFilter(mutation, key));
      }
    }
    if (mutation.has("location")) {
      mutationAnd.add(buildChrLocationFilter(mutation));
    }
    return mutationAnd;
  }

  public static AndFilterBuilder craftDonorFilters(JsonNode filters) {
    AndFilterBuilder donorAnd = FilterBuilders.andFilter();
    JsonNode donor = filters.path("donor");
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
    return donorAnd;
  }

  public static AndFilterBuilder craftGeneFilters(JsonNode filters) {
    AndFilterBuilder geneAnd = FilterBuilders.andFilter();
    JsonNode gene = filters;// .path("gene");

    for (String key : List.of("gene_type", "symbol")) {
      if (gene.has(key)) {
        geneAnd.add(buildTermFilter(gene, key));
      }
    }
    if (gene.has("gene_location")) {
      geneAnd.add(buildChrLocationFilter(gene));
    }
    return geneAnd;
  }

  private static FilterBuilder buildChrLocationFilter(JsonNode json) {
    FilterBuilder chrLocFilter;
    String LOCATION = "gene_location";

    if (json.get(LOCATION).isArray()) {
      ArrayList<String> locations = MAPPER.convertValue(json.get(LOCATION), new TypeReference<ArrayList<String>>() {});
      OrFilterBuilder manyChrLocations = FilterBuilders.orFilter();
      for (String loc : locations) {
        manyChrLocations.add(buildChrLocation(loc));
      }
      chrLocFilter = manyChrLocations;
    } else {
      String loc = MAPPER.convertValue(json.get(LOCATION), String.class);
      chrLocFilter = buildChrLocation(loc);
    }

    return chrLocFilter;
  }

  private static FilterBuilder buildChrLocation(String location) {
    AndFilterBuilder locationFilter = FilterBuilders.andFilter();
    String[] parts = location.split(":");
    locationFilter.add(FilterBuilders.termFilter("chromosome", Integer.parseInt(parts[0].replaceAll("[a-zA-Z]", ""))));
    if (parts.length == 2) {
      String[] range = parts[1].split("-");
      int start = range[0].equals("") ? 0 : Integer.parseInt(range[0].replaceAll(",", ""));
      locationFilter.add(FilterBuilders.numericRangeFilter("start").gte(start));
      if (range.length == 2) {
        int end = Integer.parseInt(range[1].replaceAll(",", ""));
        locationFilter.add(FilterBuilders.numericRangeFilter("end").lte(end));
      }
    }
    return locationFilter;
  }

  private static FilterBuilder buildRangeFilter(JsonNode json, String key) {
    NumericRangeFilterBuilder rangeFilter = FilterBuilders.numericRangeFilter(key);
    String range = MAPPER.convertValue(json.get(key), String.class);
    String[] parts = range.split("-");
    int from = parts[0].equals("") ? 0 : Integer.parseInt(parts[0].replaceAll(",", ""));
    rangeFilter.gte(from);
    if (parts.length == 2) {
      int to = Integer.parseInt(parts[1].replaceAll(",", ""));
      rangeFilter.lte(to);
    }
    return rangeFilter;
  }

  private static FilterBuilder buildTermFilter(JsonNode json, String key) {
    FilterBuilder termFilter;
    if (json.get(key).isArray()) {
      ArrayList<String> terms = MAPPER.convertValue(json.get(key), new TypeReference<ArrayList<String>>() {});
      termFilter = FilterBuilders.termsFilter(key, terms);
    } else {
      String term = MAPPER.convertValue(json.get(key), String.class);
      termFilter = FilterBuilders.termFilter(key, term);
    }
    return termFilter;
  }

}
