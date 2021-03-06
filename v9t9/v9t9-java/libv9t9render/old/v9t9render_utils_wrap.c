/*
  v9t9render_utils_wrap.c

  (c) 2010-2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
/* -----------------------------------------------------------------------------
 *  This section contains generic SWIG labels for method/variable
 *  declarations/attributes, and other compiler dependent labels.
 * ----------------------------------------------------------------------------- */

/* template workaround for compilers that cannot correctly implement the C++ standard */
#ifndef SWIGTEMPLATEDISAMBIGUATOR
# if defined(__SUNPRO_CC) && (__SUNPRO_CC <= 0x560)
#  define SWIGTEMPLATEDISAMBIGUATOR template
# elif defined(__HP_aCC)
/* Needed even with `aCC -AA' when `aCC -V' reports HP ANSI C++ B3910B A.03.55 */
/* If we find a maximum version that requires this, the test would be __HP_aCC <= 35500 for A.03.55 */
#  define SWIGTEMPLATEDISAMBIGUATOR template
# else
#  define SWIGTEMPLATEDISAMBIGUATOR
# endif
#endif

/* inline attribute */
#ifndef SWIGINLINE
# if defined(__cplusplus) || (defined(__GNUC__) && !defined(__STRICT_ANSI__))
#   define SWIGINLINE inline
# else
#   define SWIGINLINE
# endif
#endif

/* attribute recognised by some compilers to avoid 'unused' warnings */
#ifndef SWIGUNUSED
# if defined(__GNUC__)
#   if !(defined(__cplusplus)) || (__GNUC__ > 3 || (__GNUC__ == 3 && __GNUC_MINOR__ >= 4))
#     define SWIGUNUSED __attribute__ ((__unused__)) 
#   else
#     define SWIGUNUSED
#   endif
# elif defined(__ICC)
#   define SWIGUNUSED __attribute__ ((__unused__)) 
# else
#   define SWIGUNUSED 
# endif
#endif

#ifndef SWIG_MSC_UNSUPPRESS_4505
# if defined(_MSC_VER)
#   pragma warning(disable : 4505) /* unreferenced local function has been removed */
# endif 
#endif

#ifndef SWIGUNUSEDPARM
# ifdef __cplusplus
#   define SWIGUNUSEDPARM(p)
# else
#   define SWIGUNUSEDPARM(p) p SWIGUNUSED 
# endif
#endif

/* internal SWIG method */
#ifndef SWIGINTERN
# define SWIGINTERN static SWIGUNUSED
#endif

/* internal inline SWIG method */
#ifndef SWIGINTERNINLINE
# define SWIGINTERNINLINE SWIGINTERN SWIGINLINE
#endif

/* exporting methods */
#if (__GNUC__ >= 4) || (__GNUC__ == 3 && __GNUC_MINOR__ >= 4)
#  ifndef GCC_HASCLASSVISIBILITY
#    define GCC_HASCLASSVISIBILITY
#  endif
#endif

#ifndef SWIGEXPORT
# if defined(_WIN32) || defined(__WIN32__) || defined(__CYGWIN__)
#   if defined(STATIC_LINKED)
#     define SWIGEXPORT
#   else
#     define SWIGEXPORT __declspec(dllexport)
#   endif
# else
#   if defined(__GNUC__) && defined(GCC_HASCLASSVISIBILITY)
#     define SWIGEXPORT __attribute__ ((visibility("default")))
#   else
#     define SWIGEXPORT
#   endif
# endif
#endif

/* calling conventions for Windows */
#ifndef SWIGSTDCALL
# if defined(_WIN32) || defined(__WIN32__) || defined(__CYGWIN__)
#   define SWIGSTDCALL __stdcall
# else
#   define SWIGSTDCALL
# endif 
#endif

/* Deal with Microsoft's attempt at deprecating C standard runtime functions */
#if !defined(SWIG_NO_CRT_SECURE_NO_DEPRECATE) && defined(_MSC_VER) && !defined(_CRT_SECURE_NO_DEPRECATE)
# define _CRT_SECURE_NO_DEPRECATE
#endif

/* Deal with Microsoft's attempt at deprecating methods in the standard C++ library */
#if !defined(SWIG_NO_SCL_SECURE_NO_DEPRECATE) && defined(_MSC_VER) && !defined(_SCL_SECURE_NO_DEPRECATE)
# define _SCL_SECURE_NO_DEPRECATE
#endif



/* Fix for jlong on some versions of gcc on Windows */
#if defined(__GNUC__) && !defined(__INTELC__)
  typedef long long __int64;
#endif

/* Fix for jlong on 64-bit x86 Solaris */
#if defined(__x86_64)
# ifdef _LP64
#   undef _LP64
# endif
#endif

