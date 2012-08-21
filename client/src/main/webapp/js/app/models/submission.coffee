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
  Model = require 'models/base/model'
  SubmissionFiles = require 'models/submission_files'
  Report = require 'models/report'

  "use strict"

  class Submission extends Model
    idAttribute: "projectKey"

    initialize: ->
      console.debug 'Submission#initialize', @, @attributes
      super

      @urlPath = ->
        "releases/#{@attributes.release}/submissions/#{@attributes.name}"

    parse: (response) ->
      console.debug 'Submission#parse', @, response
      
      data = {
        'schemaReports': response.submissionFiles
      }
      
      if response.report
        data.report = true
        for file in data.schemaReports
          console.log "file", file
          for report in response.report.schemaReports
            console.log "report:", report
            if report.name is file.name
              _.extend(file, report) 
              break

      response.report = new Report _.extend(data,
        {"release": @attributes?.release, "projectKey": response.projectKey})

      response