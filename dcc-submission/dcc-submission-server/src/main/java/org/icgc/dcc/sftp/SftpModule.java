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
package org.icgc.dcc.sftp;

import static com.google.inject.matcher.Matchers.any;

import org.apache.sshd.SshServer;
import org.icgc.dcc.core.AbstractDccModule;

import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Dependency injection module for the SFTP subsystem.
 */
public class SftpModule extends AbstractDccModule {

  @Override
  protected void configure() {

    bind(SftpContext.class);
    bind(SshServer.class).toProvider(SshServerProvider.class).in(Singleton.class);
    bindService(SftpServerService.class);
    bindEvents();
  }

  /**
   * Configures the global event bus and registers all components as recipients.
   * 
   * @see http://spin.atomicobject.com/2012/01/13/the-guava-eventbus-on-guice/
   */
  private void bindEvents() {
    final EventBus eventBus = new EventBus("SFTP EventBus");
    bind(EventBus.class).toInstance(eventBus);
    bindListener(any(), new TypeListener() {

      @Override
      public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
        typeEncounter.register(new InjectionListener<I>() {

          @Override
          public void afterInjection(I i) {
            // Register the current component
            eventBus.register(i);
          }

        });
      }

    });
  }
}
