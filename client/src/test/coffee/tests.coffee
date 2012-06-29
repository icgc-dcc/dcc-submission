should = require('chai').should()  

it 'should be initially incomplete', ->  
    task1.status.should.equal 'incomplete'  
    
it 'should be able to be completed', ->  
    task1.complete().should.be.true  
    task1.status.should.equal 'complete'  