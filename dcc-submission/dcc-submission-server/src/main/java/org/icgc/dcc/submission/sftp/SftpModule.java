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
package org.icgc.dcc.submission.sftp;

import static com.google.inject.matcher.Matchers.inSubpackage;

import java.io.Serializable;

import lombok.extern.slf4j.Slf4j;

import org.apache.sshd.SshServer;
import org.icgc.dcc.submission.core.AbstractDccModule;

import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Dependency injection module for the SFTP subsystem.
 */
@Slf4j
public class SftpModule extends AbstractDccModule {

  @Override
  protected void configure() {
    bindEvents(new EventBus("SFTP EventBus"));
    bind(SftpAuthenticator.class).in(Singleton.class);
    bind(SftpContext.class).in(Singleton.class);
    bind(SshServer.class).toProvider(SshServerProvider.class).in(Singleton.class);
    bindService(SftpServerService.class);
  }

  /**
   * Configures the global event bus and registers all module types as recipients.
   * 
   * @param eventBus - the shared SFTP event bus
   * @see http://spin.atomicobject.com/2012/01/13/the-guava-eventbus-on-guice/
   */
  private void bindEvents(final EventBus eventBus) {
    bind(EventBus.class).toInstance(eventBus);
    bindListener(new InModule(), new TypeListener() {

      @Override
      public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
        // Register all local module instantiated objects with the event bus
        typeEncounter.register(new InjectionListener<I>() {

          @Override
          public void afterInjection(I i) {
            log.info("Registering {} with event bus...", i);
            eventBus.register(i);
          }

        });
      }

    });
  }

  /**
   * Guice injection matcher that matches all types in the current package.
   * <p>
   * Uses the adapter pattern to delegate to the already existing core matcher with an incompatible interface.
   */
  private static class InModule extends AbstractMatcher<TypeLiteral<?>> implements Serializable {

    /**
     * Adaptee delegate.
     */
    @SuppressWarnings("rawtypes")
    Matcher<Class> subpackage = inSubpackage(getClass().getPackage().getName());

    @Override
    public boolean matches(TypeLiteral<?> t) {
      // Delegate
      return subpackage.matches(t.getRawType());
    }

  }

}
