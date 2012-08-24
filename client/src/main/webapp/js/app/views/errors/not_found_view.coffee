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
  Model = require 'models/base/model'
  View = require 'views/base/view'
  template = require 'text!views/templates/errors/not_found.handlebars'

  'use strict'

  class NotFoundView extends View
    template: template
    template = null
    
    container: '#content-container'
    containerMethod: 'html'
    autoRender: false
    tagName: 'div'
    id: 'not-found-view'
    
    autoRender: true
    
    initialize: ->
      #console.debug "CompactReleaseView#initialize", @model
      @model = new Model {sec: 5}
      super
      
      @modelBind 'change', @render
      
      i = setInterval =>
        if @model.get('sec') > 0 
          @model.set 'sec', @model.get('sec') - 1
        else
          clearInterval(i)
          Chaplin.mediator.publish '!startupController', 'release', 'list'
          
      , 1000
