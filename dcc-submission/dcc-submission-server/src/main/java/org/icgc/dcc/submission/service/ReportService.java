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

import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.lsFile;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.SubmissionDataType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileState;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.model.Release;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class ReportService {

  @NonNull
  private final DccFileSystem dccFileSystem;
  @NonNull
  private final ReleaseService releaseService;

  public Report createInitialReport(@NonNull String projectKey) {
    val submissionFiles = getSubmissionFiles(projectKey);
    val fileReports = createFileReports(submissionFiles);
    val fileTypeReports = createFileTypeReports(fileReports);
    val dataTypeReports = createDataTypeReports(fileTypeReports);

    return createReport(dataTypeReports);
  }

  private Report createReport(List<DataTypeReport> dataTypeReports) {
    val report = new Report();
    report.setDataTypeReports(dataTypeReports);

    return report;
  }

  private List<DataTypeReport> createDataTypeReports(List<FileTypeReport> fileTypeReports) {
    val index = ImmutableMultimap.<SubmissionDataType, FileTypeReport> builder();
    for (val fileTypeReport : fileTypeReports) {
      val dataType = fileTypeReport.getFileType().getDataType();
      index.put(dataType, fileTypeReport);
    }

    val dataTypeReports = Lists.<DataTypeReport> newArrayList();
    val multimap = index.build();
    for (val dataType : multimap.keySet()) {
      val dataTypeReport = createDataTypeReport(dataType, multimap.get(dataType));

      dataTypeReports.add(dataTypeReport);
    }

    return dataTypeReports;
  }

  private DataTypeReport createDataTypeReport(SubmissionDataType dataType, Collection<FileTypeReport> fileTypeReports) {
    val dataTypeReport = new DataTypeReport();
    dataTypeReport.setDataType(dataType);
    dataTypeReport.setFileTypeReports(newArrayList(fileTypeReports));

    return dataTypeReport;
  }

  private List<FileTypeReport> createFileTypeReports(List<FileReport> fileReports) {
    val dictionary = getDictionary();

    val index = ImmutableMultimap.<SubmissionFileType, FileReport> builder();
    for (val fileReport : fileReports) {
      val schema = dictionary.getFileSchemaByFileName(fileReport.getFileName()).get();
      val fileType = schema.getFileType();

      index.put(fileType, fileReport);
    }

    val fileTypeReports = Lists.<FileTypeReport> newArrayList();
    val multimap = index.build();
    for (val fileType : multimap.keySet()) {
      val fileTypeReport = createFileTypeReport(fileType, multimap.get(fileType));

      fileTypeReports.add(fileTypeReport);
    }

    return fileTypeReports;
  }

  private FileTypeReport createFileTypeReport(SubmissionFileType fileType, Collection<FileReport> reports) {
    val fileTypeReport = new FileTypeReport();
    fileTypeReport.setFileType(fileType);
    fileTypeReport.setFileReports(newArrayList(reports));

    return fileTypeReport;
  }

  private List<FileReport> createFileReports(List<SubmissionFile> submissionFiles) {
    val fileReports = Lists.<FileReport> newArrayList();
    for (val submissionFile : submissionFiles) {
      val fileReport = createFileReport(submissionFile);

      fileReports.add(fileReport);
    }

    return fileReports;
  }

  private FileReport createFileReport(SubmissionFile submissionFile) {
    val fileReport = new FileReport();
    fileReport.setFileName(submissionFile.getName());
    fileReport.setFileState(FileState.NOT_VALIDATED);

    return fileReport;
  }

  private List<SubmissionFile> getSubmissionFiles(String projectKey) {
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
