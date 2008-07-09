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
package org.eclipse.tm.internal.tcf.debug.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.tm.internal.tcf.debug.Activator;
import org.eclipse.tm.internal.tcf.debug.actions.TCFAction;
import org.eclipse.tm.internal.tcf.debug.launch.TCFLaunchDelegate;
import org.eclipse.tm.tcf.protocol.IChannel;
import org.eclipse.tm.tcf.protocol.IPeer;
import org.eclipse.tm.tcf.protocol.IService;
import org.eclipse.tm.tcf.protocol.IToken;
import org.eclipse.tm.tcf.protocol.Protocol;
import org.eclipse.tm.tcf.services.IProcesses;
import org.eclipse.tm.tcf.services.IProcesses.ProcessContext;


public class TCFLaunch extends Launch {

    public interface Listener {

        public void onConnected(TCFLaunch launch);      

        public void onDisconnected(TCFLaunch launch);

        public void onContextActionsStart(TCFLaunch launch);
        
        public void onContextActionsDone(TCFLaunch launch);
    }

    private static final Collection<Listener> listeners = new ArrayList<Listener>();

    private IChannel channel;
    private Throwable error;
    private TCFBreakpointsStatus breakpoints_status;
    private String mode;
    private boolean connecting;
    private boolean disconnected;
    private boolean shutdown;
    private boolean last_context_exited;
    private ProcessContext process;

    private int context_action_cnt;
    private final HashMap<String,LinkedList<Runnable>> context_action_queue =
        new HashMap<String,LinkedList<Runnable>>();
    
    public TCFLaunch(ILaunchConfiguration launchConfiguration, String mode) {
        super(launchConfiguration, mode, null);
    }

    private void onConnected() {
        // The method is called when TCF channel is successfully connected.
        try {
            final Runnable done = new Runnable() {
                public void run() {
                    connecting = false;
                    for (Listener l : listeners) l.onConnected(TCFLaunch.this);
                    fireChanged();
                }
            };
            if (mode.equals(ILaunchManager.DEBUG_MODE)) {
                breakpoints_status = new TCFBreakpointsStatus(this);
                Activator.getBreakpointsModel().downloadBreakpoints(channel, new Runnable() {
                    public void run() {
                        if (channel.getState() != IChannel.STATE_OPEN) return;
                        runLaunchSequence(done);
                    }
                });
            }
            else {
                runLaunchSequence(done);
            }
        }
        catch (Exception x) {
            channel.terminate(x);
        }
    }
    
    private void onDisconnected(Throwable error) {
        // The method is called when TCF channel is closed.
        assert !disconnected;
        assert !shutdown;
        this.error = error;
        breakpoints_status = null;
        connecting = false;
        disconnected = true;
        for (Iterator<Listener> i = listeners.iterator(); i.hasNext();) {
            i.next().onDisconnected(this);
        }
        if (error != null) this.error = error;
        fireChanged();
        runShutdownSequence(new Runnable() {
            public void run() {
                shutdown = true;
                if (DebugPlugin.getDefault() != null) fireTerminate();
            }
        });
    }

    private String[] toArgsArray(String file, String cmd) {
        // Create arguments list from a command line.
        int i = 0;
        int l = cmd.length();
        List<String> arr = new ArrayList<String>();
        arr.add(file);
        for (;;) {
            while (i < l && cmd.charAt(i) == ' ') i++;
            if (i >= l) break;
            String s = null;
            if (cmd.charAt(i) == '"') {
                i++;
                StringBuffer bf = new StringBuffer();
                while (i < l) {
                    char ch = cmd.charAt(i++);
                    if (ch == '"') break;
                    if (ch == '\\' && i < l) ch = cmd.charAt(i++);
                    bf.append(ch);
                }
                s = bf.toString();
            }
            else {
                int i0 = i;
                while (i < l && cmd.charAt(i) != ' ') i++;
                s = cmd.substring(i0, i);
            }
            arr.add(s);
        }
        return arr.toArray(new String[arr.size()]);
    }
    
