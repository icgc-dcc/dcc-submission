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
package org.icgc.dcc.hadoop.cascading;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.mapred.Reporter;

public class FlowExecutorHeartbeat {

  private final AtomicInteger latch = new AtomicInteger();
  private final Thread beat;

  public FlowExecutorHeartbeat(final Reporter reporter, final long periodMillis) {
    beat = new Thread(
        new Runnable() {

          @Override
          public void run() {
            boolean interrupted = false;
            while (latch.get() == 0 && !interrupted) {
              try {
                Thread.sleep(periodMillis);
              } catch (InterruptedException e) {
                interrupted = true;
              }

              // Keep the task alive
              reporter.progress();

              // Call the optional custom progress method
              progress();
            }
          }
        });
  }

  public FlowExecutorHeartbeat(Reporter reporter) {
    this(reporter, 60 * 1000);
  }

  public void start() {
    beat.start();
  }

  public void stop() {
    latch.incrementAndGet();
    beat.interrupt();
  }

  protected void progress() {
    // No-op
  }

}