/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.pcawg.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import lombok.NonNull;
import lombok.Value;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.model.Record;

@Value
public class Clinical {

  /**
   * Data.
   */
  @NonNull
  List<Record> donors;
  @NonNull
  List<Record> specimens;
  @NonNull
  List<Record> samples;

  @NonNull
  List<Record> biomarker;
  @NonNull
  List<Record> family;
  @NonNull
  List<Record> exposure;
  @NonNull
  List<Record> surgery;
  @NonNull
  List<Record> therapy;

  public List<Record> get(FileType fileType) {
    checkArgument(fileType.getDataType().isClinicalType());

    if (fileType == FileType.DONOR_TYPE) {
      return donors;
    } else if (fileType == FileType.SPECIMEN_TYPE) {
      return specimens;
    } else if (fileType == FileType.SAMPLE_TYPE) {
      return samples;
    } else if (fileType == FileType.BIOMARKER_TYPE) {
      return biomarker;
    } else if (fileType == FileType.FAMILY_TYPE) {
      return family;
    } else if (fileType == FileType.EXPOSURE_TYPE) {
      return exposure;
    } else if (fileType == FileType.SURGERY_TYPE) {
      return surgery;
    } else if (fileType == FileType.THERAPY_TYPE) {
      return therapy;
    }

    throw new IllegalArgumentException("Invalid clinical file type: " + fileType);
  }

}
