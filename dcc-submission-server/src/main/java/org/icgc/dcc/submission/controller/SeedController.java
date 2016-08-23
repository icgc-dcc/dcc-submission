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
package org.icgc.dcc.submission.controller;

import java.io.IOException;
import java.io.InputStream;

import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.model.Release;
import org.mongodb.morphia.Datastore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: discard class: DCC-819 (was originally created in the context of DCC-135)
 * <p>
 * The integration test currently relies on it
 */
@Slf4j
@RestController
@RequestMapping("/ws/seed")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SeedController {

  /**
   * Dependencies.
   */
  private final Datastore datastore;
  private final DccFileSystem fileSystem;

  @SuperUser
  @PostMapping("users")
  public ResponseEntity<?> seedUsers(
      @Valid @RequestBody User[] users,
      @RequestParam(name = "delete", defaultValue = "false") boolean delete) {
    log.info("Seeding users...");
    if (delete) {
      datastore.getCollection(User.class).drop();
    }
    datastore.save(users);
    return created();
  }

  @SuperUser
  @PostMapping("projects")
  public ResponseEntity<?> seedProjects(
      @Valid @RequestBody Project[] projects,
      @RequestParam(name = "delete", defaultValue = "false") boolean delete) {
    log.info("Seeding projects...");
    if (delete) {
      datastore.getCollection(Project.class).drop();
    }
    datastore.save(projects);
    return created();
  }

  @SuperUser
  @PostMapping("releases")
  public ResponseEntity<?> seedReleases(
      @Valid @RequestBody Release[] releases,
      @RequestParam(name = "delete", defaultValue = "false") boolean delete) {
    log.info("Seeding releases...");
    if (delete) {
      datastore.getCollection(Release.class).drop();
    }
    datastore.save(releases);
    return created();
  }

  @SuperUser
  @PostMapping("dictionaries")
  public ResponseEntity<?> seedDictionaries(
      @Valid @RequestBody Dictionary[] dictionaries,
      @RequestParam(name = "delete", defaultValue = "false") boolean delete) {
    log.info("Seeding dictionaries...");
    if (delete) {
      datastore.getCollection(Dictionary.class).drop();
    }

    datastore.save(dictionaries);

    return created();
  }

  @SuperUser
  @PostMapping("codelists")
  public ResponseEntity<?> seedCodeLists(
      @Valid @RequestBody CodeList[] codelists,
      @RequestParam(name = "delete", defaultValue = "false") boolean delete) {
    log.info("Seeding code lists...");
    if (delete) {
      datastore.getCollection(CodeList.class).drop();
    }

    datastore.save(codelists);

    return created();
  }

  @SuperUser
  @PostMapping("fs/{filepath: .*}")
  public ResponseEntity<?> seedFileSystem(
      @PathVariable("filepath") String filename, InputStream fileContents) {
    log.info("Seeding file system...");
    FileSystem fs = this.fileSystem.getFileSystem();
    val destinationPath = new org.apache.hadoop.fs.Path(fileSystem.getRootStringPath() + "/" + filename);

    try {
      val fileDestination = fs.create(destinationPath);

      IOUtils.copy(fileContents, fileDestination);
      fileDestination.flush();
      fileDestination.close();
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    return created();
  }

  public ResponseEntity<?> created() {
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

}
