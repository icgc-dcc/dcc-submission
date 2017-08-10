package org.icgc.dcc.submission.ega.metadata.repo.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.config.EGAMetadataConfig;
import org.icgc.dcc.submission.ega.metadata.repo.EGAMetadataRepo;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import rx.Observable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Copyright (c) 2017 The Ontario Institute for Cancer Research. All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * <p>
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
@Slf4j
@RequiredArgsConstructor
public class EGAMetadataRepoPostgres implements EGAMetadataRepo{

  @NonNull
  private EGAMetadataConfig.EGAMetadataPostgresqlConfig config;

  private String table_name_prefix = "ega_sample_mapping_";

  private String sql_create_table =
      "CREATE TABLE IF NOT EXISTS {table_name} ( " +
      "sample_id varchar(64), " +
      "file_id varchar(64), " +
      "PRIMARY KEY(sample_id, file_id) " +
      ");";

  private String sql_create_view = "CREATE OR REPLACE VIEW {view_name} AS SELECT * from {table_name}";

  private String sql_batch_insert = "INSERT INTO {table_name} VALUES(?, ?)";

  /**
   * every time the persis(...) function is triggered, create a new data table with a timestamp postfix on the table name
   * then update the view to point to the new table
   * any queries will be against the view instead of the tables
   *
   * @param data list of (sample_id, file_id) tuple
   */
  @Override
  public void persist(Observable<List<Pair<String, String>>> data) {

    String table_name = table_name_prefix + LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(-5));
    System.out.println(table_name);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(
        new DriverManagerDataSource("jdbc:postgresql://" + config.getHost() + "/" + config.getDatabase() + "?user=" + config.getUser() + "&password=" + config.getPassword())
    );

    jdbcTemplate.update(sql_create_table.replaceAll("\\{table_name\\}", table_name));

    String sql = sql_batch_insert.replaceAll("\\{table_name\\}", table_name);
    data.subscribe(list -> {
      jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
          Pair<String, String> pair = list.get(i);
          preparedStatement.setString(1, pair.getKey());
          preparedStatement.setString(2, pair.getValue());
        }

        @Override
        public int getBatchSize() {
          return list.size();
        }
      });
    });

    jdbcTemplate.execute(sql_create_view.replaceAll("\\{view_name\\}", config.getViewName()).replaceAll("\\{table_name\\}", table_name));

  }
}
