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

package org.icgc.dcc.portal.request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.WebApplicationException;

import static org.fest.assertions.api.Assertions.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class RequestSearchQueryTest {

  @Before
  public void setUp() {}

  @Test
  public final void test_Instantiation() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().build();
    assertThat(requestSearchQuery).isNotNull();
  }

  @Test
  public final void test_Size_WhenLessThanOne() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().size(0).build();
    assertThat(requestSearchQuery.getSize()).isEqualTo(RequestSearchQuery.DEFAULT_SIZE);
  }

  @Test
  public final void test_Size_WhenGreaterThanZero() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().size(1).build();
    assertThat(requestSearchQuery.getSize()).isEqualTo(1);
  }

  @Test
  public final void test_Size_WhenGreaterThanMaxSize() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().size(RequestSearchQuery.MAX_SIZE + 1).build();
    assertThat(requestSearchQuery.getSize()).isEqualTo(RequestSearchQuery.MAX_SIZE);
  }

  @Test
  public final void test_From_WhenZero() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().from(0).build();
    assertThat(requestSearchQuery.getFrom()).isEqualTo(0);
  }

  @Test
  public final void test_From_WhenOne() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().from(1).build();
    assertThat(requestSearchQuery.getFrom()).isEqualTo(0);
  }

  @Test
  public final void test_From_WhenGreaterThanOne() {
    int from = 5;
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().from(5).build();
    assertThat(requestSearchQuery.getFrom()).isEqualTo(from - 1);
  }

  @Test
  public final void test_Filters_WhenNull() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().filters(null).build();
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{}");
  }

  @Test
  public final void test_Filters_WhenAnEmptyString() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().filters("").build();
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{}");
  }

  @Test
  public final void test_Filters_WhenWrappedInBrackets() {
    RequestSearchQuery requestSearchQuery =
        RequestSearchQuery.builder().filters("{'key1':'value1', 'key2':'value2'}").build();
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":\"value1\",\"key2\":\"value2\"}");
  }

  @Test
  public final void test_Filters_WhenSingleQuoted() {
    RequestSearchQuery requestSearchQuery =
        RequestSearchQuery.builder().filters("'key1':'value1', 'key2':'value2'").build();
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":\"value1\",\"key2\":\"value2\"}");
  }

  @Test
  public final void test_When_Filters_WhenDoubleQuoted() {
    RequestSearchQuery requestSearchQuery =
        RequestSearchQuery.builder().filters("\"key1\":\"value1\", \"key2\":\"value2\"").build();
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":\"value1\",\"key2\":\"value2\"}");
  }

  @Test
  public final void test_Filters_WhenKeysAreUnQuoted() {
    RequestSearchQuery requestSearchQuery =
        RequestSearchQuery.builder().filters("key1:'value1', key2:'value2'").build();
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":\"value1\",\"key2\":\"value2\"}");
  }

  @Test
  public final void test_Filters_WhenValueIsArray() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().filters("key1:['value1', 'value2']").build();
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":[\"value1\",\"value2\"]}");
  }

  @Test(expected = WebApplicationException.class)
  public final void test_Filters_FailsWithUnQuotedValues() {
    RequestSearchQuery.builder().filters("{\"key1\":value1, \"key2\":value2}").build();
  }

  @Test
  public final void test_Fields_WhenNull() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().fields(null).build();
    assertThat(requestSearchQuery.getFields()).isEqualTo(new String[] {});
  }

  @Test
  public final void test_Fields_WhenAnEmptyString() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().fields("").build();
    assertThat(requestSearchQuery.getFields()).isEqualTo(new String[] {});
  }

  @Test
  public final void test_Fields_WithOneField() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().fields("field1").build();
    assertThat(requestSearchQuery.getFields()).isEqualTo(new String[] {"field1"});
  }

  @Test
  public final void test_Fields_WithManyFields() {
    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder().fields("field1,field2").build();
    assertThat(requestSearchQuery.getFields()).isEqualTo(new String[] {"field1", "field2"});
  }
}
