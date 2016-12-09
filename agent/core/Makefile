CC         = gcc
JAVAH      = javah
MYCFLAGS   = -c -Wall $(EXTRA_CFLAGS)
MYLDFLAGS  = -shared $(EXTRA_CFLAGS)
COPY       = cp -a

CLASSPATH  = target/classes/
TARGET_DIR = target

ifeq ($(OS),Windows_NT)
    JNI_PLATFORM = win32
    SO_PREFIX  =
    SO_SUFFIX  = .dll
else
    JNI_PLATFORM = linux
    SO_PREFIX  = lib
    SO_SUFFIX  = .so
    MYCFLAGS   += -fPIC
    MYLDFLAGS  += -fPIC
endif

INCLUDE    = -I $(TARGET_DIR) -I "$(JAVA_HOME)/include/" -I "$(JAVA_HOME)/include/$(JNI_PLATFORM)"

HOSTNAME_SOURCES    = src/main/native/HostName.c
HOSTNAME_TARGET     = $(TARGET_DIR)/HostName.c
HOSTNAME_OBJECTS    = $(HOSTNAME_TARGET:.c=.o)
HOSTNAME_EXECUTABLE = $(SO_PREFIX)HostNameWrapper$(SO_SUFFIX)

USERNAME_SOURCES    = src/main/native/UserNameUtilImpl.c
USERNAME_TARGET     = $(TARGET_DIR)/UserNameUtilImpl.c
USERNAME_OBJECTS    = $(USERNAME_TARGET:.c=.o)
USERNAME_EXECUTABLE = $(SO_PREFIX)UserNameUtilWrapper$(SO_SUFFIX)

WINHELPER_SOURCES    = src/main/native/WindowsHelperImpl.c
WINHELPER_TARGET     = $(TARGET_DIR)/WindowsHelperImpl.c
WINHELPER_OBJECTS    = $(WINHELPER_TARGET:.c=.o)
WINHELPER_EXECUTABLE = $(SO_PREFIX)WindowsHelperWrapper$(SO_SUFFIX)

EXECUTABLES          = $(HOSTNAME_EXECUTABLE) $(USERNAME_EXECUTABLE)

.PHONY:
JNI_LIST = com.redhat.thermostat.agent.utils.hostname.HostName\
 com.redhat.thermostat.utils.username.internal.UserNameUtilImpl

ifeq ($(OS),Windows_NT)
    EXECUTABLES  += $(WINHELPER_EXECUTABLE)
    JNI_LIST     +=  com.redhat.thermostat.agent.utils.windows.WindowsHelperImpl
endif

$(JNI_LIST):
	$(JAVAH) -force -classpath $(CLASSPATH) -d $(TARGET_DIR) $(JNI_LIST)

all: $(JNI_LIST) init $(HOSTNAME_SOURCES) $(USERNAME_SOURCES) $(WINHELPER_SOURCES) $(EXECUTABLES)

.PHONY:
init:
	$(COPY) $(HOSTNAME_SOURCES) $(HOSTNAME_TARGET)
	$(COPY) $(USERNAME_SOURCES) $(USERNAME_TARGET)
	$(COPY) $(WINHELPER_SOURCES) $(WINHELPER_TARGET)

$(HOSTNAME_EXECUTABLE): $(HOSTNAME_OBJECTS)
	$(CC) $(MYLDFLAGS) $(LDFLAGS) $(HOSTNAME_OBJECTS) $(PLATFORM_LIBS) -o $(TARGET_DIR)/$@
	
$(USERNAME_EXECUTABLE): $(USERNAME_OBJECTS)
	$(CC) $(MYLDFLAGS) $(LDFLAGS) $(USERNAME_OBJECTS) $(PLATFORM_LIBS) -o $(TARGET_DIR)/$@

$(WINHELPER_EXECUTABLE): $(WINHELPER_OBJECTS)
	$(CC) $(MYLDFLAGS) $(LDFLAGS) $(WINHELPER_OBJECTS) $(PLATFORM_LIBS) -l psapi -o $(TARGET_DIR)/$@

.c.o:
	$(CC) $(MYCFLAGS) $(CFLAGS) $(INCLUDE) $< -o $@

.cpp.o:
	$(CC) $(MYCFLAGS) $(CFLAGS) $(INCLUDE) $< -o $@


clean-lib:
	rm -f $(TARGET_DIR)/$(HOSTNAME_EXECUTABLE)
	rm -f $(TARGET_DIR)/$(USERNAME_EXECUTABLE)
	rm -f $(TARGET_DIR)/$(WINHELPER_EXECUTABLE)
	
clean-obj:
	rm -f $(HOSTNAME_OBJECTS) $(HOSTNAME_TARGET)
	rm -f $(USERNAME_OBJECTS) $(USERNAME_TARGET)
	rm -f $(WINHELPER_OBJECTS) $(WINHELPER_TARGET)
	
clean: clean-obj clean-lib
	
