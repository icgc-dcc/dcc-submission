/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You shou* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.responses;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import org.icgc.dcc.search.RequestedSearch;

@Data
@EqualsAndHashCode(callSuper = false)
public class ManyResponse extends BaseResponse {
  private final ImmutableList<JsonNode> data;

  private final Pagination pagination;

  public ManyResponse(final SearchHits hits, final HttpServletRequest httpServletRequest,
      RequestedSearch requestedSearch) {
    super(httpServletRequest);
    this.data = extractData(hits.getHits());
    this.pagination = new Pagination(hits, requestedSearch);
  }

  private ImmutableList<JsonNode> extractData(final SearchHit... hits) {
    ObjectMapper mapper = new ObjectMapper();
    ImmutableList.Builder<JsonNode> list = ImmutableList.<JsonNode>builder();
    for (SearchHit hit : hits) {
      JsonNode node;
      try {
        node = mapper.readValue(hit.getSourceAsString(), JsonNode.class);
      } catch (IOException e) {
        ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, e);
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(errorResponse)
            .type(MediaType.APPLICATION_JSON_TYPE).build());
      }
      list.add(node);
    }
    return list.build();
  }

  @Data
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private static final class Pagination {
    private final int count;

    private final long total;

    private final int size;

    private final int from;

    private final double page;

    private final double pages;

    private final String sort;

    private final String order;

    public Pagination(final SearchHits hits, RequestedSearch requestedSearch) {
      this.count = hits.getHits().length;
      this.total = hits.getTotalHits();
      this.size = requestedSearch.getSize();
      this.from = requestedSearch.getFrom() + 1;
      this.sort = requestedSearch.getSort();
      this.order = requestedSearch.getOrder();
      this.page = floor(from / size) + 1;
      this.pages = ceil(total / size);
    }
  }
}
