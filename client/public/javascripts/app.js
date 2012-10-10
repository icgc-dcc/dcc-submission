(function(/*! Brunch !*/) {
  'use strict';

  var globals = typeof window !== 'undefined' ? window : global;
  if (typeof globals.require === 'function') return;

  var modules = {};
  var cache = {};

  var has = function(object, name) {
    return ({}).hasOwnProperty.call(object, name);
  };

  var expand = function(root, name) {
    var results = [], parts, part;
    if (/^\.\.?(\/|$)/.test(name)) {
      parts = [root, name].join('/').split('/');
    } else {
      parts = name.split('/');
    }
    for (var i = 0, length = parts.length; i < length; i++) {
      part = parts[i];
      if (part === '..') {
        results.pop();
      } else if (part !== '.' && part !== '') {
        results.push(part);
      }
    }
    return results.join('/');
  };

  var dirname = function(path) {
    return path.split('/').slice(0, -1).join('/');
  };

  var localRequire = function(path) {
    return function(name) {
      var dir = dirname(path);
      var absolute = expand(dir, name);
      return globals.require(absolute);
    };
  };

  var initModule = function(name, definition) {
    var module = {id: name, exports: {}};
    definition(module.exports, localRequire(name), module);
    var exports = cache[name] = module.exports;
    return exports;
  };

  var require = function(name) {
    var path = expand(name, '.');

    if (has(cache, path)) return cache[path];
    if (has(modules, path)) return initModule(path, modules[path]);

    var dirIndex = expand(path, './index');
    if (has(cache, dirIndex)) return cache[dirIndex];
    if (has(modules, dirIndex)) return initModule(dirIndex, modules[dirIndex]);

    throw new Error('Cannot find module "' + name + '"');
  };

  var define = function(bundle) {
    for (var key in bundle) {
      if (has(bundle, key)) {
        modules[key] = bundle[key];
      }
    }
  }

  globals.require = require;
  globals.require.define = define;
  globals.require.brunch = true;
})();

window.require.define({"application": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Application, Chaplin, Layout, NavigationController, SessionController, mediator, routes,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  mediator = require('mediator');

  routes = require('routes');

  SessionController = require('controllers/session_controller');

  NavigationController = require('controllers/navigation_controller');

  Layout = require('views/layout');

  module.exports = Application = (function(_super) {

    __extends(Application, _super);

    function Application() {
      return Application.__super__.constructor.apply(this, arguments);
    }

    Application.prototype.title = 'ICGC DCC Data Submission';

    Application.prototype.initialize = function() {
      Application.__super__.initialize.apply(this, arguments);
      this.initDispatcher();
      this.initLayout();
      this.initMediator();
      this.initControllers();
      this.initRouter(routes);
      return typeof Object.freeze === "function" ? Object.freeze(this) : void 0;
    };

    Application.prototype.initLayout = function() {
      return this.layout = new Layout({
        title: this.title
      });
    };

    Application.prototype.initControllers = function() {
      new SessionController();
      return new NavigationController();
    };

    Application.prototype.initMediator = function() {
      mediator.user = null;
      return mediator.seal();
    };

    return Application;

  })(Chaplin.Application);
  
}});

window.require.define({"controllers/auth_controller": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var AuthController, Chaplin, Controller,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  Controller = require('controllers/base/controller');

  module.exports = AuthController = (function(_super) {

    __extends(AuthController, _super);

    function AuthController() {
      return AuthController.__super__.constructor.apply(this, arguments);
    }

    AuthController.prototype.historyURL = 'auth';

    AuthController.prototype.logout = function() {
      var location;
      location = window.location;
      localStorage.clear();
      Chaplin.mediator.publish('!logout');
      return window.location = location;
    };

    return AuthController;

  })(Controller);
  
}});

window.require.define({"controllers/base/controller": function(exports, require, module) {
  var Chaplin, Controller,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  module.exports = Controller = (function(_super) {

    __extends(Controller, _super);

    function Controller() {
      return Controller.__super__.constructor.apply(this, arguments);
    }

    return Controller;

  })(Chaplin.Controller);
  
}});

window.require.define({"controllers/errors_controller": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Controller, ErrorController, notFoundView,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Controller = require('controllers/base/controller');

  notFoundView = require('views/errors/not_found_view');

  module.exports = ErrorController = (function(_super) {

    __extends(ErrorController, _super);

    function ErrorController() {
      return ErrorController.__super__.constructor.apply(this, arguments);
    }

    ErrorController.prototype.notFound = function(params) {
      console.debug('ErrorController#notFound', params);
      this.title = '404 - Request Not Found';
      return this.view = new notFoundView();
    };

    return ErrorController;

  })(Controller);
  
}});

window.require.define({"controllers/navigation_controller": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Controller, Navigation, NavigationController, NavigationView,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Controller = require('controllers/base/controller');

  Navigation = require('models/navigation');

  NavigationView = require('views/navigation_view');

  module.exports = NavigationController = (function(_super) {

    __extends(NavigationController, _super);

    function NavigationController() {
      return NavigationController.__super__.constructor.apply(this, arguments);
    }

    NavigationController.prototype.initialize = function() {
      NavigationController.__super__.initialize.apply(this, arguments);
      this.model = new Navigation();
      return this.view = new NavigationView({
        model: this.model
      });
    };

    return NavigationController;

  })(Controller);
  
}});

window.require.define({"controllers/project_controller": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var BaseController, Chaplin, Project, ProjectController, ProjectView, Projects, ProjectsView,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  BaseController = require('controllers/base/controller');

  Project = require('models/project');

  Projects = require('models/projects');

  ProjectView = require('views/project_view');

  ProjectsView = require('views/projects_view');

  module.exports = ProjectController = (function(_super) {

    __extends(ProjectController, _super);

    function ProjectController() {
      return ProjectController.__super__.constructor.apply(this, arguments);
    }

    ProjectController.prototype.title = 'Projects';

    ProjectController.prototype.historyURL = function(params) {
      return '';
    };

    ProjectController.prototype.show = function(params) {
      console.debug('ProjectController#show');
      this.model = new Project();
      this.view = new ProjectView({
        model: this.model
      });
      return this.model.fetch();
    };

    ProjectController.prototype.list = function(params) {
      console.debug('ProjectController#list');
      this.collection = new Projects();
      this.collection.fetch();
      console.debug('ProjectController#list', this.collection);
      return this.view = new ProjectsView({
        collection: this.collection
      });
    };

    return ProjectController;

  })(BaseController);
  
}});

window.require.define({"controllers/release_controller": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var BaseController, Chaplin, Release, ReleaseController, ReleaseView, Releases, ReleasesView, Submission, SubmissionView,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  BaseController = require('controllers/base/controller');

  Release = require('models/release');

  Submission = require('models/submission');

  Releases = require('models/releases');

  ReleaseView = require('views/release/release_view');

  SubmissionView = require('views/release/submission_view');

  ReleasesView = require('views/release/releases_view');

  module.exports = ReleaseController = (function(_super) {

    __extends(ReleaseController, _super);

    function ReleaseController() {
      return ReleaseController.__super__.constructor.apply(this, arguments);
    }

    ReleaseController.prototype.title = 'Releases';

    ReleaseController.prototype.historyURL = function(params) {
      if (params.release) {
        return "releases/" + params.release;
      } else {
        return "releases";
      }
    };

    ReleaseController.prototype.list = function(params) {
      console.debug('ReleaseController#list', params);
      this.collection = new Releases();
      this.view = new ReleasesView({
        collection: this.collection
      });
      return this.collection.fetch();
    };

    ReleaseController.prototype.show = function(params) {
      var _this = this;
      console.debug('ReleaseController#show', params);
      this.title = params.release;
      this.model = new Release({
        name: params.release
      });
      this.view = new ReleaseView({
        model: this.model
      });
      return this.model.fetch({
        success: function() {
          return _this.view.render();
        },
        error: function() {
          return Chaplin.mediator.publish('!startupController', 'release', 'list');
        }
      });
    };

    ReleaseController.prototype.submission = function(params) {
      console.debug('ReleaseController#submission', params);
      this.title = "" + params.submission + " - " + params.release;
      this.model = new Submission({
        release: params.release,
        name: params.submission
      });
      this.view = new SubmissionView({
        model: this.model
      });
      return this.model.fetch({
        error: function() {
          return Chaplin.mediator.publish('!startupController', 'release', 'show', {
            release: params.release
          });
        }
      });
    };

    return ReleaseController;

  })(BaseController);
  
}});

window.require.define({"controllers/session_controller": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, Controller, DCC, LoginView, SessionController, User,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  Controller = require('controllers/base/controller');

  User = require('models/user');

  LoginView = require('views/login_view');

  DCC = require('lib/services/dcc');

  module.exports = SessionController = (function(_super) {

    __extends(SessionController, _super);

    function SessionController() {
      this.logout = __bind(this.logout, this);

      this.serviceProviderSession = __bind(this.serviceProviderSession, this);

      this.triggerLogin = __bind(this.triggerLogin, this);
      return SessionController.__super__.constructor.apply(this, arguments);
    }

    SessionController.serviceProviders = {
      dcc: new DCC()
    };

    SessionController.prototype.loginStatusDetermined = false;

    SessionController.prototype.loginView = null;

    SessionController.prototype.serviceProviderName = null;

    SessionController.prototype.initialize = function() {
      this.subscribeEvent('serviceProviderSession', this.serviceProviderSession);
      this.subscribeEvent('logout', this.logout);
      this.subscribeEvent('userData', this.userData);
      this.subscribeEvent('!showLogin', this.showLoginView);
      this.subscribeEvent('!login', this.triggerLogin);
      this.subscribeEvent('!logout', this.triggerLogout);
      return this.getSession();
    };

    SessionController.prototype.loadServiceProviders = function() {
      var name, serviceProvider, _ref, _results;
      _ref = SessionController.serviceProviders;
      _results = [];
      for (name in _ref) {
        serviceProvider = _ref[name];
        _results.push(serviceProvider.load());
      }
      return _results;
    };

    SessionController.prototype.createUser = function(userData) {
      return Chaplin.mediator.user = new User(userData);
    };

    SessionController.prototype.getSession = function() {
      var name, serviceProvider, _ref, _results;
      this.loadServiceProviders();
      _ref = SessionController.serviceProviders;
      _results = [];
      for (name in _ref) {
        serviceProvider = _ref[name];
        _results.push(serviceProvider.done(serviceProvider.getLoginStatus));
      }
      return _results;
    };

    SessionController.prototype.showLoginView = function() {
      if (this.loginView) {
        return;
      }
      this.loadServiceProviders();
      return this.loginView = new LoginView({
        serviceProviders: SessionController.serviceProviders
      });
    };

    SessionController.prototype.triggerLogin = function(serviceProviderName) {
      var serviceProvider;
      serviceProvider = SessionController.serviceProviders[serviceProviderName];
      if (!serviceProvider.isLoaded()) {
        Chaplin.mediator.publish('serviceProviderMissing', serviceProviderName);
        return;
      }
      Chaplin.mediator.publish('loginAttempt', serviceProviderName);
      return serviceProvider.triggerLogin();
    };

    SessionController.prototype.serviceProviderSession = function(session) {
      this.serviceProviderName = session.provider.name;
      this.disposeLoginView();
      session.id = session.userId;
      delete session.userId;
      this.createUser(session);
      return this.publishLogin();
    };

    SessionController.prototype.publishLogin = function() {
      this.loginStatusDetermined = true;
      Chaplin.mediator.publish('login', Chaplin.mediator.user);
      return Chaplin.mediator.publish('loginStatus', true);
    };

    SessionController.prototype.triggerLogout = function() {
      return Chaplin.mediator.publish('logout');
    };

    SessionController.prototype.logout = function() {
      this.loginStatusDetermined = true;
      this.disposeUser();
      this.serviceProviderName = null;
      this.showLoginView();
      return Chaplin.mediator.publish('loginStatus', false);
    };

    SessionController.prototype.userData = function(data) {
      return Chaplin.mediator.user.set(data);
    };

    SessionController.prototype.disposeLoginView = function() {
      if (!this.loginView) {
        return;
      }
      this.loginView.dispose();
      return this.loginView = null;
    };

    SessionController.prototype.disposeUser = function() {
      if (!Chaplin.mediator.user) {
        return;
      }
      Chaplin.mediator.user.dispose();
      return Chaplin.mediator.user = null;
    };

    return SessionController;

  })(Controller);
  
}});

