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
package org.icgc.dcc.integration;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.integration.TestUtils.asDetailedSubmission;
import static org.icgc.dcc.integration.TestUtils.get;
import static org.icgc.dcc.integration.TestUtils.post;
import static org.icgc.dcc.integration.TestUtils.put;
import static org.icgc.dcc.integration.TestUtils.resourceToJsonArray;
import static org.icgc.dcc.integration.TestUtils.resourceToString;
import static org.icgc.dcc.release.model.SubmissionState.QUEUED;
import static org.icgc.dcc.release.model.SubmissionState.VALID;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.core.Response;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.loader.Main;
import org.icgc.dcc.release.model.DetailedSubmission;

import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.wordnik.system.mongodb.SnapshotUtil;

public class LoaderIntegrationTestExporter {

  protected Config config;

  protected Configuration conf;

  protected FileSystem fs;

  protected final Client client = ClientFactory.newClient();

  protected void setup(String fileName) throws IOException, URISyntaxException, InterruptedException {
    this.config = ConfigFactory.load(fileName);
    this.conf = new Configuration();
    this.fs = FileSystem.get(new URI(getFsUrl()), conf, "hdfs");
  }

  /**
   * @throws IOException
   */
  protected void cleanStorage() throws IOException {
    // Remove the root file system
    Path path = new Path(getRootDir());
    if(fs.exists(path)) {
      checkState(fs.delete(path, true));
    }

    // Drop test databases
    MongoURI uri = new MongoURI(getMongoUri());
    Mongo mongo = uri.connect();
    for(String databaseName : mongo.getDatabaseNames()) {
      mongo.dropDatabase(databaseName);
    }
  }

  protected String getFsUrl() {
    return config.getString("fs.url");
  }

  protected String getRootDir() {
    return config.getString("fs.root");
  }

  protected String getMongoUri() {
    return config.getString("mongo.uri");
  }

  /**
   * Configuration file. Change this to switch environments.
   */
  // NOTE: To test against HDFS:
  // - set to dev
  // - change realm.ini path in application_dev.conf
  // - comment out the test body
  // - run (will fail on validation due to jar)
  // - set to local
  // - restore realm.ini path
  // - Set loader.Main to use: public static final String MONGODB_URL = "mongodb://10.0.3.154";
  // - mvn package -DskipTests=true
  // - cd target
  // - java -Xmx1g -cp dcc-server-1.5.jar org.icgc.dcc.loader.Main dev release3
  // - view job status at http://hcn51.res.oicr.on.ca:50030/
  private static final String ENV = "local"; // local, dev, prod

  /**
   * Test metadata constants.
   */
  // @formatter:off
  private static final int RELEASE_ID = 3;
  private static final String RELEASE_NAME = "release" + RELEASE_ID;
  private static final String PROJECT_NAME = "project1";
  private static final String RELEASE = "{\"name\":\"" + RELEASE_NAME + "\", \"state\":\"OPENED\",\"submissions\":[{\"projectKey\":\"" + PROJECT_NAME + "\",\"state\":\"NOT_VALIDATED\"}],\"dictionaryVersion\":\"0.6c\"}";
  private static final String PROJECT = "{\"name\":\"Project One\",\"key\":\"" + PROJECT_NAME + "\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}";
  private static final String PROJECT_TO_SIGN_OFF = "[\"" + PROJECT_NAME + "\"]";
  private static final String PROJECTS_TO_ENQUEUE = "[{\"key\": \"" + PROJECT_NAME + "\", \"emails\": [\"a@a.ca\"]}]";
  private static final String NEXT_RELEASE = "{\"name\": \"release" + (RELEASE_ID + 1) + "\"}";
  // @formatter:on

  /**
   * REST endpoint constants.
   */
  // @formatter:off
  private static final String SEED_ENDPOINT = "/seed";
  private static final String SEED_CODELIST_ENDPOINT = SEED_ENDPOINT + "/codelists";
  private static final String SEED_DICTIONARIES_ENDPOINT = SEED_ENDPOINT + "/dictionaries";
  private static final String PROJECTS_ENDPOINT = "/projects";
  private static final String RELEASES_ENDPOINT = "/releases";
  private static final String NEXT_RELEASE_ENPOINT = "/nextRelease";
  private static final String SIGNOFF_ENDPOINT = NEXT_RELEASE_ENPOINT + "/signed";
  private static final String QUEUE_ENDPOINT = NEXT_RELEASE_ENPOINT + "/queue";
  private static final String RELEASE_ENDPOINT = RELEASES_ENDPOINT + "/" + RELEASE_NAME;
  private static final String RELEASE_SUBMISSIONS_ENDPOINT = RELEASE_ENDPOINT + "/submissions";
  // @formatter:on

