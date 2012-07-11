define ['models/release'], (Release)->
  describe "Release", ->
    release = null
    beforeEach ->
      release = new Release {name: "example"}
  
    it 'should have a urlKey', ->
      release.urlKey.should.equal 'name'
    it 'should have a urlPath', ->
      release.urlPath().should.equal 'releases/'
    it 'should expose its attributes', ->
      release.get('name').should.equal 'example'