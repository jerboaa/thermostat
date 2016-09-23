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

HOSTNAME_SOURCES    = src/main/native/HostName.c
HOSTNAME_TARGET     = $(TARGET_DIR)/HostName.c
HOSTNAME_OBJECTS    = $(HOSTNAME_TARGET:.c=.o)
HOSTNAME_EXECUTABLE = $(SO_PREFIX)HostNameWrapper$(SO_SUFFIX)

USERNAME_SOURCES    = src/main/native/UserNameUtilImpl.c
USERNAME_TARGET     = $(TARGET_DIR)/UserNameUtilImpl.c
USERNAME_OBJECTS    = $(USERNAME_TARGET:.c=.o)
USERNAME_EXECUTABLE = $(SO_PREFIX)UserNameUtilWrapper$(SO_SUFFIX)

.PHONY:
JNI_LIST = com.redhat.thermostat.agent.utils.hostname.HostName\
 com.redhat.thermostat.utils.username.internal.UserNameUtilImpl

$(JNI_LIST):
	$(JAVAH) -force -classpath $(CLASSPATH) -d $(TARGET_DIR) $(JNI_LIST)

all: $(JNI_LIST) init $(HOSTNAME_SOURCES) $(HOSTNAME_EXECUTABLE) \
  $(USERNAME_SOURCES) $(USERNAME_EXECUTABLE)

.PHONY:
init:
	$(COPY) $(HOSTNAME_SOURCES) $(HOSTNAME_TARGET)
	$(COPY) $(USERNAME_SOURCES) $(USERNAME_TARGET)

$(HOSTNAME_EXECUTABLE): $(HOSTNAME_OBJECTS)
	$(CC) $(HOSTNAME_OBJECTS) -o $(TARGET_DIR)/$@ $(MYLDFLAGS) $(LDFLAGS) $(PLATFORM_LIBS)
	
$(USERNAME_EXECUTABLE): $(USERNAME_OBJECTS)
	$(CC) $(USERNAME_OBJECTS) -o $(TARGET_DIR)/$@ $(MYLDFLAGS) $(LDFLAGS) $(PLATFORM_LIBS)

.c.o:
	$(CC) $(MYCFLAGS) $(CFLAGS) $(INCLUDE) $< -o $@

clean-lib:
	rm -f $(TARGET_DIR)/$(HOSTNAME_EXECUTABLE)
	rm -f $(TARGET_DIR)/$(USERNAME_EXECUTABLE)
	
clean-obj:
	rm -f $(HOSTNAME_OBJECTS) $(HOSTNAME_TARGET)
	rm -f $(USERNAME_OBJECTS) $(USERNAME_TARGET)
	
clean: clean-obj clean-lib
	
