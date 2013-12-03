ICGC DCC - Submission - Vagrant VirtualBox VM
===

To create, start, provision and export the virtual machine, please follow the directions below.

Setup
---
The minimum requirements for creating the VM is reasonably current VirtualBox installation, Vagrant (v2 preferred) and Ansible:

- Install [VirtualBox](https://www.virtualbox.org/wiki/Downloads)
- Install [Vagrant](http://downloads.vagrantup.com)
- Install [Ansible](http://www.ansibleworks.com/docs/intro_installation.html)

*Note*: It is important that you _not_ install the external [`vagrant-anisble`](https://github.com/dsander/vagrant-ansible) plugin as this is deprecated and is already bundled with recent versions of Vagrant. Doing so may result in `python` Ansible class loading issues.

Provisioning
---
To create and provision the VM, clone the project, navigate to the `vagrant` directory and issue `vagrant up`:
 
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

#### Ansible Templates (Jinja2)
- http://jinja.pocoo.org/docs/templates/

#### Ansible Vagrant
- http://www.ansibleworks.com/docs/guide_vagrant.html
- http://docs.vagrantup.com/v2/provisioning/ansible.html

#### Vagrant
- http://docs.vagrantup.com/v2/virtualbox/configuration.html

#### VirtualBox
- https://www.virtualbox.org/manual/ch08.html
