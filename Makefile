JAVA            ?= java
JAVAC           ?= javac
MAVEN           ?= mvn
SKIP_TESTS      ?= false
REPO_LOC        ?= $(HOME)/.thermostat-build/mvn_repository/
MAVEN_FLAGS     ?= 
USE_VNC         ?= false
VNC             ?= vncserver
VNC_DISPLAY     ?= :10
VNC_FLAGS       ?= -SecurityTypes None
XSLTPROC        ?= xsltproc

#
# Do not change anything below
#
REPO_FLAG       = -Dmaven.repo.local=$(REPO_LOC)
GOAL            = package
POM             = pom.xml
ARCH            = $(shell uname -m)

ifeq ($(SKIP_TESTS),true)
	MAVEN_SKIP_TEST = -Dmaven.test.skip=true
endif

ifeq ($(USE_VNC),true)
	DISPLAY = $(VNC_DISPLAY)
endif

# Default to cleaning the local repo and building core + eclipse
# Cleaning the repo prevents things like not seeing build failures
# after bundles have been renamed.
all: clean-repo eclipse eclipse-test

core:
	$(MAVEN) -f $(POM) $(MAVEN_FLAGS) $(MAVEN_SKIP_TEST) clean $(GOAL)

core-install: create-repo-dir
	$(MAVEN) -f $(POM) $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install

copy-core-natives: core-install
	if [ "_$(ARCH)" = "_x86_64" ]; then \
        	cp keyring/target/libGnomeKeyringWrapper.so eclipse/com.redhat.thermostat.client.feature/linux_x86_64; \
	else \
		cp keyring/target/libGnomeKeyringWrapper.so eclipse/com.redhat.thermostat.client.feature/linux_x86; \
	fi

eclipse-test: eclipse eclipse-test-p2
ifeq ($(USE_VNC),true)
	$(VNC) $(VNC_DISPLAY) $(VNC_FLAGS)
endif
	-$(MAVEN) -f eclipse/com.redhat.thermostat.eclipse.test/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)
	-$(MAVEN) -f eclipse/com.redhat.thermostat.eclipse.test.ui/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)
ifeq ($(USE_VNC),true)
	$(VNC) -kill $(VNC_DISPLAY)
endif

eclipse-test-deps: copy-core-natives
	$(MAVEN) -f eclipse/test-deps-bundle-wrapping/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install

eclipse-test-p2: eclipse-test-deps
	$(MAVEN) -f eclipse/com.redhat.thermostat.eclipse.test.deps.feature/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install
	$(MAVEN) -f eclipse/test-deps-p2-repository/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)

jfreechart-deps: copy-core-natives
	$(MAVEN) -f eclipse/jfreechart-bundle-wrapping/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install

jfreechart-p2: jfreechart-deps
	$(MAVEN) -f eclipse/jfreechart-p2-repository/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)

eclipse: jfreechart-p2
	$(MAVEN) -f eclipse/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install 

create-repo-dir:
	mkdir -p $(REPO_LOC)

clean-repo:
	if [ -d $(REPO_LOC) ] ; then \
	  find $(REPO_LOC) -name '*thermostat*' -print0 | xargs -0 rm -rf ; \
	fi
	rm -rf $(REPO_LOC).cache/tycho/
	rm -rf eclipse/core-p2-repository/target
	rm -rf eclipse/test-deps-p2-repository/target
	rm -rf eclipse/jfreechart-p2-repository/target

echo-repo:
	echo "Using private Maven repository: $(REPO_LOC)"

plugin-docs: plugin_docs.html

plugin_docs.html:
	$(JAVAC) distribution/tools/MergePluginDocs.java
	$(JAVA) -cp distribution/tools MergePluginDocs > merged-plugin-docs.xml
	$(XSLTPROC) distribution/tools/plugin-docs-html.xslt merged-plugin-docs.xml > $@

# We only have phony targets
.PHONY:	all core core-install copy-core-natives eclipse-test eclipse-test-p2 eclipse-test-deps jfreechart-deps jfreechart-p2 eclipse create-repo-dir clean-repo echo-repo plugin-docs
