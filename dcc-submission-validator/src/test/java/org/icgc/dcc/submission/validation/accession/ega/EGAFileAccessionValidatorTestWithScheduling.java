package org.icgc.dcc.submission.validation.accession.ega;

import org.junit.Assert;
import org.junit.Test;

/**
 * Copyright (c) 2017 The Ontario Institute for Cancer Research. All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * <p>
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

public class EGAFileAccessionValidatorTestWithScheduling extends EGAFileAccessionValidatorFtpProvider{

  @Test
  public void test_validate(){
    EGAFileAccessionValidator validator = new EGAFileAccessionValidator(
        "ftp://admin:admin@localhost:"+ defaultFtpPort + "/ICGC_metadata",
        "0 0 9,21 * * ?");

    validator.start();

    try {
      while(true){
        Thread.sleep(5000);

        EGAFileAccessionValidator.Result ret = validator.validate("168-02-8TR", "EGAF00000143419");
        if(!ret.isValid()) {
          System.out.println(ret.getReason());
          continue;
        }
        else
          break;

      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Assert.assertTrue( validator.validate("168-02-8TR", "EGAF00000143419").isValid() );
    Assert.assertTrue( validator.validate("168-02-8TR", "EGAF00000143420").isValid() );

    Assert.assertFalse( validator.validate("abcde", "EGAF00000143420").isValid() );
    Assert.assertFalse( validator.validate("168-02-8TR", "EGAF00000143366").isValid() );

  }

}
