AC_INIT([thermostat-keyring-native], [1.0], [thermostat@icedtea.classpath.org])
AM_INIT_AUTOMAKE([-Wall -Werror foreign])
AC_PROG_CC
PKG_CHECK_MODULES(LIBSECRET, [libsecret-1],
   [AC_SUBST([JNI_SOURCE_DIR], [libsecret])],
   [PKG_CHECK_MODULES([LIBGNOME_KEYRING], [gnome-keyring-1],
       [AC_SUBST([JNI_SOURCE_DIR], [libgnome-keyring])])
])
AC_CONFIG_FILES([
 Makefile
 src/main/native/Makefile
])
AC_OUTPUT
