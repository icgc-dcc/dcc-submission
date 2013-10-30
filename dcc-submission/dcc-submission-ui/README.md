ICGC DCC - Submission UI
===

Setup
---

Install [nodejs] (http://nodejs.org/#download) 0.10.13 or above. If you are using a package manager such as `apt`, `brew` or `macports` please ensure the correct version has been installed.

Install brunch and required modules:

	sudo npm install -g brunch coffee-script 
	cd dcc/dcc-submission/dcc-submission-ui
	npm install
	
Ensure that `brunch` has been added to your `PATH` by typing:

	brunch
	
Development
---

Start brunch (in another console):

	cd dcc/dcc-submission/dcc-submission-ui
	brunch w -s

Start the proxy (in yet another console)

	cd dcc/dcc-submission/dcc-submission-ui
	cake proxy

Start the [dcc-submission-server](dcc/dcc-submission/dcc-submission-server/README.MD)

Point your browser to:

[http://localhost:3334/](http://localhost:3334/)

	
Build
---

For CI builds. Required to translate coffee script and minify. This is also required if you intend to run the `dcc-submission-server` without `cake` in development (for non-UI developers). Note that you will need to run this command and restart the server every time a file is modified. Results are put in `./public`:

	cd dcc/dcc-submission/dcc-submission-ui
	mvn	

