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
package org.icgc.dcc.core;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;

public class DccRuntime {

  private static final Logger log = LoggerFactory.getLogger(DccRuntime.class);

  private final Set<Service> services;

  @Inject
  public DccRuntime(Set<Service> services) {
    this.services = services;
  }

  public void start() {
    for(Service service : services) {
      tryStartService(service);
    }
  }

  public void stop() {
    for(Service service : services) {
      tryStopService(service);
    }
  }

  private void tryStopService(Service service) {
    try {
      log.info("Service {} is [{}]. Stoping.", service.getClass(), service.state());
      service.stopAndWait();
      log.info("Service {} is now [{}]", service.getClass(), service.state());
    } catch(UncheckedExecutionException e) {
      log.warn("Failed to stop service {}: {}", service.getClass(), e.getCause().getMessage());
    }
  }

  private void tryStartService(Service service) {
    try {
      log.info("Service {} is [{}]. Starting.", service.getClass(), service.state());
      service.startAndWait();
      log.info("Service {} is now [{}]", service.getClass(), service.state());
    } catch(UncheckedExecutionException e) {
      log.warn("Failed to start service {}: {}", service.getClass(), e.getCause().getMessage());
    }
  }

}
