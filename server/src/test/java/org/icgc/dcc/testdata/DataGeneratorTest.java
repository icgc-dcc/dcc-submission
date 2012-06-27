/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.testdata;

import java.net.UnknownHostException;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.CoreModule;
import org.icgc.dcc.filesystem.DccFileSystemException;
import org.icgc.dcc.filesystem.FileSystemModule;
import org.icgc.dcc.filesystem.GuiceJUnitRunner;
import org.icgc.dcc.filesystem.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.http.HttpModule;
import org.icgc.dcc.http.jersey.JerseyModule;
import org.icgc.dcc.model.BaseEntity;
import org.icgc.dcc.model.ModelModule;
import org.icgc.dcc.model.dictionary.DictionaryService;
import org.icgc.dcc.service.ProjectService;
import org.icgc.dcc.service.ReleaseService;
import org.icgc.dcc.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.inject.Inject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.typesafe.config.Config;

/**
 * 
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ConfigModule.class, CoreModule.class,//
HttpModule.class, JerseyModule.class,// TODO: find out why those two seem necessary
ModelModule.class, FileSystemModule.class })
public class DataGeneratorTest {

  private FileSystem fileSystem;

  private Config config;

  private DictionaryService dictService;

  private ProjectService projectService;

  private ReleaseService releaseService;

  private UserService userService;

  @Inject
  public void setFileSystem(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  @Inject
  public void setConfig(Config config) {
    this.config = config;
  }

  @Test
  public void test_integration_datamodel() throws UnknownHostException, MongoException {
    // use local host as test MongoDB for now
    Mongo mongo = new Mongo("localhost");
    Morphia morphia = new Morphia();
    morphia.map(BaseEntity.class);
    Datastore datastore = morphia.createDatastore(mongo, "icgc");

    // Clear out the test database before each test
    mongo.getDB("icgc").getCollection("Release").drop();
    mongo.getDB("icgc").getCollection("Project").drop();
    mongo.getDB("icgc").getCollection("Dictionary").drop();
    mongo.getDB("icgc").getCollection("User").drop();
    mongo.getDB("icgc").getCollection("CodeList").drop();

    this.dictService = new DictionaryService(morphia, datastore, null);
    this.projectService = new ProjectService(morphia, datastore);
    this.releaseService = new ReleaseService(morphia, datastore);
    this.userService = new UserService(morphia, datastore);

    DataGenerator generator =
        new DataGenerator(dictService, projectService, releaseService, userService, config, fileSystem);

    generator.generateTestData();
    try {
      generator.generateFileSystem();
    } catch(DccFileSystemException e) {
      e.printStackTrace();
    }
  }

}
