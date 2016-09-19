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
package org.icgc.dcc.submission.loader.cli;

import java.util.List;

import lombok.ToString;

import org.icgc.dcc.submission.loader.model.DatabaseType;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

@ToString
public class ClientOptions {

  @Parameter(names = { "--input-dir" }, description = "Input directory with the submission data.")
  public String submissionDirectory = "/icgc/submission";

  @Parameter(names = { "--hdfs-url" }, description = "HDFS URL")
  public String fsUrl;

  @Parameter(names = { "--db-host" }, required = true, description = "Database hostname")
  public String dbHost;

  @Parameter(names = { "--db-port" }, description = "Database port")
  public String dbPort;

  @Parameter(names = { "--db-name" }, required = true, description = "Database name")
  public String dbName;

  @Parameter(names = { "--db-user" }, description = "Database user")
  public String dbUser;

  @Parameter(names = { "--db-password" }, description = "Database password")
  public String dbPassword;

  @Parameter(names = { "--submission-url" }, description = "Submission system URL.")
  public String submissionUrl;

  @Parameter(names = { "--submission-user" }, description = "Submission system user name")
  public String submissionUser;

  @Parameter(names = { "--submission-password" }, required = true, description = "Submission system user password")
  public String submissionPassword;

  @Parameter(names = { "--release" }, description = "Releases to load. Defaults to load all releases")
  public List<String> release = Lists.newArrayList();

  @Parameter(names = { "--new-files" }, arity = 1, description = "Load recently modified(less than 1 day) files only.")
  public boolean newFilesOnly = false;

  @Parameter(names = { "--exclude-files" }, description = "Files which should be excluded. E.g. ssm_m")
  public List<String> excludeFiles = Lists.newArrayList();

  @Parameter(names = { "--include-files" }, description = "Files which should be included.")
  public List<String> includeFiles = Lists.newArrayList();

  @Parameter(names = { "--exclude-projects" }, description = "Projects which should not be excluded. E.g. ALL-US")
  public List<String> excludeProjects = Lists.newArrayList();

  @Parameter(names = { "--include-projects" }, description = "Projects which should be loaded.")
  public List<String> includeProjects = Lists.newArrayList();

  @Parameter(names = { "--threads" }, description = "Number of threads. Defaults to 20")
  public int nThreads = 20;

  @Parameter(names = { "--db-type" }, description = "Defines back-end database. ORIENTDB or POSTGRES")
  public DatabaseType dbType = DatabaseType.POSTGRES;

  @Parameter(names = { "--skip-db-init" }, arity = 1, description = "Do not perform dataase initialization")
  public boolean skipDbInit = false;

  @Parameter(names = { "--output-dir" }, description = "Output directory")
  public String outputDir;

}
