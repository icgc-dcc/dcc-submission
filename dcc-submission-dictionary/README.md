The DCC docs site depends on this package for its dictionary viewer.

For the Docs site to receive updates, a new version of [@icgc/dictionary-viewer](https://www.npmjs.com/package/@icgc/dictionary-viewer) will need to be published, and the dependency on the docs site will need to be updated.

An example for minor patch updates - 
```bash
npm version patch
npm publish
```

The above commands will  

1. bump `package.json`'s patch version by 1
2. commit the change and push to github
3. trigger the prepublish command which builds the package
4. publish the new version to npm

After the new version is published, remember to update the dependency on the Docs site to receive the update