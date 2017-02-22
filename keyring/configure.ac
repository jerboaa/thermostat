AC_INIT([thermostat-keyring-native], [1.0], [thermostat@icedtea.classpath.org])
AM_INIT_AUTOMAKE([-Wall -Werror foreign])
AC_PROG_CC
PKG_CHECK_MODULES(LIBSECRET, [libsecret-1],
   [AC_SUBST([JNI_SOURCE_DIR], [libsecret])],
   [PKG_CHECK_MODULES([LIBGNOME_KEYRING], [gnome-keyring-1],
       [AC_SUBST([JNI_SOURCE_DIR], [libgnome-keyring])], AC_MSG_ERROR([No libsecret-1 or gnome-keyring-1 found. Try to install libsecret-devel or gnome-keyring. Consider adjusting the PKG_CONFIG_PATH environment variable if you installed software in a non-standard prefix. As final hack you may to set LIBSECRET/LIBGNOME_KEYRING CFLAGS and LIBS and JNI_SOURCE_DIR on your own]))
])
AC_CONFIG_FILES([
 Makefile
 src/main/native/Makefile
])
AC_OUTPUT
