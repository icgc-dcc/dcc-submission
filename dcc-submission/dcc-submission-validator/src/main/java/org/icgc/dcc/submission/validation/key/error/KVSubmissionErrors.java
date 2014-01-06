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
package org.icgc.dcc.submission.validation.key.error;

import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.DONOR_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SAMPLE_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SAMPLE_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SPECIMEN_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SPECIMEN_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_P;

import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.core.KVFileDescription;
import org.icgc.dcc.submission.validation.key.data.KVSubmissionDataDigest;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;

import com.google.common.collect.ImmutableMap;

/**
 * 
 */
@Slf4j
public class KVSubmissionErrors {

  private final Map<KVFileType, KVFileErrors> errors = new ImmutableMap.Builder<KVFileType, KVFileErrors>()
      .put(
          DONOR,
          new KVFileErrors(DONOR_PKS))
      .put(
          SPECIMEN,
          new KVFileErrors(
              SPECIMEN_PKS,
              SPECIMEN_FKS))
      .put(
          SAMPLE,
          new KVFileErrors(
              SAMPLE_PKS,
              SAMPLE_FKS))
      .put(
          SSM_M,
          new KVFileErrors(
              SSM_M_PKS,
              SSM_M_FKS1,
              SSM_M_FKS2))
      .put(
          SSM_P,
          new KVFileErrors(
              new Object(), // TODO: factory
              SSM_P_FKS))
      .put(
          CNSM_M,
          new KVFileErrors(
              CNSM_M_PKS,
              CNSM_M_FKS1,
              CNSM_M_FKS2))
      .put(
          CNSM_P,
          new KVFileErrors(
              CNSM_P_PKS,
              CNSM_P_FKS))
      .put(
          CNSM_S,
          new KVFileErrors(
              new Object(),
              CNSM_S_FKS))
      .build();

  public KVFileErrors getFileErrors(KVFileType fileType) {
    return errors.get(fileType);
  }

  /**
   * TODO: PLK: we actually only need the KVFileDescription here, not the whole {@link KVSubmissionDataDigest}.
   */
  public boolean describe(Map<KVFileType, KVFileDescription> fileDescriptions) {
    boolean status = true;
    for (val entry : errors.entrySet()) {
      val fileType = entry.getKey();
      val fileErrors = entry.getValue();

      boolean fileStatus = fileErrors.describe(fileDescriptions.get(fileType));
      status &= fileStatus;
      log.info("{}: {}", fileType, fileStatus);
    }
    return status;
  }
}
