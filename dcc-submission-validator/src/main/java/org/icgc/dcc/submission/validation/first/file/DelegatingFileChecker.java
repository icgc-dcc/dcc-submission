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
package org.icgc.dcc.submission.validation.first.file;

import org.icgc.dcc.submission.validation.first.core.AbstractDelegatingChecker;
import org.icgc.dcc.submission.validation.first.core.FileChecker;

import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DelegatingFileChecker extends AbstractDelegatingChecker implements FileChecker {

  public DelegatingFileChecker(FileChecker delegate, boolean failFast) {
    super(delegate, failFast);
  }

  public DelegatingFileChecker(FileChecker delegate) {
    this(delegate, false);
  }

  @Override
  public void checkFile(String fileName) {
    ((FileChecker) delegate).checkFile(fileName);
    log.info(banner());
    if (delegate.canContinue()) {
      log.info("Start performing {} validation...", name);
      performSelfCheck(fileName);
      log.info("End performing {} validation. Number of errors found: '{}'",
          name,
          checkErrorCount);
    }
  }

  /**
   * Template method
   */
  @VisibleForTesting
  public abstract void performSelfCheck(String fileName);

}
