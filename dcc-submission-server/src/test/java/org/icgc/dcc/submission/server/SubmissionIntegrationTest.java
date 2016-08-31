/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not,see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES,INCLUDING,BUT NOT LIMITED TO,THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,INDIRECT,                               
 * INCIDENTAL,SPECIAL,EXEMPLARY,OR CONSEQUENTIAL DAMAGES (INCLUDING,BUT NOT LIMITED                          
 * TO,PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,DATA,OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,WHETHER                              
 * IN CONTRACT,STRICT LIABILITY,OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE,EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.util.Joiners.PATH;
import static org.icgc.dcc.common.test.Tests.TEST_FIXTURES_DIR;
import static org.icgc.dcc.submission.dictionary.util.Dictionaries.writeDictionary;
import static org.icgc.dcc.submission.fs.ReleaseFileSystem.SYSTEM_FILES_DIR_NAME;
import static org.icgc.dcc.submission.release.model.ReleaseState.COMPLETED;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.QUEUED;
import static org.icgc.dcc.submission.release.model.SubmissionState.SIGNED_OFF;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALIDATING;
import static org.icgc.dcc.submission.server.test.Tests.$;
import static org.icgc.dcc.submission.server.test.Tests.CODELISTS_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.DICTIONARIES_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.NEXT_RELEASE_ENPOINT;
import static org.icgc.dcc.submission.server.test.Tests.PROJECTS_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.QUEUE_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.RELEASES_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.SEED_CODELIST_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.SEED_DICTIONARIES_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.SIGNOFF_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.UPDATE_RELEASE_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.VALIDATION_ENDPOINT;
import static org.icgc.dcc.submission.server.test.Tests.addScript;
import static org.icgc.dcc.submission.server.test.Tests.asDetailedSubmission;
import static org.icgc.dcc.submission.server.test.Tests.asRelease;
import static org.icgc.dcc.submission.server.test.Tests.asReleaseView;
import static org.icgc.dcc.submission.server.test.Tests.asString;
import static org.icgc.dcc.submission.server.test.Tests.codeListsToString;
import static org.icgc.dcc.submission.server.test.Tests.dataTypesToString;
import static org.icgc.dcc.submission.server.test.Tests.delete;
import static org.icgc.dcc.submission.server.test.Tests.dictionary;
import static org.icgc.dcc.submission.server.test.Tests.dictionaryToString;
import static org.icgc.dcc.submission.server.test.Tests.dictionaryVersion;
import static org.icgc.dcc.submission.server.test.Tests.get;
import static org.icgc.dcc.submission.server.test.Tests.post;
import static org.icgc.dcc.submission.server.test.Tests.put;
import static org.icgc.dcc.submission.server.test.Tests.replaceDictionaryVersion;
import static org.icgc.dcc.submission.server.web.ServerErrorCode.INVALID_STATE;
import static org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy.REPORT_FILES_INFO_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.dcc.AppUtils;
import org.icgc.dcc.common.core.model.FileTypes;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.server.sftp.Sftp;
import org.icgc.dcc.submission.server.test.BaseIntegrationTest;
import org.icgc.dcc.submission.server.test.MiniHadoop;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.dumbster.smtp.SimpleSmtpServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jcraft.jsch.SftpException;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubmissionIntegrationTest extends BaseIntegrationTest {

  /**
   * Switch that will change environments from "local" if {@code true} and "hadoop" if {@code false}.
   */
  private static final boolean LOCAL = true;

  /**
   * Switch that will change environments to "docker" if {@code true} and "embeeded" if {@code false}.
   * <p>
   * Only applieds when {@link #LOCAL} is {@code false}.
   */
  private static final boolean DOCKER = true;

  /**
   * Test file system.
   */
  private static final String DESTINATION_DIR_NAME = "submission";
  private static final String FS_DIR = PATH.join(TEST_FIXTURES_DIR, DESTINATION_DIR_NAME);

  /**
   * Projects.
   * 
   * If changing project names,must also change their directory counterparts under:
   * <p>
   * {@link file:///src/test/resources/fixtures/submission/fs/release1}.
   */
  private static final String PROJECT1_KEY = "project.1";
  private static final String PROJECT1 = String.format(
      "{name:'Project One',key:'%s',users:['admin','ricardo'],groups:['admin']}",
      PROJECT1_KEY);
  private static final String PROJECT2_KEY = "project.2";
  private static final String PROJECT2 = String.format(
      "{name:'Project Two',key:'%s',users:['admin','ricardo'],groups:['admin']}",
      PROJECT2_KEY);
  private static final String PROJECT3_KEY = "project.3";
  private static final String PROJECT3 = String.format(
      "{name:'Project Three',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT3_KEY);
  private static final String PROJECT4_KEY = "project.4";
  private static final String PROJECT4 = String.format(
      "{name:'Project Four',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT4_KEY);
  private static final String PROJECT5_KEY = "project.5";
  private static final String PROJECT5 = String.format(
      "{name:'Project Five',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT5_KEY);
  private static final String PROJECT6_KEY = "project.6";
  private static final String PROJECT6 = String.format(
      "{name:'Project Six',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT6_KEY);
  private static final String PROJECT7_KEY = "project.7";
  private static final String PROJECT7 = String.format(
      "{name:'Project Seven',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT7_KEY);
  private static final String PROJECT8_KEY = "project.8";
  private static final String PROJECT8 = String.format(
      "{name:'Project Eight',key:'%s',users:['admin'],groups:['admin']}",
      PROJECT8_KEY);

  private final static Map<String, SubmissionState> INITIAL_STATES =
      new ImmutableMap.Builder<String, SubmissionState>()
          .put(PROJECT1_KEY, NOT_VALIDATED)
          .put(PROJECT2_KEY, NOT_VALIDATED)
          .put(PROJECT3_KEY, NOT_VALIDATED)
          .put(PROJECT4_KEY, NOT_VALIDATED)
          .put(PROJECT5_KEY, NOT_VALIDATED)
          .put(PROJECT6_KEY, NOT_VALIDATED)
          .put(PROJECT7_KEY, NOT_VALIDATED)
          .put(PROJECT8_KEY, NOT_VALIDATED)
          .build();

  private final static Map<String, SubmissionState> POST_VALIDATION_STATES =
      new ImmutableMap.Builder<String, SubmissionState>()
          .put(PROJECT1_KEY, VALID)
          .put(PROJECT2_KEY, INVALID)
          .put(PROJECT3_KEY, INVALID)
          .put(PROJECT4_KEY, INVALID)
          .put(PROJECT5_KEY, INVALID)
          .put(PROJECT6_KEY, INVALID)
          .put(PROJECT7_KEY, INVALID)
          .put(PROJECT8_KEY, INVALID)
          .build();

  private final static Map<String, SubmissionState> POST_PARTIAL_REVALIDATION_STATES =
      new ImmutableMap.Builder<String, SubmissionState>()
          .put(PROJECT1_KEY, VALID)
          .put(PROJECT2_KEY, INVALID)
          .put(PROJECT3_KEY, INVALID)
          .put(PROJECT4_KEY, INVALID)
          .put(PROJECT5_KEY, INVALID)
          .put(PROJECT6_KEY, NOT_VALIDATED) // This project isn't included in the revalidation
          .put(PROJECT7_KEY, INVALID)
          .put(PROJECT8_KEY, INVALID)
          .build();

  private final static Map<String, SubmissionState> POST_RELEASE_STATES =
      new ImmutableMap.Builder<String, SubmissionState>()
          .put(PROJECT1_KEY, NOT_VALIDATED)
          .put(PROJECT2_KEY, INVALID)
          .put(PROJECT3_KEY, INVALID)
          .put(PROJECT4_KEY, INVALID)
          .put(PROJECT5_KEY, INVALID)
          .put(PROJECT6_KEY, NOT_VALIDATED)
          .put(PROJECT7_KEY, INVALID)
          .put(PROJECT8_KEY, INVALID)
          .build();

  private final static Map<String, SubmissionState> POST_TERM_ADDITION_STATES =
      new ImmutableMap.Builder<String, SubmissionState>()
          .put(PROJECT1_KEY, VALID)
          .put(PROJECT2_KEY, NOT_VALIDATED)
          .put(PROJECT3_KEY, NOT_VALIDATED)
          .put(PROJECT4_KEY, NOT_VALIDATED)
          .put(PROJECT5_KEY, NOT_VALIDATED)
          .put(PROJECT6_KEY, NOT_VALIDATED)
          .put(PROJECT7_KEY, NOT_VALIDATED)
          .put(PROJECT8_KEY, NOT_VALIDATED)
          .build();

  /**
   * Dictionaries.
   */
  private static final String SECOND_DICTIONARY = dictionaryToString();
  private static final String SECOND_DICTIONARY_VERSION = dictionaryVersion(SECOND_DICTIONARY);
  private static final String FIRST_DICTIONARY_VERSION = SECOND_DICTIONARY_VERSION + "-zero";
  private static final String FIRST_DICTIONARY = replaceDictionaryVersion(
      SECOND_DICTIONARY,
      SECOND_DICTIONARY_VERSION,
      FIRST_DICTIONARY_VERSION);
  private static final String SECOND_DICTIONARY_ARRAY = "[" + SECOND_DICTIONARY + "]";
  private static final String FIRST_DICTIONARY_ARRAY = "[" + FIRST_DICTIONARY + "]";

  /**
   * Releases.
   */
  private static final String INITITAL_RELEASE_NAME = "release1";
  private static final String INITIAL_RELEASE_ENDPOINT = RELEASES_ENDPOINT + "/" + INITITAL_RELEASE_NAME;
  private static final String INITIAL_RELEASE_SUBMISSIONS_ENDPOINT = INITIAL_RELEASE_ENDPOINT + "/submissions";

  private static final String INITIAL_RELEASE = "{"
      + "name: '" + INITITAL_RELEASE_NAME + "',"
      + "state: 'OPENED',"
      + "submissions: [],"
      + "dictionaryVersion: '" + FIRST_DICTIONARY_VERSION + "'}";
  private static final String NEXT_RELEASE_NAME = "release2";
  private static final String NEXT_RELEASE = "{"
      + "name:'" + NEXT_RELEASE_NAME + "', "
      + "dictionaryVersion: '" + SECOND_DICTIONARY_VERSION // FIXME: this shouldn't be pass (see DCC-1916)
      + "'}";

  /**
   * Messages.
   * 
   * @see http://stackoverflow.com/questions/1368163/is-there-a-standard-domain-for-testing-throwaway-email
   */
  private static final String PROJECT_TO_SIGN_OFF = "['" + PROJECT1_KEY + "']";
  private static final String PROJECT_DATA_TYPES = dataTypesToString();
  private static final String PROJECTS_TO_ENQUEUE = "["
      + "{key:'" + PROJECT1_KEY + "',emails:['project1@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT2_KEY + "',emails:['project2@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT3_KEY + "',emails:['project3@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT4_KEY + "',emails:['project4@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT5_KEY + "',emails:['project5@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT6_KEY + "',emails:['project6@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT7_KEY + "',emails:['project7@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT8_KEY + "',emails:['project8@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "}"
      + "]";

  private static final String PROJECTS_TO_ENQUEUE2 = "["
      + "{key:'" + PROJECT2_KEY + "',emails:['project2@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT3_KEY + "',emails:['project3@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT4_KEY + "',emails:['project4@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT5_KEY + "',emails:['project5@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT7_KEY + "',emails:['project7@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "},"
      + "{key:'" + PROJECT8_KEY + "',emails:['project8@example.org'], dataTypes: " + PROJECT_DATA_TYPES + "}]";

  /**
   * Submission file system.
   */
  private static final String PROJECT1_VALIDATION_DIR = INITITAL_RELEASE_NAME + "/" + PROJECT1_KEY + "/.validation";

  /**
   * Test utilities.
   */
  @Autowired
  private Datastore datastore;
  @Autowired
  private SubmissionProperties properties;

  private SimpleSmtpServer smtpServer;
  private MiniHadoop hadoop;
  private FileSystem fileSystem;

  @Rule
  public Sftp sftp = new Sftp("admin", "adminspasswd", false);

  @Before
  public void setUp() throws IOException {
    banner("Setting up ...");

    val config = new Configuration();
    if (LOCAL) {
      status("init", "Setting up local environment...");
      fileSystem = FileSystem.get(config);

      status("init", "Deleting local root filesystem...");
      fileSystem.delete(new Path(properties.getFs().getRoot()), true);
    } else {
      if (!DOCKER) {
        // Setup Embedded Hadoop infrastructure
        status("init", "Setting up embedded Hadoop environment...");
        hadoop = new MiniHadoop(config, 1, 1, new File("/tmp/hadoop"));
        fileSystem = hadoop.getFileSystem();

        // Config overrides for {@code SubmissionMain} consumption
        val jobConf = hadoop.createJobConf();
        System.setProperty("fs.url", jobConf.get("fs.defaultFS"));
        System.setProperty("hadoop.properties.mapred.job.tracker", jobConf.get("mapred.job.tracker"));
        AppUtils.setTestEnvironment();
      } else {
        // Setup Docker Hadoop infrastructure
        // Note: This is only useful for setting up mongo, staging files, etc. and trying to run a MR job will fail due
        // to no jar!
        status("init", "Setting up Docker Hadoop environment...");
        val fsUrl = "hdfs://localhost:8020";
        config.set("fs.defaultFS", fsUrl);
        fileSystem = FileSystem.get(config);

        status("init", "Deleting root filesystem...");
        fileSystem.delete(new Path(properties.getFs().getRoot()), true);

        // Config overrides for {@code SubmissionMain} consumption
        System.setProperty("fsUrl", fsUrl);
        System.setProperty("hadoop.fs.defaultFS", fsUrl);
        System.setProperty("hadoop.mapred.job.tracker", "localhost:8021");
      }
    }

    status("init", "Dropping database...");
    datastore.getDB().dropDatabase();

    status("init", "Starting SMTP server...");
    smtpServer = SimpleSmtpServer.start(Integer.valueOf(properties.getMail().getSmtpPort()));
  }

  @After
  @SneakyThrows
  public void tearDown() {
    log.info(repeat("-", 100));
    log.info("Tearing down ...");
    log.info(repeat("-", 100));

    status("shutdown", "Shutting down SMTP server...");
    smtpServer.stop();
    status("shutdown", "SMTP server shut down.");

    if (hadoop != null) {
      status("shutdown", "Shutting down hadoop...");
      hadoop.close();
    }

    banner("Shut down.");
  }

  @Test
  public void testSystem() throws Exception {
    status("test", "Starting test...");
    try {
      // Setup and staging of data
      seedSystem();
      adminCreatesRelease();
      userSubmitsFiles();

      userValidates();
      adminTweaksCodeListAndTerms();
      adminRevalidates();
      adminPerformsRelease();

      adminUpdatesDictionary();
      adminUpdatesRelease();
      dumpTestDictionary();
    } catch (Exception e) {
      status("test", "Caught exception: " + e);
      throw e;
    }

    status("test", "Finished test.");
  }

  private void seedSystem() throws IOException {
    status("seed", "Seeding dictionary 1 ({})...", FIRST_DICTIONARY_VERSION);
    assertThat(post(restTemplate, SEED_DICTIONARIES_ENDPOINT, FIRST_DICTIONARY_ARRAY).getStatusCode().is2xxSuccessful())
        .isTrue();

    status("seed", "Seeding dictionary 2 ({} from dcc-resources)...", SECOND_DICTIONARY_VERSION);
    assertThat(
        post(restTemplate, SEED_DICTIONARIES_ENDPOINT, SECOND_DICTIONARY_ARRAY).getStatusCode().is2xxSuccessful())
            .isTrue();

    status("seed", "Seeding code lists...");
    assertThat(post(restTemplate, SEED_CODELIST_ENDPOINT, codeListsToString()).getStatusCode().is2xxSuccessful())
        .isTrue();
  }

  private void adminCreatesRelease() throws Exception, IOException {
    status("admin", "Creating initial release...");
    createInitialRelease();
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates());

    status("admin", "Adding projects...");
    addProjects();
    checkRelease(
        INITITAL_RELEASE_NAME,
        FIRST_DICTIONARY_VERSION,
        OPENED,
        hasSubmisisonStates(getStates(INITIAL_STATES)));

    status("admin", "Updating OPEN dictionary...");
    updateDictionary(
        FIRST_DICTIONARY, FIRST_DICTIONARY_VERSION, NO_CONTENT);
  }

  @SneakyThrows
  private void userSubmitsFiles() throws IOException {
    val source = new Path(FS_DIR);
    val destination = new Path(properties.getFs().getRoot());
    status("user", "SFTP transferring files from '{}' to '{}'...", source, destination);

    boolean manipulatedFiles = false;

    try {
      sftp.connect();
      for (val releaseDir : new File(FS_DIR).listFiles()) {
        for (val projectDir : releaseDir.listFiles()) {
          val releaseName = projectDir.getParentFile().getName();
          for (val file : projectDir.listFiles()) {
            val projectName = file.getParentFile().getName();

            // Cannot submit system files via SFTP
            val system = projectName.equals(SYSTEM_FILES_DIR_NAME);
            if (system) {
              val path = new Path(destination, releaseName + "/" + SYSTEM_FILES_DIR_NAME + "/" + file.getName());
              status("user", "Installing system file from '{}' to '{}'...", file, path);
              fileSystem.copyFromLocalFile(new Path(file.getAbsolutePath()), path);
            } else {
              val path = projectName + "/" + file.getName();
              status("user", "SFTP transferring file from '{}' to '{}'...", file, path);
              sftp.put(path, file);

              if (!manipulatedFiles) {
                // Simulate some additional SFTP file interactions
                userManipulatesFiles(file, path);

                manipulatedFiles = true;
              }
            }
          }
        }
      }
    } finally {
      sftp.disconnect();
    }

    val list = fileSystem.listFiles(destination, true);
    while (list.hasNext()) {
      log.info("Copied: {}", list.next().getPath());
    }
  }

  private void userManipulatesFiles(File file, String path) throws SftpException {
    sftp.rename(path, path + ".bak");
    sftp.rename(path + ".bak", path);
    sftp.rm(path);
    sftp.put(path, file);
  }

  private void userValidates() throws Exception {
    // Triggers validations
    enqueueProjects(PROJECTS_TO_ENQUEUE, HttpStatus.NO_CONTENT);

    // TODO: Can't do this unless we can support a test method verifying a project is in either INVALID or NOT_VALIDATED
    // states due lack of synchronization with the validation scheduler (i.e. results are unpredictable):

    // status("user","Awaiting validation for project '{}'...",PROJECT2_KEY);
    // awaitValidatingState(PROJECT2_KEY);
    //
    // status("user","Cancelling validation for project '{}'...",PROJECT2_KEY);
    // cancelValidation(PROJECT2_KEY,OK);

    checkValidations();
  }

  private void adminTweaksCodeListAndTerms() throws IOException, Exception {
    status("admin", "Adding offending code list...");
    addInvalidCodeList();

    status("admin", "Adding valid code list...");
    addValidCodeLists();

    status("admin", "Adding code list term...");
    addCodeListTerms();
  }

  private void adminRevalidates() throws Exception {
    // Re-enqueue them since they have been reset by adding the term
    enqueueProjects(PROJECTS_TO_ENQUEUE2, HttpStatus.NO_CONTENT);

    status("admin", "Checking validated submission 1...");
    checkValidatedSubmission(PROJECT1_KEY, POST_PARTIAL_REVALIDATION_STATES.get(PROJECT1_KEY));

    status("admin", "Checking validated submission 2...");
    checkValidatedSubmission(PROJECT2_KEY, POST_PARTIAL_REVALIDATION_STATES.get(PROJECT2_KEY));

    status("admin", "Checking validated submission 3...");
    checkValidatedSubmission(PROJECT3_KEY, POST_PARTIAL_REVALIDATION_STATES.get(PROJECT3_KEY));

    status("admin", "Checking validated submission 4...");
    checkValidatedSubmission(PROJECT4_KEY, POST_PARTIAL_REVALIDATION_STATES.get(PROJECT4_KEY));

    status("admin", "Checking validated submission 5...");
    checkValidatedSubmission(PROJECT5_KEY, POST_PARTIAL_REVALIDATION_STATES.get(PROJECT5_KEY));

    status("admin", "Checking validated submission 6...");
    checkValidatedSubmission(PROJECT6_KEY, POST_PARTIAL_REVALIDATION_STATES.get(PROJECT6_KEY));

    status("admin", "Checking validated submission 7...");
    checkValidatedSubmission(PROJECT7_KEY, POST_PARTIAL_REVALIDATION_STATES.get(PROJECT7_KEY));

    status("admin", "Checking validated submission 8...");
    checkValidatedSubmission(PROJECT8_KEY, POST_PARTIAL_REVALIDATION_STATES.get(PROJECT8_KEY));

    // TODO: Make it such that adding a term fixed one of the submissions
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates(getStates(POST_PARTIAL_REVALIDATION_STATES)));
  }

  private void adminPerformsRelease() throws Exception {
    releaseInitialRelease();
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, COMPLETED,
        hasSubmisisonStates(SIGNED_OFF));
    checkRelease(NEXT_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates(getStates(POST_RELEASE_STATES)));
  }

  private void adminUpdatesDictionary() throws Exception, IOException {
    status("admin", "Updating CLOSED dictionary...");
    updateDictionary(
        FIRST_DICTIONARY, FIRST_DICTIONARY_VERSION, BAD_REQUEST);

    status("admin", "Updating OPENED dictionary...");
    updateDictionary(
        dictionaryToString(), SECOND_DICTIONARY_VERSION, NO_CONTENT);

    status("admin", "Adding Script restriction #1 to OPENED dictionary");
    Dictionary dictionary =
        addScript(dictionary(), FileTypes.FileType.SSM_M_TYPE.getId(),
            "note",
            "if (note == null) { return true; } else { return note != \"script_error_here\";}",
            "Note field cannot be 'script_error_here'");

    status("admin", "Adding Script restriction #2 to OPENED dictionary");
    dictionary =
        addScript(dictionary, FileTypes.FileType.SSM_M_TYPE.getId(),
            "note",
            "if (note == null) { return true; } else { return note.indexOf('_') == -1; }",
            "Note field cannot contain the underscore(_) character");

    status("admin", "Updating to new dictionary with script restrictions");
    updateDictionary(
        dictionaryToString(dictionary), SECOND_DICTIONARY_VERSION, NO_CONTENT);
  }

  private void adminUpdatesRelease() throws Exception {
    updateRelease(NEXT_RELEASE);
    checkRelease(
        NEXT_RELEASE_NAME,
        SECOND_DICTIONARY_VERSION,
        OPENED,
        hasSubmisisonStates(getStates(INITIAL_STATES)));
  }

  private void createInitialRelease() throws Exception {
    val response = put(restTemplate, RELEASES_ENDPOINT, INITIAL_RELEASE);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    val release = asRelease(response);
    assertEquals(INITITAL_RELEASE_NAME, release.getName());
  }

  private void addProjects() throws IOException {
    status("admin", "Adding project 1...");
    val response1 = post(restTemplate, PROJECTS_ENDPOINT, PROJECT1);
    assertEquals(CREATED, response1.getStatusCode());

    status("admin", "Adding project 2...");
    val response2 = post(restTemplate, PROJECTS_ENDPOINT, PROJECT2);
    assertEquals(CREATED, response2.getStatusCode());

    status("admin", "Adding project 3...");
    val response3 = post(restTemplate, PROJECTS_ENDPOINT, PROJECT3);
    assertEquals(CREATED, response3.getStatusCode());

    status("admin", "Adding project 4...");
    val response4 = post(restTemplate, PROJECTS_ENDPOINT, PROJECT4);
    assertEquals(CREATED, response4.getStatusCode());

    status("admin", "Adding project 5...");
    val response5 = post(restTemplate, PROJECTS_ENDPOINT, PROJECT5);
    assertEquals(CREATED, response5.getStatusCode());

    status("admin", "Adding project 6...");
    val response6 = post(restTemplate, PROJECTS_ENDPOINT, PROJECT6);
    assertEquals(CREATED, response6.getStatusCode());

    status("admin", "Adding project 7...");
    val response7 = post(restTemplate, PROJECTS_ENDPOINT, PROJECT7);
    assertEquals(CREATED, response7.getStatusCode());

    status("admin", "Adding project 8...");
    val response8 = post(restTemplate, PROJECTS_ENDPOINT, PROJECT8);
    assertEquals(CREATED, response8.getStatusCode());
  }

  private void enqueueProjects(String projectsToEnqueue, HttpStatus expectedStatus) throws Exception {
    status("user", "Getting queued projects...");
    ResponseEntity<String> response = get(restTemplate, QUEUE_ENDPOINT);
    String queued = asString(response);
    status("user", "Received queued projects: {}", queued);

    assertEquals(OK, response.getStatusCode());
    assertEquals("[]", queued);

    status("user", "Enqueuing projects...");
    response = post(restTemplate, QUEUE_ENDPOINT, projectsToEnqueue);
    assertEquals(expectedStatus, response.getStatusCode());
    if (expectedStatus != HttpStatus.NO_CONTENT) {
      JsonNode expected = $("{code:'" + INVALID_STATE.getFrontEndString() + "',parameters:['" + VALID + "']}");
      JsonNode actual = $(response);
      assertEquals(expected + " != " + actual, expected, actual);
    }
  }

  @SuppressWarnings("unused")
  private void cancelValidation(String projectKey, HttpStatus expectedStatus) throws Exception {
    val response = delete(restTemplate, VALIDATION_ENDPOINT + "/" + projectKey);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  private void addInvalidCodeList() throws IOException {
    // Ensure codelist is present
    status("admin", "Getting code lists...");
    ResponseEntity<String> response = get(restTemplate, CODELISTS_ENDPOINT);
    assertEquals(OK, response.getStatusCode());
    val codeListName = "appendix_B10";
    assertTrue(asString(response).contains(codeListName));

    // Attempt to add it again
    status("admin", "Adding invalid code list...");
    response =
        post(restTemplate, CODELISTS_ENDPOINT,
            "[{name:'someName'},{name:'" + codeListName + "'}]");
    assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  private void addValidCodeLists() throws IOException {
    status("admin", "Adding valid code lists...");
    val response = post(restTemplate, CODELISTS_ENDPOINT, "[{name:'someName'},{name:'someNewName'}]");
    assertEquals(CREATED, response.getStatusCode());
  }

  private void addCodeListTerms() throws Exception {
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates(getStates(POST_VALIDATION_STATES)));

    // TODO: Get codelist dynamically
    status("admin", "Adding code list terms...");
    val codeListName = "GLOBAL.0.platform.v1/terms";
    val response = post(restTemplate, CODELISTS_ENDPOINT + "/" + codeListName,
        "[{code:'1000',value:'new value 1'},{code:'10001',value:'new value 2'}]");
    assertEquals(CREATED, response.getStatusCode());

    // Only the INVALID ones should have been reset (DCC-851)
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        hasSubmisisonStates(getStates(POST_TERM_ADDITION_STATES)));
  }

  private void releaseInitialRelease() {
    // Attempts releasing (expect failure)
    status("admin", "Releasing attempt 1 (should fail)...");
    ResponseEntity<String> response = post(restTemplate, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(BAD_REQUEST, response.getStatusCode()); // no signed off projects

    // Sign off
    status("user", "Signing off project {}", PROJECT_TO_SIGN_OFF);
    response = post(restTemplate, SIGNOFF_ENDPOINT, PROJECT_TO_SIGN_OFF);
    assertEquals(OK, response.getStatusCode());

    // Attempt releasing again
    status("admin", "Releasing attempt 2 (should pass)...");
    response = post(restTemplate, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(asString(response), OK, response.getStatusCode());

    // Attempt releasing one too many times
    status("admin", "Releasing attempt 3 (should fail)...");
    response = post(restTemplate, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  private void updateDictionary(String dictionary, String dictionaryVersion, HttpStatus expectedStatus)
      throws Exception {
    val updatedSecondDictionary = dictionary.replace("Unique identifier for the donor",
        "Unique identifier for the donor (update" + 1 + ")");
    assertTrue(dictionary, dictionary.equals(updatedSecondDictionary) == false);

    status("admin", "Updating dictionary...");
    val response =
        put(restTemplate, DICTIONARIES_ENDPOINT + "/" + dictionaryVersion,
            updatedSecondDictionary);
    assertEquals(response.getHeaders().toString(), expectedStatus, response.getStatusCode());
  }

  private void updateRelease(String updatedRelease) throws Exception {
    val response = put(restTemplate, UPDATE_RELEASE_ENDPOINT, updatedRelease);
    assertEquals(OK, response.getStatusCode());

    val release = asRelease(response);
    assertEquals(NEXT_RELEASE_NAME, release.getName());
  }

  /**
   * TODO: improve this to make expectedSubmissionStates a map rather than a list (else order of project names could
   * break the test)
   * <p>
   * TODO: reuse checkValidatedSubmission() to while at it (since it's smarter and can poll)
   */
  private void checkRelease(String releaseName, String dictionaryVersion, ReleaseState expectedReleaseState,
      List<SubmissionState> expectedSubmissionStates) {
    status("admin", "Getting release '{}'...", releaseName);
    val response = get(restTemplate, RELEASES_ENDPOINT + "/" + releaseName);
    assertEquals(OK, response.getStatusCode());

    val releaseView = asReleaseView(response);
    assertNotNull(releaseView);

    assertEquals(dictionaryVersion, releaseView.getDictionaryVersion());
    assertEquals(expectedReleaseState, releaseView.getState());
    assertEquals(ImmutableList.<String> of(), releaseView.getQueue());
    assertEquals(expectedSubmissionStates.size(), releaseView.getSubmissions().size());

    int i = 0;
    for (val submission : releaseView.getSubmissions()) {
      assertEquals(submission.getProjectKey(), expectedSubmissionStates.get(i++), submission.getState());
    }
  }

  private void checkValidations() {
    status("user", "Getting release 1...");
    val response = get(restTemplate, INITIAL_RELEASE_ENDPOINT);
    assertEquals(OK, response.getStatusCode());

    status("user", "Checking validated submission 1...");
    checkValidatedSubmission(PROJECT1_KEY, POST_VALIDATION_STATES.get(PROJECT1_KEY));

    status("user", "Checking validated submission 2...");
    checkValidatedSubmission(PROJECT2_KEY, POST_VALIDATION_STATES.get(PROJECT2_KEY));

    status("user", "Checking validated submission 3...");
    checkValidatedSubmission(PROJECT3_KEY, POST_VALIDATION_STATES.get(PROJECT3_KEY));

    status("user", "Checking validated submission 4...");
    checkValidatedSubmission(PROJECT4_KEY, POST_VALIDATION_STATES.get(PROJECT4_KEY));

    status("user", "Checking validated submission 5...");
    checkValidatedSubmission(PROJECT5_KEY, POST_VALIDATION_STATES.get(PROJECT5_KEY));

    status("user", "Checking validated submission 6...");
    checkValidatedSubmission(PROJECT6_KEY, POST_VALIDATION_STATES.get(PROJECT6_KEY));

    status("user", "Checking validated submission 7...");
    checkValidatedSubmission(PROJECT7_KEY, POST_VALIDATION_STATES.get(PROJECT7_KEY));

    status("user", "Checking validated submission 8...");
    checkValidatedSubmission(PROJECT8_KEY, POST_VALIDATION_STATES.get(PROJECT8_KEY));

    // TODO: Do the negation of following for the projects the failed primary validation

    // Project 1
    assertEmptyFile(fileSystem,
        properties.getFs().getRoot(),
        PROJECT1_VALIDATION_DIR + "/donor.txt.bz2.internal" + REPORT_FILES_INFO_SEPARATOR + "errors.json");
    assertEmptyFile(fileSystem,
        properties.getFs().getRoot(),
        PROJECT1_VALIDATION_DIR + "/specimen.txt.gz.internal" + REPORT_FILES_INFO_SEPARATOR + "errors.json");
  }

  @SneakyThrows
  private void checkValidatedSubmission(String project, SubmissionState expectedSubmissionState) {
    DetailedSubmission detailedSubmission;
    do {
      sleepUninterruptibly(2, SECONDS);

      status("user", "Polling submission status...");
      val response = get(restTemplate, INITIAL_RELEASE_SUBMISSIONS_ENDPOINT + "/" + project);
      detailedSubmission = asDetailedSubmission(response);
      status("user", "Received submission status: {}", detailedSubmission);

      assertEquals(OK, response.getStatusCode());
    } while (detailedSubmission.getState() == QUEUED || detailedSubmission.getState() == VALIDATING);

    assertEquals(project, expectedSubmissionState, detailedSubmission.getState());
  }

  @SuppressWarnings("unused")
  private void awaitValidatingState(String project) {
    DetailedSubmission detailedSubmission;
    do {
      sleepUninterruptibly(2, SECONDS);

      status("user", "Polling submission validation status...");
      val response = get(restTemplate, INITIAL_RELEASE_SUBMISSIONS_ENDPOINT + "/" + project);
      detailedSubmission = asDetailedSubmission(response);
      status("user", "Received submission validation status: {}", detailedSubmission);

      assertEquals(OK, response.getStatusCode());
    } while (detailedSubmission.getState() == QUEUED);
  }

  private static List<SubmissionState> hasSubmisisonStates(SubmissionState... states) {
    return newArrayList(states);
  }

  private void dumpTestDictionary() {
    writeDictionary(
        dictionary(),
        "/tmp/dictionary_submission_integration_test.json");
  }

  private static void banner(String message) {
    // Bruce:
    log.info(repeat("-", 100));
    log.info(message);
    log.info(repeat("-", 100));
  }

  private static void status(String phase, String message, Object... args) {
    log.info("[" + phase + "] " + message, args);
  }

  private SubmissionState[] getStates(@NonNull final Map<String, SubmissionState> states) {
    checkArgument(!states.isEmpty());

    return toArray(states.values(), SubmissionState.class);
  }

}
