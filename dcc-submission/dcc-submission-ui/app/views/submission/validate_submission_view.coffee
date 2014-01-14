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
    for f, idx in @features
      f.selected = false
      @featureTable.fnUpdate(@features[idx], idx)

  selectAll: ->
    for f, idx in @features
      f.selected = true
      @featureTable.fnUpdate(@features[idx], idx)

  toggleFeature: (e) ->
    feature = $(e.currentTarget).data('feature-type')
    idx = _.pluck(@features, 'name').indexOf(feature)
    @features[idx].selected = not @features[idx].selected
    @featureTable.fnUpdate(@features[idx], idx)

    console.log "clicked", feature, idx
    console.log _.pluck(@features, 'selected')

  initialize: ->
    #console.debug "ValidateSubmissionView#initialize", @options
    @model = new Model @options.submission.getAttributes()
    @model.set({email: mediator.user.get("email")}, {silent: true})

    # TODO: probably need to run unique on this on live
    submissionFiles = @model.get "submissionFiles"
    console.log submissionFiles
    @features = []
    @features.push {'name':f.name, 'selected':true} for f in submissionFiles
    @featureTable = null

    console.log @features

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
         sTitle: "Feature Types To Validate"
         mData: (source) ->
           return source.name
      }
      {
         sTitle: "Select"
         mData: (source) ->
           if source.selected == false
             """
             <i id="toggle-feature"
                class="icon icon-check-empty"
                data-feature-type="#{source.name}">
             </i>
             """
           else
             """
             <i id="toggle-feature"
                class="icon icon-check"
                data-feature-type="#{source.name}">
             </i>
             """
           #return "abcdefg"
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
      aoColumns: aoColumns
      fnServerData: (sSource, aoData, fnCallback) =>
        console.log "aaaaaa"
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

    #TODO: Need to add file types
    nextRelease.queue [{
      key: @options.submission.get("projectKey")
      emails: @.$('#emails').val().split(',')
      featureTypes: []
    }],
    success: =>
      @$el.modal 'hide'
      mediator.publish "validateSubmission"
      mediator.publish "notify", "Submission for Project "+
        "#{@model.get('projectName')} has been queued for Validation."

