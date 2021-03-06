/*******************************************************************************
 * Copyright (c) 2009, 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/

/*
 * This header file provides definition of REG_SET - a structure that can
 * hold values of target CPU registers.
 */

#if defined(__APPLE__)
#  include <mach/thread_status.h>
#  if defined(__i386__)
     typedef x86_thread_state32_t REG_SET;
#  else
     typedef x86_thread_state64_t REG_SET;
#  endif
#endif
