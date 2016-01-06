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

FeedbackFormView = require 'modules/feedback/views/feedback_form_view'
AdminView = require 'views/admin_view'
UserView = require 'views/navigation/user_view'

Systems = require 'models/systems'
mediator = require 'mediator'
utils = require 'lib/utils'

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

    @refreshLock(mediator.locked)

    @subscribeEvent 'loginSuccessful', ->
      @render()
      $("html").toggleClass("admin", utils.is_admin())
      @subview('UserAreaView'
      new UserView
        model: Chaplin.mediator.user
      )

    @subscribeEvent 'navigation:change', (attributes) =>
      #console.debug 'NavigationView#initialize#change', attributes
      @model.clear(silent: yes)
      @model.set attributes

    @subscribeEvent 'lock', (e) ->
      # console.log "Lock changed to #{e.locked}"
      @refreshLock(e.locked)

    @delegate "click", "#admin-tab", @adminPopup
    @delegate "click", "#feedback-tab", @feedbackPopup

  refreshLock: (locked) ->
    $("html").toggleClass("locked", locked || false)

  adminPopup: (e) ->
    e.preventDefault()
    new AdminView({model: new Systems()})

  feedbackPopup: (e) ->
    e.preventDefault()
    new FeedbackFormView()

