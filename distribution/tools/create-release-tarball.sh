#!/bin/bash
#
# Copyright 2012-2017 Red Hat, Inc.
#
# This file is part of Thermostat.
#
# Thermostat is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published
# by the Free Software Foundation; either version 2, or (at your
# option) any later version.
#
# Thermostat is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Thermostat; see the file COPYING.  If not see
# <http://www.gnu.org/licenses/>.
#
# Linking this code with other modules is making a combined work
# based on this code.  Thus, the terms and conditions of the GNU
# General Public License cover the whole combination.
#
# As a special exception, the copyright holders of this code give
# you permission to link this code with independent modules to
# produce an executable, regardless of the license terms of these
# independent modules, and to copy and distribute the resulting
# executable under terms of your choice, provided that you also
# meet, for each linked independent module, the terms and conditions
# of the license of that module.  An independent module is a module
# which is not derived from or based on this code.  If you modify
# this code, you may extend this exception to your version of the
# library, but you are not obligated to do so.  If you do not wish
# to do so, delete this exception statement from your version.
#

# Usage:
#  ./create-release-tarball.sh {SNAPSHOT|RELEASE} [DESIRED_VERSION] [HG_USER]
#
# Tags the latest commit, creates the stand-alone integration test jar,
# and creates a release tarball from the tagged version. Bumps the
# version once again for further in-branch development.
# 
# Results are placed in distribution/target/release-tarball
set -ex

mvn=mvn
hg=hg

function usage() {
  echo "Usage: ./create-release-tarball.sh {SNAPSHOT|RELEASE} [DESIRED_VERSION] [HG_USER]" 1>&2
  exit 1
}

if [ $# -lt 1 ] || [ $# -gt 3 ]; then
  usage
fi

# The first parameter determines if this script should tag the HG tree
# or not:
#   SNAPSHOT == no tagging
#   RELEASE  == tag tree
is_snapshot=false
is_release=false
source_type=$1
if [ "${source_type}_" == "SNAPSHOT_" ]; then
  is_snapshot=true
elif [ "${source_type}_" == "RELEASE_" ]; then
  is_release=true
else
  echo "First parameter must be SNAPSHOT or RELEASE." 1>&2
  usage
fi
unset source_type

# We need maven and mercurial (sed, awk and md5sum too, but we assume it's there)
if ! type ${mvn}; then
  echo "Maven (mvn) is required to run this script. ${mvn} not found in PATH=${PATH}." 1>&2
  exit 1
fi
if ! type ${hg}; then
  echo "Mercurial (hg) is required to run this script. ${hg} not found in PATH=${PATH}." 1>&2
  exit 1
fi
if [ "${is_snapshot}_" == "true_" ]; then
  if ! hg strip --help > /dev/null 2>&1; then
    echo "Creating snapshot release tarball requires the HG strip extension." 1>&2
    exit 1
  fi
fi 

new_version=""
hg_user=""
if [ $# -gt 1 ]; then
  new_version="$2"
fi
if [ $# -eq 3 ]; then
  hg_user="$3"
fi
tools_dir="$(dirname $0)"
results_dir="distribution/target/release-tarball"

# Detect current version
current_version=$(${mvn} -B org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version | grep -v '\[' | sed 's/-SNAPSHOT//')
# Set new version
if [ -z "${new_version}" ]; then
  new_version=$(echo "${current_version}" | sed 's/\./ /g' | awk '{ print $1, $2, 1+$3}' | sed 's/ /\./g')
fi
if [ -z "${hg_user}" ]; then
  hg_user="create-release-tarball-script"
fi

# Get the current revision so as to be able to reproduce
# the tarball without a tag being pushed.
base_revision="$(hg id -i)"

${tools_dir}/update-version -r ${current_version} ${new_version}

# Verify build is still OK.
make for-release

mkdir -p "${results_dir}"
standalone_itest_jar=integration-tests/standalone/target/thermostat-integration-tests-standalone-${new_version}.jar
cp ${standalone_itest_jar} "${results_dir}"/

hg commit -u "${hg_user}" -m "Bump version for Thermostat release ${new_version}"
hg tag -u "${hg_user}" ${new_version}
hg archive -t tgz ${results_dir}/thermostat-${new_version}.tar.gz -r ${new_version}

# Only do this for release invocations
if [ "${is_release}_" == "true_" ]; then
  # Bump version again for further in-tree development
  after_new_version=$(echo "${new_version}" | sed 's/\./ /g' | awk '{ print $1, $2, 1+$3}' | sed 's/ /\./g')
  ${tools_dir}/update-version -d ${new_version} ${after_new_version}
  hg commit -u "${hg_user}" -m "Bump version to ${after_new_version} for further in branch development"
else
  # Strip tagging changesets since they won't get pushed
  # anyway. This depends on the strip extension.
  hg strip $(hg id -i --rev "children(${base_revision})")
fi

echo "Creating checksums"
pushd ${results_dir}
  md5sum thermostat-${new_version}.tar.gz > thermostat-${new_version}.tar.gz.md5
  md5sum thermostat-integration-tests-standalone-${new_version}.jar > thermostat-integration-tests-standalone-${new_version}.jar.md5
popd

# Provide a recipe for re-creating the same source tarball
cat > "${results_dir}/create-sourcetarball-recipe.txt" <<EOF
# In order re-create the source tarball do the following
hg clone <thermostat-tree> thermostat
pushd thermostat
hg update ${base_revision}
${tools_dir}/update-version -r ${current_version} ${new_version}
hg commit -u "${hg_user}" -m "Bump version for Thermostat release ${new_version}"
hg tag -u "${hg_user}" ${new_version}
hg archive -t tgz thermostat-${new_version}.tar.gz -r ${new_version}
popd
EOF

echo "All done. Results can be found in directory: ${results_dir}"
