/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.regex.Pattern.matches;
import static org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType.SSM_P_TYPE;

import java.util.List;

import lombok.Delegate;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.core.SubmissionConcatenator.SubmissionConcatFile;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * The "default" implementation of the {@link ValidationContext}.
 */
@Value
@Slf4j
public class DefaultValidationContext implements ValidationContext {

  /**
   * Fulfills the {@link ReportContext} contract via delegation.
   */
  @Delegate
  @NonNull
  SubmissionReportContext reportContext;

  /**
   * Supports the non-inherited {@link ValidationContext} contract.
   */
  @NonNull
  String projectKey;
  @NonNull
  List<String> emails;
  @NonNull
  Release release;
  @NonNull
  Dictionary dictionary;
  @NonNull
  DccFileSystem dccFileSystem;
  @NonNull
  List<SubmissionConcatFile> concatFiles;
  @NonNull
  PlatformStrategyFactory platformStrategyFactory;

  @Override
  public String getProjectKey() {
    return projectKey;
  }

  @Override
  public List<String> getEmails() {
    return emails;
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
  public ReleaseFileSystem getReleaseFileSystem() {
    return dccFileSystem.getReleaseFilesystem(release);
  }

  @Override
  public SubmissionDirectory getSubmissionDirectory() {
    return getReleaseFileSystem().getSubmissionDirectory(projectKey);
  }

  @Override
  public ImmutableList<SubmissionConcatFile> getConcatFiles() {
    return copyOf(concatFiles);
  }

  @Override
  public SubmissionDirectory getPreviousSubmissionDirectory() {
    // TODO: Implement
    throw new UnsupportedOperationException(
        "There currently is no way to get the previous submission directory. This requires implementation effort.");
  }

  @Override
  public DccFileSystem getDccFileSystem() {
    return dccFileSystem;
  }

  @Override
  public FileSystem getFileSystem() {
    return dccFileSystem.getFileSystem();
  }

  @Override
  public PlatformStrategy getPlatformStrategy() {
    // Round about way to get the inputs and outputs
    Path inputDir = new Path(getSubmissionDirectory().getSubmissionDirPath());
    log.info("Validation context for '{}' has inputDir = {}", projectKey, inputDir);
    Path outputDir = new Path(getSubmissionDirectory().getValidationDirPath());
    log.info("Validation context for '{}' has outputDir = {}", projectKey, outputDir);
    Path systemDir = getReleaseFileSystem().getSystemDirectory();
    log.info("Validation context for '{}' has systemDir = {}", projectKey, systemDir);

    // Abstractions to support local / Hadoop
    log.info("Creating platform strategy for project {}", projectKey);
    val platformStrategy = platformStrategyFactory.get(inputDir, outputDir, systemDir);

    return platformStrategy;
  }

  @Override
  public Optional<Path> getSsmPrimaryFile() {
    val submissionDirectory = getSubmissionDirectory();
    val ssmPrimaryFileSchema = getSsmPrimaryFileSchema(getDictionary());
    val ssmPrimaryFileNamePattern = ssmPrimaryFileSchema.getPattern();

    for (val submissionFileName : submissionDirectory.listFile()) {
      val ssmPrimary = matches(ssmPrimaryFileNamePattern, submissionFileName);
      if (ssmPrimary) {
        Path ssmPrimaryFile = new Path(submissionDirectory.getDataFilePath(submissionFileName));

        return Optional.of(ssmPrimaryFile);
      }
    }

    return Optional.<Path> absent();
  }

  @Override
  public FileSchema getSsmPrimaryFileSchema() {
    return getSsmPrimaryFileSchema(getDictionary());
  }

  private static FileSchema getSsmPrimaryFileSchema(Dictionary dictionary) {
    for (val fileSchema : dictionary.getFiles()) {
      val fileType = SubmissionFileType.from(fileSchema.getName());
      val ssmPrimary = fileType == SSM_P_TYPE;
      if (ssmPrimary) {
        return fileSchema;
      }
    }

    throw new IllegalStateException("'ssm_p' file schema missing");
  }

}
