"""
* Copyright 2012(c) The Ontario Institute for Cancer Research.
* All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the GNU Public License v3.0.
* You should have received a copy of the GNU General Public License along with
* this program. If not, see <http://www.gnu.org/licenses/>.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
"""


Chaplin = require 'chaplin'
View = require 'views/base/view'
template = require 'views/templates/navigation'

module.exports = class NavigationView extends View
  template: template
  tagName: 'nav'
  containerMethod: 'html'
  autoRender: false
  className: 'navigation'
  container: '#header-container'

  initialize: ->
    #console.debug 'NavigationView#initialize', @model
    super
    @modelBind 'change', @render

    @delegate 'click', '#logout', @triggerLogout

    @subscribeEvent 'login', @setUsername
    #@subscribeEvent 'logout', @setUsername

    @subscribeEvent 'navigation:change', (attributes) =>
      console.debug 'NavigationView#initialize#change', attributes
      @model.clear(silent: yes)
      @model.set attributes

  setUsername: (user)->
    #console.debug 'NavigationView#@setUsername', @model
    @model.set 'username', user.get('name')

  triggerLogout: (e) ->
    console.log "NavigationView#triggerLogout"

    Chaplin.mediator.publish '!logout'