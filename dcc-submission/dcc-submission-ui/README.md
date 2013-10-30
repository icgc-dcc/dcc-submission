ICGC DCC - Submission UI
===

Setup
---

Install [npm](http://nodejs.org/#download)

Install brunch:

	sudo npm install -g coffee-script brunch
	cd dcc/dcc-submission/dcc-submission-ui
	npm install
	
Development
---

Start brunch (in another console):

	cd dcc/dcc-submission/dcc-submission-ui
	brunch w

Start the proxy (in yet another console)

	cd dcc/dcc-submission/dcc-submission-ui
	cake proxy

Point your browser to:

[http://localhost:3334/](http://localhost:3334/)

	
Build
---

For CI builds. Required to translate coffee script and minify. This is also required if you intend to run the `dcc-submission-server` without `cake` in development (for non-UI developers). Note that you will need to run this command and restart the server every time a file is modified. Results are put in `./public`:

	cd dcc/dcc-submission/dcc-submission-ui
	mvn	

