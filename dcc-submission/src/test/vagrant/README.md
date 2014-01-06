## About

This project contains scripts to end-to-end, automate-testing of the DCC submission
system in an OpenStack environment. This primarily for daily integration test or ad-hoc
run of the submission system. 

The test cycle is as follows:
* Provision openstack nodes, assigning IPs
* Install base applications, create a working hadoop cluster
* Install the latest snapshot of DCC-submission
* Setup dictionary, codelist, release and project
* FTP projects to openstack
* Kickoff validation process and wait for validation to finish
* Copy logs and send out email summary
* Tear down openstack cluster


## Dependencies

The scripts require the follow to be installed:
* Vagrant 1.3.3
* Vagrant openstack plugin


## Deployment and configuration

We rely on using the provisioning script in https://github.com/SeqWare/vagrant to kick start
the openstack instances. Probably the easiest way to setup is to clone the vagrant repository elsewhere  
and copy over all the files to the vagrant repo.

An overview of the scripts

* cluster.json determines how the base systems are build
* wrapper.sh which provides a general wrapper for the integration test
* master_node.sh sets up a basic environment for the DCC-submission app
* checkValidate.py polls and keeps track of the validation status

In addition, we need to patch configuration files inside DCC-submission itself to allow us to automate the 
validation process. 

* A custom application.conf with public key authentication enabled. 

Usage:
`wrapper.sh <proj 1> <proj 2> ... <proj n>`


## Note

OpenStack provisioning (at least through Vagrant) seems to be flaky and can hang at random places.

Private keys need to to have at maximum 600 permission. Otherwise Vagrant will complain.

