#!/bin/bash
#
# Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
#
# This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
# You should have received a copy of the GNU General Public License along with
# this program. If not, see <http://www.gnu.org/licenses/>.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
# OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
# SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
# TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
# OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
# IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

traverse() 
{
  # Traverse a directory
  ls "$1" | while read i
  do
    if [ -d "$1/$i" ]; then
      traverse "$1/$i"
    else 
      echo "Uploading file: $1/$i"
      curl -XPOST -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" http://$SERVER/ws/seed/$1/$i --data-binary @$1/$i
    fi
  done
}

printusage()
{
	echo "USAGE: $0 DIRECTORY [SERVER]"
	echo "Where DIRECTORY has the following structure: "
	echo "  DIRECTORY/codelists.json"
	echo "  DIRECTORY/dictionaries.json"
	echo "  DIRECTORY/projects.json"
	echo "  DIRECTORY/releases.json"
	echo "  DIRECTORY/users.json"
	echo "  DIRECTORY/fs/[all files and subdirectories]"
	echo ""
	echo "The complete directory tree under fs will be reproduced on the remote system."
	echo "All files are optional."
	echo ""
	echo "The default value of SERVER is localhost:5380"
	exit 1
}

if [ -z "$1" ]; then
	printusage
else
	if [ -d "$1" ]; then
		SERVER=$2
		if [ -z "$2" ]; then
			SERVER="localhost:5380"
		fi
		cd $1
		for DATATYPE in users projects dictionaries releases codelists
		do
			if [ -e "$DATATYPE.json" ]; then
				echo "Seeding database: $DATATYPE"
				curl -XPOST -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" http://$SERVER/ws/seed/$DATATYPE?delete=true --data @$DATATYPE.json --header "Content-Type: application/json"
			fi
		done
	
		if [ -d "fs" ]; then
			traverse fs
		fi
	else
		echo "ERROR! Directory $1 does not exist"
		echo ""
		printusage
	fi
fi
	