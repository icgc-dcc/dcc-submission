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

/**
 * Command-line utility to initiate key validation on a specified project stored locally or in HDFS. Will use Cascading
 * local or Hadoop depending on the {@code fsUrl} argument's scheme.
 */
public class Main {

  public static void main(String... args) throws InterruptedException {
    // Resolve configuration
    val releaseName = args.length >= 1 ? args[0] : "release1";
    val projectKey = args.length >= 2 ? args[1] : "project1";
    val fsRoot = args.length >= 3 ? args[2] : "/tmp/dcc_root_dir";
    val fsUrl = args.length >= 4 ? args[4] : "file:///";

    // Validate
    val context = new KeyValidationContext(releaseName, projectKey, fsRoot, fsUrl);
    validate(context);
  }

  private static void validate(KeyValidationContext context) throws InterruptedException {
    val validator = new KeyValidator();

    validator.validate(context);
  }

}
