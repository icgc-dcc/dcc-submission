/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.data;

import org.glassfish.jersey.server.ResourceConfig;
import org.icgc.dcc.core.AbstractDccModule;
import org.icgc.dcc.data.schema.SchemaRegistry;
import org.icgc.dcc.data.web.DonorDataResource;
import org.icgc.dcc.data.web.DonorsResource;
import org.icgc.dcc.data.web.GenesResource;

import com.google.inject.Inject;

/**
 * 
 */
public class DataModule extends AbstractDccModule {

  @Override
  protected void configure() {
    bind(SchemaRegistry.class).asEagerSingleton();
    bind(RootResources.class).asEagerSingleton();
  }

  /**
   * Used to register resources in {@code Jersey}. This is required because {@code Jersey} cannot use Guice to discover
   * resources.
   */
  public static class RootResources {
    @Inject
    public RootResources(ResourceConfig config) {
      config.addClasses(DonorDataResource.class);
      config.addClasses(DonorsResource.class);
      config.addClasses(GenesResource.class);
    }
  }

}
