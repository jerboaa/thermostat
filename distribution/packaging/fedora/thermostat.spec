# Upstream Thermostat version triplet
%global major        __MAJOR__
%global minor        __MINOR__
%global patchlevel   __PATCHLEVEL__

# Thermostat requires exact versions for bundle dependencies.
# We use those versions to pass to the maven build in order for
# it to set it in relevant files.

# Real OSGi Bundle-Version is 3.6.6.Final
%global netty_bundle_version       3.9.3
%global jcommon_bundle_version     1.0.23
%global jfreechart_bundle_version  1.0.19
# apache-commons-beanutils
%global beanutils_bundle_version   1.9.2
# apache-commons-codec
%global codec_bundle_version       1.10.0
# apache-commons-collections
%global collections_bundle_version 3.2.1
# apache-commons-logging
%global logging_bundle_version     1.2.0
# Real OSGi Bundle-Version is 2.13.2.RELEASE
%global mongo_bundle_version       2.13.2
%global hc_core_bundle_version     4.4.1
%global hc_client_bundle_version   4.5.0
%global gson_bundle_version        2.3.1
# Jansi is used as bootstrap bundle and the
# bootstrap bundle properties file refers to the jar
# with version suffix. See 0001_shared_fix_bundle_loading.patch
%global jansi_version              1.11
# thread plugin needs jgraphx. See gui.properties hunk in
# 0001_shared_fix_bundle_loading.patch We pass in
# jgraphx.osgi.version via the command line.
%global jgraphx_bundle_version     3.1.2

# Base path to the JDK which will be used in boot scripts
%global jdk_base /etc/alternatives/java_sdk_openjdk

%{?scl:%scl_package thermostat}
%{!?scl:%global pkg_name %{name}}

# Global directory definitions
%global system_datadir %{_localstatedir}/lib/%{pkg_name}
%global system_cachedir %{_localstatedir}/cache/%{pkg_name}
%global system_logdir %{_localstatedir}/log/%{pkg_name}
%global system_statedir %{_localstatedir}/run/%{pkg_name}
# _root_<foo> don't seem to be defined in non-SCL context.
# Define some vars we use instead in order for the build to work
# for SCL/non-SCL contexts.
%{?scl:
  %global system_confdir %{_root_sysconfdir}
  %global system_root_datadir %{_root_datadir}
  %global system_tmpfilesdir %{_root_exec_prefix}/lib/tmpfiles.d
}
# not SCL
%{!?scl:
  %global system_confdir %{_sysconfdir}
  %global system_root_datadir %{_datadir}
  %global system_tmpfilesdir %{_tmpfilesdir}
}
# system java dir definition (non-scl)
%global system_javadir %{system_root_datadir}/java
%global scl_javadir %{_javadir}

# THERMOSTAT_HOME and USER_THERMOSTAT_HOME variables. Note that
# we use USER_THERMOSTAT_HOME only for systemd related setup.
%global thermostat_home %{_datarootdir}/%{pkg_name}
%{?scl:
  %global user_thermostat_home %{_scl_root}
}
# not SCL
%{!?scl:
  # Prefix is "/" for non-scl
  %global user_thermostat_home /
}

# thermostat-webapp specific variables
%{?scl:
  %global thermostat_catalina_base %{_datarootdir}/tomcat
}
# not SCL
%{!?scl:
  %global thermostat_catalina_base %{_localstatedir}/lib/tomcats/%{pkg_name}
}
# The port tomcat will be listening on
%global thermostat_catalina_port 8999

# Uncomment to build from snapshot out of hg.  See also Release and Source0
#%%global hgrev b7c6db90e034

Name:       %{?scl_prefix}thermostat
Version:    %{major}.%{minor}.%{patchlevel}
# If building from snapshot out of hg, uncomment and adjust below value as appropriate
#Release:    0.1.20131122hg%{hgrev}%{?dist}
Release:    2%{?dist}
Summary:    A monitoring and serviceability tool for OpenJDK
License:    GPLv2+ with exceptions and OFL
URL:        http://icedtea.classpath.org/thermostat/
# This is the source URL to be used for released versions
Source0:    http://icedtea.classpath.org/download/%{pkg_name}/%{pkg_name}-%{version}.tar.gz
# This is the source to be used for hg snapshot versions from HEAD
#wget -O thermostat-%{hgrev}.tar.bz2 http://icedtea.classpath.org/hg/%{pkg_name}/archive/%{hgrev}.tar.bz2
#Source0:    thermostat-%{hgrev}.tar.bz2
# This is the source to be used for hg snapshot versions from a release branch
#wget -O thermostat-%{major}.%{minor}-%{hgrev}.tar.bz2 http://icedtea.classpath.org/hg/release/%{pkg_name}-%{major}.${minor}/archive/%{hgrev}.tar.bz2
#Source0:    thermostat-%{major}.%{minor}-%{hgrev}.tar.bz2
# This is _NOT_ suitable for upstream at this point.
# It's very Fedora specific.
Source1:    thermostat-sysconfig
# SCL only sources
Source3:    scl-thermostat-tomcat-service-sysconfig
Source4:    fedora-thermostatrc
# This is _NOT_ suitable for upstream at this point.
# jfreechart isn't a bundle upstream. Also some httpclient* related bundles
# include transitive deps upstream, which isn't the case in Fedora (i.e. is
# properly done in Fedora)
Patch1:     0001_shared_fix_bundle_loading.patch
# Patch proposed upstream, but was denied.
# See http://icedtea.classpath.org/pipermail/thermostat/2013-October/008602.html
# For now _NOT_ suitable for upstream until felix ships an API only package which
# is 4.3 OSGi spec.
Patch2:     0002_shared_osgi_spec_fixes.patch

