package org.icgc.dcc.submission.ega.metadata.config;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.download.EGAMetadataDownloader;
import org.icgc.dcc.submission.ega.metadata.download.impl.ShellScriptDownloader;
import org.icgc.dcc.submission.ega.metadata.extractor.DataExtractor;
import org.icgc.dcc.submission.ega.metadata.extractor.impl.EGASampleFileExtractor;
import org.icgc.dcc.submission.ega.metadata.repo.EGAMetadataRepo;
import org.icgc.dcc.submission.ega.metadata.repo.impl.EGAMetadataRepoPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

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
  @Value("${ega.metadata.cron}")
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
    return new ShellScriptDownloader(
        "ftp://" + ftpUser + ":" + ftpPassword + "@" + ftpHost + ftpPath,
        System.getProperty("java.io.tmpdir") + "ega/metadata",
        "ega/metadata/download_ega_metadata.sh"
    );

  }

  @Bean
  public DataExtractor<Pair<String, String>> extractor() {
    return new EGASampleFileExtractor();

  }

  @Bean
  @ConfigurationProperties(prefix = "ega.metadata.postgresql")
  public EGAMetadataPostgresqlConfig postgresqlConfig() {
    return new EGAMetadataPostgresqlConfig();
  }

  @Bean
  public EGAMetadataRepo repo() {
    return new EGAMetadataRepoPostgres(
        postgresqlConfig()
    );
  }
}
