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
#####################################################################

# Tests for bash-completion

set -e

errors=0

TARGET="$(dirname $0)/../target"

# Join the supplied arguments into a single string using the specified separator
# $1 : the separator
# everything else: arguments to join
function __join { local IFS="$1"; shift; echo "$*"; }

# Remove trailing and leading spaces and convert \n to spaces
function __prettify {
    tr '\n' ' ' | sed -e 's/^[[:space:]]*//g' -e 's/[[:space:]]*$//g'
}

function __print_completions {
    for ((i=0;i<${#COMPREPLY[*]};i++))
    do
       echo ${COMPREPLY[i]}
    done
}

function __init {
    USER_THERMOSTAT_HOME="$TARGET/bash-completion-test-user-home"
    echo "Skipping initial thermostat setup..."
    mkdir -p $USER_THERMOSTAT_HOME/data
    echo "Exporting custom home..."
    export USER_THERMOSTAT_HOME
    touch ${USER_THERMOSTAT_HOME}/data/setup-complete.stamp

    echo "Checking that help works..."
    ${TARGET}/image/bin/thermostat help >/dev/null

    completion_file="${TARGET}/packaging/bash-completion/thermostat-completion"
    source "${completion_file}"
}

# Find completions for the given input
#
# Writes completion results to stdout. Writes any non-completion output
# to stderr.
function __find_completion {
    COMP_WORDS=("${@}")
    COMP_LINE=$(__join " " "${COMP_WORD[@]}")
    COMP_COUNT=${#COMP_LINE}
    COMP_CWORD=1
    # Use 'complete -p | sed' to find out the function that does completion,
    # then call it
    $(complete -p thermostat | sed "s/.*-F \\([^ ]*\\) .*/\\1/") 1>&2
    __print_completions
}

# $1: The text to complete
#     eg: 'thermostat --foo'
#         'thermostat g'
# $2: The expected completion output. Multiple results are separate by
#     newlines.
#     eg: '--foo-the-bar'
#         $'gc\ngui' ($'' interprets special characters)
function __check_completion {
    input=$1
    expected=$2
    expected_pretty=$(echo $expected | __prettify)
    # save completions and any other output separately and check both
    __find_completion $input >${TARGET}/completion.actual 2>${TARGET}/completion.output
    actual=$(<${TARGET}/completion.actual)
    actual_pretty=$(echo "$actual" | __prettify)
    output=$(<${TARGET}/completion.output)
    if [[ $actual == $expected && -z $output ]] ; then
        echo "[OK]   '$input' => '$actual_pretty'"
    elif [[ -z $output ]]; then
        echo -e "[\e[31mFAIL\e[0m] '$input' => '$actual_pretty' (expected '$expected_pretty')"
        errors=1
    else
        echo -e "[\e[31mFAIL\e[0m] '$input' produced output on stdout/stderr"
        errors=1
    fi
}


__init

echo "Testing bash completions..."

# sample bad test:
# __check_completion "thermostat g" "gc"

__check_completion "thermostat --v" "--version"
__check_completion "thermostat --p" "--print-osgi-info"

__check_completion "thermostat c" "clean-data"
__check_completion "thermostat list-a" "list-agents"
__check_completion "thermostat list-v" "list-vms"

__check_completion "thermostat g" $'gc\ngui'

__check_completion "thermostat web" "web-storage-service"

exit $errors
