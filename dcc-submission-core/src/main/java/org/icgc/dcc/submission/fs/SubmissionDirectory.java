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
package org.icgc.dcc.submission.fs;

import static com.google.common.base.Preconditions.checkState;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_ANALYZED_SAMPLE_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_DONOR_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_SPECIMEN_ID;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.common.core.util.Splitters.TAB;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.isFile;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.lsFile;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.rm;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.icgc.dcc.common.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.Submission;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@RequiredArgsConstructor
public class SubmissionDirectory {

  @NonNull
  private final DccFileSystem dccFileSystem;
  @NonNull
  private final ReleaseFileSystem releaseFileSystem;
  @NonNull
  private final Release release;
  @NonNull
  private final String projectKey;
  @NonNull
  private final Submission submission;

  /**
   * (non-recursive).
   */
  public Iterable<String> listFile(Pattern pattern) {
    List<Path> pathList = lsFile(
        this.dccFileSystem.getFileSystem(),
        new Path(getSubmissionDirPath()), pattern);
    return HadoopUtils.toFilenameList(pathList);
  }

  public Iterable<String> listFile() {
    return this.listFile(null);
  }

  /**
   * Returns the list of files that match a file pattern in the dictionary.
   */
  public Iterable<String> listFiles(final Iterable<String> filePatterns) {
    return Iterables.filter(listFile(), new Predicate<String>() {

      @Override
      public boolean apply(String input) {
        for (String filePattern : filePatterns) {
          if (compile(filePattern).matcher(input).matches()) {
            return true;
          }
        }
        return false;
      }
    });
  }

  public String addFile(String filename, InputStream data) {
    String filepath = this.dccFileSystem.buildFileStringPath(this.release.getName(), this.projectKey, filename);
    HadoopUtils.touch(this.dccFileSystem.getFileSystem(), filepath, data);
    return filepath;
  }

  public String deleteFile(String filename) {
    String filepath = this.dccFileSystem.buildFileStringPath(this.release.getName(), this.projectKey, filename);
    HadoopUtils.rm(this.dccFileSystem.getFileSystem(), filepath);
    return filepath;
  }

  public boolean isReadOnly() {
    val state = this.submission.getState();

    return (state.isReadOnly() || this.release.getState() == ReleaseState.COMPLETED);
  }

  public String getProjectKey() {
    return this.projectKey;
  }

  public String getSubmissionDirPath() {
    return dccFileSystem.buildProjectStringPath(release.getName(), projectKey);
  }

  /**
   * Delegates to the {@link ReleaseFileSystem} since the system dir lives at the release level (but is most typically
   * accessed in the context of the processing a submission directory).
   */
  public String getSystemDirPath() {
    return releaseFileSystem.getSystemDirPath().toUri().toString();
  }

  public String getValidationDirPath() {
    return dccFileSystem.buildValidationDirStringPath(release.getName(), projectKey);
  }

  public String getDataFilePath(String filename) {
    return dccFileSystem.buildFileStringPath(release.getName(), projectKey, filename);
  }

  public Submission getSubmission() {
    return this.submission;
  }

  public void resetValidationDir() {
    removeValidationDir();
    createEmptyValidationDir();
  }

  /**
   * TODO: port logic in here rather than in {@link DccFileSystem}
   */
  public void removeValidationDir() {
    dccFileSystem.removeDirIfExist(getValidationDirPath());
  }

  /**
   * Removes all files pertaining to validation (not including normalization), leaving nested directories untouched.
   */
  public void removeValidationFiles() {
    val fs = dccFileSystem.getFileSystem();
    for (val file : lsFile(fs, new Path(getValidationDirPath()))) {
      checkState(isFile(fs, file), "Expecting file, not a directory: '%s'", file);
      log.info("Deleting file '{}'", file);
      rm(fs, file);
    }
  }

  /**
   * TODO: port logic in here rather than in {@link DccFileSystem}
   */
  public void createEmptyValidationDir() {
    dccFileSystem.createDirIfDoesNotExist(getValidationDirPath());
  }

  public List<SubmissionDirectoryFile> getSubmissionFiles() {
    return null;
  }

