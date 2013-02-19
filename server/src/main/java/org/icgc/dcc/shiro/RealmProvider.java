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
package org.icgc.dcc.shiro;

import java.util.Collection;

import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.realm.Realm;
import org.icgc.dcc.core.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class RealmProvider implements Provider<Collection<Realm>> {
  private static final Logger log = LoggerFactory.getLogger(RealmProvider.class);

  @Inject
  private Config config;

  // @Inject
  // private UserService users;

  @Inject
  private ProjectService projects;

  /**
   * TODO <code>{@link ShiroPasswordAuthenticator#authenticate()}</code>
   */
  @Override
  public Collection<Realm> get() {
    String shiroIniFilePath = this.config.getString(ShiroConfig.SHIRO_INI_FILE);
    log.info("shiroIniFilePath = " + shiroIniFilePath);
    DccWrappingRealm dccWrappingRealm = buildDccWrappingRealm(shiroIniFilePath);

    return ImmutableSet.<Realm> of(dccWrappingRealm);
  }

  private DccWrappingRealm buildDccWrappingRealm(String shiroIniFilePath) {
    DccWrappingRealm dccWrappingRealm = new DccWrappingRealm(projects);
    dccWrappingRealm.setResourcePath("file:" + shiroIniFilePath);// TODO: existing constant for that?
    dccWrappingRealm.init();
    dccWrappingRealm.setCredentialsMatcher(new PasswordMatcher());
    // TODO investigate caching particulars
    dccWrappingRealm.setAuthorizationCachingEnabled(false);
    return dccWrappingRealm;
  }
}
