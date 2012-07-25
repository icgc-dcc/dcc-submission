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
  NextRelease = require 'models/next_release'
  template = require 'text!views/templates/release/complete_release.handlebars'

  'use strict'

  class CompleteReleaseView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'append'
    autoRender: false
    tagName: 'div'
    className: "modal fade"
    id: 'complete-release-popup'
    
    initialize: ->
      console.debug "CompleteReleaseView#initialize", @, @el
      super
      
      @model = new NextRelease()
      @model.fetch()
      @modelBind 'change', @render
      
      @$el.modal "show": true
      
      @delegate 'click', '#complete-release-button', @completeRelease
      
    completeRelease: ->
      console.debug "CompleteReleaseView#completeRelease"
      
      nextRelease = new NextRelease()
      
      nextRelease.save {name: @.$('#nextRelease').val()}
        success: (data) ->
          @.$('.modal').modal('hide')
          Chaplin.mediator.publish "completeRelease", data
          
        error: (model, error) ->
          err = error.statusText + error.responseText
          alert = @.$('.alert.alert-error')
          
          if alert.length
            alert.text(err)
          else
            @.$('fieldset')
              .before("<div class='alert alert-error'>#{err}</div>")