window.require.define({"initialize": function(exports, require, module) {
  var Application;

  Application = require('application');

  $(document).on('ready', function() {
    var app;
    app = new Application();
    return app.initialize();
  });
  
}});

window.require.define({"lib/services/dcc": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, DCC, ServiceProvider, utils,
    __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  ServiceProvider = require('lib/services/service_provider');

  utils = require('lib/utils');

  module.exports = DCC = (function(_super) {

    __extends(DCC, _super);

    DCC.prototype.baseUrl = "/ws/";

    function DCC() {
      this.loginStatusHandler = __bind(this.loginStatusHandler, this);

      this.loginHandler = __bind(this.loginHandler, this);

      var authCallback;
      DCC.__super__.constructor.apply(this, arguments);
      this.accessToken = localStorage.getItem('accessToken');
      authCallback = _(this.loginHandler).bind(this, this.loginHandler);
      Chaplin.mediator.subscribe('auth:callback:dcc', authCallback);
    }

    DCC.prototype.load = function() {
      this.resolve();
      return this;
    };

    DCC.prototype.isLoaded = function() {
      return true;
    };

    DCC.prototype.ajax = function(type, url, data) {
      var options;
      url = this.baseUrl + url;
      options = {
        url: url,
        data: data,
        type: type,
        dataType: 'json'
      };
      if (this.accessToken) {
        options["beforeSend"] = utils.sendAuthorization;
      }
      return $.ajax(options);
    };

    DCC.prototype.triggerLogin = function(loginContext) {
      return window.location.reload();
    };

    DCC.prototype.loginHandler = function(loginContext, response) {
      if (response) {
        Chaplin.mediator.publish('loginSuccessful', {
          provider: this,
          loginContext: loginContext
        });
        this.accessToken = response.accessToken;
        localStorage.setItem('accessToken', this.accessToken);
        return this.getUserData().done(this.processUserData);
      } else {
        return Chaplin.mediator.publish('loginFail', {
          provider: this,
          loginContext: loginContext
        });
      }
    };

    DCC.prototype.getUserData = function() {
      return this.ajax('get', 'users/self');
    };

    DCC.prototype.processUserData = function(response) {
      return Chaplin.mediator.publish('userData', response);
    };

    DCC.prototype.getLoginStatus = function(callback, force) {
      if (callback == null) {
        callback = this.loginStatusHandler;
      }
      if (force == null) {
        force = false;
      }
      return this.getUserData().always(callback);
    };

    DCC.prototype.loginStatusHandler = function(response, status) {
      if (!response || status === 'error') {
        return Chaplin.mediator.publish('logout');
      } else {
        return Chaplin.mediator.publish('serviceProviderSession', _.extend(response, {
          provider: this,
          userId: response.responseText,
          accessToken: this.accessToken
        }));
      }
    };

    return DCC;

  })(ServiceProvider);
  
}});

window.require.define({"lib/services/service_provider": function(exports, require, module) {
  var Chaplin, ServiceProvider, utils;

  utils = require('lib/utils');

  Chaplin = require('chaplin');

  module.exports = ServiceProvider = (function() {

    _(ServiceProvider.prototype).extend(Chaplin.EventBroker);

    ServiceProvider.prototype.loading = false;

    function ServiceProvider() {
      _(this).extend($.Deferred());
      utils.deferMethods({
        deferred: this,
        methods: ['triggerLogin', 'getLoginStatus'],
        onDeferral: this.load
      });
    }

    ServiceProvider.prototype.disposed = false;

    ServiceProvider.prototype.dispose = function() {
      if (this.disposed) {
        return;
      }
      this.unsubscribeAllEvents();
      this.disposed = true;
      return typeof Object.freeze === "function" ? Object.freeze(this) : void 0;
    };

    return ServiceProvider;

  })();

  /*

    Standard methods and their signatures:

    load: ->
      # Load a script like this:
      utils.loadLib 'http://example.org/foo.js', @loadHandler, @reject

    loadHandler: =>
      # Init the library, then resolve
      ServiceProviderLibrary.init(foo: 'bar')
      @resolve()

    isLoaded: ->
      # Return a Boolean
      Boolean window.ServiceProviderLibrary and ServiceProviderLibrary.login

    # Trigger login popup
    triggerLogin: (loginContext) ->
      callback = _(@loginHandler).bind(this, loginContext)
      ServiceProviderLibrary.login callback

    # Callback for the login popup
    loginHandler: (loginContext, response) =>

      eventPayload = {provider: this, loginContext}
      if response
        # Publish successful login
        @publishEvent 'loginSuccessful', eventPayload

        # Publish the session
        @publishEvent 'serviceProviderSession',
          provider: this
          userId: response.userId
          accessToken: response.accessToken
          # etc.

      else
        @publishEvent 'loginFail', eventPayload

    getLoginStatus: (callback = @loginStatusHandler, force = false) ->
      ServiceProviderLibrary.getLoginStatus callback, force

    loginStatusHandler: (response) =>
      return unless response
      @publishEvent 'serviceProviderSession',
        provider: this
        userId: response.userId
        accessToken: response.accessToken
        # etc.
  */

  
}});

window.require.define({"lib/support": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, support, utils;

  Chaplin = require('chaplin');

  utils = require('lib/utils');

  support = utils.beget(Chaplin.support);

  support;

  
}});

window.require.define({"lib/utils": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, mediator, utils,
    __hasProp = {}.hasOwnProperty,
    __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; },
    _this = this;

  Chaplin = require('chaplin');

  mediator = Chaplin.mediator;

  utils = Chaplin.utils.beget(Chaplin.utils);

  _(utils).extend({
    /*
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
    */

    deferMethods: function(options) {
      var deferred, func, host, methods, methodsHash, name, onDeferral, target, _i, _len, _results;
      deferred = options.deferred;
      methods = options.methods;
      host = options.host || deferred;
      target = options.target || host;
      onDeferral = options.onDeferral;
      methodsHash = {};
      if (typeof methods === 'string') {
        methodsHash[methods] = host[methods];
      } else if (methods.length && methods[0]) {
        for (_i = 0, _len = methods.length; _i < _len; _i++) {
          name = methods[_i];
          func = host[name];
          if (typeof func !== 'function') {
            throw new TypeError("utils.deferMethods: method " + name + " notfound on host " + host);
          }
          methodsHash[name] = func;
        }
      } else {
        methodsHash = methods;
      }
      _results = [];
      for (name in methodsHash) {
        if (!__hasProp.call(methodsHash, name)) continue;
        func = methodsHash[name];
        if (typeof func !== 'function') {
          continue;
        }
        _results.push(target[name] = utils.createDeferredFunction(deferred, func, target, onDeferral));
      }
      return _results;
    },
    createDeferredFunction: function(deferred, func, context, onDeferral) {
      if (context == null) {
        context = deferred;
      }
      return function() {
        var args;
        args = arguments;
        if (deferred.state() === 'resolved') {
          return func.apply(context, args);
        } else {
          deferred.done(function() {
            return func.apply(context, args);
          });
          if (typeof onDeferral === 'function') {
            return onDeferral.apply(context);
          }
        }
      };
    },
    is_admin: function() {
      return __indexOf.call(mediator.user.roles, "admin") >= 0;
    },
    is_released: function(state) {
      return state === "COMPLETED";
    },
    date: function(date) {
      return moment(date).format("YYYY-MM-DD");
    },
    fileSize: function(fs) {
      var bytes, posttxt, precision, sizes;
      sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
      posttxt = 0;
      bytes = fs * 1;
      precision = 2;
      if (bytes <= 1024) {
        precision = 0;
      }
      while (bytes >= 1024) {
        posttxt++;
        bytes = bytes / 1024;
      }
      return Number(bytes).toFixed(precision) + " " + sizes[posttxt];
    },
    sendAuthorization: function(xhr) {
      _this.accessToken = localStorage.getItem('accessToken');
      return xhr.setRequestHeader('Authorization', "X-DCC-Auth  " + _this.accessToken);
    },
    polling: function(model, timing) {
      setInterval(function() {
        return model.fetch();
      }, timing);
    }
  });

  module.exports = utils;
  
}});

window.require.define({"lib/view_helper": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var mediator, utils,
    __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  mediator = require('mediator');

  utils = require('chaplin/lib/utils');

  Handlebars.registerHelper('if_logged_in', function(options) {
    console.log(mediator.user);
    if (mediator.user) {
      return options.fn(this);
    } else {
      return options.inverse(this);
    }
  });

  Handlebars.registerHelper('is_admin', function(options) {
    return __indexOf.call(mediator.user.get("roles"), "admin") >= 0;
  });

  Handlebars.registerHelper('if_admin', function(options) {
    if (is_admin) {
      return options.fn(this);
    } else {
      return options.inverse(this);
    }
  });

  Handlebars.registerHelper('if_opened', function(state, options) {
    if (state === 'OPENED') {
      return options.fn(this);
    } else {
      return options.inverse(this);
    }
  });

  Handlebars.registerHelper('if_completed', function(state, options) {
    if (state === 'COMPLETED') {
      return options.fn(this);
    } else {
      return options.inverse(this);
    }
  });

  Handlebars.registerHelper('submission_action', function(state) {
    switch (state) {
      case "VALID":
        return new Handlebars.SafeString("<button\n  class=\"btn btn-success\"\n  id=\"signoff-submission-popup-button\"\n  data-toggle=\"modal\"\n  href=\"#signoff-submission-popup\">\n  Sign Off\n</button>");
      case "INVALID":
      case "NOT_VALIDATED":
        return new Handlebars.SafeString("<button\n  class=\"btn btn-success\"\n  id=\"validate-submission-popup-button\"\n  data-toggle=\"modal\"\n  href=\"#validate-submission-popup\">\n  Validate\n</button>");
    }
  });

  Handlebars.registerHelper('release_date', function(date) {
    if (!date) {
      return new Handlebars.SafeString('<em>Unreleased</em>');
    }
    return Handlebars.helpers.date.call(this, date);
  });

  Handlebars.registerHelper('date', function(date) {
    if (!date) {
      return false;
    }
    return new Handlebars.SafeString(moment(date).format("YYYY-MM-DD"));
  });

  Handlebars.registerHelper('underscore2space', function(string) {
    if (!string) {
      return false;
    }
    return new Handlebars.SafeString(string.replace("_", " "));
  });

  Handlebars.registerHelper('lowercase', function(string) {
    if (!string) {
      return false;
    }
    return new Handlebars.SafeString(string.toLowerCase());
  });

  Handlebars.registerHelper('release_summary', function(submissions) {
    var invalid, not_validated, queued, signed_off, submission, valid, _i, _len, _ref;
    console.log(submissions);
    signed_off = 0;
    valid = 0;
    queued = 0;
    invalid = 0;
    not_validated = 0;
    _ref = submissions.models;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      submission = _ref[_i];
      console.log(submission);
      switch (submission.get("state")) {
        case 'SIGNED_OFF':
          signed_off++;
          break;
        case 'VALID':
          valid++;
          break;
        case 'QUEUED':
          queued++;
          break;
        case 'INVALID':
          invalid++;
          break;
        case 'NOT_VALIDATED':
          not_validated++;
      }
    }
    return new Handlebars.SafeString("<li>Signed Off: " + signed_off + "</li>\n<li>Valid: " + valid + "</li>\n<li>Queued: " + queued + "</li>\n<li>Invalid: " + invalid + "</li>\n<li>Not Validated: " + not_validated + "</li>");
  });
  
}});

window.require.define({"mediator": function(exports, require, module) {
  
  module.exports = require('chaplin').mediator;
  
}});

window.require.define({"models/base/collection": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Backbone, Chaplin, Collection, utils,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Backbone = require('backbone');

  Chaplin = require('chaplin');

  utils = require('lib/utils');

  module.exports = Collection = (function(_super) {

    __extends(Collection, _super);

    function Collection() {
      return Collection.__super__.constructor.apply(this, arguments);
    }

    Collection.prototype.apiRoot = "/ws/";

    Collection.prototype.urlPath = function() {
      return '';
    };

    Collection.prototype.url = function() {
      return this.apiRoot + this.urlPath() + "?preventCache=" + (new Date()).getTime();
    };

    Collection.prototype.sync = function(method, model, options) {
      options.beforeSend = utils.sendAuthorization;
      return Backbone.sync(method, model, options);
    };

    return Collection;

  })(Chaplin.Collection);
  
}});

