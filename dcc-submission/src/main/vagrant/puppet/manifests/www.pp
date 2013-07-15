# www node 
node /www\..*/ inherits 'parent' {
  file { '/etc/motd':
    content => "web server: ${::hostname}\n"
  }

  class { 'dcc': 
    version => '1.7'
  }
}
