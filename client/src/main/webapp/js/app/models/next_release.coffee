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
  Release = require 'models/release'

  "use strict"

  class NextRelease extends Release
    urlKey: "id"
    urlPath: ->
      "nextRelease/"

    defaults:
      "submissions": [
        {"projectKey": "project1", "state": "SIGNED_OFF"}
        {"projectKey": "project2", "state": "VALID"}
        {"projectKey": "project3", "state": "NOT_VALIDATED"}
        {"projectKey": "project4", "state": "INVALID"}
        {"projectKey": "project5", "state": "NOT_VALIDATED"}
      ]

    initialize: ->
      console.debug? 'NextRelease#initialize', @

    queue: (attributes, options)->
      @urlPath = ->
        "nextRelease/queue"
      
      @attributes = attributes
      
      @save(attributes, options)

    signOff: (attributes, options)->
      @urlPath = ->
        "nextRelease/signed"
      
      @attributes = attributes
      
      @save(attributes, options)