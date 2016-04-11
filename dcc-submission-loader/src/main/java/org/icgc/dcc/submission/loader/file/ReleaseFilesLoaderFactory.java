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
package org.icgc.dcc.submission.loader.file;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import org.icgc.dcc.submission.loader.core.DependencyFactory;
import org.icgc.dcc.submission.loader.file.orientdb.OrientdbFileLoaderFactory;
import org.icgc.dcc.submission.loader.file.postgres.PostgressFileLoaderFactory;
import org.icgc.dcc.submission.loader.model.DatabaseType;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public final class ReleaseFilesLoaderFactory {

  public static ReleaseFilesLoader createReleaseFilesLoader(@NonNull String release, @NonNull DatabaseType dbType) {
    switch (dbType) {
    case ORIENTDB:
      return createOrientDbLoader(release);
    case POSTGRES:
      return createPostgresLoader(release);
    default:
      throw new IllegalArgumentException(format("Unsupported database %s", dbType));
    }

  }

  private static ReleaseFilesLoader createOrientDbLoader(String release) {
    val dependencyFactory = DependencyFactory.getInstance();
    val dbService = dependencyFactory.getDatabaseService(DependencyFactory.connect(), release);
    val fileLoaderFactory = new OrientdbFileLoaderFactory();
    val completionService = dependencyFactory.createCompletionService();

    return new ReleaseFilesLoader(release, dbService, fileLoaderFactory, completionService);
  }

  private static ReleaseFilesLoader createPostgresLoader(String release) {
    val dependencyFactory = DependencyFactory.getInstance();
    val dbService = dependencyFactory.createPostgresDatabaseService(release);
    val fileLoaderFactory = new PostgressFileLoaderFactory(release, dependencyFactory.getDataSource());
    val completionService = dependencyFactory.createCompletionService();

    return new ReleaseFilesLoader(release, dbService, fileLoaderFactory, completionService);
  }

}
