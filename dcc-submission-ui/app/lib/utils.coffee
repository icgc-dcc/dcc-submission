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

Chaplin = require 'chaplin'

# Application-specific utilities
# ------------------------------

# Shortcut to the mediator
mediator = Chaplin.mediator

# Delegate to Chaplin’s utils module
utils = Chaplin.utils.beget Chaplin.utils

# Add additional application-specific properties and methods

_(utils).extend
  # Functional helpers for handling asynchronous dependancies and I/O
  # -----------------------------------------------------------------

  ###
  Wrap methods so they can be called before a deferred is resolved.
  The actual methods are called once the deferred is resolved.

  Parameters:

  Expects an options hash with the following properties:

  deferred
    The Deferred object to wait for.

  methods
    Either:
    - A string with a method name e.g. 'method'
    - An array of strings e.g. ['method1', 'method2']
    - An object with methods e.g. {method: -> alert('resolved!')}

  host (optional)
    If you pass an array of strings in the `methods` parameter the methods
    are fetched from this object. Defaults to `deferred`.

  target (optional)
    The target object the new wrapper methods are created at.
    Defaults to host if host is given, otherwise it defaults to deferred.

  onDeferral (optional)
    An additional callback function which is invoked when the method is called
    and the Deferred isn't resolved yet.
    After the method is registered as a done handler on the Deferred,
    this callback is invoked. This can be used to trigger the resolving
    of the Deferred.

  Examples:

  deferMethods(deferred: def, methods: 'foo')
    Wrap the method named foo of the given deferred def and
    postpone all calls until the deferred is resolved.

  deferMethods(deferred: def, methods: def.specialMethods)
    Read all methods from the hash def.specialMethods and
    create wrapped methods with the same names at def.

  deferMethods(
    deferred: def, methods: def.specialMethods, target: def.specialMethods
  )
    Read all methods from the object def.specialMethods and
    create wrapped methods at def.specialMethods,
    overwriting the existing ones.

  deferMethods(deferred: def, host: obj, methods: ['foo', 'bar'])
    Wrap the methods obj.foo and obj.bar so all calls to them are postponed
    until def is resolved. obj.foo and obj.bar are overwritten
    with their wrappers.

  ###
  deferMethods: (options) ->
    # Process options
    deferred = options.deferred
    methods = options.methods
    host = options.host or deferred
    target = options.target or host
    onDeferral = options.onDeferral

    # Hash with named functions
    methodsHash = {}

    if typeof methods is 'string'
      # Transform a single method string into an object
      methodsHash[methods] = host[methods]

    else if methods.length and methods[0]
      # Transform a method list into an object
      for name in methods
        func = host[name]
        unless typeof func is 'function'
          throw new TypeError "utils.deferMethods: method #{name} not
found on host #{host}"
        methodsHash[name] = func

    else
      # Treat methods parameter as a hash, no transformation
      methodsHash = methods

    # Process the hash
    for own name, func of methodsHash
      # Ignore non-function properties
      continue unless typeof func is 'function'
      # Replace method with wrapper
      target[name] = utils.createDeferredFunction(
        deferred, func, target, onDeferral
      )

  # Creates a function which wraps `func` and defers calls to
  # it until the given `deferred` is resolved. Pass an optional `context`
  # to determine the this `this` binding of the original function.
  # Defaults to `deferred`. The optional `onDeferral` function to after
  # original function is registered as a done callback.
  createDeferredFunction: (deferred, func, context = deferred, onDeferral) ->
    # Return a wrapper function
    ->
      # Save the original arguments
      args = arguments
      if deferred.state() is 'resolved'
        # Deferred already resolved, call func immediately
        func.apply context, args
      else
        # Register a done handler
        deferred.done ->
          func.apply context, args
        # Invoke the onDeferral callback
        if typeof onDeferral is 'function'
          onDeferral.apply context

  is_admin: ->
    #console.debug 'utils#is_admin', mediator.user
    roles = mediator.user?.get("roles") or []
    "admin" in roles

  is_released: (state)->
    state is "COMPLETED"

  date: (date) ->
    moment(date).format("YYYY-MM-DD HH:mm:ss")

  fileSize: (fs) ->
    sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    posttxt = 0
    bytes = fs * 1
    precision = 2

    if bytes <= 1024
      precision = 0

    while bytes >= 1024
      posttxt++
      bytes = bytes / 1024

    Number(bytes).toFixed(precision) + " " + sizes[posttxt]

  sendAuthorization: (xhr) ->
    #console.debug 'utils#sendAuthorization', Chaplin.mediator.user
    accessToken = Chaplin.mediator.user?.get "accessToken"
    if accessToken
      # refresh expire time
      # $.cookie 'accessToken', accessToken
      xhr.setRequestHeader 'Authorization', "Basic  #{accessToken}"
    else
      Chaplin.mediator.publish '!startupController', 'session', 'login'

  getOrphanWarning: ->
    return """
    <p><i class="icon-warning-sign"></i>
    Please be advised that <em>donors</em>, <em>specimen</em>, and <em>analyzed_samples</em> with no associated
    experimental results will be ignored and not reported in the upcoming Data Portal release.</p>
    """

  getStateDisplay: (state) ->
    state = state.replace("_", " ")
    return switch state
      when "INVALID"
        "<span class='error'><i class='icon-remove-sign'></i> " + state + "</span>"
      when "VALID"
        "<span class='valid'><i class='icon-ok-sign'></i> " + state + "</span>"
      when "VALIDATING"
        "<span class='validating'><i class='icon-cogs'></i> " + state + "</span>"
      when "QUEUED"
        "<span class='queued'><i class='icon-time'></i> " + state + "</span>"
      when "ERROR"
        "<span class='error'>" + "<i class='icon-exclamation-sign'></i> " + state + "</span>"
      when "NOT VALIDATED"
        "<span><i class='icon-question-sign'></i> " + state + "</span>"
      when "SIGNED OFF"
        "<span class='valid'><i class='icon-lock'></i> " + state + "</span>"
      else
        state


  translateDataType: (dataType) ->
    switch dataType
      when "CLINICAL_CORE_TYPE"
        "Clinical"
      when "CLINICAL_SUPPLEMENTAL_TYPE"
        "Clinical - supplemental"
      when "SSM_TYPE"
        "SSM"
      when "SGV_TYPE"
        "SGV"
      when "CNSM_TYPE"
        "CNSM"
      when "STSM_TYPE"
        "StSM"
      when "STGV_TYPE"
        "StGV"
      when "METH_TYPE"
        "METH"
      when "MIRNA_TYPE"
        "miRNA"
      when "EXP_TYPE"
        "EXP"
      when "PEXP_TYPE"
        "PEXP"
      when "JCN_TYPE"
        "JCN"

      # TODO, confirm display names
      when "METH_ARRAY_TYPE"
        "METH Array"
      when "METH_SEQ_TYPE"
        "METH Seq"
      #when "GEXP_ARRAY_TYPE"
      #  dataType
      #when "GEXP_SEQ_TYPE"
      #  dataType
      #when "PEXP_ARRAY_TYPE"
      #  dataTYpe
      #when "PEXP_SEQ_TYPE"
      #  dataType
      #when "MIRNA_ARRAY_TYPE"
      #  dataType
      when "MIRNA_SEQ_TYPE"
        "miRNA Seq"
      when "EXP_ARRAY_TYPE"
        "EXP Array"
      when "EXP_SEQ_TYPE"
        "EXP Seq"
      else
        dataType

module.exports = utils