# FIXME: Self-BR in order for xmvn-subst to work for symlinking
# thermostat deps.
#BuildRequires: thermostat-webapp = %{version}
# BRs for core thermostat
BuildRequires: java-devel >= 1:1.7.0
BuildRequires: javapackages-tools
BuildRequires: maven-local
BuildRequires: maven-dependency-plugin
BuildRequires: maven-surefire-plugin
BuildRequires: maven-war-plugin
BuildRequires: maven-clean-plugin
BuildRequires: maven-assembly-plugin
BuildRequires: maven-plugin-bundle
BuildRequires: maven-javadoc-plugin
BuildRequires: maven-archetype-packaging
BuildRequires: mvn(org.apache.maven.plugins:maven-archetype-plugin)
BuildRequires: libgnome-keyring-devel
# laf-utils JNI need pkconfig files for gtk2+
BuildRequires: gtk2-devel
BuildRequires: mvn(org.apache.felix:org.apache.felix.framework)
BuildRequires: mvn(org.fusesource:fusesource-pom:pom:)
BuildRequires: mvn(org.apache.commons:commons-cli)
# jline 2.10 is known to work
BuildRequires: mvn(jline:jline) >= 2.10
BuildRequires: mvn(org.fusesource.jansi:jansi)
BuildRequires: mvn(org.apache.lucene:lucene-core) >= 4.7.0
BuildRequires: mvn(org.apache.lucene:lucene-analyzers) >= 4.7.0
BuildRequires: mvn(com.google.code.gson:gson)
BuildRequires: mvn(org.jfree:jfreechart)
BuildRequires: mvn(org.jfree:jcommon)
BuildRequires: mvn(org.apache.commons:commons-beanutils)
BuildRequires: mvn(org.mongodb:mongo-java-driver)
# Change to netty 4 once RHBZ#1053619 is
# resolved.
# The version number in mvn() means it's a compat package.
BuildRequires: mvn(io.netty:netty:%{netty_bundle_version})

# BRs for webapp sub-package
BuildRequires: tomcat
BuildRequires: mvn(javax.servlet:servlet-api) >= 2.5
BuildRequires: mvn(org.apache.commons:commons-fileupload)

# thermostat web-storage-service BRs
BuildRequires: mvn(org.eclipse.jetty:jetty-server)
BuildRequires: mvn(org.eclipse.jetty:jetty-jaas)
BuildRequires: mvn(org.eclipse.jetty:jetty-webapp)
BuildRequires: mvn(org.eclipse.jetty.toolchain:jetty-schemas)
# The thread plugin needs this for visualizing thread deadlocks
BuildRequires: mvn(com.mxgraph:jgraphx)

###################################################
# The following BRs are specified via osgi's
# symbolic name. This is to ensure exact versions
# as specified in thermostat's bundle list has
# a chance of working at runtime.
###################################################
# 1.0.14-7 has OSGi metadata and itext dep fix
BuildRequires: osgi(org.jfree.jfreechart) = %{jfreechart_bundle_version}
# 1.0.17-4 has OSGi metadata
BuildRequires: osgi(org.jfree.jcommon) = %{jcommon_bundle_version}
BuildRequires: osgi(org.apache.commons.logging) = %{logging_bundle_version}
BuildRequires: osgi(org.apache.commons.beanutils) = %{beanutils_bundle_version}
BuildRequires: osgi(org.apache.commons.codec) = %{codec_bundle_version}
BuildRequires: osgi(org.mongodb.mongo-java-driver) = %{mongo_bundle_version}
BuildRequires: osgi(org.jboss.netty) = %{netty_bundle_version}
BuildRequires: osgi(com.google.gson) = %{gson_bundle_version}
BuildRequires: osgi(org.apache.httpcomponents.httpcore) = %{hc_core_bundle_version}
# httpmime comes from httpcomponents-client just like httpclient itself
BuildRequires: osgi(org.apache.httpcomponents.httpclient) = %{hc_client_bundle_version}
BuildRequires: osgi(org.apache.httpcomponents.httpmime) = %{hc_client_bundle_version}
BuildRequires: osgi(com.mxgraph) = %{jgraphx_bundle_version}

