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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.dictionary.model.DictionaryState.OPENED;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.MISSING_REQUIRED_DATA;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.NAME_MISMATCH;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.NO_SUCH_ENTITY;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.RESOURCE_CLOSED;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.icgc.dcc.submission.dictionary.DictionaryValidator;
import org.icgc.dcc.submission.dictionary.DictionaryValidator.DictionaryConstraintViolations;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.service.DictionaryService;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ws/dictionaries")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DictionaryController {

  /**
   * Custom HTTP headers for validation.
   */
  public static final String VALIDATION_ERROR_HEADER = "X-Validation-Error";
  public static final String VALIDATION_WARNING_HEADER = "X-Validation-Warning";

  private final DictionaryService dictionaryService;

  private final boolean validate = true;

  @CrossOrigin
  @GetMapping
  public List<Dictionary> getDictionary() {
    // No authorization check necessary
    log.debug("Getting dictionaries");
    List<Dictionary> dictionaries = dictionaryService.getDictionaries();
    if (dictionaries == null) {
      dictionaries = newArrayList();
    }

    return dictionaries;
  }

  @CrossOrigin
  @GetMapping("versions")
  public ResponseEntity<?> getDictionaryVersions() {
    val response = dictionaryService.getVersions();
    if (response == null) {
      return ResponseEntity.ok(newArrayList());
    }
    return ResponseEntity.ok(response);
  }

  @CrossOrigin
  @GetMapping("current")
  public Dictionary getCurrentDictionary() {
    return dictionaryService.getCurrentDictionary();
  }

  @CrossOrigin
  @GetMapping("{version}")
  public ResponseEntity<?> getDictionary(@PathVariable("version") String version) {
    // No authorization check necessary
    log.debug("Getting dictionary: {}", version);
    Dictionary dict = this.dictionaryService.getDictionaryByVersion(version);
    if (dict == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ServerErrorResponseMessage(NO_SUCH_ENTITY, version));
    }

    return ResponseEntity.ok(dict);
  }

  /**
   * See {@link DictionaryService#addDictionary(Dictionary)} for details.
   */
  @SuperUser
  @PostMapping
  public ResponseEntity<?> addDictionary(@Valid @RequestBody Dictionary dict) {
    log.info("Adding dictionary: {}", dict == null ? null : dict.getVersion());

    val violations = validateDictionary(dict);
    if (violations.hasErrors()) {
      val errors = new StringBuilder("The request entity had the following errors:\n");
      for (val error : violations.getErrors()) {
        errors.append("  * ").append(error).append('\n');
      }

      return ResponseEntity
          .status(HttpStatus.UNPROCESSABLE_ENTITY)
          .header(VALIDATION_ERROR_HEADER, errors.toString())
          .build();
    }

    dictionaryService.addDictionary(dict);
    val url = getDictionaryURL(dict);

    if (violations.hasWarnings()) {
      val warnings = new StringBuilder("Created, but request entity had the following warnings:\n");
      for (val error : violations.getErrors()) {
        warnings.append("  * ").append(error).append('\n');
      }

      return ResponseEntity
          .created(url)
          .header(VALIDATION_WARNING_HEADER, warnings.toString())
          .build();
    }

    return ResponseEntity.created(url).build();
  }

  @SuperUser
  @PutMapping("{version}")
  public ResponseEntity<?> updateDictionary(
      @PathVariable("version") String version,
      @RequestParam(name = "reset", defaultValue = "true") boolean reset, // Reset by default
      @Valid @RequestBody Dictionary newDictionary) {
    checkArgument(version != null);
    checkArgument(newDictionary != null);
    checkArgument(newDictionary.getVersion() != null);

    log.info("Updating dictionary: {} with {}", version, newDictionary.getVersion());
    val oldDictionary = dictionaryService.getDictionaryByVersion(version);
    if (oldDictionary == null) {
      return ResponseEntity
          .status(HttpStatus.NOT_FOUND)
          .body(new ServerErrorResponseMessage(NO_SUCH_ENTITY, version));
    } else if (oldDictionary.getState() != OPENED) {
      // TODO: move check to dictionaryService.update() instead
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(new ServerErrorResponseMessage(RESOURCE_CLOSED, version));
    } else if (newDictionary.getVersion() == null) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(new ServerErrorResponseMessage(MISSING_REQUIRED_DATA, "dictionary version"));
    } else if (newDictionary.getVersion().equals(version) == false) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(new ServerErrorResponseMessage(NAME_MISMATCH, version, newDictionary.getVersion()));
    }

    val violations = validateDictionary(newDictionary);
    if (violations.hasErrors()) {
      val errors = new StringBuilder("The request entity had the following errors:\n");
      for (val error : violations.getErrors()) {
        errors.append("  * ").append(error).append('\n');
      }

      return ResponseEntity
          .status(HttpStatus.UNPROCESSABLE_ENTITY)
          .header(VALIDATION_ERROR_HEADER, errors.toString())
          .build();
    }

    dictionaryService.updateDictionary(newDictionary, reset);

    if (violations.hasWarnings()) {
      val warnings = new StringBuilder("Created, but request entity had the following warnings:\n");
      for (val error : violations.getErrors()) {
        warnings.append("  * ").append(error).append('\n');
      }

      return ResponseEntity
          .status(HttpStatus.NO_CONTENT)
          .header(VALIDATION_WARNING_HEADER, warnings.toString())
          .build();
    }

    // http://stackoverflow.com/questions/797834/should-a-restful-put-operation-return-something
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  private DictionaryConstraintViolations validateDictionary(Dictionary dictionary) {
    if (validate) {
      val validator = new DictionaryValidator(dictionary, dictionaryService.getCodeLists());
      return validator.validate();
    } else {
      val empty = Collections.<DictionaryValidator.DictionaryConstraintViolation> emptySet();
      return new DictionaryConstraintViolations(empty, empty);
    }
  }

  private static URI getDictionaryURL(Dictionary dict) {
    val controller = on(DictionaryController.class);
    controller.getDictionary(dict.getVersion());
    return fromMethodCall(controller).build().toUri();
  }

}
