ICGC DCC - Submission - Vagrant VirtualBox VM
===

To create and start the virtual machine:

Setup
---
- Install [VirtualBox 4.2.12](https://www.virtualbox.org/wiki/Downloads)
- Install [Vagrant 1.3.5](http://downloads.vagrantup.com/tags/v1.3.5)
- Install [Ansible ](http://devopsu.com/guides/ansible-mac-osx.html)

Run
---
Issue the following command:
 
 	cd dcc/dcc-submission/src/main/vagrant
	vagrant up

Ad Hoc Commands
---
Issue the following command to print `$PWD`:

	ansible all -i provisioning/inventory -u vagrant --private-key ~/.vagrant.d/insecure_private_key -m shell -a 'pwd'

Resources
---
Useful links:
- http://www.ansibleworks.com/docs/
- http://jinja.pocoo.org/docs/templates/
- http://docs.vagrantup.com/v2/provisioning/ansible.html
- http://docs.vagrantup.com/v2/virtualbox/configuration.html