window.require.define({"models/base/model": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Backbone, Chaplin, Model, utils,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Backbone = require('backbone');

  Chaplin = require('chaplin');

  utils = require('lib/utils');

  module.exports = Model = (function(_super) {

    __extends(Model, _super);

    function Model() {
      return Model.__super__.constructor.apply(this, arguments);
    }

    Model.prototype.apiRoot = "/ws/";

    Model.prototype.urlKey = "_id";

    Model.prototype.urlPath = function() {
      return '';
    };

    Model.prototype.urlRoot = function() {
      var urlPath;
      urlPath = this.urlPath();
      if (urlPath) {
        return this.apiRoot + urlPath;
      } else if (this.collection) {
        return this.collection.url();
      } else {
        throw new Error('Model must redefine urlPath');
      }
    };

    Model.prototype.url = function() {
      var base, url;
      base = this.urlRoot();
      url = this.get(this.urlKey) != null ? base + encodeURIComponent(this.get(this.urlKey)) : base;
      return url + "?preventCache=" + (new Date()).getTime();
    };

    Model.prototype.sync = function(method, model, options) {
      options.beforeSend = utils.sendAuthorization;
      return Backbone.sync(method, model, options);
    };

    return Model;

  })(Chaplin.Model);
  
}});

window.require.define({"models/navigation": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Model, Navigation,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  module.exports = Navigation = (function(_super) {

    __extends(Navigation, _super);

    function Navigation() {
      return Navigation.__super__.constructor.apply(this, arguments);
    }

    Navigation.prototype.defaults = {
      items: [
        {
          href: '/releases',
          title: 'Releases',
          active: 'active'
        }
      ]
    };

    return Navigation;

  })(Model);
  
}});

window.require.define({"models/next_release": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var NextRelease, Release,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Release = require('models/release');

  module.exports = NextRelease = (function(_super) {

    __extends(NextRelease, _super);

    function NextRelease() {
      return NextRelease.__super__.constructor.apply(this, arguments);
    }

    NextRelease.prototype.urlKey = "id";

    NextRelease.prototype.urlPath = function() {
      return "nextRelease/";
    };

    NextRelease.prototype.initialize = function() {};

    NextRelease.prototype.queue = function(attributes, options) {
      this.urlPath = function() {
        return "nextRelease/queue";
      };
      this.attributes = attributes;
      return this.save(attributes, options);
    };

    NextRelease.prototype.signOff = function(attributes, options) {
      this.urlPath = function() {
        return "nextRelease/signed";
      };
      this.attributes = attributes;
      return this.save(attributes, options);
    };

    return NextRelease;

  })(Release);
  
}});

window.require.define({"models/project": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Model, Project,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  module.exports = Project = (function(_super) {

    __extends(Project, _super);

    function Project() {
      return Project.__super__.constructor.apply(this, arguments);
    }

    Project.prototype.urlKey = "name";

    Project.prototype.url = function() {
      return "ws/projects/";
    };

    Project.prototype.fetch = function() {
      this.id = this.get("identifier");
      return Project.__super__.fetch.apply(this, arguments);
    };

    return Project;

  })(Model);
  
}});

window.require.define({"models/projects": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Collection, Project,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Collection = require('models/base/collection');

  module.exports = Project = (function(_super) {

    __extends(Project, _super);

    function Project() {
      return Project.__super__.constructor.apply(this, arguments);
    }

    Project.prototype.url = function() {
      return "/ws/projects";
    };

    Project.prototype.initialize = function() {
      return Project.__super__.initialize.apply(this, arguments);
    };

    return Project;

  })(Collection);
  
}});

window.require.define({"models/release": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Model, Release, Submissions,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  Submissions = require('models/submissions');

  module.exports = Release = (function(_super) {

    __extends(Release, _super);

    function Release() {
      return Release.__super__.constructor.apply(this, arguments);
    }

    Release.prototype.urlKey = "name";

    Release.prototype.urlPath = function() {
      return "releases/";
    };

    Release.prototype.defaults = {
      'submissions': new Submissions([], {
        "release": null
      })
    };

    Release.prototype.parse = function(response) {
      if (response != null ? response.submissions : void 0) {
        response.submissions = new Submissions(response.submissions, {
          "release": response.name
        });
      }
      return response;
    };

    return Release;

  })(Model);
  
}});

window.require.define({"models/releases": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Collection, Release, Releases,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Collection = require('models/base/collection');

  Release = require('models/release');

  module.exports = Releases = (function(_super) {

    __extends(Releases, _super);

    function Releases() {
      return Releases.__super__.constructor.apply(this, arguments);
    }

    Releases.prototype.model = Release;

    Releases.prototype.urlPath = function() {
      return "releases/";
    };

    Releases.prototype.comparator = function(release) {
      return -release.get('releaseDate');
    };

    return Releases;

  })(Collection);
  
}});

window.require.define({"models/report": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Model, Report, SchemaReports,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  SchemaReports = require('models/schema_reports');

  module.exports = Report = (function(_super) {

    __extends(Report, _super);

    function Report() {
      return Report.__super__.constructor.apply(this, arguments);
    }

    Report.prototype.initialize = function() {
      Report.__super__.initialize.apply(this, arguments);
      this.set('schemaReports', new SchemaReports(this.attributes.schemaReports, {
        release: this.attributes.release,
        projectKey: this.attributes.projectKey
      }));
      return this.urlPath = function() {
        return "releases/" + this.attributes.release + "/submissions/      " + this.attributes.submission + "/report";
      };
    };

    Report.prototype.parse = function(response) {
      if (response != null ? response.schemaReports : void 0) {
        response.schemaReports = new SchemaReports(response.schemaReports, {
          release: response.release,
          projectKey: response.projectKey
        });
      }
      return response;
    };

    return Report;

  })(Model);
  
}});

window.require.define({"models/schema_report": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Model, SchemaReport,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  module.exports = SchemaReport = (function(_super) {

    __extends(SchemaReport, _super);

    function SchemaReport() {
      return SchemaReport.__super__.constructor.apply(this, arguments);
    }

    SchemaReport.prototype.initialize = function() {
      return SchemaReport.__super__.initialize.apply(this, arguments);
    };

    return SchemaReport;

  })(Model);
  
}});

window.require.define({"models/schema_reports": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Collection, SchemaReport, SchemaReports,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Collection = require('models/base/collection');

  SchemaReport = require('models/schema_report');

  module.exports = SchemaReports = (function(_super) {

    __extends(SchemaReports, _super);

    function SchemaReports() {
      return SchemaReports.__super__.constructor.apply(this, arguments);
    }

    SchemaReports.prototype.model = SchemaReport;

    SchemaReports.prototype.urlPath = function() {
      return "releases/" + this.release + "/submissions/" + this.projectKey + "/report";
    };

    SchemaReports.prototype.initialize = function(models, options) {
      SchemaReports.__super__.initialize.apply(this, arguments);
      this.release = options.release;
      return this.projectKey = options.projectKey;
    };

    SchemaReports.prototype.parse = function(response) {
      return response.schemaReports;
    };

    return SchemaReports;

  })(Collection);
  
}});

window.require.define({"models/submission": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Model, Report, Submission, SubmissionFiles,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  SubmissionFiles = require('models/submission_files');

  Report = require('models/report');

  module.exports = Submission = (function(_super) {

    __extends(Submission, _super);

    function Submission() {
      return Submission.__super__.constructor.apply(this, arguments);
    }

    Submission.prototype.idAttribute = "projectKey";

    Submission.prototype.defaults = {
      'report': new Report()
    };

    Submission.prototype.initialize = function() {
      Submission.__super__.initialize.apply(this, arguments);
      return this.urlPath = function() {
        return "releases/" + this.attributes.release + "/submissions/" + this.attributes.name;
      };
    };

    Submission.prototype.parse = function(response) {
      var data, file, report, _i, _j, _len, _len1, _ref, _ref1, _ref2;
      data = {
        'schemaReports': response.submissionFiles
      };
      if (response.report) {
        _ref = data.schemaReports;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          file = _ref[_i];
          _ref1 = response.report.schemaReports;
          for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
            report = _ref1[_j];
            if (report.name === file.name) {
              _.extend(file, report);
              break;
            }
          }
        }
      }
      response.report = new Report(_.extend(data, {
        "release": (_ref2 = this.attributes) != null ? _ref2.release : void 0,
        "projectKey": response.projectKey
      }));
      return response;
    };

    return Submission;

  })(Model);
  
}});

window.require.define({"models/submission_file": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Model, SubmissionFile,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  module.exports = SubmissionFile = (function(_super) {

    __extends(SubmissionFile, _super);

    function SubmissionFile() {
      return SubmissionFile.__super__.constructor.apply(this, arguments);
    }

    SubmissionFile.prototype.initialize = function() {
      return SubmissionFile.__super__.initialize.apply(this, arguments);
    };

    return SubmissionFile;

  })(Model);
  
}});

window.require.define({"models/submission_files": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Collection, SubmissionFile, SubmissionFiles,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Collection = require('models/base/collection');

  SubmissionFile = require('models/submission_file');

  module.exports = SubmissionFiles = (function(_super) {

    __extends(SubmissionFiles, _super);

    function SubmissionFiles() {
      return SubmissionFiles.__super__.constructor.apply(this, arguments);
    }

    SubmissionFiles.prototype.model = SubmissionFile;

    SubmissionFiles.prototype.urlPath = function() {
      return "releases/" + this.release + "/submissions/" + this.projectKey + "/files";
    };

    SubmissionFiles.prototype.initialize = function(models, options) {
      SubmissionFiles.__super__.initialize.apply(this, arguments);
      this.release = options.release;
      return this.projectKey = options.projectKey;
    };

    return SubmissionFiles;

  })(Collection);
  
}});

window.require.define({"models/submissions": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Collection, Submission, Submissions,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Collection = require('models/base/collection');

  Submission = require('models/submission');

  module.exports = Submissions = (function(_super) {

    __extends(Submissions, _super);

    function Submissions() {
      return Submissions.__super__.constructor.apply(this, arguments);
    }

    Submissions.prototype.model = Submission;

    Submissions.prototype.urlPath = function() {
      return "releases/" + this.release;
    };

    Submissions.prototype.initialize = function(models, options) {
      return this.release = options.release;
    };

    Submissions.prototype.parse = function(response) {
      return response.submissions;
    };

    return Submissions;

  })(Collection);
  
}});

window.require.define({"models/user": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Model, User,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Model = require('models/base/model');

  module.exports = User = (function(_super) {

    __extends(User, _super);

    function User() {
      return User.__super__.constructor.apply(this, arguments);
    }

    return User;

  })(Model);
  
}});

window.require.define({"routes": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  module.exports = function(match) {
    match('', 'release#list');
    match('releases', 'release#list');
    match('releases/', 'release#list');
    match('releases/:release', 'release#show');
    match('releases/:release/', 'release#show');
    match('releases/:release/submissions/:submission', 'release#submission');
    match('releases/:release/submissions/:submission/', 'release#submission');
    match('logout', 'auth#logout');
    match('logout/', 'auth#logout');
    return match('*anything', 'errors#notFound');
  };
  
}});

window.require.define({"views/base/collection_view": function(exports, require, module) {
  var Chaplin, CollectionView, View,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  View = require('views/base/view');

  module.exports = CollectionView = (function(_super) {

    __extends(CollectionView, _super);

    function CollectionView() {
      return CollectionView.__super__.constructor.apply(this, arguments);
    }

    CollectionView.prototype.getTemplateFunction = View.prototype.getTemplateFunction;

    return CollectionView;

  })(Chaplin.CollectionView);
  
}});

window.require.define({"views/base/data_table_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, DataTableView, View,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  View = require('views/base/view');

  module.exports = DataTableView = (function(_super) {

    __extends(DataTableView, _super);

    function DataTableView() {
      return DataTableView.__super__.constructor.apply(this, arguments);
    }

    DataTableView.prototype.initialize = function() {
      DataTableView.__super__.initialize.apply(this, arguments);
      return this.createDataTable();
    };

    DataTableView.prototype.updateDataTable = function() {
      var dt;
      console.debug("DataTableView#updateDataTable");
      dt = this.$el.dataTable();
      dt.fnClearTable();
      return dt.fnAddData(this.collection.toJSON());
    };

    DataTableView.prototype.createDataTable = function() {
      throw new Error("The DataTableView#createDataTable function must be overridden");
    };

    return DataTableView;

  })(View);
  
}});

