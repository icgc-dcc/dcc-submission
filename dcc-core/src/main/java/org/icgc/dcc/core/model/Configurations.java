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
package org.icgc.dcc.core.model;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;

/**
 * Contains keys used in configuration files and used across components.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Configurations {

  /**
   * Submitter component.
   */
  public static final String FS_URL_KEY = "fs.url";
  public static final String FS_ROOT_KEY = "fs.root";
  public static final String MONGO_URI_KEY = "mongo.uri";

  /**
   * ETL component.
   */
  public static final String RELEASE_MONGO_URI_KEY = "releaseMongoUri";
  public static final String FS_LOADER_ROOT = "fsLoaderRoot";
  public static final String SUBMISSIONS_KEY = "submissions";
  public static final String HADOOP_KEY = "hadoop";
  public static final String IDENTIFIER_CLIENT_CLASS_NAME_KEY = "identifierClientClassName";
  public static final String IDENTIFIER_KEY = "identifier";
  public static final String PROJECTS_KEY = "projects";

}
