package org.icgc.dcc.submission.ega.test.metadata;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.config.EGAMetadataConfig;
import org.icgc.dcc.submission.ega.metadata.extractor.BadFormattedDataLogger;
import org.icgc.dcc.submission.ega.metadata.extractor.impl.EGAPostgresqlBadFormattedDataLogger;
import org.icgc.dcc.submission.ega.metadata.extractor.impl.EGASampleFileExtractor;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

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

public class EGASampleFileExtractorWithBadDataTest extends EGAMetadataResourcesProvider {

  private EGAMetadataConfig.EGAMetadataPostgresqlConfig config;

  private DriverManagerDataSource dataSource;

  private BadFormattedDataLogger badFormattedDataLogger;

  private String bad_data_path = "/tmp/submission/ega/test/EGAD56789/bad_data";

  @Test
  public void test_extract() {
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

    File dataDir = new File(bad_data_path);
    try {
      if(dataDir.exists()) {
        FileUtils.deleteDirectory(dataDir);
      }

      dataDir.mkdirs();

      FileOperationHelper.copyFileFromClasspathToTmpDir("/ega/metadata/sample","Sample_File_45_with_bad.map", bad_data_path);

      EGASampleFileExtractor extractor = new EGASampleFileExtractor(badFormattedDataLogger);
      List<Pair<String, String>> output = extractor.extract(new File(bad_data_path + "/Sample_File_45_with_bad.map"));

      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      List<Map<String, Object>> rets = jdbcTemplate.queryForList("select * from ega.bad_ega_sample_metadata where file_name = 'EGAD56789';");

      Assert.assertEquals(1, rets.size());

      int lineNo = Integer.parseInt( rets.get(0).get("line_number").toString() );
      String line = rets.get(0).get("line_content").toString();

      Assert.assertEquals(17, lineNo);
      Assert.assertTrue(line.contains("PD9180c-sc-eoirjsk"));
      Assert.assertTrue(line.equals("PD9180c-sc-eoirjsk\tEGAN00001196860\t12868_7#5.cram.cip"));

      Map<String, String> data = output.stream().collect(Collectors.toMap(pair->pair.getKey(), pair->pair.getValue()));

      assertEquals( data.get("PD12852b-sc-2013-08-02T01:56:51Z-1674528"), "EGAF00000406986");
      assertEquals( data.get("PD9179a-sc-1927644"), "EGAF00000604559");

      FileUtils.deleteDirectory(dataDir);

    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
