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
package org.icgc.dcc.submission.services;

import java.util.Set;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.repository.ReleaseRepository;

import com.google.inject.Inject;

@Slf4j
@NoArgsConstructor
@RequiredArgsConstructor
public class ReleaseService {

  @NonNull
  @Inject
  private ReleaseRepository releaseRepository;

  public Release find(String releaseName) {
    log.info("Request for Release {}", releaseName);
    return releaseRepository.find(releaseName);
  }

  public Set<Release> findAll() {
    log.info("Request to find all Releases");
    return releaseRepository.findAll();
  }

  /**
   * Query for {@code Release} with state {@code OPENED}
   * 
   * @return Current Open Release
   */
  public Release findOpen() {
    log.info("Request for current Open Release");
    return releaseRepository.findOpen();
  }

  /**
   * Creates a new {@code Submission} and adds it to the current open {@code Release}
   * 
   * @param projectKey
   * 
   * @param projectName
   * 
   * @return Current Open Release
   */
  public Release addSubmission(String projectKey, String projectName) {
    log.info("Creating Submission for Project {} in current open Release", projectKey);
    val openRelease = releaseRepository.findOpen();
    val submission = new Submission(projectKey, projectName, openRelease.getName());
    log.info("Created Submission {}", submission);

    val release = releaseRepository.addSubmission(submission, openRelease.getName());

    return release;
  }
}
