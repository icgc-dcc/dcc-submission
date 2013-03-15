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
package org.icgc.dcc.release.model;

import java.util.Date;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.core.model.BaseEntity;
import org.icgc.dcc.core.model.HasName;
import org.icgc.dcc.release.ReleaseException;
import org.icgc.dcc.web.validator.NameValidator;

import com.google.code.morphia.annotations.Entity;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Not meant to be used in a hash for now (override hashCode if so)
 */
@Entity
public class Release extends BaseEntity implements HasName {

  @NotBlank
  @Pattern(regexp = NameValidator.NAME_PATTERN)
  protected String name;

  protected ReleaseState state;

  protected boolean transitioning; // mutex

  @Valid
  protected List<Submission> submissions = Lists.newArrayList();

  @Valid
  protected List<QueuedProject> queue = Lists.newArrayList();

  protected Date releaseDate;

  @NotBlank
  protected String dictionaryVersion;

  public Release() {
    super();
    this.setState(ReleaseState.OPENED);
  }

  public Release(String name) {
    super();
    this.setName(name);
    this.setState(ReleaseState.OPENED);
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ReleaseState getState() {
    return state;
  }

  public void setState(ReleaseState state) {
    this.state = state;
  }

  public boolean isTransitioning() {
    return transitioning;
  }

  public void setTransitioning(boolean transitioning) {
    this.transitioning = transitioning;
  }

  public List<Submission> getSubmissions() {
    return submissions;
  }

  @JsonIgnore
  public Iterable<String> getProjectKeys() {
    return Iterables.transform(getSubmissions(), new Function<Submission, String>() {
      @Override
      public String apply(Submission input) {
        return input.getProjectKey();
      }
    });
  }

  @JsonIgnore
  public List<String> getInvalidProjectKeys() {
    List<String> invalidProjectKeys = newArrayList();
    for(Submission submission : getSubmissions()) {
      if(submission.getState() == SubmissionState.INVALID) {
        invalidProjectKeys.add(submission.getProjectKey());
      }
    }
    return ImmutableList.copyOf(invalidProjectKeys);
  }

  public Submission getSubmission(final String projectKey) {
    checkArgument(projectKey != null);

    Optional<Submission> foundSubmission = Iterables.tryFind(this.submissions, new Predicate<Submission>() {
      @Override
      public boolean apply(Submission submission) {
        return submission.getProjectKey().equals(projectKey);
      }
    });

    if(foundSubmission.isPresent()) {
      return foundSubmission.get();
    } else {
      throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
          this.name));
    }
  }

  public void addSubmission(Submission submission) {
    this.getSubmissions().add(submission);
  }

  public String getDictionaryVersion() {
    return this.dictionaryVersion;
  }

  public void setDictionaryVersion(String dictionaryVersion) {
    this.dictionaryVersion = dictionaryVersion;
  }

  public Date getReleaseDate() {
    return releaseDate != null ? new Date(releaseDate.getTime()) : null;
  }

  public void setReleaseDate() {
    this.releaseDate = new Date();
  }

  /**
   * @return the list of project keys that are queued (possibly empty)
   */
  public List<String> getQueuedProjectKeys() {
    List<String> projectKeys = Lists.newArrayList();
    for(QueuedProject qp : this.getQueue()) {
      projectKeys.add(qp.getKey());
    }
    return projectKeys;
  }

  public List<QueuedProject> getQueue() {
    return queue;
  }

  public void enqueue(QueuedProject project) {
    if(project.getKey() != null && !project.getKey().isEmpty() && !this.queue.contains(project)) {
      this.queue.add(project);
    }
  }

  public void enqueue(List<QueuedProject> queuedProjects) {
    for(QueuedProject qp : queuedProjects) {
      this.enqueue(qp);
    }
  }

  public int removeFromQueue(final String projectKey) {
    int count = 0;
    for(int i = this.queue.size() - 1; i >= 0; i--) {
      QueuedProject queuedProject = this.queue.get(i);
      if(queuedProject != null && queuedProject.getKey().equals(projectKey)) {
        this.queue.remove(i);
        count++;
      }
    }
    return count;
  }

  public int removeFromQueue(final List<String> projectKeys) {
    int count = 0;
    for(String projectKey : projectKeys) {
      count += removeFromQueue(projectKey);
    }
    return count;
  }

  /**
   * Attempts to retrieve the first element of the queue.
   */
  public Optional<QueuedProject> nextInQueue() {
    return this.queue != null && this.queue.isEmpty() == false ? Optional.<QueuedProject> of(this.queue.get(0)) : Optional
        .<QueuedProject> absent();
  }

  /**
   * Dequeues the first element of the queue, expecting the queue to contain at least one element.<br>
   * 
   * Use in combination with <code>{@link Release#nextInQueue()}</code> and guava's <code>Optional.isPresent()</code><br>
   * This method is <b>not</b> thread-safe.
   */
  public QueuedProject dequeueProject() {
    checkState(queue != null && queue.isEmpty() == false);
    return queue.remove(0);
  }

  public Optional<QueuedProject> dequeue() {
    return this.queue != null && this.queue.isEmpty() == false ? Optional.<QueuedProject> of(this.queue.remove(0)) : Optional
        .<QueuedProject> absent();
  }

  public void emptyQueue() {
    this.queue.clear();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    final Release other = (Release) obj;
    return Objects.equal(this.name, other.name);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(Release.class) //
        .add("name", this.name) //
        .add("state", this.state) //
        .add("transitioning", this.transitioning) //
        .add("releaseDate", this.releaseDate) //
        .add("dictionaryVersion", this.dictionaryVersion) //
        .add("queue", this.queue) //
        .add("submissions", this.submissions) //
        .toString();
  }
}
