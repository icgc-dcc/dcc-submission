/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.loader.file.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import lombok.Cleanup;
import lombok.val;

import org.icgc.dcc.submission.loader.meta.CodeListValuesDecoder;
import org.icgc.dcc.submission.loader.record.PostgressRecordConverter;
import org.icgc.dcc.submission.loader.record.RecordReader;
import org.icgc.dcc.submission.loader.util.AbstractPostgressTest;
import org.icgc.dcc.submission.loader.util.DatabaseFields;
import org.icgc.dcc.submission.loader.util.Readers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class PostgressFileLoaderTest extends AbstractPostgressTest {

  private static final String PROJECT = "ALL-US";
  private static final String TYPE = "donor";
  private static final String SCHEMA = "icgc20";

  private static final List<String> FIELD_NAMES = ImmutableList.<String> builder()
      .add("id")
      .add("sex")
      .add("age")
      .build();

  @Mock
  CodeListValuesDecoder codeListDecoder;
  PostgressFileLoader fileLoader;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    when(codeListDecoder.decode(anyString(), anyString())).then(invocation -> invocation.getArguments()[1]);
  }

  @Test
  public void testLoadFile() throws Exception {
    prepareDb();

    val stringBuilder = Readers.getStringBuilder(FIELD_NAMES);
    stringBuilder.append(Readers.createLine("1", "male", "age"));
    val srcString = stringBuilder.toString();
    val recordReader = new RecordReader(Readers.getReader(srcString));

    val jdbcTemplate = new SimpleJdbcInsert(dataSource)
        .withSchemaName(SCHEMA)
        .withTableName(TYPE);

    @Cleanup
    val fileLoader =
        new PostgressFileLoader(PROJECT, TYPE, recordReader, jdbcTemplate, new PostgressRecordConverter(PROJECT,
            codeListDecoder));
    fileLoader.call();

    verifyDb();
  }

  private void prepareDb() {
    val sqlBuilder = new StringBuilder();
    sqlBuilder.append("CREATE TABLE " + getTableName() + " (");
    sqlBuilder.append("id varchar(500),");
    sqlBuilder.append("sex varchar(500),");
    sqlBuilder.append("age varchar(500),");
    sqlBuilder.append(DatabaseFields.PROJECT_ID_FIELD_NAME + " varchar(500))");

    jdbcTemplate.execute("CREATE SCHEMA " + SCHEMA);
    jdbcTemplate.execute(sqlBuilder.toString());
  }

  private void verifyDb() {
    val count = jdbcTemplate.queryForObject("SELECT count(*) from " + getTableName(), Integer.class);
    assertThat(count).isEqualTo(1);
  }

  private static String getTableName() {
    return SCHEMA + "." + TYPE;
  }

}
