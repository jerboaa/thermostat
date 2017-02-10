CC         = gcc
JAVAH      = javah
MYCFLAGS   = -c -Wall -fPIC $(EXTRA_CFLAGS)
MYLDFLAGS  = -fPIC -shared $(EXTRA_CFLAGS)
COPY       = cp -a

JNI_PLATFORM = linux
CLASSPATH  = target/classes/
TARGET_DIR = target
SO_PREFIX  = lib
SO_SUFFIX  = .so

INCLUDE    = -I $(TARGET_DIR) -I "$(JAVA_HOME)/include/" -I "$(JAVA_HOME)/include/$(JNI_PLATFORM)"

WINHELPER_SOURCES    = src/main/native/WinPipesNativeHelper.c
WINHELPER_TARGET     = $(TARGET_DIR)/WinPipesNativeHelper.c
WINHELPER_OBJECTS    = $(WINHELPER_TARGET:.c=.o)
WINHELPER_EXECUTABLE = $(SO_PREFIX)WinPipesNativeWrapper$(SO_SUFFIX)

.PHONY:
JNI_LIST = com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesNativeHelper

$(JNI_LIST):
	$(JAVAH) -force -classpath $(CLASSPATH) -d $(TARGET_DIR) $(JNI_LIST)

all: $(JNI_LIST) init $(WINHELPER_SOURCES) $(WINHELPER_EXECUTABLE)

.PHONY:
init:
	$(COPY) $(WINHELPER_SOURCES) $(WINHELPER_TARGET)

$(WINHELPER_EXECUTABLE): $(WINHELPER_OBJECTS)
	$(CC) $(MYLDFLAGS) $(LDFLAGS) $(PLATFORM_LIBS) $(WINHELPER_OBJECTS) -o $(TARGET_DIR)/$@

.cpp.o:
	$(CC) $(MYCFLAGS) $(CFLAGS) $(INCLUDE) $< -o $@

.c.o:
	$(CC) $(MYCFLAGS) $(CFLAGS) $(INCLUDE) $< -o $@

clean-lib:
	rm -f $(TARGET_DIR)/$(WINHELPER_EXECUTABLE)

clean-obj:
	rm -f $(WINHELPER_OBJECTS) $(WINHELPER_TARGET)

clean: clean-obj clean-lib


