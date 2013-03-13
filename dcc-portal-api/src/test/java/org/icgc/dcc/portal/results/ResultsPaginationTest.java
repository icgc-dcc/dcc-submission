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

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class ResultsPaginationTest {

  @Mock
  RequestSearchQuery requestSearchQuery;

  @Mock
  SearchHits searchHits;

  @Before
  public void setUp() throws Exception {}

  @Test
  public final void test_Page_WhenFromLessThanSize() {
    // Just to deal with NullPointers
    when(searchHits.getHits()).thenReturn(new SearchHit[10]);
    when(requestSearchQuery.getOrder()).thenReturn("asc");

    // 10 results per page...
    when(requestSearchQuery.getSize()).thenReturn(10);
    // 5 results in...
    when(requestSearchQuery.getFrom()).thenReturn(5);

    ResultsPagination resultsPagination = new ResultsPagination(searchHits, requestSearchQuery);

    // ... so should be on page 1
    assertThat(resultsPagination.getPage()).isEqualTo(1);
  }

  @Test
  public final void test_Page_WhenFromEqualsSize() {
    // Just to deal with NullPointers
    when(searchHits.getHits()).thenReturn(new SearchHit[10]);
    when(requestSearchQuery.getOrder()).thenReturn("asc");

    // 10 results per page...
    when(requestSearchQuery.getSize()).thenReturn(10);
    // 10 results in...
    when(requestSearchQuery.getFrom()).thenReturn(10);

    ResultsPagination resultsPagination = new ResultsPagination(searchHits, requestSearchQuery);

    // ... so should be on page 2
    assertThat(resultsPagination.getPage()).isEqualTo(2);
  }

  @Test
  public final void test_Page_WhenFromGreaterThanSize() {
    // Just to deal with NullPointers
    when(searchHits.getHits()).thenReturn(new SearchHit[10]);
    when(requestSearchQuery.getOrder()).thenReturn("asc");

    // 10 results per page...
    when(requestSearchQuery.getSize()).thenReturn(10);
    // 50 results in...
    when(requestSearchQuery.getFrom()).thenReturn(50);

    ResultsPagination resultsPagination = new ResultsPagination(searchHits, requestSearchQuery);

    // ... so should be on page 6
    assertThat(resultsPagination.getPage()).isEqualTo(6);
  }

  @Test
  public final void test_Pages_WhenTotalZero() {
    // Just to deal with NullPointers
    when(searchHits.getHits()).thenReturn(new SearchHit[10]);
    when(requestSearchQuery.getOrder()).thenReturn("asc");

    // 10 results per page...
    when(requestSearchQuery.getSize()).thenReturn(10);
    // 0 total results
    when(searchHits.getTotalHits()).thenReturn(1L);

    ResultsPagination resultsPagination = new ResultsPagination(searchHits, requestSearchQuery);

    // ... so should have 1 pages
    assertThat(resultsPagination.getPages()).isEqualTo(1);
  }

  @Test
  public final void test_Pages_WhenTotalEqualToSize() {
    // Just to deal with NullPointers
    when(searchHits.getHits()).thenReturn(new SearchHit[10]);
    when(requestSearchQuery.getOrder()).thenReturn("asc");

    // 10 results per page...
    when(requestSearchQuery.getSize()).thenReturn(10);
    // 10 total results
    when(searchHits.getTotalHits()).thenReturn(10L);

    ResultsPagination resultsPagination = new ResultsPagination(searchHits, requestSearchQuery);

    // ... so should have 1 pages
    assertThat(resultsPagination.getPages()).isEqualTo(1);
  }

  @Test
  public final void test_Pages_WhenTotalGreaterThanSize() {
    // Just to deal with NullPointers
    when(searchHits.getHits()).thenReturn(new SearchHit[10]);
    when(requestSearchQuery.getOrder()).thenReturn("asc");

    // 10 results per page...
    when(requestSearchQuery.getSize()).thenReturn(10);
    // 1001 total results
    when(searchHits.getTotalHits()).thenReturn(1001L);

    ResultsPagination resultsPagination = new ResultsPagination(searchHits, requestSearchQuery);

    // ... so should have 101 pages
    assertThat(resultsPagination.getPages()).isEqualTo(101);
  }
}
