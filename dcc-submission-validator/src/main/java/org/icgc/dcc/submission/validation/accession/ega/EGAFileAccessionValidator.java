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

import org.icgc.dcc.common.ega.client.EGAAPIClient;
import org.icgc.dcc.common.ega.client.EGAEntityNotFoundException;
import org.icgc.dcc.common.ega.client.EGANotAuthorizedException;
import org.icgc.dcc.common.ega.model.EGAAccessionType;

import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
   * Dependencies.
   */
  @NonNull
  private final EGAAPIClient client;

  public Result validate(String fileId) {
    checkFileAccession(fileId);
    try {
      val file = getFile(fileId);
      log.debug("Found file: {}", file);

      return valid();
    } catch (EGAEntityNotFoundException e) {
      log.warn("Could not find file with id: {}: {}", fileId, e.getMessage());
      return invalid(e.getMessage());
    } catch (EGANotAuthorizedException e) {
      log.warn("Not authorized to access file with id: {}: {}", fileId, e.getMessage());
      return invalid(e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error getting file " + fileId + ": ", e);
      return invalid("Unexpected error getting file " + fileId + ": " + e.getMessage());
    }
  }

  private ArrayNode getFile(String fileId) {
    // Always need to login
    client.login();
    return client.getFile(fileId);
  }

  private static void checkFileAccession(String fileId) {
    val accessionType = EGAAccessionType.from(fileId);
    checkState(accessionType.isPresent(), "Could not detect accession type for value %s", fileId);
    checkState(accessionType.get().isFile(), "Accession type not file for value %s", fileId);
  }

  /**
   * Validation result.
   */
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

}
