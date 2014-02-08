/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.service;

import static org.icgc.dcc.hadoop.fs.HadoopUtils.lsFile;

import java.util.Date;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.model.Release;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class ReportService {

  @NonNull
  private final DccFileSystem dccFileSystem;
  @NonNull
  private final ReleaseService releaseService;

  public Report createInitialReport(@NonNull String projectKey) {
    val report = new Report();
    for (val submissionFile : getSubmissionFiles(projectKey)) {
      val fileName = submissionFile.getName();
      val fileType = SubmissionFileType.from(submissionFile.getDataType());
      val fileReport = new FileReport(fileName);

      report.addFileReport(fileType, fileReport);
    }

    return report;
  }

  private List<SubmissionFile> getSubmissionFiles(@NonNull String projectKey) {
    val release = getRelease();
    val dictionary = getDictionary();

    val submissionFiles = ImmutableList.<SubmissionFile> builder();
    val submissionPath = new Path(dccFileSystem.buildProjectStringPath(release.getName(), projectKey));

    for (val path : lsFile(dccFileSystem.getFileSystem(), submissionPath)) {
      val submissionFile = getSubmissionFile(dictionary, path);

      submissionFiles.add(submissionFile);
    }

    return submissionFiles.build();
  }

  private SubmissionFile getSubmissionFile(Dictionary dictionary, Path path) {
    val fileName = path.getName();
    val fileSchema = dictionary.getFileSchemaByFileName(fileName);
    val fileStatus = HadoopUtils.getFileStatus(dccFileSystem.getFileSystem(), path);
    val fileLastUpdate = new Date(fileStatus.getModificationTime());
    val fileSize = fileStatus.getLen();

    String schemaName = null;
    String dataType = null;
    if (fileSchema.isPresent()) {
      schemaName = fileSchema.get().getName();
      dataType = fileSchema.get().getDataType().name();
    } else {
      schemaName = null;
      dataType = null;
    }

    return new SubmissionFile(fileName, fileLastUpdate, fileSize, schemaName, dataType);
  }

  private Release getRelease() {
    return releaseService.getNextRelease();
  }

  private Dictionary getDictionary() {
    return releaseService.getNextDictionary();
  }

}
