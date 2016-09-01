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
package org.icgc.dcc.submission.server.web.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.core.security.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.server.web.ServerErrorCode.ALREADY_EXISTS;
import static org.icgc.dcc.submission.server.web.ServerErrorCode.NAME_MISMATCH;
import static org.icgc.dcc.submission.server.web.controller.Responses.badRequest;
import static org.icgc.dcc.submission.server.web.controller.Responses.created;
import static org.icgc.dcc.submission.server.web.controller.Responses.noContent;
import static org.icgc.dcc.submission.server.web.controller.Responses.notFound;
import static org.icgc.dcc.submission.server.web.controller.Responses.unauthorizedResponse;
import static org.springframework.http.ResponseEntity.ok;

import java.util.List;

import javax.validation.Valid;

import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.server.security.Admin;
import org.icgc.dcc.submission.server.service.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ws/codeLists")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CodeListController {

  /**
   * Dependencies
   */
  @Autowired
  private final DictionaryService dictionaryService;

  /**
   * Open-access intentional (DCC-758)
   */
  @CrossOrigin
  @GetMapping
  public ResponseEntity<List<CodeList>> getCodeLists() {
    List<CodeList> codeLists = dictionaryService.getCodeLists();
    if (codeLists == null) {
      codeLists = newArrayList();
    }

    return ok(codeLists);
  }

  @Admin
  @PostMapping
  public ResponseEntity<?> addCodeLists(@Valid @RequestBody List<CodeList> codeLists) {
    for (val codeList : codeLists) {
      val codes = Sets.<String> newHashSet();
      val values = Sets.<String> newHashSet();
      for (val term : codeList.getTerms()) {
        if (codes.contains(term.getCode()) || values.contains(term.getValue())) {
          log.warn("Code or value of {} is duplicated in {}", term, codeList);
          return ResponseEntity
              .status(HttpStatus.UNPROCESSABLE_ENTITY)
              .header(DictionaryController.VALIDATION_ERROR_HEADER,
                  "Code or value in " + term + " is duplicated in " + codeList)
              .build();
        } else {
          codes.add(term.getCode());
          values.add(term.getValue());
        }
      }
    }

    checkArgument(codeLists != null);
    dictionaryService.addCodeList(codeLists);

    return Responses.created();
  }

  @CrossOrigin
  @GetMapping("{name:.+}")
  public ResponseEntity<?> getCodeList(@PathVariable("name") String name) {
    // No authorization check necessary
    log.debug("Getting codelist: {}", name);
    checkArgument(name != null);

    val optional = dictionaryService.getCodeList(name);
    if (!optional.isPresent()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(optional.get());
  }

  /**
   * NOTE: If allow more than updating label at some point, must reset Submission states...
   */
  @PutMapping("{name:.+}")
  public ResponseEntity<?> updateCodeList(@PathVariable("name") String name, @Valid @RequestBody CodeList newCodeList,
      Authentication authentication) {
    log.info("Updating codelist: {} with {}", name, newCodeList);
    if (isSuperUser(authentication) == false) {
      return unauthorizedResponse();
    }

    checkArgument(name != null);
    checkArgument(newCodeList != null);

    val optional = dictionaryService.getCodeList(name);
    if (!optional.isPresent()) {
      return notFound(name);
    } else if (newCodeList.getName().equals(name) == false) {
      return badRequest(NAME_MISMATCH, newCodeList.getName(), name);
    }

    dictionaryService.updateCodeList(newCodeList);

    // DCC-820: add ResponseTimestamper back here?
    return noContent();
  }

  @PostMapping("{name:.+}/terms")
  public ResponseEntity<?> addTerms(@PathVariable("name") String name, @Valid @RequestBody List<Term> terms,
      Authentication authentication) {
    log.info("Adding term {} to codelist {}", terms, name);
    if (isSuperUser(authentication) == false) {
      return unauthorizedResponse();
    }

    checkArgument(name != null);
    checkArgument(terms != null);
    val optional = dictionaryService.getCodeList(name);
    if (optional.isPresent() == false) {
      return notFound(name);
    }

    val codeList = optional.get();

    // First check if the terms exist. The DictionaryService addTerm method checks too, but we don't want to add some of
    // the list and then have it fail part way through
    for (val term : terms) {
      if (codeList.containsTerm(term)) {
        return badRequest(ALREADY_EXISTS, term.getCode());
      }
    }

    for (val term : terms) {
      dictionaryService.addCodeListTerm(name, term);
    }

    return created();
  }

}
