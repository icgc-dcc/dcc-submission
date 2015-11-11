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
package org.icgc.dcc.submission.core.state;

import static com.google.common.collect.Iterables.find;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.json.Jackson.formatPrettyJson;
import static org.icgc.dcc.submission.core.report.DataTypeState.INVALID;
import static org.icgc.dcc.submission.core.report.DataTypeState.VALID;
import static org.mockito.Mockito.when;

import org.icgc.dcc.common.core.model.ClinicalType;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.DataTypeState;
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.core.report.ErrorType;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileState;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.FileTypeState;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ValidatingStateTest {

  private static final Predicate<DataTypeReport> IS_CLINICAL = new Predicate<DataTypeReport>() {

    @Override
    public boolean apply(DataTypeReport input) {
      return input.getDataType() == ClinicalType.CLINICAL_CORE_TYPE;
    }
  };

  private static final Predicate<DataTypeReport> IS_SSM = new Predicate<DataTypeReport>() {

    @Override
    public boolean apply(DataTypeReport input) {
      return input.getDataType() == FeatureType.SSM_TYPE;
    }
  };

  @Mock
  StateContext mockStateContext;

  @Test
  public void testFinishValidationCornerCase() {
    val originalReport = getOriginalReport();
    log.info(formatPrettyJson(originalReport));
    when(mockStateContext.getReport()).thenReturn(originalReport);

    val newReport = getNewReport();
    log.info(formatPrettyJson(newReport));
    SubmissionState.VALIDATING.finishValidation(
        mockStateContext,
        Lists.<DataType> newArrayList(
            ClinicalType.CLINICAL_CORE_TYPE),
        Outcome.ABORTED,
        newReport);
    log.info(formatPrettyJson(newReport));

    assertThat(find(newReport.getDataTypeReports(), IS_CLINICAL).getDataTypeState()).isEqualTo(VALID);
    assertThat(find(newReport.getDataTypeReports(), IS_SSM).getDataTypeState()).isEqualTo(INVALID);
  }

  private Report getOriginalReport() {
    val ssmMError = new Error.Builder()
        .type(ErrorType.CODELIST_ERROR)
        .number(0)
        .build();

    val donorFileReport = new FileReport("donor.1.txt", FileType.DONOR_TYPE);
    donorFileReport.setFileState(FileState.NOT_VALIDATED);

    val ssmMFileReport = new FileReport("ssm_m.1.txt", FileType.SSM_M_TYPE);
    ssmMFileReport.setFileState(FileState.INVALID);
    ssmMFileReport.addError(ssmMError);

    val donorFileTypeReport = new FileTypeReport(FileType.DONOR_TYPE);
    donorFileTypeReport.setFileTypeState(FileTypeState.NOT_VALIDATED);
    donorFileTypeReport.addFileReport(donorFileReport);

    val ssmMFileTypeReport = new FileTypeReport(FileType.SSM_M_TYPE);
    ssmMFileTypeReport.setFileTypeState(FileTypeState.INVALID);
    ssmMFileTypeReport.addFileReport(ssmMFileReport);

    val clinicalDataTypeReport = new DataTypeReport(ClinicalType.CLINICAL_CORE_TYPE);
    clinicalDataTypeReport.setDataTypeState(DataTypeState.NOT_VALIDATED);
    clinicalDataTypeReport.addFileTypeReport(donorFileTypeReport);

    val ssmDataTypeReport = new DataTypeReport(FeatureType.SSM_TYPE);
    ssmDataTypeReport.setDataTypeState(DataTypeState.INVALID);
    ssmDataTypeReport.addFileTypeReport(ssmMFileTypeReport);

    val originalReport = new Report();
    originalReport.addDataTypeReport(clinicalDataTypeReport);
    originalReport.addDataTypeReport(ssmDataTypeReport);

    return originalReport;
  }

  private Report getNewReport() {
    val donorFileReport = new FileReport("donor.2.txt", FileType.DONOR_TYPE);
    donorFileReport.setFileState(FileState.VALID);

    val donorFileTypeReport = new FileTypeReport(FileType.DONOR_TYPE);
    donorFileTypeReport.setFileTypeState(FileTypeState.VALID);
    donorFileTypeReport.addFileReport(donorFileReport);

    val clinicalDataTypeReport = new DataTypeReport(ClinicalType.CLINICAL_CORE_TYPE);
    clinicalDataTypeReport.setDataTypeState(DataTypeState.VALID);
    clinicalDataTypeReport.addFileTypeReport(donorFileTypeReport);

    val newReport = new Report();
    newReport.addDataTypeReport(clinicalDataTypeReport);
    return newReport;
  }

}
