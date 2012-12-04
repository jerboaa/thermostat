MAVEN           ?= mvn
SKIP_TESTS      ?= false
REPO_LOC        ?= $(HOME)/.thermostat/.build/mvn_repository
MAVEN_FLAGS     ?= 
USE_VNC         ?= false
VNC             ?= vncserver
VNC_DISPLAY     ?= :10
VNC_FLAGS       ?= -SecurityTypes None

#
# Do not change anything below
#
REPO_FLAG       = -Dmaven.repo.local=$(REPO_LOC)
GOAL            = package
POM             = pom.xml

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

eclipse-test: eclipse eclipse-test-p2
ifeq ($(USE_VNC),true)
	$(VNC) $(VNC_DISPLAY) $(VNC_FLAGS)
endif
	-$(MAVEN) -f eclipse/com.redhat.thermostat.eclipse.test/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)
	-$(MAVEN) -f eclipse/com.redhat.thermostat.eclipse.test.ui/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)
ifeq ($(USE_VNC),true)
	$(VNC) -kill $(VNC_DISPLAY)
endif

eclipse-test-deps: core-install
	$(MAVEN) -f eclipse/test-deps-bundle-wrapping/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install

eclipse-test-p2: eclipse-test-deps
	$(MAVEN) -f eclipse/test-deps-p2-repository/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)

jfreechart-deps: core-install
	$(MAVEN) -f eclipse/jfreechart-bundle-wrapping/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install

jfreechart-p2: jfreechart-deps
	$(MAVEN) -f eclipse/jfreechart-p2-repository/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)

eclipse: jfreechart-p2
	$(MAVEN) -f eclipse/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)

create-repo-dir:
	mkdir -p $(REPO_LOC)

# This is basically in bash speak:
#
# for i in $(find | grep thermostat); do rm -rf $i; done;
# rm -rf .cache/tycho
clean-repo:
	done=$(shell bash -c 'cd $(REPO_LOC); for i in $$(find | grep thermostat); do rm -rf $$i; done; rm -rf .cache/tycho/; echo true' )

echo-repo:
	echo "Using private Maven repository: $(REPO_LOC)"

# We only have phony targets
.PHONY:	all core core-install eclipse-test eclipse-test-p2 eclipse-test-deps jfreechart-deps jfreechart-p2 eclipse create-repo-dir clean-repo echo-repo
