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
package org.icgc.dcc.submission.validation.core;

import static java.util.regex.Pattern.matches;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.ClinicalType;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategyFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * The "default" implementation of the {@link ValidationContext}.
 */
@Slf4j
@RequiredArgsConstructor
@ToString
public class DefaultValidationContext implements ValidationContext {

  /**
   * Fulfills the {@link ReportContext} contract via delegation.
   */
  @Delegate
  @NonNull
  ReportContext reportContext;

  /**
   * Supports the non-inherited {@link ValidationContext} contract.
   */
  @NonNull
  private final String projectKey;
  @NonNull
  private final List<String> emails;
  @NonNull
  private final List<DataType> dataTypes;
  @NonNull
  private final Release release;
  @NonNull
  private final Map<String, Submission> submissions;
  @NonNull
  private final Dictionary dictionary;
  @NonNull
  private final List<CodeList> codeLists;
  @NonNull
  private final SubmissionFileSystem submissionFileSystem;
  @NonNull
  private final SubmissionPlatformStrategyFactory platformStrategyFactory;

  /**
   * Lazy-loaded.
   */
  private SubmissionPlatformStrategy platform;

  @Override
  public String getOutputDirPath() {
    throw new UnsupportedOperationException("See DCC-2431");
  }

  @Override
  public String getProjectKey() {
    return projectKey;
  }

  @Override
  public List<String> getEmails() {
    return emails;
  }

  @Override
  public Collection<DataType> getDataTypes() {
    val effectiveDataTypes = ImmutableSet.<DataType> builder();

    // Ensure clinical core is always validated
    effectiveDataTypes.add(ClinicalType.CLINICAL_CORE_TYPE);
    effectiveDataTypes.addAll(dataTypes);

    return effectiveDataTypes.build();
  }

  @Override
  public Release getRelease() {
    return release;
  }

  @Override
  public Dictionary getDictionary() {
    return dictionary;
  }

  @Override
  public List<CodeList> getCodeLists() {
    return codeLists;
  }

  @Override
  public ReleaseFileSystem getReleaseFileSystem() {
    return submissionFileSystem.getReleaseFilesystem(release, submissions);
  }

  @Override
  public SubmissionDirectory getSubmissionDirectory() {
    return getReleaseFileSystem().getSubmissionDirectory(projectKey);
  }

  @Override
  public SubmissionFileSystem getSubmissionFileSystem() {
    return submissionFileSystem;
  }

  @Override
  public FileSystem getFileSystem() {
    return submissionFileSystem.getFileSystem();
  }

  @Override
  public SubmissionPlatformStrategy getPlatformStrategy() {
    if (platform == null) {
      // Round about way to get the inputs and outputs
      Path inputDir = new Path(getSubmissionDirectory().getSubmissionDirPath());
      log.info("Validation context for '{}' has inputDir = {}", projectKey, inputDir);
      Path outputDir = new Path(getSubmissionDirectory().getValidationDirPath());
      log.info("Validation context for '{}' has outputDir = {}", projectKey, outputDir);

      // Abstractions to support local / Hadoop
      log.info("Creating platform strategy for project {}", projectKey);
      platform = platformStrategyFactory.get(inputDir, outputDir);
    }

    return platform;
  }

  @Override
  public List<Path> getFiles(FileType fileType) {
    val submissionDirectory = getSubmissionDirectory();
    val fileSchema = getFileSchema(fileType);
    if (fileSchema == null) {
      return Collections.emptyList();
    }

    val fileNamePattern = fileSchema.getPattern();

    val builder = ImmutableList.<Path> builder();
    for (val submissionFileName : submissionDirectory.listFile()) {
      val match = matches(fileNamePattern, submissionFileName);
      if (match) {
        Path file = new Path(submissionDirectory.getDataFilePath(submissionFileName));

        builder.add(file);
      }
    }

    return builder.build();
  }

  @Override
  public FileSchema getFileSchema(FileType fileType) {
    return getDictionary().getFileSchemaByName(fileType.getId()).orNull();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.icgc.dcc.submission.validation.core.ValidationContext#getReleaseSubmissions()
   */
  @Override
  public Map<String, Submission> getReleaseSubmissions() {
    // TODO Auto-generated method stub
    return null;
  }

}
