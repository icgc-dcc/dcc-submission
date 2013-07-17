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
package org.icgc.dcc.submission.shiro;

import java.util.Collection;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.Realm;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SecurityManagerProvider implements Provider<org.apache.shiro.mgt.SecurityManager> {

  @Inject
  private Collection<Realm> realms;

  @Override
  public org.apache.shiro.mgt.SecurityManager get() {
    DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager(this.realms);
    disableSessions(defaultSecurityManager);
    SecurityUtils.setSecurityManager(defaultSecurityManager);
    return defaultSecurityManager;
  }

  /**
   * Disables server-side sessions entirely
   */
  private void disableSessions(DefaultSecurityManager defaultSecurityManager) {
    DefaultSubjectDAO subjectDao = (DefaultSubjectDAO) defaultSecurityManager.getSubjectDAO();
    ((DefaultSessionStorageEvaluator) subjectDao.getSessionStorageEvaluator()).setSessionStorageEnabled(false);
  }
}
