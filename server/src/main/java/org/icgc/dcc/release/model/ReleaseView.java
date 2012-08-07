/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.release.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.release.ReleaseException;

/**
 * 
 */
public class ReleaseView {

  protected String name;

  protected ReleaseState state;

  protected List<DetailedSubmission> submissions = new ArrayList<DetailedSubmission>();

  protected List<String> queue = new ArrayList<String>();

  protected Date releaseDate;

  protected String dictionaryVersion;

  protected Map<SubmissionState, Integer> summary = new EnumMap<SubmissionState, Integer>(SubmissionState.class);

  public ReleaseView() {

  }

  public ReleaseView(Release release, List<Project> projects) {

    this.name = release.name;
    this.state = release.state;
    this.queue = release.getQueue();
    this.releaseDate = release.releaseDate;
    this.dictionaryVersion = release.dictionaryVersion;
    for(Project project : projects) {
      DetailedSubmission submission = new DetailedSubmission(release.getSubmission(project.getKey()));
      submission.setProjectName(project.getName());
      this.submissions.add(submission);

      Integer stateCount = this.summary.get(submission.getState());
      if(stateCount == null) {
        stateCount = 0;
      }
      stateCount++;
      this.summary.put(submission.getState(), stateCount);
    }
  }

  public DetailedSubmission getDetailedSubmission(String projectKey) {
    for(DetailedSubmission submission : submissions) {
      if(submission.getProjectKey().equals(projectKey)) {
        return submission;
      }
    }
    throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
        this.name));
  }

  public String getName() {
    return name;
  }

  public ReleaseState getState() {
    return state;
  }

  public List<DetailedSubmission> getSubmissions() {
    return submissions;
  }

  public List<String> getQueue() {
    return queue;
  }

  public Date getReleaseDate() {
    return releaseDate;
  }

  public String getDictionaryVersion() {
    return dictionaryVersion;
  }

  public Map<SubmissionState, Integer> getSummary() {
    return summary;
  }
}
