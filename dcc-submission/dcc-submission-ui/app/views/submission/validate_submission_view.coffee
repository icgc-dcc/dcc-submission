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
    @featureTable.fnClearTable()
    for f, idx in @features
      f.selected = false unless f.name == "CLINICAL_CORE_TYPE"
      @featureTable.fnAddData @features[idx]
      #@featureTable.fnUpdate(@features[idx], idx)

  
  selectAll: ->
    @featureTable.fnClearTable()
    for f, idx in @features
      f.selected = true
      @featureTable.fnAddData @features[idx]
      #@featureTable.fnUpdate(@features[idx], idx)

  toggleFeature: (e) ->
    feature = $(e.currentTarget).data('feature-type')
    idx = _.pluck(@features, 'name').indexOf(feature)
    @features[idx].selected = not @features[idx].selected

    # For some reason update seem to adjust td width,
    # just clear and rebuild as there are only a handful of datatypes
    @featureTable.fnClearTable()
    for f, idx in @features
      @featureTable.fnAddData @features[idx]
    #@featureTable.fnUpdate(@features[idx], idx)


  initialize: ->
    console.debug "ValidateSubmissionView#initialize", @options
    datatype = @options.datatype
    @model = new Model @options.submission.getAttributes()
    @model.set({email: mediator.user.get("email")}, {silent: true})

    @features = []
    @featureTable = null

    @dataState = {}
    for d in @model.get("dataState")
      @dataState[d.dataType] = d.state
    #console.log @dataState
  
    submissionFiles = @model.get "submissionFiles"
    for f in  submissionFiles
      name = f.dataType
      state = @dataState[name]
      idx = _.pluck(@features, 'name').indexOf(name)

      if name != null and idx == -1
        @features.push {'name':name, 'selected':true}


    # Pre-filter if datatype is provided
    if datatype
      @features.forEach (f)->
        f.selected = false unless (f.name == "CLINICAL_CORE_TYPE" or f.name == datatype)

    release = new NextRelease()
    release.fetch
      success: (data) =>
        @model.set 'queue', data.get('queue').length

    super

    @modelBind 'change', @render
    @delegate 'click', '#validate-submission-button', @validateSubmission
    @delegate 'click', '#toggle-feature', @toggleFeature
    @delegate 'click', '#feature-all', @selectAll
    @delegate 'click', '#feature-clear', @selectNone

  render: ->
    super

    # Create a feature type selection table
    aoColumns = [
      {
         sTitle: "Data Types To Valdiate"
         mData: (source) =>
           displayName = utils.translateDataType(source.name)
           if source.name != "CLINICAL_CORE_TYPE"
             if source.selected == false
               """
               <span id="toggle-feature"
                  style="cursor:pointer"
                  data-feature-type="#{source.name}">
                  <i class="icon icon-check-empty"></i> #{displayName}
               </span>
               """
             else
               """
               <span id="toggle-feature"
                  style="cursor:pointer"
                  data-feature-type="#{source.name}">
                  <i class="icon icon-check"></i> #{displayName}
               </span>
               """
           else
             """
             <span data-feature-type="#{source.name}">
               <i class="icon icon-check"></i> #{displayName}
             </span>
             """
      }
      {
        sTitle: "State"
        mData: (source) =>
          state = @dataState[source.name]
          if state
            ui_state = state.replace("_", " ")
            switch state
              when "INVALID"
                return "<span class='invalid'>"+ui_state+"</span>"
              when "VALID"
                return "<span class='valid'>"+ui_state+"</span>"
              else
                return "<span>Not Validated</span>"
          else
            return "<span>Not Validated</span>"
      }
    ]

    @featureTable = $("#validate-file-types").dataTable
      bDestroy: true
      bPaginate: false
      bFilter: false
      bSort:false
      bInfo: false
      sAjaxSource: ""
      sAjaxDataProp: ""
      fnRowCallback: (nRow, aData, iDisplayIndex, iDisplayIndexFull) ->
        if aData.name == "CLINICAL_CORE_TYPE"
          $(nRow).css {'color': '#999', 'font-style': 'italic'}
      aoColumns: aoColumns
      fnServerData: (sSource, aoData, fnCallback) =>
        fnCallback @features

  validateSubmission: (e) ->
    #console.debug "ValidateSubmissionView#completeRelease", @model
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

    # Grab the selected feature types
    featureToValidate = _.filter @features, (f) -> f.selected == true
    featureParams = _.pluck featureToValidate, 'name'

    nextRelease.queue [{
      key: @options.submission.get("projectKey")
      emails: @.$('#emails').val().split(',')
      dataTypes: featureParams
      #featureTypes: featureParams
    }],
    success: =>
      @$el.modal 'hide'
      mediator.publish "validateSubmission"
      mediator.publish "notify", "Submission for Project "+
        "#{@model.get('projectName')} has been queued for Validation."

