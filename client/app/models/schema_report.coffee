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


Model = require 'models/base/model'
SchemaReportErrors = require 'models/schema_report_errors'
SchemaReportFieldReports = require 'models/schema_report_field_reports'

module.exports = class SchemaReport extends Model

  initialize: ->
    #console.debug 'SchemaReport#initialize', @, @attributes
    super

    @urlPath = ->
      url = "releases/#{@get('release')}/" +
        "submissions/#{@get('submission')}" +
        "/report/#{@get('name')}"

    if @get "errors"
      errors = []
      for e in @get "errors"
        for c in e.columns
          c.errorType = e.errorType
          c.lineValueMap = {}
          for i in [0 .. c.lines.length - 1]
            c.lineValueMap[c.lines[i]] = c.values[i]
          c.lines = c.lines.sort((a,b)-> a - b)
          errors.push c

    @set "errors", new SchemaReportErrors errors
    @set "fieldReports", new SchemaReportFieldReports @get "fieldReports"

  parse: (response) ->
    response.state =
      if response.errors.length then "INVALID" else "VALID"

    errors = []
    for e in response.errors
      for c in e.columns
        c.errorType = e.errorType
        c.lineValueMap = {}
        for i in [0 .. c.lines.length - 1]
          c.lineValueMap[c.lines[i]] = c.values[i]
        c.lines = c.lines.sort((a,b)-> a - b)
        errors.push c

    response.errors = new SchemaReportErrors errors
    response.fieldReports = new SchemaReportFieldReports response.fieldReports
    response