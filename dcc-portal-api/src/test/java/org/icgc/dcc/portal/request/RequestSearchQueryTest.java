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

import static org.fest.assertions.api.Assertions.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class RequestSearchQueryTest {

  @Before
  public void setUp() {}

  @Test
  public final void test_Instantiation() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, null, 0, 0, "", "");
    assertThat(requestSearchQuery).isNotNull();
  }

  @Test
  public final void test_Size_WhenLessThanOne() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, null, 0, 0, "", "");
    assertThat(requestSearchQuery.getSize()).isEqualTo(RequestSearchQuery.DEFAULT_SIZE);
  }

  @Test
  public final void test_Size_WhenGreaterThanZero() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, null, 0, 1, "", "");
    assertThat(requestSearchQuery.getSize()).isEqualTo(1);
  }

  @Test
  public final void test_Size_WhenGreaterThanMaxSize() {
    RequestSearchQuery requestSearchQuery =
        new RequestSearchQuery(null, null, 0, RequestSearchQuery.MAX_SIZE + 1, "", "");
    assertThat(requestSearchQuery.getSize()).isEqualTo(RequestSearchQuery.MAX_SIZE);
  }

  @Test
  public final void test_From_WhenZero() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, null, 0, 0, "", "");
    assertThat(requestSearchQuery.getFrom()).isEqualTo(0);
  }

  @Test
  public final void test_From_WhenOne() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, null, 1, 0, "", "");
    assertThat(requestSearchQuery.getFrom()).isEqualTo(0);
  }

  @Test
  public final void test_From_WhenGreaterThanOne() {
    int from = 5;
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, null, from, 0, "", "");
    assertThat(requestSearchQuery.getFrom()).isEqualTo(from - 1);
  }

  @Test
  public final void test_Filters_WhenNull() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, null, 5, 0, "", "");
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{}");
  }

  @Test
  public final void test_Filters_WhenAnEmptyString() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery("", null, 0, 0, "", "");
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{}");
  }

  @Test
  public final void test_Filters_WhenWrappedInBrackets() {
    RequestSearchQuery requestSearchQuery =
        new RequestSearchQuery("{'key1':'value1', 'key2':'value2'}", null, 0, 0, "", "");
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":\"value1\",\"key2\":\"value2\"}");
  }

  @Test
  public final void test_Filters_WhenSingleQuoted() {
    RequestSearchQuery requestSearchQuery =
        new RequestSearchQuery("'key1':'value1', 'key2':'value2'", null, 0, 0, "", "");
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":\"value1\",\"key2\":\"value2\"}");
  }

  @Test
  public final void test_When_Filters_WhenDoubleQuoted() {
    RequestSearchQuery requestSearchQuery =
        new RequestSearchQuery("\"key1\":\"value1\", \"key2\":\"value2\"", null, 0, 0, "", "");
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":\"value1\",\"key2\":\"value2\"}");
  }

  @Test
  public final void test_Filters_WhenKeysAreUnQuoted() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery("key1:'value1', key2:'value2'", null, 0, 0, "", "");
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":\"value1\",\"key2\":\"value2\"}");
  }

  @Test
  public final void test_Filters_WhenValueIsArray() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery("key1:['value1', 'value2']", null, 0, 0, "", "");
    assertThat(requestSearchQuery.getFilters().toString()).isEqualTo("{\"key1\":[\"value1\",\"value2\"]}");
  }

  @Test(expected = javax.ws.rs.WebApplicationException.class)
  public final void test_Filters_FailsWithUnQuotedValues() {
    new RequestSearchQuery("{\"key1\":value1, \"key2\":value2}", null, 0, 0, "", "");
  }

  @Test
  public final void test_Fields_WhenNull() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, null, 0, 0, "", "");
    assertThat(requestSearchQuery.getFields()).isEqualTo(new String[] {});
  }

  @Test
  public final void test_Fields_WhenAnEmptyString() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, "", 0, 0, "", "");
    assertThat(requestSearchQuery.getFields()).isEqualTo(new String[] {});
  }

  @Test
  public final void test_Fields_WithOneField() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, "field1", 5, 0, "", "");
    assertThat(requestSearchQuery.getFields()).isEqualTo(new String[] {"field1"});
  }

  @Test
  public final void test_Fields_WithManyFields() {
    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(null, "field1, field2", 5, 0, "", "");
    assertThat(requestSearchQuery.getFields()).isEqualTo(new String[] {"field1", "field2"});
  }
}