Requires: javapackages-tools
Requires: java-devel >= 1:1.8.0
# Only require mongodb-server on arches where it's available
%ifarch %{arm} %{ix86} x86_64
Requires: mongodb-server
# Fedora's thermostat-setup uses mongo directly
Requires: mongodb
%endif
Requires: libgnome-keyring
Requires(post): systemd
Requires(preun): systemd
Requires(postun): systemd
###################################################
# The following Rs are specified via osgi's
# symbolic name. This is to ensure exact versions
# as specified in thermostat's bundle list has
# a chance of working at runtime.
###################################################
Requires: osgi(org.jfree.jfreechart) >= %{jfreechart_bundle_version}
Requires: osgi(org.jfree.jcommon) >= %{jcommon_bundle_version}
Requires: osgi(org.apache.commons.logging) >= %{logging_bundle_version}
Requires: osgi(org.apache.commons.beanutils) >= %{beanutils_bundle_version}
Requires: osgi(org.apache.commons.codec) >= %{codec_bundle_version}
Requires: osgi(org.mongodb.mongo-java-driver) >= %{mongo_bundle_version}
Requires: osgi(org.jboss.netty) = %{netty_bundle_version}
Requires: osgi(com.google.gson) >= %{gson_bundle_version}
Requires: osgi(org.apache.httpcomponents.httpcore) >= %{hc_core_bundle_version}
# httpmime comes from httpcomponents-client just like httpclient itself
Requires: osgi(org.apache.httpcomponents.httpclient) >= %{hc_client_bundle_version}
Requires: osgi(org.apache.httpcomponents.httpmime) >= %{hc_client_bundle_version}

# This module has been removed to fix CVE-2014-8120
Obsoletes: %{?scl_prefix}mvn(com.redhat.thermostat:thermostat-agent-proxy-common) <= %{version}

%{?scl:Requires: %scl_runtime}

%description
Thermostat is a monitoring and instrumentation tool for the Hotspot JVM,
with support for monitoring multiple JVM instances. The system is made
up of two processes: an Agent, which collects data, and a Client which
allows users to visualize this data. These components communicate via
a MongoDB-based storage layer. A pluggable agent and gui framework
allows for collection and visualization of performance data beyond that
which is included out of the box.

%package javadoc
Summary:    Javadocs for %{pkg_name}
Group:      Documentation
Requires:   javapackages-tools

BuildArch:  noarch

%description javadoc
This package contains the API documentation for %{pkg_name}

%package webapp
Summary:    Web storage endpoint for Thermostat
BuildArch:  noarch
# Not sure if we need this, but better be safe than sorry.
# This version will allow custom catalina-base/systemd stuff as
# we do it.
Requires:   tomcat >= 7.0.42-3
Requires:   %{name} = %{version}-%{release}

%description webapp
This package contains the exploded web archive. This web application
contains the server-side parts for deploying thermostat with improved
security.

%prep
# When Source0 is released version. 
%setup -q -n %{pkg_name}-%{version}
# When Source0 is a snapshot from HEAD.
#%%setup -q -n %%{pkg_name}-%%{hgrev}
# When Source 0 is a snapshot from a release branch.
#%%setup -q -n %%{pkg_name}-%%{major}-%%{minor}-%%{hgrev}
%patch1 -p1
%patch2 -p1

# Fix up artifact names which have different name upstream
#  lucene
%pom_remove_dep "org.apache.servicemix.bundles:org.apache.servicemix.bundles.lucene" vm-heap-analysis/common
%pom_remove_dep "org.apache.servicemix.bundles:org.apache.servicemix.bundles.lucene" vm-heap-analysis/distribution
%pom_remove_dep "org.apache.servicemix.bundles:org.apache.servicemix.bundles.lucene-analyzers-common" vm-heap-analysis/common
%pom_remove_dep "org.apache.servicemix.bundles:org.apache.servicemix.bundles.lucene-analyzers-common" vm-heap-analysis/distribution
%pom_add_dep "org.apache.lucene:lucene-analyzers:5.2.0" vm-heap-analysis/common
%pom_add_dep "org.apache.lucene:lucene-analyzers:5.2.0" vm-heap-analysis/distribution
%pom_add_dep "org.apache.lucene:lucene-core:5.2.0" vm-heap-analysis/common
%pom_add_dep "org.apache.lucene:lucene-core:5.2.0" vm-heap-analysis/distribution
# Fix up artifact names for jgraphx
%pom_remove_dep "org.tinyjee.jgraphx:jgraphx"
%pom_add_dep "com.mxgraph:jgraphx:3.1.2.0"
%pom_remove_dep "org.tinyjee.jgraphx:jgraphx" thread/client-swing
%pom_add_dep "com.mxgraph:jgraphx:3.1.2.0" thread/client-swing
#  httpclient
%pom_remove_dep org.apache.httpcomponents:httpclient-osgi web/client
%pom_add_dep org.apache.httpcomponents:httpclient:4.4.0 web/client
%pom_remove_dep org.apache.httpcomponents:httpclient-osgi client/command
%pom_add_dep org.apache.httpcomponents:httpclient:4.4.0 client/command
#  add httpmime dep. this is included in upstreams' strange jar
%pom_add_dep org.apache.httpcomponents:httpmime:4.4.0 web/client
#  httpcore
%pom_remove_dep org.apache.httpcomponents:httpcore-osgi web/client
%pom_add_dep org.apache.httpcomponents:httpcore:4.4.0 web/client
# need jline 2.10 (otherwise this resolves to jline 1)
%pom_xpath_remove "pom:properties/pom:jline.version"
%pom_xpath_inject "pom:properties" "<jline.version>2.10</jline.version>"
#  netty
#%%pom_remove_dep org.jboss.netty:netty 
%pom_remove_dep org.jboss.netty:netty common/command
%pom_remove_dep org.jboss.netty:netty client/command
%pom_remove_dep org.jboss.netty:netty agent/command
%pom_add_dep io.netty:netty:%{netty_bundle_version} common/command
%pom_add_dep io.netty:netty:%{netty_bundle_version} client/command
%pom_add_dep io.netty:netty:%{netty_bundle_version} agent/command