window.require.define({"views/base/page_view": function(exports, require, module) {
  var PageView, View,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  module.exports = PageView = (function(_super) {

    __extends(PageView, _super);

    function PageView() {
      return PageView.__super__.constructor.apply(this, arguments);
    }

    PageView.prototype.container = '#page-container';

    PageView.prototype.autoRender = true;

    PageView.prototype.renderedSubviews = false;

    PageView.prototype.initialize = function() {
      var rendered,
        _this = this;
      PageView.__super__.initialize.apply(this, arguments);
      if (this.model || this.collection) {
        rendered = false;
        return this.modelBind('change', function() {
          if (!rendered) {
            _this.render();
          }
          return rendered = true;
        });
      }
    };

    PageView.prototype.renderSubviews = function() {};

    PageView.prototype.render = function() {
      PageView.__super__.render.apply(this, arguments);
      if (!this.renderedSubviews) {
        this.renderSubviews();
        return this.renderedSubviews = true;
      }
    };

    return PageView;

  })(View);
  
}});

window.require.define({"views/base/view": function(exports, require, module) {
  var Chaplin, View,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  require('lib/view_helper');

  module.exports = View = (function(_super) {

    __extends(View, _super);

    function View() {
      return View.__super__.constructor.apply(this, arguments);
    }

    View.prototype.getTemplateFunction = function() {
      return this.template;
    };

    return View;

  })(Chaplin.View);
  
}});

window.require.define({"views/errors/not_found_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, Model, NotFoundView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  Model = require('models/base/model');

  View = require('views/base/view');

  template = require('views/templates/errors/not_found');

  module.exports = NotFoundView = (function(_super) {

    __extends(NotFoundView, _super);

    function NotFoundView() {
      return NotFoundView.__super__.constructor.apply(this, arguments);
    }

    NotFoundView.prototype.template = template;

    template = null;

    NotFoundView.prototype.container = '#page-container';

    NotFoundView.prototype.containerMethod = 'html';

    NotFoundView.prototype.autoRender = false;

    NotFoundView.prototype.tagName = 'div';

    NotFoundView.prototype.id = 'not-found-view';

    NotFoundView.prototype.autoRender = true;

    NotFoundView.prototype.initialize = function() {
      var i,
        _this = this;
      this.model = new Model({
        sec: 5
      });
      NotFoundView.__super__.initialize.apply(this, arguments);
      this.modelBind('change', this.render);
      return i = setInterval(function() {
        if (_this.model.get('sec') > 0) {
          return _this.model.set('sec', _this.model.get('sec') - 1);
        } else {
          clearInterval(i);
          return Chaplin.mediator.publish('!startupController', 'release', 'list');
        }
      }, 1000);
    };

    return NotFoundView;

  })(View);
  
}});

window.require.define({"views/layout": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, Layout,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  module.exports = Layout = (function(_super) {

    __extends(Layout, _super);

    function Layout() {
      return Layout.__super__.constructor.apply(this, arguments);
    }

    Layout.prototype.initialize = function() {
      return Layout.__super__.initialize.apply(this, arguments);
    };

    return Layout;

  })(Chaplin.Layout);
  
}});

window.require.define({"views/login_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, LoginView, View, template, utils,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  utils = require('lib/utils');

  View = require('views/base/view');

  template = require('views/templates/login');

  module.exports = LoginView = (function(_super) {

    __extends(LoginView, _super);

    function LoginView() {
      return LoginView.__super__.constructor.apply(this, arguments);
    }

    LoginView.prototype.template = template;

    LoginView.prototype.id = 'login';

    LoginView.prototype.className = 'modal hide fade';

    LoginView.prototype.container = 'body';

    LoginView.prototype.autoRender = true;

    LoginView.prototype.initialize = function(options) {
      LoginView.__super__.initialize.apply(this, arguments);
      console.debug('LoginView#initialize', this.el, this.$el, options);
      this.initButtons(options.serviceProviders);
      return this.$el.modal({
        "keyboard": false,
        "backdrop": "static",
        "show": true
      });
    };

    LoginView.prototype.initButtons = function(serviceProviders) {
      var buttonSelector, failed, loaded, loginHandler, serviceProvider, serviceProviderName, _results;
      console.debug('LoginView#initButtons', serviceProviders);
      _results = [];
      for (serviceProviderName in serviceProviders) {
        serviceProvider = serviceProviders[serviceProviderName];
        buttonSelector = "." + serviceProviderName;
        this.$(buttonSelector).addClass('service-loading');
        loginHandler = _(this.loginWith).bind(this, serviceProviderName, serviceProvider);
        this.delegate('click', buttonSelector, loginHandler);
        loaded = _(this.serviceProviderLoaded).bind(this, serviceProviderName, serviceProvider);
        serviceProvider.done(loaded);
        failed = _(this.serviceProviderFailed).bind(this, serviceProviderName, serviceProvider);
        _results.push(serviceProvider.fail(failed));
      }
      return _results;
    };

    LoginView.prototype.loginWith = function(serviceProviderName, serviceProvider, e) {
      var loginDetails;
      console.debug('LoginView#loginWith', serviceProviderName, serviceProvider, e);
      e.preventDefault();
      loginDetails = this.$("form").serializeObject();
      this.accessToken = btoa(loginDetails.username.concat(":", loginDetails.password));
      localStorage.setItem('accessToken', this.accessToken);
      console.debug(this.accessToken, atob(this.accessToken));
      if (!serviceProvider.isLoaded()) {
        return;
      }
      Chaplin.mediator.publish('login:pickService', serviceProviderName);
      return Chaplin.mediator.publish('!login', serviceProviderName);
    };

    LoginView.prototype.serviceProviderLoaded = function(serviceProviderName) {
      console.debug('LoginView#serviceProviderLoaded', serviceProviderName);
      return this.$("." + serviceProviderName).removeClass('service-loading');
    };

    LoginView.prototype.serviceProviderFailed = function(serviceProviderName) {
      console.debug('LoginView#serviceProviderFailed', serviceProviderName);
      return this.$("." + serviceProviderName).removeClass('service-loading').addClass('service-unavailable').attr('disabled', true).attr('title', "Error connecting. Please check whether you are        blocking " + (utils.upcase(serviceProviderName)) + ".");
    };

    return LoginView;

  })(View);
  
}});

window.require.define({"views/navigation_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, NavigationView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  View = require('views/base/view');

  template = require('views/templates/navigation');

  module.exports = NavigationView = (function(_super) {

    __extends(NavigationView, _super);

    function NavigationView() {
      return NavigationView.__super__.constructor.apply(this, arguments);
    }

    NavigationView.prototype.template = template;

    NavigationView.prototype.tagName = 'nav';

    NavigationView.prototype.containerMethod = 'html';

    NavigationView.prototype.autoRender = true;

    NavigationView.prototype.className = 'navigation';

    NavigationView.prototype.container = '#header-container';

    NavigationView.prototype.initialize = function() {
      var _this = this;
      NavigationView.__super__.initialize.apply(this, arguments);
      this.modelBind('change', this.render);
      this.subscribeEvent('login', this.setUsername);
      return this.subscribeEvent('navigation:change', function(attributes) {
        console.debug('NavigationView#initialize#change', attributes);
        _this.model.clear({
          silent: true
        });
        return _this.model.set(attributes);
      });
    };

    NavigationView.prototype.setUsername = function(user) {
      return this.model.set('username', user.get('name'));
    };

    return NavigationView;

  })(View);
  
}});

window.require.define({"views/release/compact_release_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var CompactReleaseView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  template = require('views/templates/release/compact_release');

  module.exports = CompactReleaseView = (function(_super) {

    __extends(CompactReleaseView, _super);

    function CompactReleaseView() {
      return CompactReleaseView.__super__.constructor.apply(this, arguments);
    }

    CompactReleaseView.prototype.template = template;

    template = null;

    CompactReleaseView.prototype.className = 'release';

    CompactReleaseView.prototype.tagName = 'tr';

    return CompactReleaseView;

  })(View);
  
}});

window.require.define({"views/release/complete_release_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, CompleteReleaseView, NextRelease, Release, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  View = require('views/base/view');

  Release = require('models/release');

  NextRelease = require('models/next_release');

  template = require('views/templates/release/complete_release');

  module.exports = CompleteReleaseView = (function(_super) {

    __extends(CompleteReleaseView, _super);

    function CompleteReleaseView() {
      return CompleteReleaseView.__super__.constructor.apply(this, arguments);
    }

    CompleteReleaseView.prototype.template = template;

    template = null;

    CompleteReleaseView.prototype.container = '#page-container';

    CompleteReleaseView.prototype.containerMethod = 'append';

    CompleteReleaseView.prototype.autoRender = true;

    CompleteReleaseView.prototype.tagName = 'div';

    CompleteReleaseView.prototype.className = "modal hide fade";

    CompleteReleaseView.prototype.id = 'complete-release-popup';

    CompleteReleaseView.prototype.initialize = function() {
      console.debug("CompleteReleaseView#initialize", this.options, this, this.el);
      CompleteReleaseView.__super__.initialize.apply(this, arguments);
      this.model = new Release(this.options);
      if (this.options.show) {
        this.$el.modal('show');
      }
      return this.delegate('click', '#complete-release-button', this.completeRelease);
    };

    CompleteReleaseView.prototype.errors = function(err) {
      switch (err.code) {
        case "InvalidName":
          return "A release name must only use letters[a-z],        numbers(0-9), underscores(_) and dashes(-)";
        case "DuplicateReleaseName":
          return "That release name has already been used.";
        case "SignedOffSubmissionRequired":
          return "The release needs at least one SIGNED OFF        submission before it can be COMPLETED.";
      }
    };

    CompleteReleaseView.prototype.completeRelease = function() {
      var nextRelease,
        _this = this;
      console.debug("CompleteReleaseView#completeRelease");
      nextRelease = new NextRelease({
        name: this.$('#nextRelease').val()
      });
      return nextRelease.save({}, {
        success: function(data) {
          _this.$el.modal('hide');
          return Chaplin.mediator.publish("completeRelease", data);
        },
        error: function(model, error) {
          var alert, err;
          err = $.parseJSON(error.responseText);
          alert = _this.$('.alert.alert-error');
          if (alert.length) {
            return alert.text(_this.errors(err));
          } else {
            return _this.$('.alert').before("<div class='alert alert-error'>" + (_this.errors(err)) + "</div>");
          }
        }
      });
    };

    return CompleteReleaseView;

  })(View);
  
}});

window.require.define({"views/release/release_header_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var ReleaseHeaderView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  template = require('views/templates/release/release_header');

  module.exports = ReleaseHeaderView = (function(_super) {

    __extends(ReleaseHeaderView, _super);

    function ReleaseHeaderView() {
      return ReleaseHeaderView.__super__.constructor.apply(this, arguments);
    }

    ReleaseHeaderView.prototype.template = template;

    template = null;

    ReleaseHeaderView.prototype.autoRender = true;

    ReleaseHeaderView.prototype.initialize = function() {
      ReleaseHeaderView.__super__.initialize.apply(this, arguments);
      return this.modelBind('change', this.render);
    };

    return ReleaseHeaderView;

  })(View);
  
}});

