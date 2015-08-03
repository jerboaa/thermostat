#!/bin/bash
#
# Copyright 2012-2015 Red Hat, Inc.
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
#
# Usage:
#  ./create-fedora-rhel-srpm.sh [RPM_RELEASE] [HG_USER]
#
# Creates a source tarball (using create-source-tarball.sh) and 
# after an SRPM from the fedora/rhel spec file.
# 
# The result is placed in distribution/target/srpms
set -ex

mvn=mvn
rpmbuild=rpmbuild
tarball_results_dir="distribution/target/release-tarball"
spec_and_sources_dir="distribution/packaging/fedora"

# We need rpmbuild and (transitively) maven, mercurial
# (sed, awk and md5sum too, but we assume it's there)
if ! type ${rpmbuild}; then
  echo "rpmbuild is required to run this script. ${rpmbuild} not found in PATH=${PATH}." 1>&2
  exit 1
fi

if [ $# -eq 1 ]; then
  custom_rpm_release="$1"
fi
if [ $# -eq 2 ]; then
  custom_rpm_release="$1"
  hg_user="$2"
fi
if [ $# -gt 2 ]; then
  echo "usage: ./create-fedora-rhel-srpm.sh [RPM_RELEASE] [HG_USER]" 1>&2
  exit 1
fi
tools_dir="$(dirname $0)"
results_dir="distribution/target/srpms"

if [ -z "${hg_user}" ]; then
  hg_user="tarball for SRPM"
fi

# Create a source tarball via the snapshot invocation
${tools_dir}/create-release-tarball.sh SNAPSHOT "${new_version}" "${hg_user}"

# Extract version info from created tarball
new_version=$(basename "${tarball_results_dir}"/thermostat-*.tar.gz | sed 's/thermostat-//g;s/\.tar\.gz//g')

# Create the SRPM by using stubs in distribution/packaging/fedora
srpm_tmp_build_dir="distribution/target/srpm-tmp-build"
top_dir="$(pwd)"
mkdir -p ${srpm_tmp_build_dir}
mkdir -p ${results_dir}
cp "${spec_and_sources_dir}"/* "${srpm_tmp_build_dir}"
cp "${tarball_results_dir}"/thermostat-${new_version}.tar.gz "${srpm_tmp_build_dir}"
# Verify that patches apply for tarball.
tmpdir="$(mktemp -d)"
pushd ${tmpdir}
  tar -xf "${top_dir}/${tarball_results_dir}"/thermostat-${new_version}.tar.gz
  pushd "thermostat-${new_version}"
    # This depends on patches beeing named corresponding to the
    # order they're applied
    echo "Testing whether patches apply correctly to sources"
    for patch in $(ls -1 "${top_dir}/${srpm_tmp_build_dir}"/*.patch); do
       patch -f -p1 < "$patch"
    done
  popd
popd
# Remove tmp directory since patches applied correctly
rm -rf "${tmpdir}"
major_version=$(echo "${new_version}" | sed 's/\./ /g' | awk '{ print $1}')
minor_version=$(echo "${new_version}" | sed 's/\./ /g' | awk '{ print $2}')
micro_version=$(echo "${new_version}" | sed 's/\./ /g' | awk '{ print $3}')
sed -i "s/__MAJOR__/${major_version}/g;s/__MINOR__/${minor_version}/g;s/__PATCHLEVEL__/${micro_version}/g" "${srpm_tmp_build_dir}"/thermostat.spec
default_release=$(grep __DEFAULT_RELEASE__ "${srpm_tmp_build_dir}"/thermostat.spec | cut -d' ' -f2)
# Remove the line with __DEFAULT_RELEASE__ in spec
sed -i "s/^__DEFAULT_RELEASE__.*$//g" "${srpm_tmp_build_dir}"/thermostat.spec
if [ "${custom_rpm_release}_" != "_" ]; then
  rpm_release="${custom_rpm_release}"
else
  rpm_release="${default_release}"
fi
sed -i "s/__RELEASE__/${rpm_release}/g" "${srpm_tmp_build_dir}"/thermostat.spec
pushd "${srpm_tmp_build_dir}"
  ${rpmbuild} \
    --define "_sourcedir $(pwd)" \
    --define "_srcrpmdir ${top_dir}/${results_dir}" \
    --define "dist .fedora" \
    -bs thermostat.spec
popd
rm -rf "${top_dir}/${srpm_tmp_build_dir}"

echo "All done. Results can be found in directory: ${results_dir}"
