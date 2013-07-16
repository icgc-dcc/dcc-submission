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
package org.icgc.dcc.submission.release;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.List;

import org.icgc.dcc.core.model.QProject;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.release.model.Release;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.collect.ImmutableList;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public abstract class BaseRelease implements HasRelease, Serializable {

  private final transient Datastore datastore;

  private final transient Morphia morphia;

  private final transient DccFileSystem dccFilesystem;

  private final Release release;

  protected BaseRelease(Release release, Morphia morphia, Datastore datastore, DccFileSystem fs) {
    checkArgument(release != null);
    checkArgument(morphia != null);
    checkArgument(datastore != null);
    checkArgument(fs != null);
    this.release = release;
    this.morphia = morphia;
    this.datastore = datastore;
    this.dccFilesystem = fs;
  }

  @Override
  public ReleaseFileSystem getReleaseFilesystem() {
    return this.dccFilesystem.getReleaseFilesystem(this.release);
  }

  @Override
  public List<Project> getProjects() {
    return new MorphiaQuery<Project>(morphia(), datastore(), QProject.project).where(
        QProject.project.key.in(ImmutableList.copyOf(getRelease().getProjectKeys()))).list();
  }

  public List<String> getProjectKeys() {
    return ImmutableList.copyOf(getRelease().getProjectKeys());
  }

  /**
   * Returns the {@code Release} (guaranteed not to be null).
   */
  @Override
  public Release getRelease() {
    return release;
  }

  protected Morphia morphia() {
    return morphia;
  }

  protected Datastore datastore() {
    return datastore;
  }

  protected DccFileSystem fileSystem() {
    return dccFilesystem;
  }

}
