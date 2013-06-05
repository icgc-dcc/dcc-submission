class dcc($version) 
{
  # Initialize the Puppet Artifactory module
  package {'curl':} ->
  class {'artifactory':
    url => 'http://seqwaremaven.oicr.on.ca',
  }

  artifactory::artifact {'dcc-submission-server':
    gav        => 'org.icgc.dcc:dcc-submission-server:1.7',
    repository => 'dcc-release',
    output     => '/tmp/dcc-submission-server.jar',
  }
}
