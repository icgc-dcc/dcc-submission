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

import static lombok.AccessLevel.PRIVATE;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.hadoop.cascading.connector.CascadingConnectors;
import org.icgc.dcc.hadoop.cascading.connector.DistributedConnectors;
import org.icgc.dcc.hadoop.cascading.connector.LocalConnectors;
import org.icgc.dcc.hadoop.cascading.taps.CascadingTaps;
import org.icgc.dcc.hadoop.cascading.taps.DistributedTaps;
import org.icgc.dcc.hadoop.cascading.taps.LocalTaps;

/**
 * TODO: consider adding {@link FileSystem} as well?
 */
@Value
@RequiredArgsConstructor(access = PRIVATE)
public class CascadingContext {

  private static CascadingContext LOCAL_DEFAULT = new CascadingContext(
      new LocalTaps(),
      new LocalConnectors());

  private static CascadingContext DISTRIBUTED_DEFAULT = new CascadingContext(
      new DistributedTaps(),
      new DistributedConnectors());

  private final CascadingTaps taps;
  private final CascadingConnectors connectors;

  public static final CascadingContext getLocal() {
    return LOCAL_DEFAULT;
  }

  public static final CascadingContext getDistributed() {
    return DISTRIBUTED_DEFAULT;
  }

}