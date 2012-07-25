"""
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
"""

define (require) ->
  Chaplin = require 'chaplin'
  ServiceProvider = require 'lib/services/service_provider'
  utils = require 'lib/utils'

  class DCC extends ServiceProvider
    baseUrl: "http://localhost:3001/ws/"

    constructor: ->
      console.debug 'DCCServiceProvider#constructor', localStorage
      super
      @accessToken = localStorage.getItem 'accessToken'
      authCallback = _(@loginHandler).bind(this, @loginHandler)
      Chaplin.mediator.subscribe 'auth:callback:dcc', authCallback

    load: ->
      console.debug 'DCCServiceProvider#load'
      @resolve()
      this

    isLoaded: ->
      console.debug 'DCCServiceProvider#isLoaded'
      yes

    ajax: (type, url, data) ->
      console.debug 'DCCServiceProvider#ajax', type, url, data
      url = @baseUrl + url
      #url += "?access_token=#{@accessToken}" if @accessToken
      options = {url, data, type, dataType: 'json'}
      options["beforeSend"] = utils.sendAuthorization if @accessToken
      #$.ajax {url, data, type, dataType: 'json', beforeSend: utils.sendAuthorization}
      $.ajax options

    # Trigger login popup
    triggerLogin: (loginContext) ->
      console.debug 'DCCServiceProvider#triggerLogin', loginContext, @
      #callback = _(@loginHandler).bind(this, @loginHandler)
      #window.location = URL
      window.location.reload()

    # Callback for the login popup
    loginHandler: (loginContext, response) =>
      console.debug 'DCCServiceProvider#loginHandler', loginContext, response
      if response
        # Publish successful login
        Chaplin.mediator.publish 'loginSuccessful', {provider: this, loginContext}

        # Publish the session
        @accessToken = response.accessToken
        localStorage.setItem 'accessToken', @accessToken
        # We don't use user data
        @getUserData().done(@processUserData)
      else
        Chaplin.mediator.publish 'loginFail', provider: this, loginContext: loginContext

    getUserData: ->
      console.debug 'DCCServiceProvider#getUserData'
      @ajax('get', 'users/self')

    processUserData: (response) ->
      console.debug 'DCCServiceProvider#processUserData', response
      Chaplin.mediator.publish 'userData', response

    getLoginStatus: (callback = @loginStatusHandler, force = false) ->
      console.debug 'DCCServiceProvider#getLoginStatus'
      @getUserData().always(callback)

    loginStatusHandler: (response, status) =>
      console.debug 'DCCServiceProvider#loginStatusHandler', response, status
      
      if not response or status is 'error'
        Chaplin.mediator.publish 'logout'
      else
        Chaplin.mediator.publish 'serviceProviderSession', _.extend response,
          provider: this
          userId: response.responseText
          accessToken: @accessToken