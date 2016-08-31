"""
* Copyright 2016(c) The Ontario Institute for Cancer Research.
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
PageView = require 'views/base/page_view'
template = require 'views/templates/login'
User = require 'models/user'

module.exports = class LoginView extends PageView
  template: template
  id: 'login'
  container: '#page-container'
  containerMethod: 'html'
  autoRender: true

  baseUrl: "/ws/"

  # Expects the serviceProviders in the options
  initialize: ->
    console.log "LoginView#initialize", window.location
    super

    if window.location.pathname isnt "/login"
      window.location = "/#login"

    @delegate 'submit', '#login-form', @triggerLogin

  triggerLogin: (e) ->
    #console.log "LoginView#triggerLogin"
    e.preventDefault()

    loginDetails = $(e.currentTarget).serializeObject()
    #user = new User {
    #  username: form.username
    #  password: form.password
    #}

    @getUserData loginDetails

  errors: (status) ->
    switch status
      when 400
        "Invalid format for input."
      when 401
        "The email address or password you provided does not match our records."
      when 500
        "Server down. Please try again later."

  getUserData: (loginDetails) ->
    #console.debug 'DCCServiceProvider#ajax', type, url, data
    errors = @errors
    if not window.btoa then window.btoa = base64.encode
    if not window.atob then window.atob = base64.decode
    accessToken = btoa loginDetails.username.concat ":", loginDetails.password
    $.ajax '/ws/users/self', {
      type: 'get'
      dataType: 'json'
      beforeSend: (xhr) ->
        xhr.setRequestHeader 'Authorization', "Basic  #{accessToken}"

      success: (data, status, xhr) ->
        data.accessToken = accessToken
        user = new User data
        Chaplin.mediator.user = user
        Chaplin.mediator.publish 'loginSuccessful'
        Chaplin.mediator.publish '!startupController', 'release', 'list'

        # Piggyback a successful login to send release infomration
        releaseName = xhr.getResponseHeader "x-icgc-submission-version"
        commitId = xhr.getResponseHeader "x-icgc-submission-commitid"
        Chaplin.mediator.publish 'releaseInfo', releaseName, commitId

      error: (jqXHR) =>
        alert = @.$('#login-error')
        error = errors(jqXHR.status)

        if alert.length
          alert.text(error)
        else
          @.$('form')
            .before("<div id='login-error' class='alert alert-error'>
              #{error}</div>")
    }

  afterRender: ->
    super
    text_input = document.getElementById('username')
    text_input.focus()
    text_input.select()
