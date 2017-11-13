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
package org.icgc.dcc.submission.ega.imports;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.comparing;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.icgc.dcc.common.core.mail.Mailer;
import org.icgc.dcc.common.ega.dump.EGAMetadataDumpAnalyzer;
import org.icgc.dcc.common.ega.dump.EGAMetadataDumper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class EGAImporter {

  /**
   * Configuration.
   */
  @Value("${workspace.dir}")
  File workspaceDir;
  @Value("${importer.retainCount}")
  int retainCount;

  /**
   * Dependencies
   */
  @Autowired
  Mailer mailer;
  @Autowired
  EGAMetadataDumper dumper;

  /**
   * State.
   */
  final boolean enabled = true;
  volatile boolean running;

  public boolean isRunning() {
    return running;
  }

  @Async
  public void execute() {
    log.info("Running...");
    run();
  }

  @Scheduled(cron = "${importer.cron}")
  public void executePeriodically() {
    if (!enabled) {
      log.info("Cron disabled, skipping...");
      return;
    }

    run();
  }

  private void run() {
    if (running) {
      log.info("Alreading running, skipping...");
      return;
    }

    running = true;
    val watch = Stopwatch.createStarted();

    val date = formatDate();
    val dumpFile = getDumpFile(date);
    val reportFile = getReportFile(date);

    try {
      if (!workspaceDir.exists()) {
        log.info("Creating workspace...");
        checkState(workspaceDir.mkdirs(), "Could not create workspace dir %s", workspaceDir);
      }

      {
        log.info("Writing dump file to: {}", dumpFile.getAbsolutePath());
        createDump(dumpFile);
        relinkFile(dumpFile, "icgc-ega-dump.jsonl");
        pruneFiles("icgc-ega-dump");
      }
      {
        log.info("Reading dump file from: {}", dumpFile.getAbsolutePath());
        log.info("Writing report file to: {}", reportFile.getAbsolutePath());
        analyzeDump(dumpFile, reportFile);
        relinkFile(reportFile, "icgc-ega-report.jsonl");
        pruneFiles("icgc-ega-report");
      }
    } catch (Exception e) {
      log.error("Error importing EGA: {}", e);
      mailer.sendMail("EGA Failure Importing - " + date, e.getMessage());

      return;
    } finally {
      running = false;
    }

    log.info("Finished in {}", watch);
    mailer.sendMail("EGA Success Importing - " + date, "Finished in " + watch);
  }

  private void relinkFile(File file, String symlink) throws IOException {
    val reportSymlink = new File(workspaceDir, symlink);
    if (reportSymlink.exists()) {
      reportSymlink.delete();
    }

    Files.createSymbolicLink(reportSymlink.toPath(), file.toPath());
  }

  private void createDump(File dumpFile) {
    dumper.create(dumpFile);
  }

  private void analyzeDump(File dumpFile, File reportFile) throws FileNotFoundException {
    @Cleanup
    val writer = new PrintWriter(reportFile);
    val analyzer = new EGAMetadataDumpAnalyzer(writer);
    analyzer.analyze(dumpFile);
  }

  private void pruneFiles(String fileName) {
    val files = getFiles(fileName);

    for (int i = 0; i < files.size() - retainCount; i++) {
      val file = files.get(i);
      log.info("Deleting {}...", file);
      file.delete();
    }
  }

  private File getDumpFile(String date) {
    return getFile("icgc-ega-dump", date);
  }

  private File getReportFile(String date) {
    return getFile("icgc-ega-report", date);
  }

  @SneakyThrows
  private List<File> getFiles(String fileName) {
    return Files
        .list(workspaceDir.toPath())
        .map(Path::toFile)
        .filter(file -> file.getName().matches(fileName + "\\.[^.]+\\.jsonl"))
        .sorted(comparing(File::getName))
        .collect(toImmutableList());
  }

  private File getFile(String fileName, String date) {
    return new File(workspaceDir, fileName + "." + date + ".jsonl");
  }

  private static String formatDate() {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now());
  }

}
