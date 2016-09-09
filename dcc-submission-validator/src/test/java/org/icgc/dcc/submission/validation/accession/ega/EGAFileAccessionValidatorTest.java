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

import org.icgc.dcc.common.ega.client.EGAClient;
import org.icgc.dcc.submission.validation.accession.ega.EGAFileAccessionValidator;
import org.icgc.dcc.submission.validation.accession.ega.EGAFileAccessionValidator.Result;
import org.junit.Test;

import lombok.val;

public class EGAFileAccessionValidatorTest {

  EGAClient client = new EGAClient();
  EGAFileAccessionValidator verifier = new EGAFileAccessionValidator(client);

  @Test
  public void testValidateValid() throws Exception {
    val fileId = "EGAF00001160861";
    val result = verify(fileId);
    assertThat(result.isValid()).isTrue();
  }

  @Test
  public void testValidateInvalid() throws Exception {
    val fileId = "EGAF00000000000";
    val result = verify(fileId);
    assertThat(result.isValid()).isFalse();
    assertThat(result.getReason()).startsWith("Not authorized to access entity at path /files/" + fileId);
  }

  @Test(expected = IllegalStateException.class)
  public void testValidateError() throws Exception {
    val fileId = "xxx";
    verify(fileId);
  }

  @Test(expected = NullPointerException.class)
  public void testValidateNull() throws Exception {
    verifier.validate(null);
  }

  private Result verify(String fileId) {
    return verifier.validate(fileId);
  }

}
