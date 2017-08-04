package org.icgc.dcc.submission.validation.accession.ega;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.common.ega.model.EGAAccessionType;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.validation.accession.ega.download.EGAMetadataDownloader;
import org.icgc.dcc.submission.validation.accession.ega.download.impl.ShellScriptDownloader;
import org.icgc.dcc.submission.validation.accession.ega.extractor.impl.EGASampleFileExtractor;
import org.quartz.*;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkState;

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
public class EGAFileAccessionValidator {

  private static String defaultFtpConnectionUrl = "ftp://ega-box-138:tMHG3Uke@ftp-private.ebi.ac.uk/ICGC_metadata";
  private static String defaultMetaDataUpdateTrigger = "0 0 9,21 * * ?";

  private SubmissionProperties.EGAProperties properties;

  private Scheduler scheduler;

  private Cache<String, Set<String>> cache = null;

  private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  private EGAMetadataDownloader downloader = null;

  public EGAFileAccessionValidator(){
    this(defaultFtpConnectionUrl, defaultMetaDataUpdateTrigger);
  }

  public EGAFileAccessionValidator(String ftpConnectionUrl, String metaDataUpdateTrigger) {
    this(new SubmissionProperties.EGAProperties(ftpConnectionUrl, metaDataUpdateTrigger));
  }

  public EGAFileAccessionValidator(SubmissionProperties.EGAProperties properties){
    this.properties = properties;
    downloader = initializeMetadataDownloader();
  }

  private EGAMetadataDownloader initializeMetadataDownloader() {
    return new ShellScriptDownloader(this.properties.getFtpConnectionUrl(), System.getProperty("java.io.tmpdir") + "ega/metadata", "/metadata.sh");
  }

  private Scheduler initializeScheduler() {

    try {
      SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
      Scheduler sched = schedFact.getScheduler();
      sched.start();

      JobKey jobKey = new JobKey("metadata_job", "ega_validator");
      JobDataMap jobData = new JobDataMap();
      jobData.put("validator", this);
      JobDetail job = JobBuilder.newJob(MetadataUpdateJob.class).withIdentity(jobKey).setJobData(jobData).build();
      Trigger trigger = TriggerBuilder.newTrigger().withIdentity("metadata_trigger", "ega_validator").withSchedule(CronScheduleBuilder.cronSchedule(this.properties.getMetadataUpdateTrigger())).forJob(jobKey).startNow().build();

      sched.scheduleJob(job, trigger);

      sched.triggerJob(jobKey);

      return sched;
    } catch (SchedulerException e) {
      e.printStackTrace();
    }
    return null;

  }

  private Cache<String, Set<String>> initializeCache() {

    Optional<File> dataDir = this.downloader.download();
    if(!dataDir.isPresent())
      throw new RuntimeException("Downloading EGA metadata failed ...");

    Cache<String, Set<String>> ret =
      this.buildCache(
        parseSampleFiles(
            getSampleFiles(dataDir.get())
        )
    );

    try {
      FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpDir") + "/ega"));
    } catch (IOException e) {
      log.warn("Can't clean the downloading directory ...");
    }

    return ret;
  }

  private Observable<File> getSampleFiles(File dataDir){

    return
      Observable.from(
        dataDir.listFiles((dir, fileName) -> fileName.startsWith("EGA") )
      ).flatMap(dir -> {
        File mapFile = new File(dir.getAbsolutePath() + "/delimited_maps/Sample_File.map");
        if (mapFile.exists())
          return Observable.just(mapFile);
        else
          return Observable.empty();
      });

  }

  private Observable<Pair<String, String>> parseSampleFiles(Observable<File> sampleFiles) {
    EGASampleFileExtractor extractor = new EGASampleFileExtractor();
    return sampleFiles.flatMap(extractor::extract);
  }

  private Cache<String, Set<String>> buildCache(Observable<Pair<String, String>> rawData){

    Cache<String, Set<String>> inst = CacheBuilder.newBuilder().<String, Set<String>>build();
    rawData.groupBy(pair -> pair.getLeft(), pair -> pair.getRight()).subscribe(group -> {
      String key = group.getKey();
      Set<String> set = new HashSet<>();
      group.subscribe(ega_file_id -> {
        set.add(ega_file_id);
      });
      inst.put(key, set);
    });

    return inst;
  }

  public void start() {
    if(scheduler == null){
      scheduler = Preconditions.checkNotNull(initializeScheduler(), "failed to create the scheduler!");
    }
  }

  public Result validate(@NonNull String analyzedSampleId, String fileId){
    lock.readLock().lock();

    if(cache == null) {
      lock.readLock().unlock();
      return initializing();
    }

    checkFileAccession(fileId);

    Set<String> files = this.cache.getIfPresent(analyzedSampleId);
    if(files != null && files.contains(fileId)) {
      lock.readLock().unlock();
      return valid();
    }
    else{
      lock.readLock().unlock();
      return invalid("Could not match file to sample in: " + files);
    }
  }

  private static void checkFileAccession(String fileId) {
    val accessionType = EGAAccessionType.from(fileId);
    checkState(accessionType.isPresent(), "Could not detect accession type for value %s", fileId);
    checkState(accessionType.get().isFile(), "Accession type not file for value %s", fileId);
  }

  @Value
  public static class Result {

    boolean valid;
    String reason;

  }

  private static Result valid() {
    return new Result(true, null);
  }

  private static Result invalid(String reason) {
    return new Result(false, reason);
  }

  private static Result initializing() {
    return invalid("EGA file accession validator is initializing ... ");
  }

  public static class MetadataUpdateJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      log.info("Downloading metadata files is triggered at " + jobExecutionContext.getFireTime().toString());
      Date next = jobExecutionContext.getNextFireTime();
      EGAFileAccessionValidator validator = (EGAFileAccessionValidator) jobExecutionContext.getJobDetail().getJobDataMap().get("validator");
      Cache<String, Set<String>> newCache = validator.initializeCache();
      validator.lock.writeLock().lock();
      validator.cache = newCache;
      validator.lock.writeLock().unlock();
      log.info("Updating the metadata cache is finished ...");
      if(next != null) {
        log.info("Next downloading will be triggered at " + next.toString());
      }
    }
  }
}
