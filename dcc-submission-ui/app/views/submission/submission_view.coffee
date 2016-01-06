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


View = require 'views/base/view'
SubmissionHeaderView = require 'views/submission/submission_header_view'
ReportDatatypeView = require 'views/release/report_datatype_view'
SignOffSubmissionView = require 'views/submission/signoff_submission_view'
ValidateSubmissionView = require 'views/submission/validate_submission_view'
CancelSubmissionView = require 'views/submission/cancel_submission_view'
WarningView = require 'views/warning_view'
Model = require 'models/base/model'

utils = require 'lib/utils'
mediator = require 'mediator'
template = require 'views/templates/submission/submission'

module.exports = class SubmissionView extends View
  template: template
  template = null

  container: '#page-container'
  containerMethod: 'html'
  autoRender: true
  tagName: 'div'
  id: 'submission-view'

  initialize: ->
    #console.debug 'SubmissionView#initialize', @model
    super

    @subscribeEvent "signOffSubmission", -> @refresh()

    @subscribeEvent "validateSubmission", -> @refresh()

    @subscribeEvent "cancelSubmission", ->
      @model.fetch()
      $('#cancel-submission-popup-button').html('Cancelling...')
      $('#cancel-submission-popup-button').attr('disabled', 'disabled')


    @delegate 'click', '#signoff-submission-popup-button',
      @signOffSubmissionPopup

    @delegate 'click', '#validate-submission-popup-button',
      @validateSubmissionPopup

    @delegate 'click', '#cancel-submission-popup-button',
      @cancelSubmissionPopup

    i = setInterval( =>
      if @model
        popup = @subviewsByName.validateSubmissionView?.$el.hasClass('in')
        if not popup
          @refresh()
      else
        clearInterval i
    , 10000)

  refresh: ->
    @model.fetch
      success: =>
        # Update lock status
        mediator.locked = @model.get("locked")
        mediator.publish 'lock', {locked: mediator.locked}

  signOffSubmissionPopup: (e) ->
    #console.debug "SubmissionView#signOffSubmissionPopup", e
    @subview("signOffSubmissionView"
      new SignOffSubmissionView
        "submission": @model
    )

  validateSubmissionPopup: (e) ->
    #console.debug "SubmissionView#validateSubmissionPopup", e
    datatype = $(e.target.parentNode).data("datatype")
    @subview("validateSubmissionView"
      new ValidateSubmissionView
        "submission": @model
        datatype: datatype
    )

  cancelSubmissionPopup: (e) ->
    @subview("cancelSubmissionView"
      new CancelSubmissionView
        "submission": @model
    )

  render: ->
    #console.debug "SubmissionView#render", @model
    super

    @subview('SubmissionHeadeView'
      new SubmissionHeaderView {
        @model
        el: @.$("#submission-header-container")
      }
    )

    @subview('Clinical_core'
      new ReportDatatypeView {
        @model
        el: @.$("#datatype-report-container")
      }
    )

