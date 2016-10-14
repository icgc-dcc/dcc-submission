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

import static com.google.common.collect.Maps.newHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.val;

@NoArgsConstructor
@Getter
@ToString
public class ReleaseView {

  protected Date created;
  protected Date lastUpdate;

  @Setter
  protected boolean locked;
  protected String name;
  protected ReleaseState state;
  protected List<String> queue = new ArrayList<String>();
  protected Date releaseDate;
  protected String dictionaryVersion;
  protected Map<SubmissionState, Integer> summary = newHashMap();

  public ReleaseView(Release release, Collection<SubmissionState> submissionsStates) {
    this.created = release.getCreated();
    this.lastUpdate = release.getLastUpdate();
    this.name = release.name;
    this.state = release.state;
    this.queue = release.getQueuedProjectKeys();
    this.releaseDate = release.releaseDate;
    this.dictionaryVersion = release.dictionaryVersion;

    for (val submissionState : submissionsStates) {
      val count = summary.getOrDefault(submissionState, 0);
      summary.put(submissionState, count + 1);
    }
  }

}
