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
package org.icgc.dcc.submission.loader.file;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;

import org.icgc.dcc.submission.loader.db.DatabaseService;
import org.icgc.dcc.submission.loader.model.FileTypePath;
import org.icgc.dcc.submission.loader.model.Project;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ReleaseFilesLoader implements Closeable {

  /**
   * Configuration.
   */
  @NonNull
  protected final String release;

  /**
   * Dependencies.
   */
  @NonNull
  protected final DatabaseService databaseService;
  @NonNull
  protected final FileLoaderFactory fileLoaderFactory;
  @NonNull
  protected final CompletionService<Void> completionService;

  public void prepareDb(Iterable<Project> projects) {
    databaseService.initializeDb(release, projects);
  }

  @SneakyThrows
  public void loadFiles(Map<String, List<FileTypePath>> files) {
    val tasksCount = getTasksCount(files);
    int completedTasks = 0;
    submitTasks(files);

    while (tasksCount > completedTasks) {
      val result = completionService.take();
      result.get();
      completedTasks++;
      log.info("[{}/{}] tasks finished.", completedTasks, tasksCount);
    }
  }

  public void finalizeDatabase() {
    databaseService.finalizeDb(release);
  }

  @Override
  public void close() throws IOException {

  }

  private void submitTasks(Map<String, List<FileTypePath>> projectFiles) {
    for (val entry : projectFiles.entrySet()) {
      val project = entry.getKey();
      val files = entry.getValue();
      for (val file : files) {
        val documentLoader = fileLoaderFactory.createFileLoader(project, release, file);
        completionService.submit(documentLoader);
      }
    }
  }

  private static long getTasksCount(Map<String, List<FileTypePath>> files) {
    return files.entrySet().stream()
        .flatMap(e -> e.getValue().stream())
        .count();
  }

}
