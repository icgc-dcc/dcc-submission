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

import org.icgc.dcc.model.BaseEntity;
import org.icgc.dcc.model.dictionary.DictionaryService;
import org.icgc.dcc.service.ProjectService;
import org.icgc.dcc.service.ReleaseService;
import org.icgc.dcc.service.UserService;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

/**
 * 
 */
public class DataGeneratorTest {

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

    DictionaryService dictService = new DictionaryService(morphia, datastore, null);
    ProjectService projectService = new ProjectService(morphia, datastore);
    ReleaseService releaseService = new ReleaseService(morphia, datastore);
    UserService userService = new UserService(morphia, datastore);

    DataGenerator generator = new DataGenerator(dictService, projectService, releaseService, userService);
    generator.generateTestData();
  }

}
