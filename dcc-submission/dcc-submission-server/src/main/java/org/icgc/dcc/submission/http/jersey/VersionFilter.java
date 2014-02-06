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
package org.icgc.dcc.submission.http.jersey;

import static javax.ws.rs.BindingPriority.HEADER_DECORATOR;
import static org.icgc.dcc.core.util.VersionUtils.getApiVersion;
import static org.icgc.dcc.core.util.VersionUtils.getCommitId;
import static org.icgc.dcc.core.util.VersionUtils.getVersion;

import java.io.IOException;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import lombok.val;

import org.icgc.dcc.submission.web.util.Versions;

/**
 * Adds system version and commit id to each response.
 */
@Provider
@BindingPriority(HEADER_DECORATOR)
public class VersionFilter implements ContainerResponseFilter {

  /**
   * Header name constants.
   */
  private static final String SERVER_COMMIT_ID_HEADER = "X-ICGC-Submission-CommitId";
  private static final String SERVER_VERSION_HEADER = "X-ICGC-Submission-Version";
  private static final String SERVER_API_VERSION_HEADER = "X-ICGC-Submission-Api-Version";

  /**
   * System version constants.
   */
  private static final Versions VERSIONS = new Versions(getVersion(), getApiVersion(), getCommitId());

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {

    // Add versions to response
    val headers = responseContext.getHeaders();
    headers.add(SERVER_VERSION_HEADER, VERSIONS.getVersion());
    headers.add(SERVER_API_VERSION_HEADER, VERSIONS.getApiVersion());
    headers.add(SERVER_COMMIT_ID_HEADER, VERSIONS.getCommit());
  }

}
