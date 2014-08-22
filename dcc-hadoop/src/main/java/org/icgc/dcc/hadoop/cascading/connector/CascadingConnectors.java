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
package org.icgc.dcc.hadoop.cascading.connector;

import java.util.Map;

import lombok.NonNull;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.icgc.dcc.hadoop.util.HadoopConstants;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.flow.Flow;
import cascading.flow.FlowConnector;

import com.google.common.collect.ImmutableMap;

/**
 * Interface to connect {@link Flow}s and {@link Cascade}s.
 */
public interface CascadingConnectors {

  LocalConnectors LOCAL = new LocalConnectors();
  DistributedConnectors DISTRIBUTED = new DistributedConnectors();

  String describe();

  /**
   * Do *not* use in production.
   */
  FlowConnector getTestFlowConnector();

  FlowConnector getFlowConnector();

  FlowConnector getFlowConnector(Map<?, ?> properties);

  CascadeConnector getCascadeConnector();

  CascadeConnector getCascadeConnector(Map<?, ?> properties);

  public static class Utils {

    public static Map<String, String> getProperties(@NonNull final String fsDefault) {
      return ImmutableMap.of(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, fsDefault);
    }

    public static Map<String, String> getProperties(
        @NonNull final String fsDefault,
        @NonNull final String jobTracker) {
      return ImmutableMap.of(
          CommonConfigurationKeys.FS_DEFAULT_NAME_KEY, fsDefault,
          HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY, jobTracker);
    }

  }

}
