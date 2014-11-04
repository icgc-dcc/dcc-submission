/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.tryFind;
import static org.icgc.dcc.submission.release.model.ReleaseState.COMPLETED;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.SIGNED_OFF;

import java.util.Date;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.submission.core.model.BaseEntity;
import org.icgc.dcc.submission.core.model.HasName;
import org.icgc.dcc.submission.core.model.Views.Digest;
import org.icgc.dcc.submission.core.util.NameValidator;
import org.icgc.dcc.submission.release.ReleaseException;
import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Not meant to be used in a hash for now (override hashCode if so)
 */
@Slf4j
@Entity
@ToString
@EqualsAndHashCode(of = "name", callSuper = false)
public class Release extends BaseEntity implements HasName {

  public static final Predicate<Submission> SIGNED_OFF_PROJECTS_PREDICATE = new Predicate<Submission>() {

    @Override
    public boolean apply(Submission submission) {
      return SIGNED_OFF == submission.getState();
    }
  };

  @NotBlank
  @Pattern(regexp = NameValidator.DEFAULT_NAME_PATTERN)
  @JsonView(Digest.class)
  @Getter
  @Setter
  protected String name;

  @JsonView(Digest.class)
  @Getter
  @Setter
  protected ReleaseState state;

  @JsonView(Digest.class)
  protected Date releaseDate;

  @NotBlank
  @JsonView(Digest.class)
  @Getter
  @Setter
  protected String dictionaryVersion;

  @Valid
  @JsonView(Digest.class)
  @Getter
  protected List<Submission> submissions = Lists.newArrayList();

  @Valid
  @Getter
  protected List<QueuedProject> queue = Lists.newArrayList();

  public Release() {
    this.setState(OPENED);
  }

  public Release(@NonNull String name) {
    this.setName(name);
    this.setState(OPENED);
  }

  public Release(@NonNull String name, @NonNull String dictionaryVersion) {
    this.setName(name);
    this.setDictionaryVersion(dictionaryVersion);
    this.setState(OPENED);
  }

  public Optional<Submission> getSubmission(final @NonNull String projectKey) {
    return tryFind(getSubmissions(), new Predicate<Submission>() {

      @Override
      public boolean apply(Submission submission) {
        return submission.getProjectKey().equals(projectKey);
      }

    });
  }

  /**
   * Not thread-safe.
   */
  public void complete() {
    setState(COMPLETED);
    resetReleaseDate();

    for (int i = submissions.size() - 1; i >= 0; i--) {
      val submission = getSubmissions().get(i);
      if (submission.getState() != SIGNED_OFF) {
        getSubmissions().remove(i);
      }
    }
  }

  @JsonIgnore
  public Iterable<String> getProjectKeys() {
    return transform(getSubmissions(), new Function<Submission, String>() {

      @Override
      public String apply(Submission input) {
        return input.getProjectKey();
      }

    });
  }

  @JsonIgnore
  public List<String> getInvalidProjectKeys() {
    val invalidProjectKeys = ImmutableList.<String> builder();
    for (val submission : getSubmissions()) {
      if (submission.getState() == INVALID) {
        invalidProjectKeys.add(submission.getProjectKey());
      }
    }

    return invalidProjectKeys.build();
  }

  public void addSubmission(Submission submission) {
    getSubmissions().add(submission);
  }

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

  public Date getReleaseDate() {
    return releaseDate != null ? new Date(releaseDate.getTime()) : null;
  }

  public void setReleaseDate() {
    this.releaseDate = new Date();
  }

  public void resetReleaseDate() {
    setReleaseDate();
  }

  @JsonIgnore
  public boolean isQueued() {
    return !queue.isEmpty();
  }

  /**
   * Returns the list of project keys that are queued (possibly empty)
   */
  public List<String> getQueuedProjectKeys() {
    val projectKeys = ImmutableList.<String> builder();
    for (val queuedProject : queue) {
      projectKeys.add(queuedProject.getKey());
    }

    return projectKeys.build();
  }

  public void enqueue(@NonNull List<QueuedProject> queuedProjects) {
    for (val queuedProject : queuedProjects) {
      enqueue(queuedProject);
    }
  }

  public void enqueue(@NonNull QueuedProject queuedProject) {
    log.info("Enqueing '{}' from current queue state {}...", queuedProject, queue);

    // Not sure why there is a test / expectation for this, but here it is:
    if (queuedProject.getKey() == null || queuedProject.getKey().isEmpty()) {
      return;
    }

    if (queue.contains(queuedProject)) {
      throw new ReleaseException("Project '%s' already exists in queue: '%s'", queuedProject, queue);
    }

    queue.add(queuedProject);
  }

  public void removeFromQueue(@NonNull final Iterable<String> projectKeys) {
    for (val projectKey : projectKeys) {
      removeFromQueue(projectKey);
    }
  }

  public void removeFromQueue(@NonNull final String projectKey) {
    log.info("Removing '{}' from current queue state {}...", projectKey, queue);

    val iterator = queue.iterator();
    while (iterator.hasNext()) {
      val queuedProject = iterator.next();
      if (queuedProject.getKey().equals(projectKey)) {
        iterator.remove();
      }
    }
  }

  /**
   * Attempts to retrieve the first element of the queue.
   */
  public Optional<QueuedProject> nextInQueue() {
    return Optional.<QueuedProject> fromNullable(isQueued() ? queue.get(0) : null);
  }

  /**
   * Dequeues the first element of the queue, expecting the queue to contain at least one element.<br>
   * 
   * Use in combination with <code>{@link Release#nextInQueue()}</code> and guava's <code>Optional.isPresent()</code><br>
   * This method is <b>not</b> thread-safe.
   */
  public QueuedProject dequeueProject() {
    log.info("Dequeing from current queue state {}...", queue);
    checkState(isQueued());
    return queue.remove(0);
  }

  public void emptyQueue() {
    log.info("Emptying from current queue state {}...", queue);
    queue.clear();
  }

}
