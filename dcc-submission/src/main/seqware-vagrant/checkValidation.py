#!/usr/bin/python

import sys
import json
import urllib2
import time
import smtplib

host = sys.argv[1]
release = sys.argv[2]
auth = sys.argv[3]


# State
validation_state = {}

# Common header
headers = {
   'Accept': 'application/json',
   #'Authorization': 'X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk',
   'Authorization': 'X-DCC-Auth ' + auth,
   'Content-Type': 'application/json'
}


# Check validation status
while True:
   print '>>> Checking validation status...', host, release
   c = 0
   request = urllib2.Request(host + '/ws/releases/' + release, headers=headers)
   response = urllib2.urlopen(request)
   data = json.loads(response.read())

   state_list = []
   #print data['submissions']

   for proj in data['submissions']:
      proj_state = proj['state']
      proj_key = proj['projectKey']

      state_list.append(proj_state)

      # There are probably more precise/safer time measures.
      # But since we are talking about minutes for the validations, this simple method should be sufficient.
      if proj_key not in validation_state:
         validation_state[proj_key] = {}
         validation_state[proj_key]['state'] = proj_state
         validation_state[proj_key]['start'] = time.time()
         validation_state[proj_key]['end'] = -1


      if validation_state[proj_key]['state'] in ['NOT_VALIDATED']:
         continue
      
      if validation_state[proj_key]['state'] != proj_state:
         # Status change. Ignore start, else send email report
         validation_state[proj_key]['state'] = proj_state
         #if proj_state in ['ERROR', 'INVALID']:
         #   send_mail('Error in ' + proj_key)

      elif validation_state[proj_key]['state'] not in ['QUEUED', 'VALIDATING', 'OPENED']:
         # The submission has reached some type of final state
         if validation_state[proj_key]['end'] == -1:
            validation_state[proj_key]['end'] = time.time()
         c = c + 1

   if c == len(validation_state):
      break;
   else:
      print '\t'.join(state_list)
      #print 'Waiting for', (len(validation_state) - c ), ' projects'
      time.sleep(60)

fh = open('summary.txt', 'w')
for proj in validation_state.keys():
   time_elapsed = (validation_state[proj]['end'] - validation_state[proj]['start'])
   print proj, time_elapsed, "seconds"
   fh.write( proj + ' | ' + validation_state[proj]['state']  +  ' | ' + str(time_elapsed) + ' seconds' )
fh.close()



