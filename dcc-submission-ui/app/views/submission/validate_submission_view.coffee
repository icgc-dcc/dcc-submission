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


mediator = require 'mediator'
View = require 'views/base/view'
Model = require 'models/base/model'
NextRelease = require 'models/next_release'
Queue = require 'models/queue'
template = require 'views/templates/submission/validate_submission'
utils = require 'lib/utils'

module.exports = class ValidateSubmissionView extends View
  template: template
  template = null

  container: '#page-container'
  containerMethod: 'append'
  autoRender: true
  tagName: 'div'
  className: "modal hide fade"
  id: 'validate-submission-popup'



  selectNone: ->
    @dataTypeTable.fnClearTable()
    for f, idx in @dataTypes
      f.selected = false unless f.dataType == "CLINICAL_CORE_TYPE"
      @dataTypeTable.fnAddData @dataTypes[idx]

  
  selectAll: ->
    @dataTypeTable.fnClearTable()
    for f, idx in @dataTypes
      f.selected = true
      @dataTypeTable.fnAddData @dataTypes[idx]

  toggleFeature: (e) ->
    feature = $(e.currentTarget).data('feature-type')
    idx = _.pluck(@dataTypes, 'dataType').indexOf(feature)
    @dataTypes[idx].selected = not @dataTypes[idx].selected

    # For some reason update seem to adjust td width,
    # just clear and rebuild as there are only a handful of datatypes
    @dataTypeTable.fnClearTable()
    for f, idx in @dataTypes
      @dataTypeTable.fnAddData @dataTypes[idx]


  initialize: ->
    #console.debug "ValidateSubmissionView#initialize", @options
    datatype = @options.datatype
    @model = new Model @options.submission.getAttributes()
    @model.set({email: mediator.user.get("email")}, {silent: true})

    @dataTypeTable = null

    @report = @model.get "report"


    # Only need shallow copy
    @dataTypes = _.clone( @report.dataTypeReports )


    # Default, preselect NOT_VALIDATED and CLINICAL
    @dataTypes.forEach (d)->
      if d.dataType == "CLINICAL_CORE_TYPE" or d.dataTypeState in ["INVALID", "NOT_VALIDATED"]
        d.selected = true
      else
        d.selected = false


    # Make clinical related files go first
    @dataTypes = _.sortBy @dataTypes, (dataType)->
      switch dataType.dataType
        when "CLINICAL_CORE_TYPE"
          return 0
        when "CLINICAL_OPTIONAL_TYPE"
          return 1
        else
          return 10

    # Pre-filter if datatype is provided
    if datatype
      @dataTypes.forEach (f)->
        if f.dataType == "CLINICAL_CORE_TYPE" or f.dataType == datatype
          f.selected = true
        else
          f.selected = false

    # Grab queue length
    queue = new Queue()
    queue.fetch
      success: (data) =>
        @model.set 'queue', data.get 'queue'

    @dismissed = false
    super

    @modelBind 'change', @render
    @delegate 'click', '#validate-submission-button', @validateSubmission
    @delegate 'click', '#toggle-feature', @toggleFeature
    @delegate 'click', '#feature-all', @selectAll
    @delegate 'click', '#feature-clear', @selectNone
    @delegate 'input propertychange', '#emails', @checkEmail

    @render()

  render: ->
    super


    # Check if email is preset
    @checkEmail(null)

    # Create a feature type selection table
    aoColumns = [
      {
         sTitle: "Data Type"
         mData: (source) ->
           displayName = utils.translateDataType(source.dataType)
           if source.dataType != "CLINICAL_CORE_TYPE"
             if source.selected == false
               """
               <span id="toggle-feature"
                  style="cursor:pointer"
                  data-feature-type="#{source.dataType}">
                  <i class="icon icon-check-empty"></i> #{displayName}
               </span>
               """
             else
               """
               <span id="toggle-feature"
                  style="cursor:pointer"
                  data-feature-type="#{source.dataType}">
                  <i class="icon icon-check"></i> #{displayName}
               </span>
               """
           else
             """
             <span data-feature-type="#{source.dataType}">
               <i class="icon icon-check"></i> #{displayName}
             </span>
             """
      }
      {
        sTitle: "State"
        mData: (source) =>
          return utils.getStateDisplay source.dataTypeState
      }
    ]

    @dataTypeTable = $("#validate-file-types").dataTable
      bDestroy: true
      bPaginate: false
      bFilter: false
      bSort:false
      bInfo: false
      sAjaxSource: ""
      sAjaxDataProp: ""
      fnRowCallback: (nRow, aData, iDisplayIndex, iDisplayIndexFull) ->
        if aData.dataType == "CLINICAL_CORE_TYPE"
          $(nRow).css {'color': '#999', 'font-style': 'italic'}
      aoColumns: aoColumns
      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @dataTypes


  checkEmail: (e)->
    val = @.$("#emails").val()
    if val and val.length > 0
      @.$("#validate-submission-button").prop('disabled', false)
    else
      @.$("#validate-submission-button").prop('disabled', true)

     
  validateSubmission: (e) ->
    #console.debug "ValidateSubmissionView#completeRelease", @model, e

    @.$("#validate-submission-button").prop("disabled", true)
    emails = @.$("#emails")
    alert = @.$('#email-error')
    val = @.$("#emails").val()
    mediator.user.set "email", val

    if not val or not val.match /.+@.+\..+/i
      if alert.length
        alert.text("You must enter at least one valid email
           address before submitting submission for Validation!")
      else
        emails
          .before("<div id='email-error' class='error'>You must enter at least
            one valid email address before submitting submission for
            Validation!</div>")
      return


    nextRelease = new NextRelease()

    # Grab the selected datatypes
    dataTypesToValidate = _.filter @dataTypes, (f) -> f.selected == true
    dataTypeParams = _.pluck dataTypesToValidate, 'dataType'

    nextRelease.queue [{
      key: @options.submission.get("projectKey")
      emails: @.$('#emails').val().split(',')
      dataTypes: dataTypeParams
    }],
    success: =>
      @.$("#validate-submission-button").prop("disabled", false)
      @$el.modal 'hide'
      mediator.publish "validateSubmission"
      mediator.publish "notify", "Submission for Project "+
        "#{@model.get('projectName')} has been queued for Validation."

