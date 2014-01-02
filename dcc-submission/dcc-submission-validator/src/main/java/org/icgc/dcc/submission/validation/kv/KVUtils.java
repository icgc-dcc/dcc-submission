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
package org.icgc.dcc.submission.validation.kv;

import static java.lang.String.format;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVSubmissionType.INCREMENTAL_FILE;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVSubmissionType.EXISTING_FILE;

import java.io.File;

import org.icgc.dcc.submission.validation.kv.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.kv.enumeration.KVSubmissionType;

/**
 * Utils method for the key validation.
 * <p>
 * In particular, it detects and retrieves files.
 */
public class KVUtils {

  public static final String TO_BE_REMOVED_FILE_NAME = "TO_BE_REMOVED";
  private static final String PARENT_DIR = "src/test/resources/DCC-1993-tmp";

  public static String getDataFilePath(KVSubmissionType submissionType, KVFileType fileType) {
    return format("%s/%s/%s.txt",
        PARENT_DIR, submissionType.getSubDirectory(), fileType.toString().toLowerCase());
  }

  public static String getToBeRemovedFile() {
    return format("%s/%s/%s.txt",
        PARENT_DIR, INCREMENTAL_FILE.getSubDirectory(), TO_BE_REMOVED_FILE_NAME);
  }

  public static boolean hasToBeRemovedFile() {
    return new File(getToBeRemovedFile()).exists();
  }

  public static boolean hasExistingData() {
    return hasExistingClinicalData();
  }

  public static boolean hasExistingClinicalData() {
    return new File(getDataFilePath(EXISTING_FILE, DONOR)).exists();
  }

  public static boolean hasExistingSsmData() {
    return new File(getDataFilePath(EXISTING_FILE, SSM_M)).exists();
  }

  public static boolean hasExistingCnsmData() {
    return new File(getDataFilePath(EXISTING_FILE, CNSM_M)).exists();
  }

  public static boolean hasIncrementalClinicalData() {
    return new File(getDataFilePath(INCREMENTAL_FILE, DONOR)).exists();
  }

  public static boolean hasIncrementalSsmData() {
    return new File(getDataFilePath(INCREMENTAL_FILE, SSM_M)).exists();
  }

  public static boolean hasIncrementalCnsmData() {
    return new File(getDataFilePath(INCREMENTAL_FILE, CNSM_M)).exists();
  }
}
