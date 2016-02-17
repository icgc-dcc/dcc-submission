# ICGC DCC - Submission UI

This is the UI module which powers the front-end of the submission system.

## Libraries

The UI module is written using the following technology stack:

- [CoffeeScript](http://coffeescript.org/)
- [Chaplin](http://chaplinjs.org/)
- [BackboneJS](http://backbonejs.org/)
- [Stylus](http://stylus-lang.com/)
- [Cake](http://coffeescript.org/documentation/docs/cake.html)
- [Brunch](http://brunch.io/)

## Setup

Install [nodejs] (http://nodejs.org/#download) 0.10.13 or above. If you are using a package manager such as `apt`, `brew` or `macports` please ensure the correct version has been installed.

Install [brunch](http://brunch.io/), [cake](http://coffeescript.org/documentation/docs/cake.html) and required modules:

```shell
sudo npm install -g brunch@1.7.14 
sudo npm install -g coffee-script@1.6.3 
cd dcc/dcc-submission/dcc-submission-ui
npm install
```
	

Ensure that `brunch` has been added to your `PATH` by typing:

```shell
brunch
```
	
Ensure that `cack` has been added to your `PATH` by typing:

```shell
cake
```
	
## Development

Start brunch (in another console):

	cd dcc/dcc-submission/dcc-submission-ui
	brunch w -s

Start the proxy (in yet another console)

	cd dcc/dcc-submission/dcc-submission-ui
	cake proxy

Start the [dcc-submission-server](../dcc-submission-server/README.md)

Point your browser to:

[http://localhost:3334/](http://localhost:3334/)

	
## Build

For CI builds. Required to translate coffee script and minify. This is also required if you intend to run the `dcc-submission-server` without `cake` in development (for non-UI developers). Results are put in `./public`:

	cd dcc/dcc-submission/dcc-submission-ui
	mvn	

*Note:* If not using the proxy above you will need to run this command and restart the `dcc-submission-server` every time a file is modified. 