    @SuppressWarnings("unchecked")
    protected void runLaunchSequence(final Runnable done) {
        try {
            ILaunchConfiguration cfg = getLaunchConfiguration();
            if (cfg == null) {
                Protocol.invokeLater(done);
                return;
            }
            final String file = cfg.getAttribute(TCFLaunchDelegate.ATTR_PROGRAM_FILE, "");
            if (file.length() == 0) {
                Protocol.invokeLater(done);
                return;
            }
            final String dir = cfg.getAttribute(TCFLaunchDelegate.ATTR_WORKING_DIRECTORY, "");
            final String args = cfg.getAttribute(TCFLaunchDelegate.ATTR_PROGRAM_ARGUMENTS, "");
            final Map<String,String> env = cfg.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map)null);
            final boolean append = cfg.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
            final boolean attach = mode.equals(ILaunchManager.DEBUG_MODE);
            final IProcesses ps = channel.getRemoteService(IProcesses.class);
            if (ps == null) throw new Exception("Target does not provide Processes service");
            IProcesses.DoneGetEnvironment done_env = new IProcesses.DoneGetEnvironment() {
                public void doneGetEnvironment(IToken token, Exception error, Map<String,String> def) {
                    if (error != null) {
                        channel.terminate(error);
                        return;
                    }
                    Map<String,String> vars = new HashMap<String,String>();
                    if (append) vars.putAll(def);
                    if (env != null) vars.putAll(env);
                    ps.start(dir, file, toArgsArray(file, args), vars, attach, new IProcesses.DoneStart() {
                        public void doneStart(IToken token, Exception error, ProcessContext process) {
                            if (error != null) {
                                channel.terminate(error);
                                return;
                            }
                            TCFLaunch.this.process = process;
                            Protocol.invokeLater(done);
                        }
                    });
                }
            };
            if (append) ps.getEnvironment(done_env);
            else done_env.doneGetEnvironment(null, null, null);
        }
        catch (Exception x) {
            channel.terminate(x);
        }
    }
    
    protected void runShutdownSequence(final Runnable done) {
        done.run();
    }
    
    /*--------------------------------------------------------------------------------------------*/

    public Throwable getError() {
        return error;
    }
    
    public void setError(Throwable x) {
        if (error != null) return;
        error = x;
        if (channel != null && channel.getState() == IChannel.STATE_OPEN) {
            channel.terminate(x);
        }
        else if (!connecting) {
            disconnected = true;
        }
        fireChanged();
    }
    
    public TCFBreakpointsStatus getBreakpointsStatus() {
        return breakpoints_status;
    }

    public static void addListener(Listener listener) {
        assert Protocol.isDispatchThread();
        listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        assert Protocol.isDispatchThread();
        listeners.remove(listener);
    }

    public IChannel getChannel() {
        assert Protocol.isDispatchThread();
        return channel;
    }
    
    public IProcesses.ProcessContext getProcessContext() {
        return process;
    }
    
    public boolean isConnecting() {
        return connecting;
    }
    
    public void onLastContextRemoved() {
        last_context_exited = true;
        channel.close();
    }

    public IPeer getPeer() {
        assert Protocol.isDispatchThread();
        return channel.getRemotePeer();
    }

    public <V extends IService> V getService(Class<V> cls) {
        assert Protocol.isDispatchThread();
        return channel.getRemoteService(cls);
    }

    public boolean canDisconnect() {
        return !disconnected;
    }

    public boolean isDisconnected() {
        return disconnected;
    }
    
    public void disconnect() throws DebugException {
        try {
            Protocol.invokeLater(new Runnable() {
                public void run() {
                    if (channel != null && channel.getState() != IChannel.STATE_CLOSED) {
                        channel.close();
                    }
                }
            });
        }
        catch (IllegalStateException x) {
            disconnected = true;
        }
    }
    
    public boolean canTerminate() {
        return false;
    }

    public boolean isTerminated() {
        return disconnected;
    }

    public void terminate() throws DebugException {
    }

    public boolean isExited() {
        return last_context_exited;
    }
    
    public void launchTCF(String mode, IPeer peer) throws DebugException {
        assert Protocol.isDispatchThread();
        this.mode = mode;
        connecting = true;
        channel = peer.openChannel();
        channel.addChannelListener(new IChannel.IChannelListener() {

            public void onChannelOpened() {
                onConnected();
            }

            public void congestionLevel(int level) {
            }

            public void onChannelClosed(Throwable error) {
                channel.removeChannelListener(this);
                onDisconnected(error);
            }

        });
        assert channel.getState() == IChannel.STATE_OPENNING; 
    }
    
    public void addContextAction(TCFAction action, String context_id) {
        assert Protocol.isDispatchThread();
        LinkedList<Runnable> list = context_action_queue.get(context_id);
        if (list == null) {
            list = new LinkedList<Runnable>();
            context_action_queue.put(context_id, list);
        }
        list.add(action);
        context_action_cnt++;
        if (context_action_cnt == 1) {
            for (Listener l : listeners) l.onContextActionsStart(this);
        }
        if (list.getFirst() == action) Protocol.invokeLater(action);
    }
    
    public void removeContextAction(TCFAction action, String context_id) {
        assert Protocol.isDispatchThread();
        LinkedList<Runnable> list = context_action_queue.get(context_id);
        if (list == null) return;
        assert list.getFirst() == action;
        list.removeFirst();
        context_action_cnt--;
        if (!list.isEmpty()) {
            assert context_action_cnt > 0;
            Protocol.invokeLater(list.getFirst());
        }
        else if (context_action_cnt == 0) {
            for (Listener l : listeners) l.onContextActionsDone(this);
        }
    }
    
    public void removeContextActions(String context_id) {
        assert Protocol.isDispatchThread();
        LinkedList<Runnable> list = context_action_queue.remove(context_id);
        if (list == null) return;
        context_action_cnt -= list.size();
        if (context_action_cnt == 0) {
            for (Listener l : listeners) l.onContextActionsDone(this);
        }
    }
    
    public boolean hasPendingContextActions() {
        return context_action_cnt > 0;
    }
}
