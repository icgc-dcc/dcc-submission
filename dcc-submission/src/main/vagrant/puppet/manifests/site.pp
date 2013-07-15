import 'www.pp'

node 'parent' {
  Exec { 
    path => ['/bin', '/usr/bin'],
  }
}
