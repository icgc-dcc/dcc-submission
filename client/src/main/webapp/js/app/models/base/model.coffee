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
  Backbone = require 'backbone'
  Chaplin = require 'chaplin'
  utils = require 'lib/utils'
  
  "use strict"

  class Model extends Chaplin.Model
    # Place your application-specific model features here
    apiRoot: utils.apiRoot
    urlKey: "_id"

    urlPath: ->
      console.debug? 'Model#urlPath'
      ''

    urlRoot: ->
      console.debug? 'Model#urlRoot'
      urlPath = @urlPath()
      if urlPath
        @apiRoot + urlPath
      else if @collection
        @collection.url()
      else
        throw new Error('Model must redefine urlPath')

    url: ->
      console.debug? 'Model#url'
      base = @urlRoot()
      url = if @get(@urlKey)?
        base + encodeURIComponent(@get(@urlKey))
      else
        base
      url + "?preventCache="+ (new Date()).getTime()
    
    sync: (method, model, options) ->
      console.debug? 'Model#sync', method, model, options
      
      options.beforeSend = utils.sendAuthorization

      Backbone.sync(method, model, options)