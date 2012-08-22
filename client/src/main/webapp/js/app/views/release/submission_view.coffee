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
  View = require 'views/base/view'
  SubmissionHeaderView = require 'views/release/submission_header_view'
  ReportTableView = require 'views/release/report_table_view'
  SubmissionFilesTableView = require 'views/release/submission_files_table_view'
  SignOffSubmissionView = require 'views/release/signoff_submission_view'
  ValidateSubmissionView = require 'views/release/validate_submission_view'
  template = require 'text!views/templates/release/submission.handlebars'

  'use strict'

  class SubmissionView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'html'
    autoRender: true
    tagName: 'div'
    id: 'submission-view'
    
    initialize: ->
      console.debug 'SubmissionView#initialize', @model
      super
      
      @subscribeEvent "signOffSubmission", -> @model.fetch()
      @subscribeEvent "validateSubmission", -> @model.fetch()
      
      @delegate 'click', '#signoff-submission-popup-button', @signOffSubmissionPopup
      @delegate 'click', '#validate-submission-popup-button', @validateSubmissionPopup
    
    signOffSubmissionPopup: (e) ->
      console.debug "SubmissionView#signOffSubmissionPopup", e
      @subview("signOffSubmissionView"
        new SignOffSubmissionView
          "submission": @model
      )
      
    validateSubmissionPopup: (e) ->
      console.debug "SubmissionView#validateSubmissionPopup", e
      @subview("validateSubmissionView"
        new ValidateSubmissionView
          "submission": @model
      )
      
    render: ->
      console.debug "SubmissionView#render", @model
      super
      
      @subview('SubmissionHeader'
        new SubmissionHeaderView {
          @model
          el: @.$("#submission-header-container")
        }
      )
      
      @subview('Report'
        new ReportTableView {
          @model
          el: @.$("#report-container")
        }
      )

