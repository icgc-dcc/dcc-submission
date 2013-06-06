ICGC DCC - Submission - Vagrant VirtualBox VM
===

To create and start the virtual machine:

Setup
---
- Install [VirtualBox 4.2.12](https://www.virtualbox.org/wiki/Downloads)
- Install [Ruby 2.0.0](http://www.ruby-lang.org/en/downloads/)
- Install [Vagrant 1.2.2](http://downloads.vagrantup.com/tags/v1.2.2)

Puppet
---
Install ruby gems:

	sudo gem install puppet
	sudo gem install librarian-puppet

Puppet Modules
---
Install puppet modules:

	librarian-puppet install

Run
---
Issue the following command:
 
	vagrant up
