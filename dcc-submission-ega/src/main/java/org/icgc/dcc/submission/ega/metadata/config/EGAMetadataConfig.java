package org.icgc.dcc.submission.ega.metadata.config;

import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.config.DriverManagerDataSourceWrapper;
import org.icgc.dcc.submission.ega.metadata.download.EGAMetadataDownloader;
import org.icgc.dcc.submission.ega.metadata.download.impl.ShellScriptDownloader;
import org.icgc.dcc.submission.ega.metadata.extractor.BadFormattedDataLogger;
import org.icgc.dcc.submission.ega.metadata.extractor.DataExtractor;
import org.icgc.dcc.submission.ega.metadata.extractor.impl.EGASampleFileExtractor;
import org.icgc.dcc.submission.ega.metadata.extractor.impl.EGAPostgresqlBadFormattedDataLogger;
import org.icgc.dcc.submission.ega.metadata.repo.EGAMetadataRepo;
import org.icgc.dcc.submission.ega.metadata.repo.impl.EGAMetadataRepoPostgres;
import org.icgc.dcc.submission.ega.metadata.service.EGAMetadataService;
import org.icgc.dcc.submission.ega.metadata.service.impl.EGAMetadataServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

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

@Configuration
@EnableScheduling
public class EGAMetadataConfig {
  @Value("${ega.metadata.ftp.host}")
  String ftpHost;
  @Value("${ega.metadata.ftp.user}")
  String ftpUser;
  @Value("${ega.metadata.ftp.password}")
  String ftpPassword;
  @Value("${ega.metadata.ftp.path}")
  String ftpPath;
  @Value("${ega.metadata.cron.data}")
  String cron;

  @Data
  public static class EGAMetadataPostgresqlConfig {
    String host;
    String database;
    String user;
    String password;
    String viewName;
  }



  @Bean
  public EGAMetadataDownloader downloader() {

    String systemDir = System.getProperty("java.io.tmpdir");
    return new ShellScriptDownloader(
        "ftp://" + ftpUser + ":" + ftpPassword + "@" + ftpHost + ftpPath,
        (systemDir.endsWith("/")?systemDir.substring(0, systemDir.length()-1):systemDir) + "/ega/metadata",
        "/ega/metadata/download_ega_metadata.sh"
    );

  }

  @Bean
  public DataExtractor<Pair<String, String>> extractor(BadFormattedDataLogger logger) {
    return new EGASampleFileExtractor(logger);

  }

  @Bean
  @ConfigurationProperties(prefix = "ega.metadata.postgresql")
  @Scope("singleton")
  public EGAMetadataPostgresqlConfig postgresqlConfig() {
    return new EGAMetadataPostgresqlConfig();
  }

  @Bean
  public EGAMetadataRepo repo(EGAMetadataPostgresqlConfig config, @Qualifier("driverManagerDataSource") DriverManagerDataSourceWrapper dataSource) {
    return new EGAMetadataRepoPostgres(config, dataSource.getDataSource());

  }

  @Bean
  public EGAMetadataService egaMetadataService(EGAMetadataPostgresqlConfig config) {
    return new EGAMetadataServiceImpl(config);
  }

  @Bean
  public DriverManagerDataSourceWrapper driverManagerDataSource(EGAMetadataPostgresqlConfig config) {

    return
      new DriverManagerDataSourceWrapper(
        new DriverManagerDataSource(
      "jdbc:postgresql://" + config.getHost() + "/" + config.getDatabase() + "?user=" + config.getUser() + "&password=" + config.getPassword()
        )
      );

  }

  @Bean
  @Lazy
  public DataSource dataSource(EGAMetadataPostgresqlConfig config){
    return
        new DriverManagerDataSource(
            "jdbc:postgresql://" + config.getHost() + "/" + config.getDatabase() + "?user=" + config.getUser() + "&password=" + config.getPassword()
        );
  }

  @Bean
  public BadFormattedDataLogger badFormattedDataLogger(@Qualifier("driverManagerDataSource") DriverManagerDataSourceWrapper dataSource) {
    return new EGAPostgresqlBadFormattedDataLogger(dataSource.getDataSource());
  }
}