  /**
   * Resource constants.
   */
  // @formatter:off
  private static final String INTEGRATION_TEST_RESOURCE_DIR = "/loader-integration-test";
  private static final String CODELISTS_RESOURCE = INTEGRATION_TEST_RESOURCE_DIR + "/codelists.json";
  private static final String DICTIONARY_RESOURCE = INTEGRATION_TEST_RESOURCE_DIR + "/dictionary.json";
  // @formatter:on

  /**
   * File system constants.
   */
  // @formatter:off
  private static final String INTEGRATION_TEST_DIR = "src/test/resources/loader-integration-test";
  private static final String MONGO_IMPORT_DIR = INTEGRATION_TEST_DIR + "/mongo-import";
  private static final String FS_DIR = INTEGRATION_TEST_DIR + "/fs";
  private static final String SYSTEM_FILES_DIR = FS_DIR + "/SystemFiles";
  // @formatter:on

  /**
   * Perform a clean release with a single validated project
   * 
   * @throws Exception
   */
  public void export() throws Exception {
    setup(Main.CONFIG.valueOf(ENV).filename);

    // Basic sequence to initialize and validate a single project
    cleanStorage();
    startValidator();
    seedDb();
    createRelease();
    addProject();
    uploadSubmission();
    enqueueProject();
    validateSubmission();
    signOffProject();
    releaseRelease();

    exportDb("icgc-dev", MONGO_IMPORT_DIR);

    teardown();
  }

  public static void main(String[] args) throws Exception {
    new LoaderIntegrationTestExporter().export();
  }

  /**
   * Shuts down the validator / submitter threads.
   */
  public void teardown() {
    org.icgc.dcc.Main.shutdown();
  }

  /**
   * Starts the validator web service.
   * 
   * @throws IOException
   */
  private void startValidator() throws IOException {
    String[] args = { ENV };
    org.icgc.dcc.Main.main(args);
  }

  /**
   * Seeds reference data.
   * 
   * @throws IOException
   */
  private void seedDb() throws IOException {
    post(client, SEED_DICTIONARIES_ENDPOINT, resourceToJsonArray(DICTIONARY_RESOURCE));
    post(client, SEED_CODELIST_ENDPOINT, resourceToString(CODELISTS_RESOURCE));
  }

  /**
   * Creates a release.
   * 
   * @throws Exception
   */
  private void createRelease() throws Exception {
    Response response = put(client, RELEASES_ENDPOINT, RELEASE);
    assertEquals(200, response.getStatus());
  }

  /**
   * Adds a new project.
   */
  private void addProject() {
    Response response = post(client, PROJECTS_ENDPOINT, PROJECT);
    assertEquals(201, response.getStatus());
  }

  /**
   * Uploads a valid submission.
   * 
   * @throws IOException
   * @throws URISyntaxException
   */
  private void uploadSubmission() throws IOException, URISyntaxException {
    // Creating a release makes the dir again, so delete (hadoop fs requirement?)
    fs.delete(new Path(getRootDir() + "/" + RELEASE_NAME), true);

    fs.copyFromLocalFile(false, true, new Path(FS_DIR + "/" + RELEASE_NAME),
        new Path(getRootDir() + "/" + RELEASE_NAME));
    fs.copyFromLocalFile(false, true, new Path(SYSTEM_FILES_DIR), new Path(getRootDir() + "/" + RELEASE_NAME
        + "/SystemFiles"));
  }

  /**
   * Queue the added project.
   * 
   * @throws Exception
   */
  private void enqueueProject() throws Exception {
    Response response = get(client, QUEUE_ENDPOINT);
    assertEquals(200, response.getStatus());

    response = post(client, QUEUE_ENDPOINT, PROJECTS_TO_ENQUEUE);
    assertEquals(200, response.getStatus());
  }

  /**
   * Validates the queued project.
   * 
   * @throws Exception
   */
  private void validateSubmission() throws Exception {
    DetailedSubmission detailedSubmission;
    do {
      sleepUninterruptibly(2, SECONDS);

      Response response = get(client, RELEASE_SUBMISSIONS_ENDPOINT + "/" + PROJECT_NAME);
      assertEquals(200, response.getStatus());

      detailedSubmission = asDetailedSubmission(response);
    } while(detailedSubmission.getState() == QUEUED);

    assertEquals(VALID, detailedSubmission.getState());
  }

  /**
   * Signs off the validated project
   */
  private void signOffProject() {
    Response response = post(client, SIGNOFF_ENDPOINT, PROJECT_TO_SIGN_OFF);
    assertEquals(200, response.getStatus());
  }

  /**
   * Releases the signed-off project.
   */
  private void releaseRelease() {
    Response response = post(client, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(200, response.getStatus());
  }

  /**
   * Export all collections in {@code dbName} to {@code outputDir} as serialized sequence files of BSON objects.
   * 
   * @param dbName
   * @param outputDir
   */
  private static void exportDb(String dbName, String outputDir) {
    SnapshotUtil.main("-d", dbName, "-o", outputDir);
  }

}
