/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
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
 * Double linked list support.
 */

#ifndef D_link
#define D_link

#include <stddef.h>

typedef struct LINK LINK;

struct LINK {
    LINK * next;
    LINK * prev;
};


#define TCF_LIST_INIT(list)  {&(list), &(list)}

#define list_init(list) { \
            (list)->next = (list)->prev = (list); \
        }

#define list_is_empty(list) ((list)->next == (list) || (list)->next == NULL)

#define list_remove(item) { \
            LINK * l = item; \
            l->prev->next = l->next; \
            l->next->prev = l->prev; \
            l->next = l->prev = l; \
        }

#define list_add_first(item,list) { \
            (item)->next = (list)->next; (item)->prev = (list); \
            (list)->next->prev = (item); (list)->next = (item); \
        }

#define list_add_last(item,list) { \
            (item)->next = (list); (item)->prev = (list)->prev; \
            (list)->prev->next = (item); (list)->prev = (item); \
        }

#define list_concat(item,list) { \
            if (!list_is_empty(list)) { \
                (item)->prev->next = (list)->next; \
                (list)->next->prev = (item)->prev; \
                (item)->prev = (list)->prev; \
                (list)->prev->next = (item); \
            } \
        }

#endif /* D_link */
