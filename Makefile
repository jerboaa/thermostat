MAVEN           ?= mvn
SKIP_TESTS      ?= false
REPO_LOC        ?= $(HOME)/.thermostat/.build/mvn_repository
MAVEN_FLAGS     ?= 

#
# Do not change anything below
#
REPO_FLAG       = -Dmaven.repo.local=$(REPO_LOC)
GOAL            = package
POM             = pom.xml

ifeq ($(SKIP_TESTS),true)
	MAVEN_SKIP_TEST = -Dmaven.test.skip=true
endif

# Default to cleaning the local repo and building core + eclipse
# Cleaning the repo prevents things like not seeing build failures
# after bundles have been renamed.
all: clean-repo eclipse

core:
	$(MAVEN) -f $(POM) $(MAVEN_FLAGS) $(MAVEN_SKIP_TEST) clean $(GOAL)

core-install: create-repo-dir
	$(MAVEN) -f $(POM) $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install

eclipse-test-deps: core-install
	$(MAVEN) -f eclipse/test-deps-bundle-wrapping/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install

eclipse-test-p2: eclipse-test-deps
	$(MAVEN) -f eclipse/test-deps-p2-repository/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)

jfreechart-deps: core-install
	$(MAVEN) -f eclipse/jfreechart-bundle-wrapping/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean install

jfreechart-p2: jfreechart-deps
	$(MAVEN) -f eclipse/jfreechart-p2-repository/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)

eclipse: eclipse-test-p2 jfreechart-p2
	$(MAVEN) -f eclipse/pom.xml $(MAVEN_FLAGS) $(REPO_FLAG) $(MAVEN_SKIP_TEST) clean $(GOAL)

create-repo-dir:
	mkdir -p $(REPO_LOC)

clean-repo:
	find $(REPO_LOC)/com/redhat/thermostat -print0 | xargs -0 rm -rf

echo-repo:
	echo "Using private Maven repository: $(REPO_LOC)"

# We only have phony targets
.PHONY:	all core eclipse-test-deps eclipse-test-p2 eclipse create-repo-dir clean-repo echo-repo