window.require.define({"views/release/release_table_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var CompleteReleaseView, DataTableView, ReleaseTableView, utils,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  DataTableView = require('views/base/data_table_view');

  CompleteReleaseView = require('views/release/complete_release_view');

  utils = require('lib/utils');

  module.exports = ReleaseTableView = (function(_super) {
    var template;

    __extends(ReleaseTableView, _super);

    function ReleaseTableView() {
      return ReleaseTableView.__super__.constructor.apply(this, arguments);
    }

    ReleaseTableView.prototype.template = template;

    template = null;

    ReleaseTableView.prototype.autoRender = true;

    ReleaseTableView.prototype.initialize = function() {
      console.debug("ReleasesTableView#initialize", this.collection, this.el);
      ReleaseTableView.__super__.initialize.apply(this, arguments);
      this.modelBind('reset', this.updateDataTable);
      return this.delegate('click', '#complete-release-popup-button', this.completeReleasePopup);
    };

    ReleaseTableView.prototype.completeReleasePopup = function(e) {
      console.debug("ReleaseTableView#completeRelease", e);
      return this.subview('CompleteReleases', new CompleteReleaseView({
        'name': $(e.currentTarget).data('release-name')
      }));
    };

    ReleaseTableView.prototype.createDataTable = function() {
      var aoColumns,
        _this = this;
      console.debug("ReleasesTableView#createDataTable");
      aoColumns = [
        {
          sTitle: "Name",
          mDataProp: "name",
          bUseRendered: false,
          fnRender: function(oObj, sVal) {
            return "<a href='/releases/" + sVal + "'>" + sVal + "</a>";
          }
        }, {
          sTitle: "State",
          mDataProp: "state"
        }, {
          sTitle: "Release Date",
          mDataProp: "releaseDate",
          fnRender: function(oObj, sVal) {
            if (sVal) {
              return utils.date(sVal);
            } else {
              if (utils.is_admin) {
                return "<a\n  id=\"complete-release-popup-button\"\n  data-toggle=\"modal\"\n  data-release-name=\"" + oObj.aData.name + "\"\n  href=\"#complete-release-popup\">\n  Release Now\n</a>";
              } else {
                return "<em>Unreleased</em>";
              }
            }
          }
        }, {
          sTitle: "Studies",
          mDataProp: "submissions.length"
        }
      ];
      return this.$el.dataTable({
        sDom: "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        bPaginate: false,
        oLanguage: {
          "sLengthMenu": "_MENU_ releases per page"
        },
        aaSorting: [[2, "desc"]],
        aoColumns: aoColumns,
        sAjaxSource: "",
        sAjaxDataProp: "",
        fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
          var cell;
          cell = $('td:nth-child(2)', nRow);
          switch (aData.state) {
            case "OPENED":
              return cell.css('color', '#468847');
          }
        },
        fnServerData: function(sSource, aoData, fnCallback) {
          return fnCallback(_this.collection.toJSON());
        }
      });
    };

    return ReleaseTableView;

  })(DataTableView);
  
}});

window.require.define({"views/release/release_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var CompleteReleaseView, PageView, ReleaseHeaderView, ReleaseView, SubmissionSummaryView, SubmissionTableView, template, utils,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  PageView = require('views/base/view');

  ReleaseHeaderView = require('views/release/release_header_view');

  SubmissionSummaryView = require('views/release/submission_summary_view');

  CompleteReleaseView = require('views/release/complete_release_view');

  SubmissionTableView = require('views/release/submission_table_view');

  utils = require('lib/utils');

  template = require('views/templates/release/release');

  module.exports = ReleaseView = (function(_super) {

    __extends(ReleaseView, _super);

    function ReleaseView() {
      return ReleaseView.__super__.constructor.apply(this, arguments);
    }

    ReleaseView.prototype.template = template;

    template = null;

    ReleaseView.prototype.container = '#page-container';

    ReleaseView.prototype.containerMethod = 'html';

    ReleaseView.prototype.tagName = 'div';

    ReleaseView.prototype.id = 'release-view';

    ReleaseView.prototype.initialize = function() {
      console.debug('ReleaseView#initialize', this.model);
      ReleaseView.__super__.initialize.apply(this, arguments);
      this.subscribeEvent("completeRelease", function(data) {
        this.model.set("next", data.get("name"));
        return this.model.fetch();
      });
      this.subscribeEvent("validateSubmission", function() {
        return this.model.fetch();
      });
      this.subscribeEvent("signOffSubmission", function() {
        return this.model.fetch();
      });
      this.delegate('click', '#complete-release-popup-button', this.completeReleasePopup);
      return utils.polling(this.model, 60000);
    };

    ReleaseView.prototype.completeReleasePopup = function(e) {
      console.debug("ReleaseView#completeRelease", e);
      return this.subview('CompleteReleases', new CompleteReleaseView({
        'name': this.model.get('name'),
        'show': true
      }));
    };

    ReleaseView.prototype.render = function() {
      console.debug("ReleaseView#render", this.model);
      ReleaseView.__super__.render.apply(this, arguments);
      this.subview('ReleaseHeader', new ReleaseHeaderView({
        model: this.model,
        el: this.$("#release-header-container")
      }));
      this.subview('SubmissionSummary', new SubmissionSummaryView({
        model: this.model,
        el: this.$("#submission-summary-container")
      }));
      return this.subview('SubmissionsTable', new SubmissionTableView({
        model: this.model,
        el: this.$("#submissions-table")
      }));
    };

    return ReleaseView;

  })(PageView);
  
}});

window.require.define({"views/release/releases_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var PageView, ReleaseTableView, ReleasesView, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  PageView = require('views/base/page_view');

  ReleaseTableView = require('views/release/release_table_view');

  template = require('views/templates/release/releases');

  module.exports = ReleasesView = (function(_super) {

    __extends(ReleasesView, _super);

    function ReleasesView() {
      return ReleasesView.__super__.constructor.apply(this, arguments);
    }

    ReleasesView.prototype.template = template;

    template = null;

    ReleasesView.prototype.container = '#page-container';

    ReleasesView.prototype.containerMethod = 'html';

    ReleasesView.prototype.tagName = 'div';

    ReleasesView.prototype.id = 'releases-view';

    ReleasesView.prototype.initialize = function() {
      console.debug("ReleasesView#initialize", this.collection);
      ReleasesView.__super__.initialize.apply(this, arguments);
      return this.subscribeEvent('completeRelease', function() {
        return this.collection.fetch();
      });
    };

    ReleasesView.prototype.render = function() {
      ReleasesView.__super__.render.apply(this, arguments);
      return this.subview('ReleasesTable', new ReleaseTableView({
        collection: this.collection,
        el: this.$("#releases-table")
      }));
    };

    return ReleasesView;

  })(PageView);
  
}});

window.require.define({"views/release/report_table_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var DataTableView, ReportTableView, utils,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  DataTableView = require('views/base/data_table_view');

  utils = require('lib/utils');

  module.exports = ReportTableView = (function(_super) {
    var template;

    __extends(ReportTableView, _super);

    function ReportTableView() {
      return ReportTableView.__super__.constructor.apply(this, arguments);
    }

    ReportTableView.prototype.template = template;

    template = null;

    ReportTableView.prototype.autoRender = false;

    ReportTableView.prototype.initialize = function() {
      console.debug("ReportTableView#initialize", this.model, this.el);
      this.report = this.model.get("report");
      this.collection = this.report.get("schemaReports");
      ReportTableView.__super__.initialize.apply(this, arguments);
      this.modelBind('change', this.update);
      this.anOpen = [];
      this.delegate('click', '.control', this.rowDetails);
      return this.delegate('click', '.summary', this.rowSummary);
    };

    ReportTableView.prototype.update = function() {
      console.debug("ReportTableView#update", this.model);
      this.report = this.model.get("report");
      this.collection = this.report.get("schemaReports");
      return this.updateDataTable();
    };

    ReportTableView.prototype.rowDetails = function(e) {
      var control, dT, data, nTr, style;
      control = e.target;
      nTr = control.parentNode.parentNode;
      dT = this.$el.dataTable();
      data = dT.fnGetData(nTr);
      if (data.errors) {
        style = 'alert-danger';
      } else {
        style = 'alert-info';
      }
      if (__indexOf.call(this.anOpen, nTr) >= 0) {
        this.anOpen = _.without(this.anOpen, nTr);
        $(control).text('view');
        return dT.fnClose(nTr);
      } else {
        this.anOpen.push(nTr);
        $(control).text('hide');
        return dT.fnOpen(nTr, this.formatDetails(data), "details " + style);
      }
    };

    ReportTableView.prototype.rowSummary = function(e) {
      var control, dT, data, nTr, style;
      console.debug("ReportTableView#rowSummary", e, this.anOpen);
      control = e.target;
      nTr = control.parentNode.parentNode;
      dT = this.$(nTr.parentNode.parentNode).dataTable();
      data = dT.fnGetData(nTr);
      style = 'signed';
      if (__indexOf.call(this.anOpen, nTr) >= 0) {
        this.anOpen = _.without(this.anOpen, nTr);
        $(control).text('show');
        return dT.fnClose(nTr);
      } else {
        this.anOpen.push(nTr);
        $(control).text('hide');
        return dT.fnOpen(nTr, this.summaryDetails(data), "summary_details well");
      }
    };

    ReportTableView.prototype.summaryDetails = function(data) {
      var key, sOut, type, value, _ref;
      console.debug("ReportTableView#summaryDetails", data);
      type = (function() {
        switch (data.type) {
          case "AVERAGE":
            return "Summary Statistics";
          case "FREQUENCY":
            return "Value Frequencies";
        }
      })();
      sOut = "<dt>" + type + "</dt>";
      _ref = data.summary;
      for (key in _ref) {
        value = _ref[key];
        value = key === 'stddev' || key === 'avg' ? Number(value).toFixed(2) : value;
        sOut += "<dd><strong>" + key + ": </strong>" + value + "<br></dd>";
      }
      return sOut;
    };

    ReportTableView.prototype.formatErrorKey = function(key) {
      switch (key) {
        case "expectedValue":
          return "Expected Value";
        case "expectedType":
          return "Expected Type";
        case "firstOffset":
          return "First Occurence";
        case "minRange":
          return "Min Accepted Value";
        case "maxRange":
          return "Max Accetped Value";
        case "actualNumColumns":
          return "Expected Number of Columns";
        case "relationSchema":
          return "Relation Schema";
        case "relationColumnNames":
          return "Relation Columns";
      }
    };

    ReportTableView.prototype.formatParams = function(error) {
      var i, key, lines, map, out, value, _i, _j, _len, _ref, _ref1;
      console.debug("ReportTableView#formatParams", error);
      out = "";
      _ref = error.parameters;
      for (key in _ref) {
        value = _ref[key];
        out += "<strong>" + (this.formatErrorKey(key)) + "</strong> : " + value + "<br>";
      }
      out += "<table class='table'>      <th style='border:none'>Line</th>      <th style='border:none'>Value</th>";
      map = {};
      for (i = _i = 0, _ref1 = error.lines.length - 1; 0 <= _ref1 ? _i <= _ref1 : _i >= _ref1; i = 0 <= _ref1 ? ++_i : --_i) {
        map[error.lines[i]] = error.values[i];
      }
      lines = error.lines.sort(function(a, b) {
        return a - b;
      });
      for (_j = 0, _len = lines.length; _j < _len; _j++) {
        i = lines[_j];
        out += "<tr><td style='border:none'>" + i + "</td>      <td style='border:none'>" + map[i] + "</td></tr>";
      }
      out += "</table>";
      return out;
    };

    ReportTableView.prototype.formatDetails = function(data) {
      var error, errorObj, sErr, sOut, _i, _j, _len, _len1, _ref, _ref1;
      console.debug("ReportTableView#formatDetails", data);
      sOut = '';
      sErr = '';
      if (data.errors) {
        sOut += "<table class='table table-striped'>\n<thead>\n  <tr>\n  <th>Error Type</th>\n  <th>Column Name</th>\n  <th>Count</th>\n  <th>Parameters</th>\n  </tr>\n</thead>\n<tbody>";
        _ref = data.errors;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          errorObj = _ref[_i];
          console.log(errorObj);
          _ref1 = errorObj.columns;
          for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
            error = _ref1[_j];
            console.log(errorObj, error);
            sOut += "<tr>";
            sOut += "<td>" + errorObj.errorType + "</td>            <td>" + (error.columnNames.join(', ')) + "</td>";
            sOut += "<td>" + error.count + "</td>";
            if (errorObj.errorType === "MISSING_VALUE_ERROR") {
              sOut += "<td>" + (error.lines.sort(function(a, b) {
                return a - b;
              }).join(', ')) + "</td>";
            } else {
              sOut += "<td>" + (this.formatParams(error)) + "</td>";
            }
            sOut += "</tr>";
          }
        }
        sOut += "</tbody></table>";
        return $(sOut).dataTable({
          bPaginate: false
        });
      } else if (data.fieldReports) {
        sOut += "<table class='sub_report table table-striped'></table>";
        return $(sOut).dataTable({
          bPaginate: false,
          aaData: data.fieldReports,
          aoColumns: [
            {
              sTitle: "Field Name",
              mDataProp: "name"
            }, {
              sTitle: "Completeness<br>(%)",
              mDataProp: "completeness"
            }, {
              sTitle: "Populated<br>(# rows)",
              mDataProp: "populated"
            }, {
              sTitle: "Missing<br>(# rows)",
              mDataProp: "missing"
            }, {
              sTitle: "Nulls<br>(# rows)",
              mDataProp: "nulls"
            }, {
              sTitle: "Summary",
              mDataProp: "summary",
              bSortable: false,
              bUseRendered: false,
              fnRender: function(oObj, sVal) {
                if (!$.isEmptyObject(sVal)) {
                  return "<span class='summary link'>show</span></td>";
                } else {
                  return "";
                }
              }
            }
          ],
          aaSorting: [[1, "asc"]]
        });
      }
    };

    ReportTableView.prototype.updateDataTable = function() {
      var dt, errors, fieldReport, item, srs;
      srs = this.model.get('report').get('schemaReports');
      errors = (function() {
        var _i, _len, _ref, _results;
        _ref = srs.pluck("errors");
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          item = _ref[_i];
          if (item != null) {
            _results.push(item);
          }
        }
        return _results;
      })();
      fieldReport = (function() {
        var _i, _len, _ref, _results;
        _ref = srs.pluck("fieldReports");
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          item = _ref[_i];
          if (item != null) {
            _results.push(item);
          }
        }
        return _results;
      })();
      if (errors || fieldReports) {
        dt = this.$el.dataTable();
        dt.fnSetColumnVis(3, true);
        dt.fnSetColumnVis(4, true);
      }
      return ReportTableView.__super__.updateDataTable.apply(this, arguments);
    };

    ReportTableView.prototype.createDataTable = function() {
      var aoColumns,
        _this = this;
      console.debug("ReportTableView#createDataTable", this.$el);
      aoColumns = [
        {
          sTitle: "File",
          mDataProp: "name"
        }, {
          sTitle: "Last Updated",
          mDataProp: "lastUpdate",
          sType: "date",
          fnRender: function(oObj, sVal) {
            return utils.date(sVal);
          }
        }, {
          sTitle: "Size",
          mDataProp: "size",
          bUseRendered: false,
          fnRender: function(oObj, Sval) {
            return utils.fileSize(Sval);
          }
        }, {
          sTitle: "Status",
          mDataProp: null,
          bVisible: false,
          fnRender: function(oObj, Sval) {
            if (oObj.aData.errors) {
              return "INVALID";
            } else if (oObj.aData.fieldReports) {
              return "VALID";
            } else {
              return "NOT VALIDATED";
            }
          }
        }, {
          sTitle: "Report",
          mDataProp: null,
          bSortable: false,
          bVisible: false,
          fnRender: function(oObj, Sval) {
            if (oObj.aData.errors || oObj.aData.fieldReports) {
              return "<span class='link control'>view</span>";
            } else {
              return "";
            }
          }
        }
      ];
      return this.$el.dataTable({
        sDom: "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        bPaginate: false,
        aaSorting: [[1, "asc"]],
        aoColumns: aoColumns,
        sAjaxSource: "",
        sAjaxDataProp: "",
        fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
          var cell;
          cell = $('td:nth-child(4)', nRow);
          switch (cell.html()) {
            case "VALID":
              return cell.css('color', '#468847');
            case "INVALID":
            case "ERROR":
              return cell.css('color', '#B94A48');
          }
        },
        fnServerData: function(sSource, aoData, fnCallback) {
          return fnCallback(_this.collection.toJSON());
        }
      });
    };

    return ReportTableView;

  })(DataTableView);
  
}});

