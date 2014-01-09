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
package org.icgc.dcc.submission.validation.key;

import lombok.val;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Command-line utility to initiate key validation on a specified project stored locally or in HDFS. Will use Cascading
 * local or Hadoop depending on the {@code fsUrl} argument's scheme.
 */
public class Main {

  public static void main(String... args) throws InterruptedException {
    Logger.getRootLogger().setLevel(Level.INFO);

    // Resolve configuration
    int i = 0;
    val previousReleaseName = args.length >= ++i ? args[i - 1] : "release1";
    val releaseName = args.length >= ++i ? args[i - 1] : "release2";
    val projectKey = args.length >= ++i ? args[i - 1] : "project1";
    val fsRoot = args.length >= ++i ? args[i - 1] : "/tmp/dcc_root_dir";
    val fsUrl = args.length >= ++i ? args[i - 1] : "file:///";
    val context = new KeyValidationContext(previousReleaseName, releaseName, projectKey, fsRoot, fsUrl);

    // Validate
    validate(context);
  }

  private static void validate(KeyValidationContext context) throws InterruptedException {
    val validator = new KeyValidator();

    validator.validate(context);
  }

}
