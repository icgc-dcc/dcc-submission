package org.icgc.dcc.submission.ega.metadata.extractor.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icgc.dcc.submission.ega.metadata.config.EGAMetadataConfig;
import org.icgc.dcc.submission.ega.metadata.extractor.BadFormattedDataLogger;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
public class EGAPostgresqlBadFormattedDataLogger implements BadFormattedDataLogger{

  @NonNull
  private DriverManagerDataSource dataSource;

  private String bad_data_table_name = "bad_ega_sample_metadata";

  private String sql_create_table = "CREATE TABLE IF NOT EXISTS ega." + bad_data_table_name + " (" +
      "timestamp bigint, " +
      "file_name varchar(64), " +
      "line_number int, " +
      "line_content varchar(128)" +
      ");";
  private String sql_batch_insert = "INSERT INTO ega." + bad_data_table_name + " VALUES (?, ?, ?, ?);";

  @Override
  public void log(List<BadFormattedData> data) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update(sql_create_table);

    jdbcTemplate.batchUpdate(sql_batch_insert, new BatchPreparedStatementSetter() {
      @Override
      public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
        BadFormattedData badData = data.get(i);
        preparedStatement.setLong(1, badData.timestamp);
        preparedStatement.setString(2, badData.fileName);
        preparedStatement.setInt(3, badData.lineNo);
        preparedStatement.setString(4, badData.lineContent);
      }

      @Override
      public int getBatchSize() {
        return data.size();
      }
    });
  }
}