window.require.define({"views/release/report_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Report, ReportView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  Report = require('models/report');

  template = require('views/templates/release/report');

  module.exports = ReportView = (function(_super) {

    __extends(ReportView, _super);

    function ReportView() {
      return ReportView.__super__.constructor.apply(this, arguments);
    }

    ReportView.prototype.template = template;

    template = null;

    ReportView.prototype.autoRender = false;

    ReportView.prototype.initialize = function() {
      console.debug("ReportView#initialize", this, this.options);
      ReportView.__super__.initialize.apply(this, arguments);
      this.model = new Report({
        release: this.options.release,
        submission: this.options.submission
      });
      this.modelBind('change', this.render);
      return this.model.fetch();
    };

    return ReportView;

  })(View);
  
}});

window.require.define({"views/release/signoff_submission_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, NextRelease, SignOffSubmissionView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  View = require('views/base/view');

  NextRelease = require('models/next_release');

  template = require('views/templates/release/signoff_submission');

  module.exports = SignOffSubmissionView = (function(_super) {

    __extends(SignOffSubmissionView, _super);

    function SignOffSubmissionView() {
      return SignOffSubmissionView.__super__.constructor.apply(this, arguments);
    }

    SignOffSubmissionView.prototype.template = template;

    template = null;

    SignOffSubmissionView.prototype.container = '#page-container';

    SignOffSubmissionView.prototype.containerMethod = 'append';

    SignOffSubmissionView.prototype.autoRender = true;

    SignOffSubmissionView.prototype.tagName = 'div';

    SignOffSubmissionView.prototype.className = "modal fade";

    SignOffSubmissionView.prototype.id = 'signoff-submission-popup';

    SignOffSubmissionView.prototype.initialize = function() {
      console.debug("SignOffSubmissionView#initialize", this.options.submission);
      this.model = this.options.submission;
      SignOffSubmissionView.__super__.initialize.apply(this, arguments);
      return this.delegate('click', '#signoff-submission-button', this.signOffSubmission);
    };

    SignOffSubmissionView.prototype.signOffSubmission = function(e) {
      var nextRelease,
        _this = this;
      console.debug("SignOffSubmissionView#completeRelease");
      nextRelease = new NextRelease();
      return nextRelease.signOff([this.model.get("projectKey")], {
        success: function() {
          _this.$el.modal('hide');
          return Chaplin.mediator.publish("signOffSubmission");
        }
      });
    };

    return SignOffSubmissionView;

  })(View);
  
}});

window.require.define({"views/release/submission_files_table_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var DataTableView, SubmissionFilesTableView, utils,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  DataTableView = require('views/base/data_table_view');

  utils = require('lib/utils');

  module.exports = SubmissionFilesTableView = (function(_super) {
    var template;

    __extends(SubmissionFilesTableView, _super);

    function SubmissionFilesTableView() {
      return SubmissionFilesTableView.__super__.constructor.apply(this, arguments);
    }

    SubmissionFilesTableView.prototype.template = template;

    template = null;

    SubmissionFilesTableView.prototype.autoRender = true;

    SubmissionFilesTableView.prototype.initialize = function() {
      console.debug("SubmissionFilesTableView#initialize", this.model, this.el);
      this.collection = this.model.get("submissionFiles");
      console.log("!~!~~!~!~!~~!", this.collection);
      return SubmissionFilesTableView.__super__.initialize.apply(this, arguments);
    };

    SubmissionFilesTableView.prototype.createDataTable = function() {
      var aoColumns,
        _this = this;
      console.debug("SubmissionFilesTableView#createDataTable", this.$el);
      aoColumns = [
        {
          sTitle: "File",
          mDataProp: "name",
          bUseRendered: true,
          fnRender: function(oObj, sVal) {
            return "<i class='icon-file'></i> " + sVal;
          }
        }, {
          sTitle: "Last Updated",
          mDataProp: "lastUpdate",
          sType: "date",
          sWidth: "150px",
          fnRender: function(oObj, sVal) {
            return utils.date(sVal);
          }
        }, {
          sTitle: "Size",
          mDataProp: "size",
          bUseRendered: false,
          sWidth: "100px",
          fnRender: function(oObj, Sval) {
            return utils.fileSize(Sval);
          }
        }
      ];
      return this.$el.dataTable({
        sDom: "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        bPaginate: false,
        oLanguage: {
          "sLengthMenu": "_MENU_ files per page"
        },
        aaSorting: [[1, "desc"]],
        aoColumns: aoColumns,
        sAjaxSource: "",
        sAjaxDataProp: "",
        fnServerData: function(sSource, aoData, fnCallback) {
          return fnCallback(_this.collection.toJSON());
        }
      });
    };

    return SubmissionFilesTableView;

  })(DataTableView);
  
}});

window.require.define({"views/release/submission_header_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var SubmissionHeaderView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  template = require('views/templates/release/submission_header');

  module.exports = SubmissionHeaderView = (function(_super) {

    __extends(SubmissionHeaderView, _super);

    function SubmissionHeaderView() {
      return SubmissionHeaderView.__super__.constructor.apply(this, arguments);
    }

    SubmissionHeaderView.prototype.template = template;

    template = null;

    SubmissionHeaderView.prototype.autoRender = true;

    SubmissionHeaderView.prototype.initialize = function() {
      SubmissionHeaderView.__super__.initialize.apply(this, arguments);
      return this.modelBind('change', this.render);
    };

    return SubmissionHeaderView;

  })(View);
  
}});

window.require.define({"views/release/submission_summary_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var SubmissionSummaryView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  template = require('views/templates/release/submission_summary');

  module.exports = SubmissionSummaryView = (function(_super) {

    __extends(SubmissionSummaryView, _super);

    function SubmissionSummaryView() {
      return SubmissionSummaryView.__super__.constructor.apply(this, arguments);
    }

    SubmissionSummaryView.prototype.template = template;

    template = null;

    SubmissionSummaryView.prototype.autoRender = true;

    SubmissionSummaryView.prototype.initialize = function() {
      SubmissionSummaryView.__super__.initialize.apply(this, arguments);
      return this.modelBind('change', this.render);
    };

    return SubmissionSummaryView;

  })(View);
  
}});

