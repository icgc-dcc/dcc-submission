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
package org.icgc.dcc.submission.release;

import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;

/**
 * Class that handles locks throughout the application.
 * <p>
 * locks: <br>
 * - updating release<br>
 * - releasing release<br>
 * <p>
 * TODO: consider burying Datastore under an abstraction like this to ensure use of locks?
 */
public class DccLocking {

  private static final Logger log = LoggerFactory.getLogger(DccLocking.class);

  private static final String TRANSITIONING_PROPERTY = "transitioning";

  private Datastore datastore;

  void setDatastore(Datastore datastore) { // TODO: DCC-685; make field final and set in constructor (will need guice
                                           // provider)
    if(this.datastore == null) this.datastore = datastore;
  }

  public Release acquireReleasingLock() {
    // name has been checked to be unique and associated with the only ? release
    log.info("acquiring releasing lock");
    Release release = datastore.findAndModify( //
        datastore.createQuery(Release.class) //
            .filter("state", ReleaseState.OPENED) //
            .filter(TRANSITIONING_PROPERTY, false), //
        datastore.createUpdateOperations(Release.class) //
            .set(TRANSITIONING_PROPERTY, true) //
        );
    log.info("acquired releasing lock");
    return release; // TODO: add shard key when switching to a sharded environment
  }

  public Release relinquishReleasingLock() {
    log.info("relinquishing release lock");
    // name has been checked to be unique and associated with the only ? release
    Release release = datastore.findAndModify( //
        datastore.createQuery(Release.class) //
            .filter(TRANSITIONING_PROPERTY, true), //
        datastore.createUpdateOperations(Release.class) //
            .set(TRANSITIONING_PROPERTY, false) //
        );
    log.info("relinquished release lock");
    return release; // TODO: add shard key when switching to a sharded environment
  }
}
