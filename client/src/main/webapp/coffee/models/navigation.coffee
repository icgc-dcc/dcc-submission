define (require) -> 
  Model = require 'models/base/model'
  
  class Navigation extends Model
    defaults:
      items: [
        {href: '/releases', title: 'Releases', active:'active'}
      ]