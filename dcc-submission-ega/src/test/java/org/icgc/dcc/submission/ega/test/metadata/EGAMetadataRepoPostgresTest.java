package org.icgc.dcc.submission.ega.test.metadata;

import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.config.EGAMetadataConfig;
import org.icgc.dcc.submission.ega.metadata.repo.EGAMetadataRepo;
import org.icgc.dcc.submission.ega.metadata.repo.impl.EGAMetadataRepoPostgres;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import rx.Observable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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

public class EGAMetadataRepoPostgresTest extends EGAMetadataResourcesProvider {

  @Test
  public void test_persist() {

    EGAMetadataConfig.EGAMetadataPostgresqlConfig config = new EGAMetadataConfig.EGAMetadataPostgresqlConfig();
    config.setHost("localhost:5435");
    config.setDatabase("ICGC_metadata");
    config.setUser("sa");
    config.setPassword("");
    config.setViewName("view_ega_sample_mapping");

    DriverManagerDataSource dataSource = new DriverManagerDataSource(
        "jdbc:postgresql://" + config.getHost() + "/" + config.getDatabase() + "?user=" + config.getUser() + "&password=" + config.getPassword()
    );

    EGAMetadataRepo repo = new EGAMetadataRepoPostgres(config, dataSource);

    List<Pair<String, String>> data = new ArrayList<>();
    Set<String> keySet = new HashSet<>();
    Set<String> valueSet = new HashSet<>();

    for(int i = 0;i<100;i++){
      data.add(Pair.of("sample_id_" + i, "file_id_"+i));
      data.add(Pair.of("sample_id_" + i, "file_id_"+i + "" + i));
      keySet.add("sample_id_" +i);
      valueSet.add("file_id_" + i);
      valueSet.add("file_id_" + i + "" + i);
    }

    repo.persist(Observable.just(Pair.of("dataset-1", data)));

    JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://localhost:5435/ICGC_metadata?user=sa&password="));

    List<Map<String, Object>> ret = jdbcTemplate.queryForList("select * from ega.view_ega_sample_mapping;");

    Assert.assertEquals(200, ret.size());


    ret.stream().forEach(map -> {

      Assert.assertTrue( keySet.contains(map.get("sample_id")) );
      Assert.assertTrue( valueSet.contains(map.get("file_id")) );
      Assert.assertTrue( map.get("dataset_id").equals("dataset-1"));

    });


  }

  @Test
  public void test_cleanHistoryData() {
    EGAMetadataConfig.EGAMetadataPostgresqlConfig config = new EGAMetadataConfig.EGAMetadataPostgresqlConfig();
    config.setHost("localhost:5435");
    config.setDatabase("ICGC_metadata");
    config.setUser("sa");
    config.setPassword("");
    config.setViewName("view_ega_sample_mapping");

    DriverManagerDataSource dataSource = new DriverManagerDataSource(
        "jdbc:postgresql://" + config.getHost() + "/" + config.getDatabase() + "?user=" + config.getUser() + "&password=" + config.getPassword()
    );

    EGAMetadataRepo repo = new EGAMetadataRepoPostgres(config, dataSource);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://localhost:5435/ICGC_metadata?user=sa&password="));

    jdbcTemplate.execute("create schema if not exists \"ega\";");

    String sql = "CREATE TABLE IF NOT EXISTS ega.{table_name} ( " +
        "sample_id varchar(64), " +
        "file_id varchar(64), " +
        "dataset_id varchar(64)" +
        ");";
    jdbcTemplate.execute(sql.replaceAll("\\{table_name\\}", "ega_sample_mapping_100"));
    jdbcTemplate.execute(sql.replaceAll("\\{table_name\\}", "ega_sample_mapping_110"));
    jdbcTemplate.execute(sql.replaceAll("\\{table_name\\}", "ega_sample_mapping_150"));
    jdbcTemplate.execute(sql.replaceAll("\\{table_name\\}", "ega_sample_mapping_200"));
    jdbcTemplate.execute(sql.replaceAll("\\{table_name\\}", "ega_sample_mapping_300"));
    jdbcTemplate.execute(sql.replaceAll("\\{table_name\\}", "ega_sample_mapping_500"));

    repo.cleanHistoryData(250);

    sql = "select table_name from information_schema.tables where table_schema = 'ega' and table_name like 'ega_sample_mapping_%';";

    jdbcTemplate.query(sql, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        while(resultSet.next()) {
          String table_name = resultSet.getString(1);
          long timstamp = Long.parseLong(table_name.substring(table_name.lastIndexOf("_") + 1));
          Assert.assertTrue(timstamp == 300 || timstamp == 500);
        }
      }
    });

  }
}
