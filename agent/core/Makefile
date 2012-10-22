CC         = gcc
JAVAH      = javah
MYCFLAGS   = -c -Wall -fPIC
MYLDFLAGS  = -fPIC -shared
COPY       = cp -a

CLASSPATH  = target/classes/
TARGET_DIR = target

INCLUDE    = -I $(TARGET_DIR) -I $(JAVA_HOME)/include/ -I $(JAVA_HOME)/include/linux
SOURCES    = src/main/native/HostName.c
TARGET     = $(TARGET_DIR)/HostName.c
OBJECTS    = $(TARGET:.c=.o)

EXECUTABLE = libHostNameWrapper.so

.PHONY:
JNI_LIST = com.redhat.thermostat.utils.hostname.HostName

$(JNI_LIST):
	$(JAVAH) -force -classpath $(CLASSPATH) -d $(TARGET_DIR) $(JNI_LIST)

all: $(JNI_LIST) init $(SOURCES) $(EXECUTABLE)

.PHONY:
init:
	$(COPY) $(SOURCES) $(TARGET)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(OBJECTS) -o $(TARGET_DIR)/$@ $(MYLDFLAGS) $(LDFLAGS)

.c.o:
	$(CC) $(MYCFLAGS) $(CFLAGS) $(INCLUDE) $< -o $@

clean-lib:
	rm $(TARGET_DIR)/$(EXECUTABLE)
	
clean-obj:
	rm $(OBJECTS) $(TARGET)
	
clean: clean-obj clean-lib
	