#include <jni.h>
#include <stdlib.h>
#include <string.h>


/* Support for throwing Java exceptions */
typedef enum {
  SWIG_JavaOutOfMemoryError = 1, 
  SWIG_JavaIOException, 
  SWIG_JavaRuntimeException, 
  SWIG_JavaIndexOutOfBoundsException,
  SWIG_JavaArithmeticException,
  SWIG_JavaIllegalArgumentException,
  SWIG_JavaNullPointerException,
  SWIG_JavaDirectorPureVirtual,
  SWIG_JavaUnknownError
} SWIG_JavaExceptionCodes;

typedef struct {
  SWIG_JavaExceptionCodes code;
  const char *java_exception;
} SWIG_JavaExceptions_t;


static void SWIGUNUSED SWIG_JavaThrowException(JNIEnv *jenv, SWIG_JavaExceptionCodes code, const char *msg) {
  jclass excep;
  static const SWIG_JavaExceptions_t java_exceptions[] = {
    { SWIG_JavaOutOfMemoryError, "java/lang/OutOfMemoryError" },
    { SWIG_JavaIOException, "java/io/IOException" },
    { SWIG_JavaRuntimeException, "java/lang/RuntimeException" },
    { SWIG_JavaIndexOutOfBoundsException, "java/lang/IndexOutOfBoundsException" },
    { SWIG_JavaArithmeticException, "java/lang/ArithmeticException" },
    { SWIG_JavaIllegalArgumentException, "java/lang/IllegalArgumentException" },
    { SWIG_JavaNullPointerException, "java/lang/NullPointerException" },
    { SWIG_JavaDirectorPureVirtual, "java/lang/RuntimeException" },
    { SWIG_JavaUnknownError,  "java/lang/UnknownError" },
    { (SWIG_JavaExceptionCodes)0,  "java/lang/UnknownError" } };
  const SWIG_JavaExceptions_t *except_ptr = java_exceptions;

  while (except_ptr->code != code && except_ptr->code)
    except_ptr++;

  (*jenv)->ExceptionClear(jenv);
  excep = (*jenv)->FindClass(jenv, except_ptr->java_exception);
  if (excep)
    (*jenv)->ThrowNew(jenv, excep, msg);
}


/* Contract support */

#define SWIG_contract_assert(nullreturn, expr, msg) if (!(expr)) {SWIG_JavaThrowException(jenv, SWIG_JavaIllegalArgumentException, msg); return nullreturn; } else


#include "render.h"


#ifdef __cplusplus
extern "C" {
#endif

SWIGEXPORT void JNICALL Java_v9t9_jni_v9t9render_utils_V9t9RenderUtilsJNI_scaleImage(JNIEnv *jenv, jclass jcls, jbyteArray jarg1, jbyteArray jarg2, jint jarg3, jint jarg4, jint jarg5, jint jarg6, jint jarg7, jint jarg8, jint jarg9, jint jarg10, jint jarg11, jint jarg12, jint jarg13) {
  char *arg1 = (char *) 0 ;
  char *arg2 = (char *) 0 ;
  int arg3 ;
  int arg4 ;
  int arg5 ;
  int arg6 ;
  int arg7 ;
  int arg8 ;
  int arg9 ;
  int arg10 ;
  int arg11 ;
  int arg12 ;
  int arg13 ;
  
  (void)jenv;
  (void)jcls;
  {
    arg1 = (char *) (*jenv)->GetByteArrayElements(jenv, jarg1, 0); 
  }
  {
    arg2 = (char *) (*jenv)->GetByteArrayElements(jenv, jarg2, 0); 
  }
  arg3 = (int)jarg3; 
  arg4 = (int)jarg4; 
  arg5 = (int)jarg5; 
  arg6 = (int)jarg6; 
  arg7 = (int)jarg7; 
  arg8 = (int)jarg8; 
  arg9 = (int)jarg9; 
  arg10 = (int)jarg10; 
  arg11 = (int)jarg11; 
  arg12 = (int)jarg12; 
  arg13 = (int)jarg13; 
  scaleImage(arg1,(char const *)arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10,arg11,arg12,arg13);
  {
    (*jenv)->ReleaseByteArrayElements(jenv, jarg1, (jbyte *) arg1, 0); 
  }
  {
    (*jenv)->ReleaseByteArrayElements(jenv, jarg2, (jbyte *) arg2, 0); 
  }
  
  
}


SWIGEXPORT void JNICALL Java_v9t9_jni_v9t9render_utils_V9t9RenderUtilsJNI_scaleImageToRGBA(JNIEnv *jenv, jclass jcls, jintArray jarg1, jbyteArray jarg2, jint jarg3, jint jarg4, jint jarg5, jint jarg6, jint jarg7, jint jarg8, jint jarg9, jint jarg10, jint jarg11, jint jarg12, jint jarg13) {
  int *arg1 = (int *) 0 ;
  char *arg2 = (char *) 0 ;
  int arg3 ;
  int arg4 ;
  int arg5 ;
  int arg6 ;
  int arg7 ;
  int arg8 ;
  int arg9 ;
  int arg10 ;
  int arg11 ;
  int arg12 ;
  int arg13 ;
  
  (void)jenv;
  (void)jcls;
  {
    if (!jarg1) {
      SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "array null");
      return ;
    }
    if ((*jenv)->GetArrayLength(jenv, jarg1) == 0) {
      SWIG_JavaThrowException(jenv, SWIG_JavaIndexOutOfBoundsException, "Array must contain at least 1 element");
      return ;
    }
    arg1 = (int *) (*jenv)->GetIntArrayElements(jenv, jarg1, 0); 
  }
  {
    arg2 = (char *) (*jenv)->GetByteArrayElements(jenv, jarg2, 0); 
  }
  arg3 = (int)jarg3; 
  arg4 = (int)jarg4; 
  arg5 = (int)jarg5; 
  arg6 = (int)jarg6; 
  arg7 = (int)jarg7; 
  arg8 = (int)jarg8; 
  arg9 = (int)jarg9; 
  arg10 = (int)jarg10; 
  arg11 = (int)jarg11; 
  arg12 = (int)jarg12; 
  arg13 = (int)jarg13; 
  scaleImageToRGBA(arg1,(char const *)arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10,arg11,arg12,arg13);
  {
    (*jenv)->ReleaseIntArrayElements(jenv, jarg1, (jint *)arg1, 0); 
  }
  {
    (*jenv)->ReleaseByteArrayElements(jenv, jarg2, (jbyte *) arg2, 0); 
  }
  
  
}