# Don't use maven-exec-plugin. We do things manually in order to avoid this
# additional dep. It's used in agent/core/pom.xml et.al.
%pom_remove_plugin org.codehaus.mojo:exec-maven-plugin agent/core
%pom_remove_plugin org.codehaus.mojo:exec-maven-plugin keyring
%pom_remove_plugin org.codehaus.mojo:exec-maven-plugin laf-utils

# Remove license plugin in main pom.xml
%pom_remove_plugin com.mycila:license-maven-plugin

# Remove javacoco-coverage plugin (in main pom.xml and web/war/pom.xml)
%pom_remove_plugin org.jacoco:jacoco-maven-plugin
%pom_remove_plugin org.jacoco:jacoco-maven-plugin web/war

# Remove pmd plugin
%pom_remove_plugin org.apache.maven.plugins:maven-pmd-plugin

# Remove m2e's lifecyle plugin
%pom_remove_plugin org.eclipse.m2e:lifecycle-mapping

# Disable test modules
%pom_disable_module testutils storage
%pom_disable_module test common
%pom_disable_module integration-tests
%pom_remove_dep com.redhat.thermostat:thermostat-storage-testutils vm-cpu/common
%pom_remove_dep com.redhat.thermostat:thermostat-storage-testutils vm-profiler/common
%pom_remove_dep com.redhat.thermostat:thermostat-storage-testutils thread/collector
# Disable some dev modules we don't ship
%pom_disable_module ide-launcher dev
%pom_disable_module schema-info-command dev
%pom_disable_module perflog-analyzer dev
%pom_remove_dep com.redhat.thermostat:thermostat-schema-info-distribution distribution

# Remove system scope and systempath from tools jar dependency.
# That dependency is activated via the needs-tools-jar profile.
%pom_xpath_remove "pom:profiles/pom:profile[pom:id='needs-tools-jar']/pom:dependencies/pom:dependency/pom:scope"
%pom_xpath_remove "pom:profiles/pom:profile[pom:id='needs-tools-jar']/pom:dependencies/pom:dependency/pom:systemPath"

# Remove depencency on the web archive for web-storage-service we'll make deps
# available manually
%pom_remove_dep "com.redhat.thermostat:thermostat-web-war" web/endpoint-plugin/web-service

# Skip automatic installation of zip artifacts. We only use it for our build
# to assemble plug-ins.
%mvn_package com.redhat.thermostat::zip: __noinstall
# Skip automatic installation of the war module.
# We install it manually. Without this "config" %mvn_build -f
# fails. See RHBZ#963838
%mvn_package com.redhat.thermostat:thermostat-web-war __noinstall
# Don't install :thermostat-common-test, it's a test only dep which
# isn't run during the build.
%mvn_package com.redhat.thermostat:thermostat-common-test __noinstall

# These are just upstream build helpers. Don't install them.
%mvn_package com.redhat.thermostat:thermostat-distribution __noinstall
%mvn_package com.redhat.thermostat:thermostat-assembly __noinstall

# thermostat-web-server and thermostat-web-endpoint should be part of
# the webapp sub-package
%mvn_package com.redhat.thermostat:thermostat-web-server webapp
%mvn_package "com.redhat.thermostat:thermostat-web-endpoint-plugin" webapp
%mvn_package "com.redhat.thermostat:thermostat-web-endpoint:pom:" webapp
%mvn_package "com.redhat.thermostat:thermostat-web-endpoint-distribution:pom:" webapp
# Do not embed jgraphx dependency in thread client.
%pom_xpath_remove "pom:project/pom:build/pom:plugins/pom:plugin[pom:artifactId='maven-bundle-plugin']/pom:configuration/pom:instructions/pom:Embed-Dependency" thread/client-swing

%build
export CFLAGS="$RPM_OPT_FLAGS" LDFLAGS="$RPM_LD_FLAGS"
# Set JAVA_HOME. make uses this
. /usr/share/java-utils/java-functions
set_jvm
export JAVA_HOME

################## Build JNI bits ########################
# JNI bits depend on NativeLibraryResolver so compile that
# first and relevant Java classes with native methods
# after.
pushd annotations
  mkdir -p target/classes
  javac -d target/classes \
           src/main/java/com/redhat/thermostat/annotations/Service.java
popd
pushd config
  mkdir -p target/classes
  javac -d target/classes \
        -cp ../annotations/target/classes \
           src/main/java/com/redhat/thermostat/shared/config/NativeLibraryResolver.java \
           src/main/java/com/redhat/thermostat/shared/config/CommonPaths.java \
           src/main/java/com/redhat/thermostat/shared/config/internal/CommonPathsImpl.java \
           src/main/java/com/redhat/thermostat/shared/config/InvalidConfigurationException.java \
           src/main/java/com/redhat/thermostat/shared/locale/Translate.java \
           src/main/java/com/redhat/thermostat/shared/locale/LocalizedString.java \
           src/main/java/com/redhat/thermostat/shared/locale/internal/LocaleResources.java
