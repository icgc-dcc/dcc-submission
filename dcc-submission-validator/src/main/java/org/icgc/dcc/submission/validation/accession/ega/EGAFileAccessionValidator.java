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
package org.icgc.dcc.submission.validation.accession.ega;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Suppliers.memoizeWithExpiration;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.icgc.dcc.common.ega.model.EGAAccessionType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * EGA specific file accession validator.
 * <p>
 * Validates that a file accession (e.g. {@code EGAF00000000001} exists in EGA.
 * <p>
 * Note that a 401 is returned from the EGA {@code https://ega.ebi.ac.uk/ega/rest/access/v2/files/EGF00000000001} API
 * when either:
 * <ul>
 * <li>The request is unauthorized, or</li>
 * <li>The file does not exist</li>
 * </ul>
 * This ambiguity implies that existence can be certain where as non-existence is most likely true if the DAC of the
 * {@link EGAClient} has access to the corresponding study's dataset.
 */
@Slf4j
@RequiredArgsConstructor
public class EGAFileAccessionValidator {

  /**
   * Constants.
   */
  public static final String DEFAULT_REPORT_URL = "http://hsubmission-dcc.oicr.on.ca:8080/api/v1/ega/report";

  /**
   * Dependencies.
   */
  @NonNull
  private String reportUrl;

  /**
   * State.
   */
  private final Supplier<Multimap<String, ObjectNode>> memoizer = memoizeWithExpiration(this::indexFiles, 1, HOURS);

  public EGAFileAccessionValidator() {
    this(DEFAULT_REPORT_URL);
  }

  public Result checkFile(String fileId) {
    checkFileAccession(fileId);
    val files = getFilesById(fileId);
    if (files.isEmpty()) {
      return invalid("No files found with id " + fileId, fileId);
    }
    return valid();
  }

  public Result validate(@NonNull String sampleId, String fieldName, String fileId) {
    checkFileAccession(fileId);
    try {
      val files = getFilesById(fileId);
      if (files.isEmpty()) {
        return invalid("No files found with id " + fileId, fileId);
      }

      for (val file : files) {
        val submitterSampleId = file.get("submitterSampleId").textValue();
        if (sampleId.equals(submitterSampleId)) {
          log.debug("Found files: {}", files);
          return valid();
        }
      }

      return invalid( format("Missing EGA File ID for %s: %s", fieldName, sampleId), fileId);
    } catch (Exception e) {
      log.error("Unexpected error getting file " + fileId + ": ", e);
      return invalid("Unexpected error getting file " + fileId + ": " + e.getMessage(), fileId);
    }
  }

  private static void checkFileAccession(String fileId) {
    val accessionType = EGAAccessionType.from(fileId);
    checkState(accessionType.isPresent(), "Could not detect accession type for value %s", fileId);
    checkState(accessionType.get().isFile(), "Accession type not file for value %s", fileId);
  }

  private Collection<ObjectNode> getFilesById(String fileId) {
    return memoizer.get().get(fileId);
  }

  private Multimap<String, ObjectNode> indexFiles() {
    return Multimaps.index(readFiles(), file -> file.get("fileId").textValue());
  }

  @SneakyThrows
  private List<ObjectNode> readFiles() {
    val watch = Stopwatch.createStarted();
    try {
      log.info("Reading file report...");
      return DEFAULT.readValue(new URL(reportUrl), new TypeReference<List<ObjectNode>>() {});
    } finally {
      log.info("Finished reading file report in {}", watch);
    }
  }

  /**
   * Validation result.
   */
  @Value
  public static class Result {

    boolean valid;
    String reason;
    String fileId;

  }

  private static Result valid() {
    return new Result(true, null, "");
  }

  private static Result invalid(String reason, String fileId) {
    return new Result(false, reason, fileId);
  }

}
