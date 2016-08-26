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
package org.icgc.dcc.submission.security;

import static com.google.common.collect.Maps.newConcurrentMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.icgc.dcc.submission.core.security.Authority.projectViewPrivilege;

import java.util.Map;

import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.service.ProjectService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubmissionUserDetailsService implements UserDetailsService {

  /**
   * Dependencies.
   */
  private final ProjectService projectService;

  /**
   * State.
   */
  private final Map<String, User> users = newConcurrentMap();

  public SubmissionUserDetailsService(SubmissionProperties properties, ProjectService projectService) {
    this.projectService = projectService;

    for (val user : properties.getAuth().getUsers()) {
      log.info("Adding user: {}...", user);
      users.put(user.getUsername(), new User(user.getUsername(), user.getPassword(), concat(
          user.getRoles().stream().map(role -> "ROLE_" + role.toUpperCase()),
          user.getAuthorities().stream())
              .map(SimpleGrantedAuthority::new)
              .collect(toList())));
    }
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    val user = users.get(username);
    if (user == null) throw new UsernameNotFoundException(username);

    val projects = projectService.getProjectsByUser(username);

    // Combine authorities
    return new User(user.getUsername(), user.getPassword(), concat(
        user.getAuthorities().stream(),
        projects.stream()
            .map(project -> projectViewPrivilege(project.getKey()))
            .map(SimpleGrantedAuthority::new))
                .collect(toList()));
  }

}