window.require.define({"views/release/submission_table_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var DataTableView, SubmissionTableView, signOffSubmissionView, utils, validateSubmissionView,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  DataTableView = require('views/base/data_table_view');

  signOffSubmissionView = require('views/release/signoff_submission_view');

  validateSubmissionView = require('views/release/validate_submission_view');

  utils = require('lib/utils');

  module.exports = SubmissionTableView = (function(_super) {
    var template;

    __extends(SubmissionTableView, _super);

    function SubmissionTableView() {
      return SubmissionTableView.__super__.constructor.apply(this, arguments);
    }

    SubmissionTableView.prototype.template = template;

    template = null;

    SubmissionTableView.prototype.autoRender = true;

    SubmissionTableView.prototype.initialize = function() {
      console.debug("SubmissionsTableView#initialize", this.model, this.el);
      this.collection = this.model.get("submissions");
      SubmissionTableView.__super__.initialize.apply(this, arguments);
      this.modelBind('change', this.update);
      this.delegate('click', '#signoff-submission-popup-button', this.signOffSubmissionPopup);
      return this.delegate('click', '#validate-submission-popup-button', this.validateSubmissionPopup);
    };

    SubmissionTableView.prototype.update = function() {
      this.collection = this.model.get("submissions");
      return this.updateDataTable();
    };

    SubmissionTableView.prototype.signOffSubmissionPopup = function(e) {
      console.debug("ReleaseView#signOffSubmissionPopup", e);
      return this.subview("signOffSubmissionView", new signOffSubmissionView({
        "submission": this.collection.get($(e.currentTarget).data("submission"))
      }));
    };

    SubmissionTableView.prototype.validateSubmissionPopup = function(e) {
      console.debug("ReleaseView#validateSubmissionPopup", e);
      return this.subview("validateSubmissionView", new validateSubmissionView({
        "submission": this.collection.get($(e.currentTarget).data("submission"))
      }));
    };

    SubmissionTableView.prototype.createDataTable = function() {
      var aoColumns,
        _this = this;
      console.debug("SubmissionsTableView#createDataTable", this.$el, this.collection);
      aoColumns = [
        {
          sTitle: "Project Key",
          mDataProp: "projectName",
          fnRender: function(oObj, sVal) {
            var r, s;
            r = _this.collection.release;
            s = oObj.aData.projectKey.replace(/<.*?>/g, '');
            return "<a href='/releases/" + r + "/submissions/" + s + "'>" + sVal + "</a>";
          }
        }, {
          sTitle: "Files",
          mDataProp: "submissionFiles",
          bUseRendered: false,
          fnRender: function(oObj, sVal) {
            var f, size, _i, _len;
            size = 0;
            for (_i = 0, _len = sVal.length; _i < _len; _i++) {
              f = sVal[_i];
              size += f.size;
            }
            return "" + sVal.length + " (" + (utils.fileSize(size)) + ")";
          }
        }, {
          sTitle: "State",
          mDataProp: "state",
          fnRender: function(oObj, sVal) {
            return sVal.replace('_', ' ');
          }
        }, {
          sTitle: "Last Updated",
          mDataProp: "lastUpdated",
          fnRender: function(oObj, sVal) {
            return utils.date(sVal);
          }
        }, {
          sTitle: "Report",
          mDataProp: null,
          bSortable: false,
          fnRender: function(oObj) {
            var r, s;
            switch (oObj.aData.state) {
              case "VALID":
              case "SIGNED OFF":
              case "INVALID":
                r = _this.collection.release;
                s = oObj.aData.projectKey.replace(/<.*?>/g, '');
                return "<a href='/releases/" + r + "/submissions/" + s + "'>View</a>";
              default:
                return "";
            }
          }
        }, {
          sTitle: "",
          mDataProp: null,
          bSortable: false,
          sWidth: "75px",
          bVisible: !utils.is_released(this.model.get("state")),
          fnRender: function(oObj) {
            var ds;
            switch (oObj.aData.state) {
              case "VALID":
                ds = oObj.aData.projectKey.replace(/<.*?>/g, '');
                return "<a id=\"signoff-submission-popup-button\"\n   data-submission=\"" + ds + "\"\n   data-toggle=\"modal\"\n   href='#signoff-submission-popup'>\n   Sign Off\n</a>";
              case "NOT VALIDATED":
              case "INVALID":
              case "ERROR":
                if (oObj.aData.submissionFiles.length) {
                  ds = oObj.aData.projectKey.replace(/<.*?>/g, '');
                  return "<a id=\"validate-submission-popup-button\"\n   data-submission=\"" + ds + "\"\n   data-toggle=\"modal\"\n   href='#validate-submission-popup'>\n   Validate\n</a>";
                } else {
                  return "<em>Upload Files</em>";
                }
                break;
              default:
                return "";
            }
          }
        }
      ];
      return this.$el.dataTable({
        sDom: "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        bPaginate: false,
        oLanguage: {
          "sLengthMenu": "_MENU_ submissions per page"
        },
        aaSorting: [[3, "desc"]],
        aoColumns: aoColumns,
        sAjaxSource: "",
        sAjaxDataProp: "",
        fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
          var cell;
          cell = $('td:nth-child(3)', nRow);
          switch (aData.state) {
            case "SIGNED OFF":
              return cell.css('color', '#3A87AD');
            case "VALID":
              return cell.css('color', '#468847');
            case "QUEUED":
              return cell.css('color', '#C09853');
            case "INVALID":
            case "ERROR":
              return cell.css('color', '#B94A48');
          }
        },
        fnServerData: function(sSource, aoData, fnCallback) {
          return fnCallback(_this.collection.toJSON());
        }
      });
    };

    return SubmissionTableView;

  })(DataTableView);
  
}});

window.require.define({"views/release/submission_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var ReportTableView, SignOffSubmissionView, SubmissionFilesTableView, SubmissionHeaderView, SubmissionView, ValidateSubmissionView, View, template, utils,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  View = require('views/base/view');

  SubmissionHeaderView = require('views/release/submission_header_view');

  ReportTableView = require('views/release/report_table_view');

  SubmissionFilesTableView = require('views/release/submission_files_table_view');

  SignOffSubmissionView = require('views/release/signoff_submission_view');

  ValidateSubmissionView = require('views/release/validate_submission_view');

  utils = require('lib/utils');

  template = require('views/templates/release/submission');

  module.exports = SubmissionView = (function(_super) {

    __extends(SubmissionView, _super);

    function SubmissionView() {
      return SubmissionView.__super__.constructor.apply(this, arguments);
    }

    SubmissionView.prototype.template = template;

    template = null;

    SubmissionView.prototype.container = '#page-container';

    SubmissionView.prototype.containerMethod = 'html';

    SubmissionView.prototype.autoRender = true;

    SubmissionView.prototype.tagName = 'div';

    SubmissionView.prototype.id = 'submission-view';

    SubmissionView.prototype.initialize = function() {
      console.debug('SubmissionView#initialize', this.model);
      SubmissionView.__super__.initialize.apply(this, arguments);
      this.subscribeEvent("signOffSubmission", function() {
        return this.model.fetch();
      });
      this.subscribeEvent("validateSubmission", function() {
        return this.model.fetch();
      });
      this.delegate('click', '#signoff-submission-popup-button', this.signOffSubmissionPopup);
      return this.delegate('click', '#validate-submission-popup-button', this.validateSubmissionPopup);
    };

    SubmissionView.prototype.signOffSubmissionPopup = function(e) {
      console.debug("SubmissionView#signOffSubmissionPopup", e);
      return this.subview("signOffSubmissionView", new SignOffSubmissionView({
        "submission": this.model
      }));
    };

    SubmissionView.prototype.validateSubmissionPopup = function(e) {
      console.debug("SubmissionView#validateSubmissionPopup", e);
      return this.subview("validateSubmissionView", new ValidateSubmissionView({
        "submission": this.model
      }));
    };

    SubmissionView.prototype.render = function() {
      console.debug("SubmissionView#render", this.model);
      SubmissionView.__super__.render.apply(this, arguments);
      this.subview('SubmissionHeader', new SubmissionHeaderView({
        model: this.model,
        el: this.$("#submission-header-container")
      }));
      return this.subview('Report', new ReportTableView({
        model: this.model,
        el: this.$("#report-container")
      }));
    };

    return SubmissionView;

  })(View);
  
}});

window.require.define({"views/release/validate_submission_view": function(exports, require, module) {
  "* Copyright 2012(c) The Ontario Institute for Cancer Research.\n* All rights reserved.\n*\n* This program and the accompanying materials are made available under the\n* terms of the GNU Public License v3.0.\n* You should have received a copy of the GNU General Public License along with\n* this program. If not, see <http://www.gnu.org/licenses/>.\n*\n* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE\n* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR\n* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN\n* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)\n* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\n* POSSIBILITY OF SUCH DAMAGE.";

  var Chaplin, NextRelease, ValidateSubmissionView, View, template,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  Chaplin = require('chaplin');

  View = require('views/base/view');

  NextRelease = require('models/next_release');

  template = require('views/templates/release/validate_submission');

  module.exports = ValidateSubmissionView = (function(_super) {

    __extends(ValidateSubmissionView, _super);

    function ValidateSubmissionView() {
      return ValidateSubmissionView.__super__.constructor.apply(this, arguments);
    }

    ValidateSubmissionView.prototype.template = template;

    template = null;

    ValidateSubmissionView.prototype.container = '#page-container';

    ValidateSubmissionView.prototype.containerMethod = 'append';

    ValidateSubmissionView.prototype.autoRender = true;

    ValidateSubmissionView.prototype.tagName = 'div';

    ValidateSubmissionView.prototype.className = "modal fade";

    ValidateSubmissionView.prototype.id = 'validate-submission-popup';

    ValidateSubmissionView.prototype.initialize = function() {
      var release,
        _this = this;
      console.debug("ValidateSubmissionView#initialize", this.options);
      this.model = this.options.submission;
      release = new NextRelease();
      release.fetch({
        success: function(data) {
          return _this.model.set('queue', data.get('queue').length);
        }
      });
      ValidateSubmissionView.__super__.initialize.apply(this, arguments);
      this.modelBind('change', this.render);
      return this.delegate('click', '#validate-submission-button', this.validateSubmission);
    };

    ValidateSubmissionView.prototype.validateSubmission = function(e) {
      var nextRelease,
        _this = this;
      console.debug("ValidateSubmissionView#completeRelease", this.model);
      nextRelease = new NextRelease();
      return nextRelease.queue([
        {
          key: this.options.submission.get("projectKey"),
          emails: this.$('#emails').val().split(',')
        }
      ], {
        success: function() {
          _this.$el.modal('hide');
          return Chaplin.mediator.publish("validateSubmission");
        }
      });
    };

    return ValidateSubmissionView;

  })(View);
  
}});

window.require.define({"views/templates/errors/not_found": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, foundHelper, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;


    buffer += "<div class=\"row-fluid page-header\">\n  <h1><small>404</small> Request Not Found</h1>\n</div>\n<div class=\"alert alert-info\">Redirecting to the <a href=\"/releases/\">Releases</a> page in ";
    foundHelper = helpers.sec;
    stack1 = foundHelper || depth0.sec;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "sec", { hash: {} }); }
    buffer += escapeExpression(stack1) + " seconds...</div>\n";
    return buffer;});
}});

window.require.define({"views/templates/login": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<div class=\"modal-header\">\n	<h3>DCC Submission Login</h3>\n</div>\n<form class=\"form-horizontal\" style=\"margin:0\">\n	<div class=\"modal-body\">\n		<div class=\"control-group \">\n			<label class=\"control-label\" for=\"username\">Username</label>\n			<div class=\"controls\">\n				<input type=\"text\" name=\"username\" value=\"\" required=\"required\" id=\"username\">\n			</div>\n		</div>\n		<div class=\"control-group \">\n			<label class=\"control-label\" for=\"password\">Password</label>\n			<div class=\"controls\">\n				<input type=\"password\" name=\"password\" value=\"\" required=\"required\" id=\"password\">\n			</div>\n		</div>\n	</div>\n	<div class=\"modal-footer\">\n		<button type=\"submit\" class=\"dcc btn btn-primary\">\n			<i class=\"icon-lock icon-white\"></i>\n			Login\n		</button>\n	</div>\n</form>\n";});
}});

window.require.define({"views/templates/navigation": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, tmp1, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;

  function program1(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n    <ul class=\"nav\">\n      <li class=\"active\"><a href=\"/releases\">Releases</a></li>\n    </ul>\n    <ul class=\"nav pull-right\">\n      <li><p class=\"navbar-text\">";
    foundHelper = helpers.username;
    stack1 = foundHelper || depth0.username;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "username", { hash: {} }); }
    buffer += escapeExpression(stack1) + " (<a href=\"/logout\">logout</a>)</p></li>\n    </ul>\n    ";
    return buffer;}

    buffer += "<div class=\"navbar-inne\">\n	<div class=\"container-fluid\">\n	<a href=\"http://www.icgc.org\"><img src=\"/images/logo-icgc.png\"></a>\n  <a href=\"http://www.oicr.on.ca\"><img class=\"pull-right\" src=\"/images/logo-oicr.jpg\"></a>\n	</div>\n</div>\n<div class=\"navbar\">\n  <div class=\"navbar-inner\">\n    <span class=\"brand\">DCC Data Submission</span>\n    ";
    foundHelper = helpers.username;
    stack1 = foundHelper || depth0.username;
    stack2 = helpers['if'];
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n  </div>\n</div>\n";
    return buffer;});
}});

window.require.define({"views/templates/project": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, foundHelper, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;


    buffer += "<div>project: ";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "name", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</div><i>";
    foundHelper = helpers.projectKey;
    stack1 = foundHelper || depth0.projectKey;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "projectKey", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</i>";
    return buffer;});
}});

window.require.define({"views/templates/projects": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<ol class=\"projects\">projects</ol>";});
}});

window.require.define({"views/templates/release/compact_release": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, tmp1, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression, blockHelperMissing=helpers.blockHelperMissing;

  function program1(depth0,data) {
    
    var buffer = "", stack1, stack2;
    buffer += "\n<td>";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    foundHelper = helpers.release_action;
    stack2 = foundHelper || depth0.release_action;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "release_action", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "</td>\n";
    return buffer;}

    buffer += "<td><a href=\"releases/";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "name", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "name", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</a></td>\n<td>";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "state", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</td>\n<td>";
    foundHelper = helpers.releaseDate;
    stack1 = foundHelper || depth0.releaseDate;
    foundHelper = helpers.release_date;
    stack2 = foundHelper || depth0.release_date;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "release_date", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "</td>\n<td>";
    foundHelper = helpers.submissions;
    stack1 = foundHelper || depth0.submissions;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.length);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "submissions.length", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</td>\n";
    foundHelper = helpers.if_admin;
    stack1 = foundHelper || depth0.if_admin;
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack1 === functionType) { stack1 = stack1.call(depth0, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n  ";
    return buffer;});
}});

