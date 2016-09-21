CC         = gcc
JAVAH      = javah
MYCFLAGS   = -c -Wall -fPIC $(EXTRA_CFLAGS)
MYLDFLAGS  = -fPIC -shared $(EXTRA_CFLAGS)
COPY       = cp -a

CLASSPATH  = target/classes/
TARGET_DIR = target
SO_PREFIX  = lib
SO_SUFFIX  = .so

INCLUDE    = -I $(TARGET_DIR) -I "$(JAVA_HOME)/include/" -I "$(JAVA_HOME)/include/$(JNI_PLATFORM)"
SOURCES    = src/main/native/GTKThemeUtils.c
TARGET     = $(TARGET_DIR)/GTKThemeUtils.c
OBJECTS    = $(TARGET:.c=.o)

EXECUTABLE = $(SO_PREFIX)GTKThemeUtils$(SO_SUFFIX)

MYCFLAGS   += `pkg-config --cflags gtk+-2.0`
MYCFLAGS   += `pkg-config gthread-2.0 --cflags`

MYLDFLAGS  += `pkg-config --libs gtk+-2.0`
MYLDFLAGS  += `pkg-config gthread-2.0 --libs`

.PHONY:
JNI_LIST = com.redhat.thermostat.internal.utils.laf.gtk.GTKThemeUtils

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
	
