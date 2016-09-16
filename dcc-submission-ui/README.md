# ICGC DCC - Submission UI

This is the UI module which powers the front-end of the submission system.

## Libraries

The UI module is written using the following technology stack:

- [React](https://facebook.github.io/react/)
  - [react-bootstrap-table](https://www.npmjs.com/package/react-bootstrap-table)
  - [react-breadcrumbs](https://www.npmjs.com/package/react-breadcrumbs)
- [MobX](https://github.com/mobxjs/mobx)
- [PostCSS](https://github.com/postcss/postcss)
  - [postcss-import](https://github.com/postcss/postcss-import)
  - [PreCSS](https://github.com/jonathantneal/precss)
- [webpack](https://webpack.github.io/)

## Setup

Install [nodejs] (http://nodejs.org/#download) 4 or above. [nvm](https://github.com/creationix/nvm) is recommended.  

```shell
cd dcc/dcc-submission/dcc-submission-ui
npm install
```

## Development

   cd dcc/dcc-submission/dcc-submission-ui
   npm start

Start the [dcc-submission-server](../dcc-submission-server/README.md)

Point your browser to:

[http://localhost:3333/](http://localhost:3333/)


## Build

```shell
cd dcc/dcc-submission/dcc-submission-ui
mvn
```
