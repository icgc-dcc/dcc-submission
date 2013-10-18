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
package org.icgc.dcc.submission.checker;

import java.util.List;
import java.util.regex.Pattern;

import org.elasticsearch.common.collect.Lists;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.SubmissionDirectory;

import com.google.common.collect.ImmutableList;

public class SubmissionChecker {

  final private boolean failFast;

  public SubmissionChecker(boolean failFast) {
    this.failFast = failFast;
  }

  public List<FirstPassValidationError> check(Dictionary dict, SubmissionDirectory submissionDir) {
    Iterable<String> files = submissionDir.listFile();

    List<String> fileSchemas = Lists.newLinkedList();
    for (String file : files) {
      FileSchema fileSchema = getFileSchema(dict, file);
      if (fileSchema != null) fileSchemas.add(fileSchema.getName());
    }
    if (fileSchemas.isEmpty()) {
      throw new RuntimeException("No files in submission directory");
    } else {
      return ImmutableList.<FirstPassValidationError> of();
    }
  }

  private FileSchema getFileSchema(Dictionary dict, String filename) {
    for (FileSchema schema : dict.getFiles()) {
      if (Pattern.matches(schema.getPattern(), filename)) {
        return schema;
      }
    }
    return null;
  }

  public boolean isFailFast() {
    return failFast;
  }

}
