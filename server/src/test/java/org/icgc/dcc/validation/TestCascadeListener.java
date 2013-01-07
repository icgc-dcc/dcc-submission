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
package org.icgc.dcc.validation;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeListener;

/**
 * Class to allow tests that run a validation to block until it is complete
 */
public class TestCascadeListener implements CascadeListener {

  private boolean isRunning = true;

  @Override
  public void onStarting(Cascade cascade) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onStopping(Cascade cascade) {
    isRunning = false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see cascading.cascade.CascadeListener#onCompleted(cascading.cascade.Cascade)
   */
  @Override
  public void onCompleted(Cascade cascade) {
    isRunning = false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see cascading.cascade.CascadeListener#onThrowable(cascading.cascade.Cascade, java.lang.Throwable)
   */
  @Override
  public boolean onThrowable(Cascade cascade, Throwable throwable) {
    isRunning = false;
    return false;
  }

  public boolean isRunning() {
    return isRunning;
  }

}
