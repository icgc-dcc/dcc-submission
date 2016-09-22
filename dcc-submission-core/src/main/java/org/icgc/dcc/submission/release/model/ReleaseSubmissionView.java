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
package org.icgc.dcc.submission.release.model;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.tryFind;
import static java.util.Collections.emptyList;
import static org.icgc.dcc.submission.release.model.SubmissionState.SIGNED_OFF;

import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.val;

import org.icgc.dcc.submission.core.model.Views.Digest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * {@link Release} with includes {@link Submission}
 */
@ToString
@EqualsAndHashCode(callSuper = true)
public class ReleaseSubmissionView extends Release {

  @Valid
  @JsonView(Digest.class)
  @Getter
  @Setter
  protected List<Submission> submissions = Lists.newArrayList();

  public ReleaseSubmissionView(@NonNull String name) {
    super(name);
  }

  public ReleaseSubmissionView(@NonNull Release release) {
    this(release, emptyList());
  }

  public ReleaseSubmissionView(@NonNull Release release, @NonNull Iterable<Submission> submissions) {
    this.name = release.name;
    this.state = release.state;
    this.releaseDate = release.releaseDate == null ? null : (Date) release.releaseDate.clone();
    this.dictionaryVersion = release.dictionaryVersion;
    this.queue = Lists.newArrayList(release.queue);
    this.submissions = Lists.newArrayList(submissions);
  }

  @Deprecated
  @JsonIgnore
  public boolean isSignOffAllowed() {
    // At least one submission must be signed off on
    for (val submission : getSubmissions()) {
      val signedOff = submission.getState() == SIGNED_OFF;
      if (signedOff) {
        return true;
      }
    }

    return false;
  }

  @JsonIgnore
  @Deprecated
  public Iterable<String> getProjectKeys() {
    return copyOf(transform(getSubmissions(), new Function<Submission, String>() {

      @Override
      public String apply(Submission input) {
        return input.getProjectKey();
      }

    }));
  }

  public Optional<Submission> getSubmission(final @NonNull String projectKey) {
    return tryFind(getSubmissions(), new Predicate<Submission>() {

      @Override
      public boolean apply(Submission submission) {
        return submission.getProjectKey().equals(projectKey);
      }

    });
  }

}
