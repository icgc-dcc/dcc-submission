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
NextRelease = require 'models/next_release'
template = require 'views/templates/submission/validate_submission'

module.exports = class ValidateSubmissionView extends View
  template: template
  template = null

  container: '#page-container'
  containerMethod: 'append'
  autoRender: true
  tagName: 'div'
  className: "modal fade"
  id: 'validate-submission-popup'

  initialize: ->
    console.debug "ValidateSubmissionView#initialize", @options
    @model = @options.submission

    release = new NextRelease()
    release.fetch
      success: (data) =>
        @model.set 'queue', data.get('queue').length

    super

    @modelBind 'change', @render

    @delegate 'click', '#validate-submission-button', @validateSubmission

  validateSubmission: (e) ->
    console.debug "ValidateSubmissionView#completeRelease", @model
    nextRelease = new NextRelease()

    nextRelease.queue [
      {key: @options.submission.get("projectKey")
      emails: @.$('#emails').val().split(',')}
      ],
      success: =>
        @$el.modal 'hide'
        Chaplin.mediator.publish "validateSubmission"
