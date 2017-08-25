package org.icgc.dcc.submission.ega.metadata;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.download.EGAMetadataDownloader;
import org.icgc.dcc.submission.ega.metadata.extractor.DataExtractor;
import org.icgc.dcc.submission.ega.metadata.repo.EGAMetadataRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class EGAMetadataImporter {

  @NonNull
  private EGAMetadataDownloader downloader;

  @NonNull
  private DataExtractor<Pair<String, String>> extractor;

  @NonNull
  private EGAMetadataRepo repo;

  private EGAMetadataImporterStatus status = EGAMetadataImporterStatus.WAITING;

  @PostConstruct
  public void executeOnBooting() {
    ( new Thread(() -> {
      executePeriodically();
    }) ).start();

  }

  @Scheduled(cron = "${ega.metadata.cron.data}")
  public void executePeriodically() {

    synchronized (status){
      if (status == EGAMetadataImporterStatus.TRIGGERED)
        return;
      else
        status = EGAMetadataImporterStatus.TRIGGERED;
    }

    log.info("EGA data import is triggered on " + LocalDateTime.now(ZoneId.of("America/Toronto")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    Optional<File> dataDir = this.downloader.download();

    if(!dataDir.isPresent()) {
      throw new RuntimeException("Data directory which holds the downloaded data is not present at " + dataDir.get().getAbsolutePath());
    }

    this.persist(
        parseSampleFiles(
            getSampleFiles(dataDir.get())
        )
    );

    String tmpDir = System.getProperty("java.io.tmpdir") + "/ega";
    try {
      FileUtils.deleteDirectory(new File(tmpDir));
    } catch (IOException e) {
      log.warn("Failed to clean the downloading directory at " + tmpDir);
    }

    synchronized (status){
      status = EGAMetadataImporterStatus.WAITING;
    }


  }

  private Observable<File> getSampleFiles(File dataDir){

    File[] lstFile = dataDir.listFiles((dir, fileName) -> fileName.startsWith("EGA") );

    if (lstFile == null || lstFile.length == 0)
      return Observable.empty();

    return
        Observable.from(lstFile)
        .flatMap(dir -> {
          File mapFile = new File(dir.getAbsolutePath() + "/delimited_maps/Sample_File.map");
          if (mapFile.exists())
            return Observable.just(mapFile);
          else
            return Observable.empty();
        });

  }

  private Observable<List<Pair<String, String>>> parseSampleFiles(Observable<File> sampleFiles) {
    return sampleFiles.map(extractor::extract);

  }

  private void persist(Observable<List<Pair<String, String>>> rawData) {
    repo.persist(rawData);

  }

  /**
   *  every time executePeriodically() runs, a new sample metadata table is created and the view points to it
   *  so this function is used to clean the old tables
   *
   *  only save one-week data
   *
   */
  @Scheduled(cron = "${ega.metadata.cron.clean}")
  public void cleanHistoryData() {

    log.info("starting");

    repo.cleanHistoryData(
        LocalDateTime.now(ZoneId.of("America/Toronto")).atZone(ZoneId.of("America/Toronto")).toEpochSecond()
    );
  }

  public enum EGAMetadataImporterStatus {
    TRIGGERED,
    WAITING
  }

}
