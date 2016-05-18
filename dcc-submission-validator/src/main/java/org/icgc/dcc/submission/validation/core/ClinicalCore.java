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
package org.icgc.dcc.submission.validation.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.model.Record;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.Value;
import lombok.val;

@Value
public class ClinicalCore implements Iterable<List<Record>> {

  /**
   * Data - Core.
   */
  @NonNull
  List<Record> donors;
  @NonNull
  List<Record> specimens;
  @NonNull
  List<Record> samples;

  public Optional<List<Record>> get(FileType fileType) {
    checkArgument(fileType.getDataType().isClinicalType());
    val clinicalType = fileType.getDataType().asClinicalType();
    checkArgument(clinicalType.isCoreClinicalType());

    if (fileType == FileType.DONOR_TYPE) {
      return Optional.of(donors);
    } else if (fileType == FileType.SPECIMEN_TYPE) {
      return Optional.of(specimens);
    } else if (fileType == FileType.SAMPLE_TYPE) {
      return Optional.of(samples);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Iterator<List<Record>> iterator() {
    return ImmutableList.of(donors, specimens, samples).iterator();
  }

  public static Iterable<FileType> getFileTypes() {
    return ImmutableList.of(FileType.DONOR_TYPE, FileType.SPECIMEN_TYPE, FileType.SAMPLE_TYPE);
  }

}