popd
pushd keyring
  mkdir -p target/classes
  javac -cp ../config/target/classes:../annotations/target/classes \
        -d target/classes \
           src/main/java/com/redhat/thermostat/utils/keyring/Keyring.java \
           src/main/java/com/redhat/thermostat/utils/keyring/KeyringException.java \
           src/main/java/com/redhat/thermostat/utils/keyring/impl/KeyringImpl.java
  make all
popd
pushd agent/core
  mkdir -p target/classes
  javac -cp ../../config/target/classes:../../annotations/target/classes \
        -d target/classes \
         src/main/java/com/redhat/thermostat/agent/utils/hostname/HostName.java \
         src/main/java/com/redhat/thermostat/agent/utils/username/UserNameUtil.java \
         src/main/java/com/redhat/thermostat/agent/utils/username/UserNameLookupException.java \
         src/main/java/com/redhat/thermostat/utils/username/internal/UserNameUtilImpl.java
  make all
popd
pushd laf-utils
  mkdir -p target/classes
  javac -cp ../config/target/classes \
        -d target/classes src/main/java/com/redhat/thermostat/internal/utils/laf/gtk/GTKThemeUtils.java
  make all
popd
################## Build JNI bits (end) ##################

# This is roughly equivalent to:
#   mvn 
#     -Dthermostat.home=%{_datarootdir}/%{pkg_name} \
#    install javadoc:aggregate
# Everything after '--' is passed to plain xmvn/mvn
%mvn_build -f -- -Dthermostat.home=%{thermostat_home} \
                 -Dthermostat.system.user=thermostat \
                 -Dthermostat.system.group=thermostat \
                 -Dnetty.version=%{netty_bundle_version}.Final \
                 -Dcommons-logging.version=%{logging_bundle_version} \
                 -Dcommons-collections.version=%{collections_bundle_version} \
                 -Dcommons-codec.osgi-version=%{codec_bundle_version} \
                 -Dcommons-beanutils.version=%{beanutils_bundle_version} \
                 -Dgson.version=%{gson_bundle_version} \
                 -Dmongo-driver.osgi-version=%{mongo_bundle_version}.RELEASE \
                 -Dhttpcomponents.core.version=%{hc_core_bundle_version} \
                 -Dhttpcomponents.client.version=%{hc_client_bundle_version} \
                 -Dhttpcomponents.mime.version=%{hc_client_bundle_version} \
                 -Djansi.version=%{jansi_version} \
                 -Djcommon.osgi.version=%{jcommon_bundle_version} \
                 -Djfreechart.osgi.version=%{jfreechart_bundle_version} \
                 -Dlucene-core.bundle.symbolic-name=org.apache.lucene.core \
                 -Dlucene-analysis.bundle.symbolic-name=org.apache.lucene.analysis \
                 -Dosgi.compendium.bundle.symbolic-name=org.osgi.compendium \
                 -Dosgi.compendium.osgi-version=4.1.0 \
                 -Djgraphx.osgi.version=%{jgraphx_bundle_version}

# Make path to java so that it keeps working after updates.
# We require java >= 1.7.0
sed -i 's|^JAVA=.*|JAVA="%{jdk_base}/bin/java"|' distribution/target/image/bin/thermostat
sed -i 's|^JAVA=.*|JAVA="%{jdk_base}/bin/java"|' distribution/target/image/bin/thermostat-agent-proxy
# Fix path to tools.jar
sed -i 's|^TOOLS_JAR=.*|TOOLS_JAR="%{jdk_base}/lib/tools.jar"|' distribution/target/image/etc/thermostatrc
sed -i 's|^TOOLS_JAR=.*|TOOLS_JAR="%{jdk_base}/lib/tools.jar"|' distribution/target/image/bin/thermostat-agent-proxy


%install
#######################################################
# Thermostat core
#######################################################
mkdir -p %{buildroot}%{_bindir}
mkdir -p %{buildroot}%{_sysconfdir}/%{pkg_name}
mkdir -p %{buildroot}%{system_confdir}/sysconfig
mkdir -p %{buildroot}%{_datarootdir}/java/%{?scl_prefix}%{pkg_name}
# JNI things live there
mkdir -p %{buildroot}%{_libdir}/%{pkg_name}
mkdir -p %{buildroot}%{_jnidir}
# Systemd files live there
mkdir -p %{buildroot}%{_unitdir}
# Thermostat icon lives there
mkdir -p %{buildroot}%{_datarootdir}/icons/hicolor/scalable/apps
# Thermostat desktop lives there
mkdir -p %{buildroot}%{_datarootdir}/applications
# Example config files are in docdir
mkdir -p %{buildroot}%{_docdir}/%{pkg_name}

# Dance the xmvn install limbo. This only makes sense if %mvn_build does NOT
# have the '-i' switch.
%mvn_install

