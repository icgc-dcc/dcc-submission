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
  View = require 'views/base/view'
  Release = require 'models/release'
  NextRelease = require 'models/next_release'
  template = require 'text!views/templates/release/complete_release.handlebars'

  'use strict'

  class CompleteReleaseView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'append'
    autoRender: true
    tagName: 'div'
    className: "modal hide fade"
    id: 'complete-release-popup'
    
    initialize: ->
      console.debug "CompleteReleaseView#initialize", @options, @, @el
      super
      
      @model = new Release(@options)
      
      # In ReleasesView this will cause the model to immediately 
      # close, but without it the model never opens in ReleaseView
      # ... I don't get it.
      @$el.modal('show') if @options.show

      @delegate 'click', '#complete-release-button', @completeRelease
    
    errors: (err) ->
      switch err.code
        when "InvalidName"
          "A release name must only use letters[a-z], numbers(0-9), underscores(_) and dashes(-)"
        when "DuplicateReleaseName"
          "That release name has already been used."
        when "SignedOffSubmissionRequired"
          "The release needs at least one SIGNED OFF submission before it can be COMPLETED."
        
      
    completeRelease: ->
      console.debug "CompleteReleaseView#completeRelease"
      
      nextRelease = new NextRelease {name: @.$('#nextRelease').val()}
      
      nextRelease.save {},
        success: (data) =>
          @$el.modal('hide')
          Chaplin.mediator.publish "completeRelease", data
          
        error: (model, error) =>
          err = $.parseJSON error.responseText
          alert = @.$('.alert.alert-error')
          
          if alert.length
            alert.text(@errors(err))
          else
            @.$('.alert')
              .before("<div class='alert alert-error'>#{@errors(err)}</div>")