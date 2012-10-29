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


DataTableView = require 'views/base/data_table_view'
signOffSubmissionView = require 'views/submission/signoff_submission_view'
validateSubmissionView = require 'views/submission/validate_submission_view'
utils = require 'lib/utils'

module.exports = class SubmissionTableView extends DataTableView
  template: template
  template = null

  autoRender: true

  initialize: ->
    #console.debug "SubmissionsTableView#initialize", @model, @el
    @collection = @model.get "submissions"

    super

    @modelBind 'change', @update

    @delegate 'click', '#signoff-submission-popup-button',
      @signOffSubmissionPopup
    @delegate 'click', '#validate-submission-popup-button',
      @validateSubmissionPopup

  update: ->
    @collection = @model.get "submissions"
    @updateDataTable()

  signOffSubmissionPopup: (e) ->
    #console.debug "ReleaseView#signOffSubmissionPopup", e
    @subview("signOffSubmissionView"
      new signOffSubmissionView
        "submission": @collection.get $(e.currentTarget).data("submission")
    )

  validateSubmissionPopup: (e) ->
    #console.debug "ReleaseView#validateSubmissionPopup", e
    @subview("validateSubmissionView"
      new validateSubmissionView
        "submission": @collection.get $(e.currentTarget).data("submission")
    )

  createDataTable: ->
    #console.debug "SubmissionsTableView#createDataTable", @$el, @collection
    aoColumns = [
        {
          sTitle: "Project Key"
          mData: (source) =>
            r = @collection.release
            k = source.projectKey
            "<a href='/releases/#{r}/submissions/#{k}'>#{k}</a>"
        }
        {
          sTitle: "Project Name"
          mData: (source) ->
            source.projectName
        }
        {
          sTitle: "Files"
          bUseRendered: false
          mData: (source, type, val) ->
            if type is "display"
              size = 0
              fs = source.submissionFiles
              for f in fs
                size += f.size
              if size
                return "#{fs.length} (#{utils.fileSize size})"
              else
                return "<i>No files</i>"

            source.submissionFiles
        }
        {
          sTitle: "State"
          mData: (source, type, val) ->
            if type is "display"
              return switch source.state
                when "NOT_VALIDATED"
                  "<span><i class='icon-question-sign'></i> " +
                    source.state.replace('_', ' ') + "</span>"
                when "ERROR"
                  "<span class='error'>" +
                    "<i class='icon-exclamation-sign'></i> " +
                    source.state.replace('_', ' ') + "</span>"
                when "INVALID"
                  "<span class='error'><i class='icon-remove-sign'></i> " +
                    source.state.replace('_', ' ') + "</span>"
                when "QUEUED"
                  "<span class='queued'><i class='icon-time'></i> " +
                    source.state.replace('_', ' ') + "</span>"
                when "VALID"
                  "<span class='valid'><i class='icon-ok-sign'></i> " +
                    source.state.replace('_', ' ') + "</span>"
                when "SIGNED_OFF"
                  "<span class='valid'><i class='icon-lock'></i> " +
                    source.state.replace('_', ' ') + "</span>"

            source.state
        }
        {
          sTitle: "Date Created"
          mData: (source) ->
            utils.date source.lastUpdated
        }
        {
          sTitle: "Report"
          bSortable: false
          mData: (source) =>
            switch source.state
              when "VALID", "SIGNED_OFF", "INVALID"
                r = @collection.release
                s = source.projectKey.replace(/<.*?>/g, '')
                "<a href='/releases/#{r}/submissions/#{s}'>View</a>"
              else ""
        }
        {
          sTitle: "Actions"
          bSortable: false
          bVisible: not utils.is_released(@model.get "state")
          mData: (source) ->
            switch source.state
              when "VALID"
                ds = source.projectKey.replace(/<.*?>/g, '')
                """
                <button
                  class="m-btn green-stripe mini"
                  id="signoff-submission-popup-button"
                  data-submission="#{ds}"
                  data-toggle="modal"
                  href='#signoff-submission-popup'>
                  Sign Off
                </a>
                """
              when "NOT_VALIDATED"
                if source.submissionFiles.length
                  ds = source.projectKey.replace(/<.*?>/g, '')
                  """
                  <button
                    class="m-btn blue-stripe mini"
                    id="validate-submission-popup-button"
                    data-submission="#{ds}"
                    data-toggle="modal"
                    href='#validate-submission-popup'>
                    Validate
                  </a>
                  """
                else
                  "<em>Upload Submission Files</em><br>" +
                  "<small>hwww.res.oicr.on.ca:/#{source.projectKey}</small>"
              else ""
        }
      ]

    @$el.dataTable
      sDom:
        "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>"
      bPaginate: false
      oLanguage:
        "sLengthMenu": "_MENU_ submissions per page"
        "sEmptyTable": "You have no submissions for this release"
      aaSorting: [[ 3, "desc" ]]
      aoColumns: aoColumns
      sAjaxSource: ""
      sAjaxDataProp: ""

      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @collection.toJSON()
