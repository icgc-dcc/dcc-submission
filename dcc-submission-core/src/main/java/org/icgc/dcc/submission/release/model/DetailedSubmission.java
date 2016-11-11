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
package org.icgc.dcc.submission.release.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.fs.SubmissionFile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO: DetailedSubmission shouldn't extend Submission (DCC-721)
@NoArgsConstructor
@Getter
public class DetailedSubmission extends Submission {

  @Setter
  private boolean locked;
  private String projectName;
  private String projectAlias;

  @Setter
  private List<SubmissionFile> submissionFiles;

  public DetailedSubmission(Submission submission, Project project) {
    super();
    checkArgument(submission.projectKey != null && //
        submission.projectKey.equals(project.getKey())); // By design
    this.projectKey = project.getKey();
    this.projectName = project.getName();
    this.projectAlias = project.getAlias();

    this.releaseName = submission.releaseName;
    this.state = submission.state;
    this.report = submission.report;
    this.lastUpdated = submission.lastUpdated;
    this.submissionFiles = new ArrayList<SubmissionFile>();
  }

  @Override
  public void setState(SubmissionState nextState) {
    this.state = nextState;
  }

}
