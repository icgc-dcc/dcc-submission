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
package org.icgc.dcc.hadoop.fs;

import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tap.local.FileTap;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;

/**
 * Very low-tech replacement for {@link DccFileSystem}, as discussed with @Bob Tiernay around 13/11/07 (see DCC-1876).
 * This is a temporary solution until a proper re-modelling of the file operations related objects can happen.
 */
public class DccFileSystem2 {

  public static Tap<?, ?, ?> getNormalizationOutputTap(Config config, String releaseName, String projectKey) {
    String path = getNormalizationOutput(releaseName, projectKey);
    System.out.println(path);
    return usesHadoop(config) ?
        new Hfs(new cascading.scheme.hadoop.TextDelimited(true, "\t"), path) :
        new FileTap(new cascading.scheme.local.TextDelimited(true, "\t"), path);
  }

  public static String getNormalizationOutput(String releaseName, String projectKey) {
    return String.format("/icgc/%s/projects/%s/normalization/ssm__p.txt", releaseName, projectKey);
  }

  public static boolean usesHadoop(Config config) {
    Preconditions.checkState(
        config.hasPath("fs.url"),
        "fs.url should be present in the config");
    return config.getString("fs.url")
        .startsWith("hdfs");
  }
}
