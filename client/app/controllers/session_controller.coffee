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
Controller = require 'controllers/base/controller'
User = require 'models/user'
LoginView = require 'views/login_view'
DCC = require 'lib/services/dcc'

module.exports = class SessionController extends Controller
  # Service provider instances as static properties
  # This just hardcoded here to avoid async loading of service providers.
  # In the end you might want to do this.
  @serviceProviders = {
    dcc: new DCC()
  }

  # Was the login status already determined?
  loginStatusDetermined: false

  # This controller governs the LoginView
  loginView: null

  # Current service provider
  serviceProviderName: null

  initialize: ->
    #console.debug 'SessionController#initialize'
    # Login flow events
    @subscribeEvent 'serviceProviderSession', @serviceProviderSession

    # Handle login
    @subscribeEvent 'logout', @logout
    @subscribeEvent 'userData', @userData

    # Handler events which trigger an action

    # Show the login dialog
    @subscribeEvent '!showLogin', @showLoginView
    # Try to login with a service provider
    @subscribeEvent '!login', @triggerLogin
    # Initiate logout
    @subscribeEvent '!logout', @triggerLogout

    # Determine the logged-in state
    @getSession()

  # Load the libraries of all service providers
  loadServiceProviders: ->
    #console.debug 'SessionController#loadServiceProviders'
    for name, serviceProvider of SessionController.serviceProviders
      serviceProvider.load()

  # Instantiate the user with the given data
  createUser: (userData) ->
    #console.debug 'SessionController#createUser'
    Chaplin.mediator.user = new User userData

  # Try to get an existing session from one of the login providers
  getSession: ->
    #console.debug 'SessionController#getSession',
    #  SessionController.serviceProviders
    @loadServiceProviders()
    for name, serviceProvider of SessionController.serviceProviders
      serviceProvider.done serviceProvider.getLoginStatus

  # Handler for the global !showLoginView event
  showLoginView: ->
    #console.debug 'SessionController#showLoginView', @loginView
    return if @loginView
    @loadServiceProviders()
    @loginView = new LoginView
      serviceProviders: SessionController.serviceProviders

  # Handler for the global !login event
  # Delegate the login to the selected service provider
  triggerLogin: (serviceProviderName) =>
    #console.debug 'SessionController#triggerLogin'
    serviceProvider = SessionController.serviceProviders[serviceProviderName]

    # Publish an event in case the provider library could not be loaded
    unless serviceProvider.isLoaded()
      Chaplin.mediator.publish 'serviceProviderMissing', serviceProviderName
      return

    # Publish a global loginAttempt event
    Chaplin.mediator.publish 'loginAttempt', serviceProviderName

    # Delegate to service provider
    serviceProvider.triggerLogin()

  # Handler for the global serviceProviderSession event
  serviceProviderSession: (session) =>
    #console.debug 'SessionController#serviceProviderSession'
    # Save the session provider used for login
    @serviceProviderName = session.provider.name

    # Hide the login view
    @disposeLoginView()

    # Transform session into user attributes and create a user
    session.id = session.userId
    delete session.userId
    @createUser session

    @publishLogin()

  # Publish an event to notify all application components of the login
  publishLogin: ->
    #console.debug 'SessionController#publishLogin'
    @loginStatusDetermined = true

    # Publish a global login event passing the user
    Chaplin.mediator.publish 'login', Chaplin.mediator.user
    Chaplin.mediator.publish 'loginStatus', true

  # Logout
  # ------

  # Handler for the global !logout event
  triggerLogout: ->
    #console.debug 'SessionController#triggerLogout'
    # Just publish a logout event for now
    Chaplin.mediator.publish 'logout'

  # Handler for the global logout event
  logout: =>
    #console.debug 'SessionController#logout'
    @loginStatusDetermined = true

    @disposeUser()

    # Discard the login info
    @serviceProviderName = null

    # Show the login view again
    @showLoginView()

    Chaplin.mediator.publish 'loginStatus', false

  # Handler for the global userData event
  # -------------------------------------

  userData: (data) ->
    #console.debug 'SessionController#userData'
    Chaplin.mediator.user.set data

  # Disposal
  # --------

  disposeLoginView: ->
    #console.debug 'SessionController#disposeLoginView'
    return unless @loginView
    @loginView.dispose()
    @loginView = null

  disposeUser: ->
    #console.debug 'SessionController#disposeUser'
    return unless Chaplin.mediator.user
    # Dispose the user model
    Chaplin.mediator.user.dispose()
    # Nullify property on the mediator
    Chaplin.mediator.user = null