ICGC DCC - Submission - Vagrant VirtualBox VM
===

To create and start the virtual machine, please follow the directions below.

Setup
---
The minimum requirements for creating the VM is reasonably current VirtualBox installation, Vagrant (v2 preferred) and Ansible:

- Install [VirtualBox 4.2.12](https://www.virtualbox.org/wiki/Downloads)
- Install [Vagrant 1.3.5](http://downloads.vagrantup.com/tags/v1.3.5)
- Install [Ansible ](http://devopsu.com/guides/ansible-mac-osx.html)

*Note*: It is important that you _not_ install the `vagrant-anisble` plugin as this is already bundled with recent versions of Vagrant. Doing so may result in `python` class loading issues.

Run
---
To create the VM, clone the project, navigate to the `vagrant` directory and issue `vagrant up`:
 
 	git clone git@github.com:icgc-dcc/dcc.git
 	cd dcc/dcc-submission/src/main/vagrant
	vagrant up

Ad Hoc Commands
---
Sometimes it may be convenient to issue [ad-hoc commands](http://www.ansibleworks.com/docs/intro_adhoc.html); something that you might type in to do something really quick, but don’t want to save for later. For example, in the [Vagrant environment](http://www.ansibleworks.com/docs/guide_vagrant.html#id5) you may issue the following command to print `$PWD`:

	ansible all -i provisioning/inventory -u vagrant --private-key ~/.vagrant.d/insecure_private_key -m shell -a 'pwd'

Exporting
---
After the VM is provisioned to taste, you may wish to [export the instance](https://www.virtualbox.org/manual/ch08.html#vboxmanage-export) as an appliance. Doing so will result in standard [`OVA`](http://en.wikipedia.org/wiki/Open_Virtualization_Format) file that is portable and convenient for end users to [consume using VirtualBox](https://www.virtualbox.org/manual/ch01.html#ovf). To start the export process, issue the following command to export the appliance:

	VBoxManage export dcc-validator-vm -o dcc-valiator-vm.ova --vsys 0 --product "DCC Validator VM 2.x"

Resources
---
When developing Ansible playbooks targeting the VirtualBox provider, you may find the following resources useful.

#### Ansible
- http://www.ansibleworks.com/docs/

#### Ansible Templates
- http://jinja.pocoo.org/docs/templates/

#### Ansible Vagrant
- http://www.ansibleworks.com/docs/guide_vagrant.html
- http://docs.vagrantup.com/v2/provisioning/ansible.html

#### Vagrant
- http://docs.vagrantup.com/v2/virtualbox/configuration.html

#### VirtualBox
- https://www.virtualbox.org/manual/ch08.html