window.require.define({"views/templates/release/complete_release": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, foundHelper, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;


    buffer += "<div class=\"modal-header\">\n  <button type=\"button\" class=\"close\" data-dismiss=\"modal\">\n    ×\n  </button>\n  <h3>Complete Release</h3>\n</div>\n<div class=\"modal-body\">\n  <div class=\"alert\">Once you confirmed completing the current release <b>";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "name", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</b>, there is no going back. It will be marked as COMPLETED and only submissions that are SIGNED OFF will be processed and included in this release. Please be certain.\n  </div>\n  <fieldset class=\"form-horizontal\">\n    <div class=\"control-group\">\n      <label class=\"control-label\" for=\"nextRelease\">Next Release Name</label>\n      <div class=\"controls\">\n        <input type=\"text\" id=\"nextRelease\">\n      </div>\n    </div>\n  </fieldset>\n</div>\n<div class=\"modal-footer\">\n  <a href=\"#\" class=\"btn\" data-dismiss=\"modal\">Close</a>\n  <button type=\"submit\" id=\"complete-release-button\" href=\"#\" class=\"btn btn-success\"><i class=\"icon-ok icon-white\"></i> Complete Release</button>\n</div>";
    return buffer;});
}});

window.require.define({"views/templates/release/release": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<div id=\"release-header-container\"></div>\n<div id=\"submission-summary-container\"></div>\n<ul class=\"nav nav-tabs\">\n  <li class=\"active\"><a href=\"#files\" data-toggle=\"tab\">Submissions</a></li>\n</ul>\n<div class=\"tab-content\">\n  <table id=\"submissions-table\" width=\"100%\" class=\"submissions table table-striped\"></table>\n</div>";});
}});

window.require.define({"views/templates/release/release_header": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, tmp1, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression, blockHelperMissing=helpers.blockHelperMissing;

  function program1(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n<div class=\"alert alert-success\"><button class=\"close\" data-dismiss=\"alert\">×</button>This release has been completed succesfully! The current open release is <a href=\"/releases/";
    foundHelper = helpers.next;
    stack1 = foundHelper || depth0.next;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "next", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.next;
    stack1 = foundHelper || depth0.next;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "next", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</a>.</div>\n";
    return buffer;}

  function program3(depth0,data) {
    
    var buffer = "", stack1, stack2;
    buffer += "\n    <small> on ";
    foundHelper = helpers.releaseDate;
    stack1 = foundHelper || depth0.releaseDate;
    foundHelper = helpers.date;
    stack2 = foundHelper || depth0.date;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "date", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "</small>\n    ";
    return buffer;}

  function program5(depth0,data) {
    
    
    return "\n    <button id=\"complete-release-popup-button\" class=\"btn btn-success\">Complete</button>\n    ";}

    foundHelper = helpers.next;
    stack1 = foundHelper || depth0.next;
    stack2 = helpers['if'];
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n<div class=\"row-fluid page-header\">\n    <div class=\"pull-left\">\n    <h1><small>RELEASE</small> ";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "name", { hash: {} }); }
    buffer += escapeExpression(stack1) + "<br>\n    <small class=\"";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    foundHelper = helpers.lowercase;
    stack2 = foundHelper || depth0.lowercase;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "lowercase", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "state", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</small>\n    ";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    foundHelper = helpers.if_completed;
    stack2 = foundHelper || depth0.if_completed;
    tmp1 = self.program(3, program3, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n    </h1>\n  </div>\n  <div class=\"pull-right\">\n    ";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    foundHelper = helpers.if_opened;
    stack2 = foundHelper || depth0.if_opened;
    tmp1 = self.program(5, program5, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n  </div>\n</div>\n";
    return buffer;});
}});

window.require.define({"views/templates/release/releases": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<div class=\"row-fluid page-header\">\n  <div>\n    <h1>Releases</h1>\n  </div>\n</div>\n<table id=\"releases-table\" class=\"releases table table-striped\"></table>\n";});
}});

window.require.define({"views/templates/release/report": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, tmp1, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;

  function program1(depth0,data) {
    
    var buffer = "", stack1, stack2;
    buffer += "\n  <h3 class=\"";
    foundHelper = helpers.errors;
    stack1 = foundHelper || depth0.errors;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "errors", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "name", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</h3>\n  ";
    foundHelper = helpers.fieldReports;
    stack1 = foundHelper || depth0.fieldReports;
    stack2 = helpers.each;
    tmp1 = self.program(2, program2, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n";
    return buffer;}
  function program2(depth0,data) {
    
    var buffer = "", stack1, stack2;
    buffer += "\n    <div class=\"well well-small\">\n    ";
    stack1 = depth0;
    foundHelper = helpers.field_report;
    stack2 = foundHelper || depth0.field_report;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "field_report", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "\n    <dt>Summary</dt>\n    ";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    foundHelper = helpers.field_report;
    stack2 = foundHelper || depth0.field_report;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "field_report", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "\n    </div>\n  ";
    return buffer;}

    foundHelper = helpers.schemaReports;
    stack1 = foundHelper || depth0.schemaReports;
    stack2 = helpers.each;
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n";
    return buffer;});
}});

window.require.define({"views/templates/release/report_table": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<table class=\"report table table-striped\"></table>";});
}});

window.require.define({"views/templates/release/signoff_submission": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, foundHelper, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;


    buffer += "<div class=\"modal-header\">\n  <button type=\"button\" class=\"close\" data-dismiss=\"modal\">\n    ×\n  </button>\n  <h3>Sign Off Submission</h3>\n</div>\n<div class=\"modal-body\">\n  <div class=\"alert\">Once you have confirmed signing off on the submission <strong>";
    foundHelper = helpers.projectKey;
    stack1 = foundHelper || depth0.projectKey;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "projectKey", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</strong>, there is no going back. It will be marked as SIGNED OFF and will be processed and included in this release. Please be certain.\n  </div>\n</div>\n<div class=\"modal-footer\">\n  <a href=\"#\" class=\"btn\" data-dismiss=\"modal\">Close</a>\n  <button type=\"submit\" id=\"signoff-submission-button\" href=\"#\" class=\"btn btn-success\"><i class=\"icon-ok icon-white\"></i> Sign Off Submission</button>\n</div>";
    return buffer;});
}});

window.require.define({"views/templates/release/submission": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<div id=\"submission-header-container\"></div>\n<ul class=\"nav nav-tabs\">\n  <li class=\"active\"><a href=\"#report\" data-toggle=\"tab\">Report</a></li>\n</ul>\n<div class=\"tab-content\">\n  <div class=\"tab-pane active\" id=\"report\">\n    <table id=\"report-container\" width=\"100%\" class=\"report table table-striped\"></table>\n  </div>\n</div>\n";});
}});

window.require.define({"views/templates/release/submission_header": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;


    buffer += "<div class=\"row-fluid page-header\">\n  <ul class=\"breadcrumb\">\n    <li>\n      <a href=\"/releases/";
    foundHelper = helpers.release;
    stack1 = foundHelper || depth0.release;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "release", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.release;
    stack1 = foundHelper || depth0.release;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "release", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</a> <span class=\"divider\">/</span>\n    </li>\n  </ul>\n  <div class=\"pull-left\">\n    <h1><small>SUBMISSION</small> ";
    foundHelper = helpers.projectName;
    stack1 = foundHelper || depth0.projectName;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "projectName", { hash: {} }); }
    buffer += escapeExpression(stack1) + "<br>\n      <small class=\"";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    foundHelper = helpers.lowercase;
    stack2 = foundHelper || depth0.lowercase;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "lowercase", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    foundHelper = helpers.underscore2space;
    stack2 = foundHelper || depth0.underscore2space;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "underscore2space", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "</small>\n      <small>as of ";
    foundHelper = helpers.lastUpdated;
    stack1 = foundHelper || depth0.lastUpdated;
    foundHelper = helpers.date;
    stack2 = foundHelper || depth0.date;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "date", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "</small>\n    </h1>\n  </div>\n  <div class=\"pull-right\">\n    ";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    foundHelper = helpers.submission_action;
    stack2 = foundHelper || depth0.submission_action;
    if(typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, { hash: {} }); }
    else if(stack2=== undef) { stack1 = helperMissing.call(depth0, "submission_action", stack1, { hash: {} }); }
    else { stack1 = stack2; }
    buffer += escapeExpression(stack1) + "\n  </div>\n</div>";
    return buffer;});
}});

window.require.define({"views/templates/release/submission_summary": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, tmp1, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;

  function program1(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n        <tr class=\"signed\">\n          <td width=\"200\">Signed Off</td>\n          <td>";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.SIGNED_OFF);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "summary.SIGNED_OFF", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</td>\n        </tr>\n        ";
    return buffer;}

  function program3(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n        <tr class=\"valid\">\n          <td width=\"200\">Valid</td>\n          <td>";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.VALID);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "summary.VALID", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</td>\n        </tr>\n        ";
    return buffer;}

  function program5(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n        <tr class=\"queued\">\n          <td width=\"200\">Queued</td>\n          <td>";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.QUEUED);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "summary.QUEUED", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</td>\n        </tr>\n        ";
    return buffer;}

  function program7(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n        <tr class=\"invalid\">\n          <td width=\"200\">Invalid</td>\n          <td>";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.INVALID);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "summary.INVALID", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</td>\n        </tr>\n        ";
    return buffer;}

  function program9(depth0,data) {
    
    var buffer = "", stack1;
    buffer += "\n        <tr>\n          <td width=\"200\">Not Validated</td>\n          <td>";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.NOT_VALIDATED);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "summary.NOT_VALIDATED", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</td>\n        </tr>\n        ";
    return buffer;}

    buffer += "<div class=\"row-fluid\">\n  <div class=\"span6\">\n    <ul class=\"nav nav-list\">\n      <li class=\"nav-header\">Submission Summary</li>\n      <table class=\"table summary\">\n        <tbody>\n        ";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.SIGNED_OFF);
    stack2 = helpers['if'];
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n        ";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.VALID);
    stack2 = helpers['if'];
    tmp1 = self.program(3, program3, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n        ";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.QUEUED);
    stack2 = helpers['if'];
    tmp1 = self.program(5, program5, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n        ";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.INVALID);
    stack2 = helpers['if'];
    tmp1 = self.program(7, program7, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n        ";
    foundHelper = helpers.summary;
    stack1 = foundHelper || depth0.summary;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.NOT_VALIDATED);
    stack2 = helpers['if'];
    tmp1 = self.program(9, program9, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n        </tbody>\n      </table>\n    </ul>\n  </div>\n</div>\n";
    return buffer;});
}});

window.require.define({"views/templates/release/submissions_table": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<table class=\"submissions table table-striped\"></table>";});
}});

window.require.define({"views/templates/release/validate_submission": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, foundHelper, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;


    buffer += "<div class=\"modal-header\">\n  <button type=\"button\" class=\"close\" data-dismiss=\"modal\">\n    ×\n  </button>\n  <h3>Validate Submission</h3>\n</div>\n<div class=\"modal-body\">\n<div class=\"alert alert-error\"><strong>Careful!</strong> Validation may take several hours and cannot be cancelled once it has started!</div>\n<div class=\"alert alert-info\">\nThere are currently <strong>";
    foundHelper = helpers.queue;
    stack1 = foundHelper || depth0.queue;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "queue", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</strong> submission(s) in the Validation Queue.<br>\n<br>\nEnter a comma(,) seperated list of the email addresses that should be notified when validation is finished:<br>\n<textarea style=\"width:100%\" id=\"emails\"></textarea>\n</div>\n</div>\n<div class=\"modal-footer\">\n  <a href=\"#\" class=\"btn\" data-dismiss=\"modal\">Close</a>\n  <button type=\"submit\" id=\"validate-submission-button\" href=\"#\" class=\"btn btn-success\"><i class=\"icon-ok icon-white\"></i> Validate ";
    foundHelper = helpers.projectKey;
    stack1 = foundHelper || depth0.projectKey;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "projectKey", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</button>\n</div>";
    return buffer;});
}});

