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

import static org.assertj.core.api.Assertions.assertThat;

import org.icgc.dcc.submission.validation.accession.ega.EGAFileAccessionValidator.Result;
import org.junit.Ignore;
import org.junit.Test;

import lombok.val;

@Ignore("Need EGA credentials to run")
public class EGAFileAccessionValidatorTest {

  EGAFileAccessionValidator verifier = new EGAFileAccessionValidator();

  @Test
  public void testValidateValid() throws Exception {
    val analyzedSampleId = "BD30T";
    val fileId = "EGAF00000664330";
    val result = verify(analyzedSampleId, fileId);
    assertThat(result.isValid()).isTrue();
  }

  @Test
  public void testValidateInvalidSampleId() throws Exception {
    val analyzedSampleId = "SA!BAD";
    val fileId = "EGAF00000664330";
    val result = verify(analyzedSampleId, fileId);
    assertThat(result.isValid()).isFalse();
    assertThat(result.getReason()).startsWith(
        "Could not match file to sample in: [{\"projectId\":\"BTCA-JP\",\"fileId\":\"EGAF00000664330\",\"submitterSampleId\":\"BD30T\"}]");
  }

  @Test
  public void testValidateMissingFileId() throws Exception {
    val analyzedSampleId = "SA1";
    val fileId = "EGAF00000000000";
    val result = verify(analyzedSampleId, fileId);
    assertThat(result.isValid()).isFalse();
    assertThat(result.getReason()).startsWith("No files found with id " + fileId);
  }

  @Test(expected = IllegalStateException.class)
  public void testValidateError() throws Exception {
    val analyzedSampleId = "xxx";
    val fileId = "yyy";
    verify(analyzedSampleId, fileId);
  }

  @Test(expected = NullPointerException.class)
  public void testValidateNull() throws Exception {
    verifier.validate("SA1", null);
  }

  private Result verify(String analyzedSampleId, String fileId) {
    return verifier.validate(analyzedSampleId, fileId);
  }

}
