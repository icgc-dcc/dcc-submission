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

import java.util.Collection;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;

/**
 * The umbilical cord to the rest of the system provided to {@link Validator}s to act as a "façade" that reduces
 * coupling.
 * <p>
 * Its interface should be minimized over time as per "LoD". A context instance gets passed from a {@link Validator} in
 * a {@link Validation} and validators are expected to call inherited {@link ReportContext} methods to add errors and
 * statistics. In this sense, it simultaneously an "accumulating parameter".
 */
public interface ValidationContext extends ReportContext {

  /**
   * Gets the project key of the project under validation.
   */
  String getProjectKey();

  /**
   * Gets the email addresses of whom to email after validation.
   */
  List<String> getEmails();

  /**
   * Gets the list of data types to validate.
   */
  Collection<DataType> getDataTypes();

  /**
   * Gets the current release.
   */
  Release getRelease();

  /**
   * Gets the current release dictionary.
   */
  Dictionary getDictionary();

  /**
   * Gets the current code lists.
   */
  List<CodeList> getCodeLists();

  /**
   * Gets the submission directory of the associated project under validation.
   */
  SubmissionDirectory getSubmissionDirectory();

  /**
   * Gets the available file(s) of {@code fileType} of the associated project under validation.
   */
  List<Path> getFiles(FileType fileType);

  /**
   * Gets the {@code FileSchema} associated with the {@code fileType}.
   */
  FileSchema getFileSchema(FileType fileType);

  /**
   * Gets the root DCC file system.
   */
  DccFileSystem getDccFileSystem();

  /**
   * Gets the platform abstracted file system.
   */
  FileSystem getFileSystem();

  /**
   * Gets the current release file system.
   */
  ReleaseFileSystem getReleaseFileSystem();

  /**
   * Gets the cascading platform strategy for cascading-based {@link Validator}s.
   */
  SubmissionPlatformStrategy getPlatformStrategy();

  /**
   * Temporarily here (see DCC-2431).
   */
  String getOutputDirPath();

}