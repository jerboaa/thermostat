CC         = gcc
JAVAH      = javah
CFLAGS     = -c -Wall -fPIC
LDFLAGS    = -fPIC -shared
COPY       = cp -a

CLASSPATH  = target/classes/
TARGET_DIR = target

INCLUDE    = -I $(TARGET_DIR) -I $(JAVA_HOME)/include/ -I $(JAVA_HOME)/include/linux
SOURCES    = src/main/native/GnomeKeyringLibraryNative.c
TARGET     = $(TARGET_DIR)/GnomeKeyringLibraryNative.c
OBJECTS    = $(TARGET:.c=.o)

EXECUTABLE = libGnomeKeyringWrapper.so

CFLAGS     += `pkg-config --cflags gnome-keyring-1`
LDFLAGS    += `pkg-config --libs gnome-keyring-1`

.phoni:
JNI_LIST = com.redhat.thermostat.utils.keyring.GnomeKeyringLibraryNative

$(JNI_LIST):
	$(JAVAH) -force -classpath $(CLASSPATH) -d $(TARGET_DIR) $(JNI_LIST)

all: $(JNI_LIST) init $(SOURCES) $(EXECUTABLE)

.phoni:
init:
	$(COPY) $(SOURCES) $(TARGET)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(OBJECTS) -o  $(TARGET_DIR)/$@ $(LDFLAGS)

.c.o:
	$(CC) $(CFLAGS) $(INCLUDE)  $< -o $@

clean-lib:
	rm $(TARGET_DIR)/$(EXECUTABLE)
	
clean-obj:
	rm $(OBJECTS) $(TARGET)
	
clean: clean-obj clean-lib
	