SWIGEXPORT void JNICALL Java_v9t9_jni_v9t9render_utils_V9t9RenderUtilsJNI_addNoise(JNIEnv *jenv, jclass jcls, jbyteArray jarg1, jint jarg2, jint jarg3, jint jarg4, jint jarg5, jint jarg6, jint jarg7) {
  char *arg1 = (char *) 0 ;
  int arg2 ;
  int arg3 ;
  int arg4 ;
  int arg5 ;
  int arg6 ;
  int arg7 ;
  
  (void)jenv;
  (void)jcls;
  {
    arg1 = (char *) (*jenv)->GetByteArrayElements(jenv, jarg1, 0); 
  }
  arg2 = (int)jarg2; 
  arg3 = (int)jarg3; 
  arg4 = (int)jarg4; 
  arg5 = (int)jarg5; 
  arg6 = (int)jarg6; 
  arg7 = (int)jarg7; 
  addNoise(arg1,arg2,arg3,arg4,arg5,arg6,arg7);
  {
    (*jenv)->ReleaseByteArrayElements(jenv, jarg1, (jbyte *) arg1, 0); 
  }
  
}


SWIGEXPORT void JNICALL Java_v9t9_jni_v9t9render_utils_V9t9RenderUtilsJNI_addNoiseRGBA(JNIEnv *jenv, jclass jcls, jintArray jarg1, jint jarg2, jint jarg3, jint jarg4, jint jarg5, jint jarg6, jint jarg7) {
  int *arg1 = (int *) 0 ;
  int arg2 ;
  int arg3 ;
  int arg4 ;
  int arg5 ;
  int arg6 ;
  int arg7 ;
  
  (void)jenv;
  (void)jcls;
  {
    if (!jarg1) {
      SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "array null");
      return ;
    }
    if ((*jenv)->GetArrayLength(jenv, jarg1) == 0) {
      SWIG_JavaThrowException(jenv, SWIG_JavaIndexOutOfBoundsException, "Array must contain at least 1 element");
      return ;
    }
    arg1 = (int *) (*jenv)->GetIntArrayElements(jenv, jarg1, 0); 
  }
  arg2 = (int)jarg2; 
  arg3 = (int)jarg3; 
  arg4 = (int)jarg4; 
  arg5 = (int)jarg5; 
  arg6 = (int)jarg6; 
  arg7 = (int)jarg7; 
  addNoiseRGBA(arg1,arg2,arg3,arg4,arg5,arg6,arg7);
  {
    (*jenv)->ReleaseIntArrayElements(jenv, jarg1, (jint *)arg1, 0); 
  }
  
}


#ifdef __cplusplus
}
#endif

