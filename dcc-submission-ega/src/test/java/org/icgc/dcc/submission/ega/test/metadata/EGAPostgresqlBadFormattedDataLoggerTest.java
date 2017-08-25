package org.icgc.dcc.submission.ega.test.metadata;

import org.icgc.dcc.submission.ega.metadata.config.EGAMetadataConfig;
import org.icgc.dcc.submission.ega.metadata.extractor.BadFormattedDataLogger;
import org.icgc.dcc.submission.ega.metadata.extractor.impl.EGAPostgresqlBadFormattedDataLogger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

public class EGAPostgresqlBadFormattedDataLoggerTest extends EGAMetadataResourcesProvider{

  private EGAMetadataConfig.EGAMetadataPostgresqlConfig config;

  private DriverManagerDataSource dataSource;

  private BadFormattedDataLogger badFormattedDataLogger;


  @Test
  public void test_log() {

    config = new EGAMetadataConfig.EGAMetadataPostgresqlConfig();
    config.setHost("localhost:5435");
    config.setDatabase("ICGC_metadata");
    config.setUser("sa");
    config.setPassword("");
    config.setViewName("view_ega_sample_mapping");

    dataSource = new DriverManagerDataSource(
        "jdbc:postgresql://" + config.getHost() + "/" + config.getDatabase() + "?user=" + config.getUser() + "&password=" + config.getPassword()
    );

    badFormattedDataLogger = new EGAPostgresqlBadFormattedDataLogger(dataSource);
    List<BadFormattedDataLogger.BadFormattedData> data = new ArrayList<>();
    data.add(new BadFormattedDataLogger.BadFormattedData("EGAF00009", "bad_1", 100, 1234567L));
    data.add(new BadFormattedDataLogger.BadFormattedData("EGAF00010", "bad_50", 200, 346334L));
    data.add(new BadFormattedDataLogger.BadFormattedData("EGAF000133", "bad_432", 300, 1234567L));
    data.add(new BadFormattedDataLogger.BadFormattedData("EGAF000i45287", "bad_760", 400, 875093L));

    badFormattedDataLogger.log(data);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    List<Map<String, Object>> rets = jdbcTemplate.queryForList("select * from ega.bad_ega_sample_metadata where timestamp = 1234567 and file_name = 'EGAF00009' and line_number = 100 and line_content = 'bad_1' ;");
    Assert.assertEquals(1, rets.size());
    rets = jdbcTemplate.queryForList("select * from ega.bad_ega_sample_metadata where timestamp = 1234567;");
    Assert.assertEquals(2, rets.size());
  }
}
