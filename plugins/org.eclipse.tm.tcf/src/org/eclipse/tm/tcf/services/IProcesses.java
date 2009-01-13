/*******************************************************************************
 * Copyright (c) 2007, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tm.tcf.services;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tm.tcf.protocol.IService;
import org.eclipse.tm.tcf.protocol.IToken;


/**
 * IProcesses service provides access to the target OS's process 
 * information, allows to start and terminate a process, and allows
 * to attach and detach a process for debugging. Debug services,
 * like IMemory and IRunControl, require a process to be attached
 * before they can access it. 
 */
public interface IProcesses extends IService {

    static final String NAME = "Processes";
    
    /**
     * Retrieve context info for given context ID.
     * A context corresponds to an execution thread, process, address space, etc.
     * Context IDs are valid across TCF services, so it is allowed to issue
     * 'IProcesses.getContext' command with a context that was obtained,
     * for example, from Memory service.
     * However, 'Processes.getContext' is supposed to return only process specific data,
     * If the ID is not a process ID, 'IProcesses.getContext' may not return any
     * useful information
     *    
     * @param id � context ID. 
     * @param done - call back interface called when operation is completed.
     */
    IToken getContext(String id, DoneGetContext done);

    /**
     * Client call back interface for getContext().
     */
    interface DoneGetContext {
        /**
         * Called when context data retrieval is done.
         * @param error � error description if operation failed, null if succeeded.
         * @param context � context data.
         */
        void doneGetContext(IToken token, Exception error, ProcessContext context);
    }

    /**
     * Retrieve children of given context.
     *   
     * @param parent_context_id � parent context ID. Can be null �
     * to retrieve top level of the hierarchy, or one of context IDs retrieved
     * by previous getContext or getChildren commands. 
     * @param done - call back interface called when operation is completed.
     */
    IToken getChildren(String parent_context_id, boolean attached_only, DoneGetChildren done);

    /**
     * Client call back interface for getChildren().
     */
    interface DoneGetChildren {
        /**
         * Called when context list retrieval is done.
         * @param error � error description if operation failed, null if succeeded.
         * @param context_ids � array of available context IDs.
         */
        void doneGetChildren(IToken token, Exception error, String[] context_ids);
    }
    
    /**
     * Context property names.
     */
    static final String
        /** The TCF context ID */
        PROP_ID = "ID",
        
        /** The TCF parent context ID */
        PROP_PARENTID = "ParentID",
        
        /** Is the context attached */
        PROP_ATTACHED = "Attached",
        
        /** Can terminate the context */
        PROP_CAN_TERMINATE = "CanTerminate",
        
        /** Process name. Client UI can show this name to a user */
        PROP_NAME = "Name";
    
    interface ProcessContext {
        
        /** 
         * Get context ID.
         * Same as getProperties().get(�ID�)
         */
        String getID();

        /**
         * Get parent context ID.
         * Same as getProperties().get(�ParentID�)
         */
        String getParentID();

        /**
         * Get process name.
         * Client UI can show this name to a user.
         * Same as getProperties().get(�Name�)
         */
        String getName();

        /**
         * Utility method to read context property PROP_ATTACHED.
         * Services like IRunControl, IMemory, IBreakpoints work only with attached processes.
         * @return value of PROP_ATTACHED.
         */
        boolean isAttached();

        /**
         * Utility method to read context property PROP_CAN_TERMINATE.
         * @return value of PROP_CAN_TERMINATE.
         */
        boolean canTerminate();

        /**
         * Get all available context properties.
         * @return Map 'property name' -> 'property value'
         */
        Map<String, Object> getProperties();
        
