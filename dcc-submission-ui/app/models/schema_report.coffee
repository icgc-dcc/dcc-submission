"""
* Copyright 2016(c) The Ontario Institute for Cancer Research.
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


Model = require 'models/base/model'
SchemaReportErrors = require 'models/schema_report_errors'
SchemaReportFieldReports = require 'models/schema_report_field_reports'
SchemaReportSummaryReports = require 'models/schema_report_summary_reports'

module.exports = class SchemaReport extends Model

  initialize: ->
    #console.debug 'SchemaReport#initialize', @, @attributes
    super

    @urlPath = ->
      url = "releases/#{@get('release')}/" +
        "submissions/#{@get('submission')}" +
        "/files/#{@get('name')}/report"

    if @get "errorReports"
      errors = []
      for e in @get "errorReports"
        for c in e.fieldErrorReports

          # Make datatable happy, ensure there are no nulls
          c.fieldNames = [] if c.fieldNames == null
          c.lineNumbers = [] if c.lineNumbers == null

          c.errorType = e.errorType
          c.lineValueMap = {}
          for i in [0 .. c.lineNumbers.length - 1]
            c.lineValueMap[c.lineNumbers[i]] = c.values[i]
          c.lineNumbers = c.lineNumbers.sort((a,b)-> a - b)



          errors.push c




    @set "errorReports", new SchemaReportErrors errors
    @set "fieldReports", new SchemaReportFieldReports @get "fieldReports"
    @set "summaryReports", new SchemaReportSummaryReports @get "summaryReports"

  parse: (response) ->
    #response.state =
    #  if response.errors.length then "INVALID" else "VALID"

    errors = []
    for e in response.errorReports
      for c in e.fieldErrorReports
        # Make datatable happy, ensure there are no nulls
        c.fieldNames = [] if c.fieldNames == null
        c.lineNumbers = [] if c.lineNumbers == null

        c.errorType = e.errorType
        c.lineValueMap = {}
        for i in [0 .. c.lineNumbers.length - 1]
          c.lineValueMap[c.lineNumbers[i]] = c.values[i]
        c.lineNumbers = c.lineNumbers.sort((a,b)-> a - b)
        errors.push c

    response.errorReports = new SchemaReportErrors errors
    response.fieldReports = new SchemaReportFieldReports response.fieldReports
    response.summaryReports =
      new SchemaReportSummaryReports response.summaryReports
    response
