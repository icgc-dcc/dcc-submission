ICGC DCC - Submission UI
===

Setup
---

Install [npm](http://nodejs.org/#download)

Install brunch:

	sudo npm install -g coffee-script brunch
	cd dcc/dcc-submission/dcc-submission-ui
	npm install
Run
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

Translate coffee script and minify. Results in `./public`:

	cd dcc/dcc-submission/dcc-submission-ui
	brunch b m