        /**
         * Attach debugger to a process.
         * Services like IRunControl, IMemory, IBreakpoints work only with attached processes.
         * @param done - call back interface called when operation is completed.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken attach(DoneCommand done);

        /**
         * Detach debugger from a process.
         * Process execution will continue without debugger supervision.
         * @param done - call back interface called when operation is completed.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken detach(DoneCommand done);
        
        /**
         * Terminate a process. 
         * @param done - call back interface called when operation is completed.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken terminate(DoneCommand done);
    
        /**
         * Send a signal to a process.
         * @param signal - signal ID.
         * @param done - call back interface called when operation is completed.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken signal(int signal, DoneCommand done);
        
        /**
         * Get list of signals that can be send to the process.
         * @param done - call back interface called when operation is completed.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken getSignalList(DoneGetSignalList done);

        /**
         * Get process signal mask.
         * Bits in the mask control how signals should be handled by debug agent.
         * @param done - call back interface called when operation is completed.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken getSignalMask(DoneGetSignalMask done);
        
        /**
         * Set process signal mask. 
         * @param intercept - bit-set of signals that should suspend execution of the process.
         * Process is suspended before it receives the signal.  
         * @param ignore - bit-set of signals that should not be passed to the process.
         * @param done - call back interface called when operation is completed.
         * @return pending command handle, can be used to cancel the command.
         */
        IToken setSignalMask(int intercept, int ignore, DoneCommand done);
    }
    
    /**
     * Call-back interface to be called when command is complete.
     */
    interface DoneCommand {
        void doneCommand(IToken token, Exception error);
    }
    
    /**
     * Call-back interface to be called when "getSignalList" command is complete.
     */
    interface DoneGetSignalList {
        void doneGetSignalList(IToken token, Collection<Map<String,Object>> list, Exception error);
    }
    
    /**
     * Signal property names used by "getSignalList" command.
     */
    static final String
        SIG_NAME = "Name",
        SIG_CODE = "Code",
        SIG_DESCRIPTION = "Description";
    
    /**
     * Call-back interface to be called when "getSignalMask" command is complete.
     */
    interface DoneGetSignalMask {
        void doneGetSignalMask(IToken token, int intercept, int ignore, Exception error);
    }
    
    /**
     * Get default set of environment variables used to start a new process.
     * @param done - call back interface called when operation is completed.
     * @return pending command handle, can be used to cancel the command.
     */
    IToken getEnvironment(DoneGetEnvironment done);
    
    /**
     * Call-back interface to be called when "getEnvironment" command is complete.
     */
    interface DoneGetEnvironment {
        void doneGetEnvironment(IToken token, Exception error, Map<String,String> environment);
    }

    /**
     * Start a new process on remote machine.
     * Clients can register ProcessesListener to receive process output.
     * 
     * @param directory - initial value of working directory for the process.
     * @param file - process image file.
     * @param command_line - command line arguments for the process.
     * @param environment - map of environment variables for the process,
     * if null then default set of environment variables will be used. 
     * @param attach - if true debugger should be attached to the process.
     * @param done - call back interface called when operation is completed.
     * @return pending command handle, can be used to cancel the command.
     */
    IToken start(String directory, String file,
            String[] command_line, Map<String,String> environment,
            boolean attach, DoneStart done);
    
    /**
     * Call-back interface to be called when "start" command is complete.
     */
    interface DoneStart {
        void doneStart(IToken token, Exception error, ProcessContext process);
    }
    
    /**
     * Add processes service event listener.
     * @param listener - event listener implementation.
     */
    void addListener(ProcessesListener listener);

    /**
     * Remove processes service event listener.
     * @param listener - event listener implementation.
     */
    void removeListener(ProcessesListener listener);

    /**
     * Process event listener is notified when a process exits or
     * sends a text to stdout/stderr.
     * Event are reported only for processes that were started by 'start' command. 
     */
    interface ProcessesListener {
        
        /**
         * Called every time a process output is received.
         * @param process_id - process context ID
         * @param stream_id - 0 stdout, 1 - stderr
         * @param data - byte array of process output data
         */
        void output(String process_id, int stream_id, byte[] data);
        
        /**
         * Called when a process exits.
         * @param process_id - process context ID
         * @param exit_code - process exit code
         */
        void exited(String process_id, int exit_code);
    }
}
