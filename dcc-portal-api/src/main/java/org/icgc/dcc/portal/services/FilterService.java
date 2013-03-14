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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.index.query.*;

import java.util.ArrayList;
import java.util.List;

import static org.icgc.dcc.portal.core.JsonUtils.MAPPER;

public class FilterService {

  private static final TypeReference<ArrayList<String>> TYPE_REF = new TypeReference<ArrayList<String>>() {};

  public static NestedFilterBuilder buildNestedFilter(String key, AndFilterBuilder andFilters) {
    return FilterBuilders.nestedFilter(key, andFilters);
  }

  public static AndFilterBuilder buildFilters(ImmutableMap<String, ImmutableList<String>> modelFilters, JsonNode filters) {
    AndFilterBuilder andFilter = FilterBuilders.andFilter();
    for (String key : modelFilters.get("terms")) {
      if (filters.has(key)) {
        andFilter.add(buildTermFilter(filters, key));
      }
    }
    for (String key : modelFilters.get("locations")) {
      if (filters.has(key)) {
        andFilter.add(buildChrLocationFilter(filters, key));
      }
    }
    for (String key : modelFilters.get("ranges")) {
      if (filters.has(key)) {
        andFilter.add(buildRangeFilter(filters, key));
      }
    }
    return andFilter;
  }

  private static FilterBuilder buildChrLocationFilter(JsonNode json, String location) {
    FilterBuilder chrLocFilter;

    if (json.get(location).isArray()) {
      ArrayList<String> locations = MAPPER.convertValue(json.get(location), new TypeReference<ArrayList<String>>() {});
      OrFilterBuilder manyChrLocations = FilterBuilders.orFilter();
      for (String loc : locations) {
        manyChrLocations.add(parseChrLocation(loc));
      }
      chrLocFilter = manyChrLocations;
    } else {
      String loc = MAPPER.convertValue(json.get(location), String.class);
      chrLocFilter = parseChrLocation(loc);
    }

    return chrLocFilter;
  }

  private static FilterBuilder parseChrLocation(String location) {
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
      List<String> terms = MAPPER.convertValue(json.get(key), TYPE_REF);
      termFilter = FilterBuilders.termsFilter(key, terms);
    } else {
      String term = MAPPER.convertValue(json.get(key), String.class);
      termFilter = FilterBuilders.termFilter(key, term);
    }
    return termFilter;
  }

}