pushd distribution/target/image/libs
# JNI jars need to be in %{_jnidir}, we symlink to
# %{_libdir}/%{pkg_name} files. Files are moved to
# %{_libdir}/%{pkg_name} next.
for i in thermostat-keyring-*.jar \
    thermostat-agent-core-*.jar \
    thermostat-laf-utils-*.jar; do
  ln -s %{_libdir}/%{pkg_name}/$i %{buildroot}%{_jnidir}/$i
done
# JNI files are in %{_libdir}
mv thermostat-keyring-*.jar \
   thermostat-agent-core-*.jar \
   thermostat-laf-utils-*.jar \
   %{buildroot}%{_libdir}/%{pkg_name}
# Make native libs executable so that debuginfos get properly
# generated
chmod +x native/*.so
mv native/* %{buildroot}%{_libdir}/%{pkg_name}
popd

# Install systemd unit files
pushd distribution/packaging/shared/systemd
  sed -i 's/User=thermostat/User=root/g' thermostat-agent.service
  sed -i 's/Group=thermostat/Group=root/g' thermostat-agent.service
  # FIXME: install or not-to-install agent service running as root?
  #        Currently: Don't install.
  cp -a thermostat-storage.service %{buildroot}%{_unitdir}/
popd

# Install tmpfiles.d config file for /var/run/%{pkg_name}
mkdir -p %{buildroot}%{system_tmpfilesdir}
install -m 0644 distribution/packaging/shared/systemd/tmpfiles.d/%{pkg_name}.conf %{buildroot}%{system_tmpfilesdir}/%{pkg_name}.conf

rm -rf distribution/target/image/bin/%{pkg_name}.orig
# Remove developer setup things.
rm distribution/target/image/bin/thermostat-devsetup
rm distribution/target/image/etc/devsetup.input

# We'll install webapp later, move it out of the way
mv distribution/target/image/webapp webstorage-webapp
# Move everything else into $THERMOSTAT_HOME
cp -a distribution/target/image %{buildroot}%{thermostat_home}
# Replace with distro's version of system thermostatrc
cp %{SOURCE4} %{buildroot}%{thermostat_home}/etc/thermostatrc

# Replace jars with symlinks to installed libs
pushd %{buildroot}%{thermostat_home}/libs
  xmvn-subst .
popd
# Do the same for thermostat plugin dirs
pushd %{buildroot}%{thermostat_home}/plugins
for plugin_name in $(ls); do
  pushd $plugin_name
    xmvn-subst .
  popd
done
popd

pushd %{buildroot}%{_libdir}/%{pkg_name}
# symlink JNI jars
for i in *.jar; do
  ln -s %{_libdir}/%{pkg_name}/$i \
        %{buildroot}%{thermostat_home}/libs/$i
done
# symlink shared libs
for i in *.so; do
  ln -s %{_libdir}/%{pkg_name}/$i \
        %{buildroot}%{thermostat_home}/libs/native/$i
done
popd

# Symlink the thermostat script(s) in /usr/bin
ln -s %{_datarootdir}/%{pkg_name}/bin/thermostat \
    %{buildroot}%{_bindir}/thermostat
ln -s %{_datarootdir}/%{pkg_name}/bin/thermostat-setup \
    %{buildroot}%{_bindir}/thermostat-setup

# Move config files to /etc and symlink stuff under
# $THERMOSTAT_HOME/etc to it. Put example configs
# in docdir.
mv %{buildroot}%{thermostat_home}/etc/examples \
   %{buildroot}%{_docdir}/%{pkg_name}/
mv %{buildroot}%{thermostat_home}/etc/* \
   %{buildroot}%{_sysconfdir}/%{pkg_name}
rmdir %{buildroot}%{thermostat_home}/etc
ln -s %{_sysconfdir}/%{pkg_name}/ \
          %{buildroot}%{thermostat_home}/etc

# Install sysconfig file. This is so as to set various env vars
# which controls how thermostat behaves. In the systemd case we
# want thermostat to run as system user.
sed 's#__thermostat_home__#%{thermostat_home}/#g' %{SOURCE1} > thermostat_sysconfig.env
sed -i 's#__thermostat_user_home__#%{user_thermostat_home}#g' thermostat_sysconfig.env
cp thermostat_sysconfig.env %{buildroot}%{system_confdir}/sysconfig/%{pkg_name}

# Set up directory structure for running thermostat storage/
# thermostat agend via systemd
%{__install} -d -m 0775 %{buildroot}%{system_datadir}
echo "setup-complete.stamp for thermostat-storage systemd service" > %{buildroot}%{system_datadir}/setup-complete.stamp
%{__install} -d -m 0775 %{buildroot}%{system_cachedir}
%{__install} -d -m 0775 %{buildroot}%{system_logdir}
%{__install} -d -m 0775 %{buildroot}%{system_statedir}
# Symlink storage/agent directories so that they can be run
# as systemd services. The target directories will have
# appropriate permissions for the thermostat user to allow
# writing.
ln -s %{system_datadir} %{buildroot}%{thermostat_home}/data
ln -s %{system_statedir} %{buildroot}%{thermostat_home}/run
ln -s %{system_logdir} %{buildroot}%{thermostat_home}/logs
ln -s %{system_cachedir} %{buildroot}%{thermostat_home}/cache
#######################################################
# Thermostat web storage webapp
#######################################################
mkdir -p %{buildroot}%{thermostat_catalina_base}/webapps
pushd webstorage-webapp
# Fixup THERMOSTAT_HOME in web.xml
 sed -i '/<param-name>THERMOSTAT_HOME<[/]param-name>/,/<param-value>.*<[/]param-value>/{ s$<param-value>.*</param-value>$<param-value>%{thermostat_home}</param-value>$ }' \
 WEB-INF/web.xml
popd
cp -r webstorage-webapp %{buildroot}%{thermostat_catalina_base}/webapps/%{pkg_name}
# Provide a link to webapp in THERMOSTAT_HOME
ln -s %{thermostat_catalina_base}/webapps/%{pkg_name} %{buildroot}%{thermostat_home}/webapp
 
# Replace jars with symlinks
pushd %{buildroot}%{thermostat_catalina_base}/webapps/%{pkg_name}/WEB-INF/lib
  xmvn-subst .
popd

# We use a custom CATALINA_BASE with core tomcat directories
# symlinked. This allows us to deploy the thermostat webapp
# nicely configured without any configuration required prior
# starting tomcat via systemd.
sed 's#__jaas_config__#%{_sysconfdir}/%{pkg_name}/%{pkg_name}_jaas.conf#g' %{SOURCE3} > tomcat_service_thermostat.txt
cp tomcat_service_thermostat.txt %{buildroot}%{system_confdir}/sysconfig/tomcat@%{pkg_name}
# Create a symlinked CATALINA_BASE in order to make tomcat deploy
# the scl-ized tomcat web-app. We use our own copy of conf/server.xml in order
# to not port-conflict with system tomcat. See RHBZ#1054396
pushd %{buildroot}/%{thermostat_catalina_base}
  for i in lib logs work temp; do
    ln -s %{system_root_datadir}/tomcat/$i $i
  done
  mkdir conf
popd
# Symlink everything other than server.xml
pushd %{system_root_datadir}/tomcat/conf
  for i in *; do
    ln -s %{system_root_datadir}/tomcat/conf/$i %{buildroot}/%{thermostat_catalina_base}/conf/$i
  done
  rm %{buildroot}/%{thermostat_catalina_base}/conf/server.xml
  cp -p server.xml %{buildroot}/%{thermostat_catalina_base}/conf/server.xml
popd
pushd %{buildroot}/%{thermostat_catalina_base}/conf
  # Fix the connector port, use a different access log file name
  sed -i -e 's/<Connector port="8080"/<Connector port="%{thermostat_catalina_port}"/g' \
         -e 's/prefix="localhost_access_log."/prefix="localhost_thermostat_access_log."/g' server.xml
popd

%check
# Perform some sanity checks on paths to JAVA/TOOLS_JAR
# in important boot scripts. See RHBZ#1052992 and
# RHBZ#1053123
TOOLS_JAR="$(grep -E THERMOSTAT_EXT_BOOT_CLASSPATH='.*tools.jar' %{buildroot}/etc/thermostat/thermostatrc | cut -d= -f2 | cut -d\" -f2)"
test "${TOOLS_JAR}" = "%{jdk_base}/lib/tools.jar"
TOOLS_JAR="$(grep 'TOOLS_JAR=' %{buildroot}/%{thermostat_home}/bin/thermostat-agent-proxy | cut -d= -f2 | cut -d\" -f2)"
test "${TOOLS_JAR}" = "%{jdk_base}/lib/tools.jar"
JAVA="$(grep 'JAVA=' %{buildroot}/%{thermostat_home}/bin/thermostat | cut -d= -f2 | cut -d\" -f2)"
test "${JAVA}" = "%{jdk_base}/bin/java"
JAVA="$(grep 'JAVA=' %{buildroot}/%{thermostat_home}/bin/thermostat-agent-proxy | cut -d= -f2 | cut -d\" -f2)"
test "${JAVA}" = "%{jdk_base}/bin/java"

%pre
# add the thermostat user and group
%{_sbindir}/groupadd -r thermostat 2>/dev/null || :
%{_sbindir}/useradd -c "Thermostat system user" -g thermostat \
    -s /sbin/nologin -r -d %{thermostat_home} thermostat 2>/dev/null || :

%post
# Install but don't activate
%systemd_post %{pkg_name}-storage.service
# Required for icon cache (i.e. Thermostat icon)
/bin/touch --no-create %{_datadir}/icons/hicolor &>/dev/null || :

%preun
%systemd_preun %{pkg_name}-storage.service

%postun
# Required for icon cache (i.e. Thermostat icon)
if [ $1 -eq 0 ] ; then
    /bin/touch --no-create %{_datadir}/icons/hicolor &> /dev/null
    /usr/bin/gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || :
fi
%systemd_postun %{pkg_name}-storage.service

%posttrans
# Required for icon cache (i.e. Thermostat icon)
/usr/bin/gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || :

%files -f .mfiles
%doc LICENSE
%license COPYING
%license OFL.txt
%doc README
%doc README.api
# Own appropriate files in /etc/ part of them belong to the
# webapp sub-package
%config(noreplace) %dir %{_sysconfdir}/%{pkg_name}
# This file is only used by the systemd service running agent.
# Only root should be able to read/write to it.
%attr(0600,root,root) %config(noreplace) %{_sysconfdir}/%{pkg_name}/agent.auth
%config(noreplace) %{_sysconfdir}/%{pkg_name}/agent.properties
%config(noreplace) %{_sysconfdir}/%{pkg_name}/db.properties
%config(noreplace) %{_sysconfdir}/%{pkg_name}/logging.properties
%config %{_sysconfdir}/%{pkg_name}/bash-complete-logging.properties
%config(noreplace) %{_sysconfdir}/%{pkg_name}/plugins.d
%config(noreplace) %{_sysconfdir}/%{pkg_name}/ssl.properties
%config %{_sysconfdir}/%{pkg_name}/commands
%config %{_sysconfdir}/%{pkg_name}/osgi-export.properties
%config %{_sysconfdir}/%{pkg_name}/thermostatrc
# Required for systemd services
%config(noreplace) %{system_confdir}/sysconfig/%{pkg_name}
%{_datadir}/%{pkg_name}/etc
%{_datadir}/%{pkg_name}/bin
%{_datadir}/%{pkg_name}/lib
%{_datadir}/%{pkg_name}/libs
%{_datadir}/%{pkg_name}/plugins/host-cpu
%{_datadir}/%{pkg_name}/plugins/host-memory
%{_datadir}/%{pkg_name}/plugins/host-overview
%{_datadir}/%{pkg_name}/plugins/killvm
%{_datadir}/%{pkg_name}/plugins/notes
%{_datadir}/%{pkg_name}/plugins/numa
%{_datadir}/%{pkg_name}/plugins/storage-profile
%{_datadir}/%{pkg_name}/plugins/thread
%{_datadir}/%{pkg_name}/plugins/validate
%{_datadir}/%{pkg_name}/plugins/vm-classstat
%{_datadir}/%{pkg_name}/plugins/vm-cpu
%{_datadir}/%{pkg_name}/plugins/vm-gc
%{_datadir}/%{pkg_name}/plugins/vm-heap-analysis
%{_datadir}/%{pkg_name}/plugins/vm-io
%{_datadir}/%{pkg_name}/plugins/vm-jmx
%{_datadir}/%{pkg_name}/plugins/vm-memory
%{_datadir}/%{pkg_name}/plugins/vm-overview
%{_datadir}/%{pkg_name}/plugins/vm-profiler
%{_datadir}/%{pkg_name}/cache
%{_datadir}/%{pkg_name}/data
%{_datadir}/%{pkg_name}/logs
%{_datadir}/%{pkg_name}/run
%{_libdir}/%{pkg_name}
%{_jnidir}/thermostat-*.jar
%{_bindir}/thermostat
%{_bindir}/thermostat-setup
%{_unitdir}/%{pkg_name}-storage.service
%{system_tmpfilesdir}/%{pkg_name}.conf
# To these directories get written to when thermostat storage/agent
# run as systemd services
%attr(0770,thermostat,thermostat) %dir %{system_datadir}
%attr(0660,thermostat,thermostat) %{system_datadir}/setup-complete.stamp
%attr(0770,thermostat,thermostat) %dir %{system_cachedir}
%attr(0770,thermostat,thermostat) %dir %{system_logdir}
%attr(0770,thermostat,thermostat) %dir %{system_statedir}
%doc %{_docdir}/%{pkg_name}

%files javadoc -f .mfiles-javadoc
%doc LICENSE
%license COPYING
%license OFL.txt

%files webapp -f .mfiles-webapp
%{thermostat_catalina_base}
%config(noreplace) %{_sysconfdir}/%{pkg_name}/%{pkg_name}_jaas.conf
%config(noreplace) %{_sysconfdir}/%{pkg_name}/web-storage-service.properties
# Those files should be readable by root and tomcat only
%attr(0640,root,tomcat) %config(noreplace) %{_sysconfdir}/%{pkg_name}/%{pkg_name}-users.properties
%attr(0640,root,tomcat) %config(noreplace) %{_sysconfdir}/%{pkg_name}/%{pkg_name}-roles.properties
%attr(0640,root,tomcat) %config(noreplace) %{_sysconfdir}/%{pkg_name}/web.auth
# We need an extra file in order to make thermostat-webapp work with
# our custom CATALINA_BASE. This sets the JAAS-config option.
%config(noreplace) %{system_confdir}/sysconfig/tomcat@%{pkg_name}
%{_datadir}/%{pkg_name}/webapp
%{_datadir}/%{pkg_name}/plugins/embedded-web-endpoint

%changelog
* Wed Jul 01 2015 Severin Gehwolf <sgehwolf@redhat.com> - __MAJOR__.__MINOR__.__PATCHLEVEL__-2
- Add jgraphx dependency.
- List bash-complete-logging.properties in files section.

* Wed Jun 24 2015 Severin Gehwolf <sgehwolf@redhat.com> - __MAJOR__.__MINOR__.__PATCHLEVEL__-1
- Update to upstream __MAJOR__.__MINOR__.__PATCHLEVEL__ release.
