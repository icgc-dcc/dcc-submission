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

# Configure the AMD module loader
requirejs.config
  # The path where your JavaScripts are located
  baseUrl: '/js/app/'
  # Specify the paths of vendor libraries
  paths:
    jquery: '../vendor/jquery'
    jqSerializeObject: '../vendor/jquery.ba-serializeobject',
    dataTables: '../vendor/jquery.dataTables.min',
    DT_bootstrap: '../vendor/dataTables.bootstrap2',
    underscore: '../vendor/underscore'
    backbone: '../vendor/backbone'
    chaplin: '../vendor/chaplin'
    bootstrap: '../vendor/bootstrap.min'
    moment: '../vendor/moment'
    text: '../vendor/require-text'
    handlebars: '../vendor/handlebars'
    
  # Underscore and Backbone are not AMD-capable per default,
  # so we need to use the AMD wrapping of RequireJS
  shim: 
    backbone:
      deps: ['underscore', 'jquery']
      exports: 'Backbone'
    underscore:
      exports: '_'
    bootstrap:
      deps: ['jquery']
    jqSerializeObject:
      deps: ['jquery']
    dataTables:
      deps: ['jquery']
    DT_bootstrap:
      deps: ['dataTables']
    
  # For easier development, disable browser caching
  # Of course, this should be removed in a production environment
  , urlArgs: 'bust=' +  (new Date()).getTime()


# Add any extra deps that should be loaded with jquery
define "base", [
  'jquery'
  'bootstrap'
  'jqSerializeObject'
  'moment'
  'dataTables'
  'DT_bootstrap'
], ($) -> $

# Bootstrap the application
require ['dcc_submission_application', 'base'], (DccSubmissionApplication) ->
  dcc = new DccSubmissionApplication()
  dcc.initialize()