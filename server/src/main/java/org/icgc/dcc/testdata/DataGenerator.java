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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.UserService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.FileSchemaRole;
import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.dictionary.model.ValueType;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.CompletedRelease;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.typesafe.config.Config;

/**
 * using data.js as a template to populate data
 */
public class DataGenerator {

  private final DictionaryService dictionaryService;

  private final ProjectService projectService;

  private final ReleaseService releaseService;

  private final UserService userService;

  private final Config config;

  private final FileSystem fileSystem;

  @Inject
  public DataGenerator(DictionaryService dict, ProjectService project, ReleaseService release, UserService user,
      Config config, FileSystem fileSystem) {
    this.dictionaryService = dict;
    this.projectService = project;
    this.releaseService = release;
    this.userService = user;
    this.config = config;
    this.fileSystem = fileSystem;
  }

  public void generateTestData() {
    // add Admin User
    User user = new User();
    user.setUsername("admin");
    user.getRoles().add("admin");
    this.userService.saveUser(user);

    // add Project1
    Project project1 = new Project("project1");
    project1.setKey("project1");
    List<String> users = new ArrayList<String>();
    users.add("admin");
    project1.setUsers(users);
    List<String> groups = new ArrayList<String>();
    groups.add("admin");
    project1.setGroups(groups);
    this.projectService.addProject(project1);

    // add Project2
    Project project2 = new Project("project2");
    project2.setKey("project2");
    users.clear();
    users.add("admin");
    project2.setUsers(users);
    groups.clear();
    groups.add("admin");
    project2.setGroups(groups);
    this.projectService.addProject(project2);

    // add Project3
    Project project3 = new Project("project3");
    project3.setKey("project3");
    users.clear();
    users.add("bogus");
    project3.setUsers(users);
    groups.clear();
    groups.add("bogus");
    project3.setGroups(groups);
    this.projectService.addProject(project3);

    // add Project4
    Project project4 = new Project("project4");
    project4.setKey("project4");
    users.clear();
    users.add("bogus");
    project4.setUsers(users);
    groups.clear();
    groups.add("bogus");
    project4.setGroups(groups);
    this.projectService.addProject(project4);

    // add Dictionary
    Dictionary firstDict = new Dictionary("1.0");

    // add FileSchema to Dictionary
    FileSchema file = new FileSchema("donor");
    file.setPattern("^\\w+__\\d+__\\d+__donor__\\d+\\.txt$");
    file.setRole(FileSchemaRole.SUBMISSION);

    // create at least one restriction
    BasicDBObject config = new BasicDBObject();
    config.put("values", "");

    Restriction restriction = new Restriction();
    restriction.setType("in");
    restriction.setConfig(config);
    List<Restriction> restrictions = Arrays.asList(restriction);

    // add Field donor_id to FileSchema
    Field donor_id = new Field();
    donor_id.setName("donor_id");
    donor_id
        .setLabel("Unique identifier for the donor; assigned by data provider. It must be coded, and correspond to a donor ID listed in the donor data file.");
    donor_id.setValueType(ValueType.TEXT);
    file.addField(donor_id);

    // add Field donor_sex to FileSchema
    Field donor_sex = new Field();
    donor_sex.setName("donor_sex");
    donor_sex
        .setLabel("Donor biological sex. \"Other\" has been removed from the controlled vocabulary due to identifiability concerns.");
    donor_sex.setValueType(ValueType.TEXT);
    file.addField(donor_sex);

    // add Field specimen_id to FileSchema
    Field donor_notes = new Field();
    donor_notes.setName("donor_notes");
    donor_notes.setLabel("notes");
    donor_notes.setValueType(ValueType.TEXT);
    donor_notes.setRestrictions(restrictions);
    file.addField(donor_notes);

    firstDict.addFile(file);

    this.dictionaryService.add(firstDict);

    // add CodeList
    this.dictionaryService.createCodeList("appendix_B10");
    this.dictionaryService.addTerm("appendix_B10", new Term("1", "GRCh37", ""));
    this.dictionaryService.addTerm("appendix_B10", new Term("2", "NCBI36", ""));

    this.dictionaryService.createCodeList("appendix_B12");
    this.dictionaryService.addTerm("appendix_B12", new Term("1", "EGA", ""));
    this.dictionaryService.addTerm("appendix_B12", new Term("2", "dbSNP", ""));

    // add Release
    Release firstRelease = new Release("release1");
    firstRelease.setDictionary(firstDict);
    firstRelease.setState(ReleaseState.OPENED);

    // add submission1
    Submission submission1 = new Submission();
    submission1.setProjectKey("project1");
    submission1.setState(SubmissionState.SIGNED_OFF);
    firstRelease.getSubmissions().add(submission1);

    // add submission2
    Submission submission2 = new Submission();
    submission2.setProjectKey("project2");
    submission2.setState(SubmissionState.SIGNED_OFF);
    firstRelease.getSubmissions().add(submission2);

    // add submission3
    Submission submission3 = new Submission();
    submission3.setProjectKey("project3");
    submission3.setState(SubmissionState.QUEUED);
    firstRelease.getSubmissions().add(submission3);

    // add submission4
    Submission submission4 = new Submission();
    submission4.setProjectKey("project4");
    submission4.setState(SubmissionState.QUEUED);
    firstRelease.getSubmissions().add(submission4);

    // firstRelease.enqueue("project3");
    // firstRelease.enqueue("project4");

    this.releaseService.createInitialRelease(firstRelease);

    // release release1
    Release secondRelease = new Release("release2");
    secondRelease.setDictionary(firstDict);

    Submission submission5 = new Submission();
    submission5.setProjectKey("project5");
    submission5.setState(SubmissionState.QUEUED);
    secondRelease.getSubmissions().add(submission5);

    Submission submission6 = new Submission();
    submission6.setProjectKey("project6");
    submission6.setState(SubmissionState.NOT_VALIDATED);
    secondRelease.getSubmissions().add(submission6);

    // this.releaseService.getNextRelease().release(secondRelease);
  }

  public void generateFileSystem() {
    DccFileSystem filesystem =
        new DccFileSystem(this.config, this.releaseService, this.projectService, this.fileSystem);

    for(CompletedRelease release : this.releaseService.getCompletedReleases()) {
      filesystem.createReleaseFilesystem(release.getRelease());
    }

    filesystem.createReleaseFilesystem(this.releaseService.getNextRelease().getRelease());
  }
}