  /**
   * Must close stream after usage.
   */
  @SneakyThrows
  public DataInputStream open(@NonNull String fileName) {
    return dccFileSystem.getFileSystem()
        .open(new Path(getDataFilePath(fileName)));
  }

  /**
   * Must close stream after usage. The extension is expected to match the actual encoding at this point. The client
   * code can read data from this stream without having to worry about what compression is used.
   */
  @SneakyThrows
  public InputStream getDecompressingInputStream(String fileName) {
    val in = open(fileName);
    val codec = new CompressionCodecFactory(dccFileSystem.getFileSystemConfiguration())
        .getCodec(new Path(getDataFilePath(fileName)));
    return codec == null ?
        in : // This is assumed to be PLAIN_TEXT
        codec.createInputStream(in);
  }

  /**
   * Returns a map of sample IDs to their corresponding donor IDs, a mapping commonly needed.
   * <p>
   * TODO: properly handle using cascading rather.
   */
  public Map<String, String> getSampleToDonorMap(Dictionary dictionary) {

    val sampleToSpecimen = Maps.<String, String> newTreeMap();
    val sampleFileSchema = dictionary.getFileSchema(SAMPLE_TYPE);
    val sampleSampleIdOrdinal = sampleFileSchema.getFieldOrdinal(SUBMISSION_ANALYZED_SAMPLE_ID).get();
    val sampleSpecimenIdOrdinal = sampleFileSchema.getFieldOrdinal(SUBMISSION_SPECIMEN_ID).get();
    val sampleFileNames = listFile(compile(sampleFileSchema.getPattern()));

    for (val sampleFileName : sampleFileNames) {
      boolean first = true;
      for (String row : readClinicalFile(sampleFileName)) { // Clinical files are small
        if (!first) {
          val fields = Lists.<String> newArrayList(TAB.split(row));
          val sampleId = fields.get(sampleSampleIdOrdinal);
          val specimenId = fields.get(sampleSpecimenIdOrdinal);
          checkState(!sampleToSpecimen.containsKey(sampleId));
          sampleToSpecimen.put(sampleId, specimenId);
        }
        first = false;
      }
    }
    log.debug("Sample to specimen mapping: {}", sampleToSpecimen);

    val specimenToDonor = Maps.<String, String> newTreeMap();
    val specimenFileSchema = dictionary.getFileSchema(SPECIMEN_TYPE);
    val specimenSpecimenIdOrdinal = specimenFileSchema.getFieldOrdinal(SUBMISSION_SPECIMEN_ID).get();
    val specimenDonorIdOrdinal = specimenFileSchema.getFieldOrdinal(SUBMISSION_DONOR_ID).get();
    val specimenFileNames = listFile(compile(specimenFileSchema.getPattern()));

    for (val specimenFileName : specimenFileNames) {
      boolean first = true;
      for (String row : readClinicalFile(specimenFileName)) { // Clinical files are small
        if (!first) {
          val fields = Lists.<String> newArrayList(TAB.split(row));
          val specimenId = fields.get(specimenSpecimenIdOrdinal);
          val donorId = fields.get(specimenDonorIdOrdinal);
          checkState(!specimenToDonor.containsKey(specimenId));
          specimenToDonor.put(specimenId, donorId);
        }
        first = false;
      }
    }
    log.debug("Specimen to donor mapping: {}", specimenToDonor);

    val sampleToDonor = Maps.<String, String> newTreeMap();
    for (val entry : sampleToSpecimen.entrySet()) {
      sampleToDonor.put(
          entry.getKey(),
          specimenToDonor.get(entry.getValue()));
    }
    log.debug("Sample to donor mapping: {}", sampleToDonor);

    return sampleToDonor;
  }

  /**
   * Only intended for clinical files (small enough).
   */
  @SneakyThrows
  private List<String> readClinicalFile(String fileName) {
    @Cleanup
    BufferedReader br = new BufferedReader(new InputStreamReader(getDecompressingInputStream(fileName)));
    val lines = Lists.<String> newArrayList();
    for (String line; (line = br.readLine()) != null;) {
      lines.add(line);
    }
    return lines;
  }

  @Override
  public String toString() {
    return String.format("SubmissionDirectory [%s]", getSubmissionDirPath());
  }

}
