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
utils = require 'lib/utils'
View = require 'views/base/view'
template = require 'views/templates/login'

module.exports = class LoginView extends View
  template: template
  id: 'login'
  container: 'body'
  autoRender: true

  # Expects the serviceProviders in the options
  initialize: (options) ->
    super
    #console.debug 'LoginView#initialize', @el, @$el, options
    @initButtons options.serviceProviders
    #@$el.modal
    #  "keyboard": false
    #  "backdrop": "static"
    #  "show": true

  # In this project we currently only have one service provider and therefore
  # one button. But this should allow for different service providers.
  initButtons: (serviceProviders) ->
    #console.debug 'LoginView#initButtons', serviceProviders

    for serviceProviderName, serviceProvider of serviceProviders

      buttonSelector = ".#{serviceProviderName}"
      @$(buttonSelector).addClass('service-loading')

      loginHandler = _(@loginWith).bind(
        this, serviceProviderName, serviceProvider
      )
      @delegate 'click', buttonSelector, loginHandler

      loaded = _(@serviceProviderLoaded).bind(
        this, serviceProviderName, serviceProvider
      )
      serviceProvider.done loaded

      failed = _(@serviceProviderFailed).bind(
        this, serviceProviderName, serviceProvider
      )
      serviceProvider.fail failed

  loginWith: (serviceProviderName, serviceProvider, e) ->
    console.debug 'LoginView#loginWith', e
    e.preventDefault()

    # TODO - added just to make it work
    loginDetails = @$("form").serializeObject()
    @accessToken = btoa loginDetails.username.concat ":", loginDetails.password
    $.cookie 'accessToken', @accessToken

    return unless serviceProvider.isLoaded()
    Chaplin.mediator.publish 'login:pickService', serviceProviderName
    Chaplin.mediator.publish '!login', serviceProviderName

  serviceProviderLoaded: (serviceProviderName) ->
    #console.debug 'LoginView#serviceProviderLoaded', serviceProviderName
    @$(".#{serviceProviderName}").removeClass('service-loading')

  serviceProviderFailed: (serviceProviderName) ->
    #console.debug 'LoginView#serviceProviderFailed', serviceProviderName
    @$(".#{serviceProviderName}")
      .removeClass('service-loading')
      .addClass('service-unavailable')
      .attr('disabled', true)
      .attr('title', "Error connecting. Please check whether you are
        blocking #{utils.upcase(serviceProviderName)}.")