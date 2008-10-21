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
package org.eclipse.tm.internal.tcf.services.local;

import java.lang.reflect.Method;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.tm.internal.tcf.core.LocalPeer;
import org.eclipse.tm.internal.tcf.core.RemotePeer;
import org.eclipse.tm.tcf.core.AbstractChannel;
import org.eclipse.tm.tcf.core.AbstractPeer;
import org.eclipse.tm.tcf.protocol.IChannel;
import org.eclipse.tm.tcf.protocol.IPeer;
import org.eclipse.tm.tcf.protocol.IToken;
import org.eclipse.tm.tcf.protocol.JSON;
import org.eclipse.tm.tcf.protocol.Protocol;
import org.eclipse.tm.tcf.services.ILocator;


/**
 * Locator service uses transport layer to search
 * for peers and to collect and maintain up-to-date
 * data about peer�s attributes.
 */
public class LocatorService implements ILocator {
    
    private static final int DISCOVEY_PORT = 1534;
    private static final int MAX_PACKET_SIZE = 0x1000;

    private static LocatorService locator;
    private static final Map<String,IPeer> peers = new HashMap<String,IPeer>();
    private static final ArrayList<LocatorListener> listeners = new ArrayList<LocatorListener>();
    
    private final HashSet<SubNet> subnets = new HashSet<SubNet>();
    private final ArrayList<Slave> slaves = new ArrayList<Slave>();
    private final byte[] inp_buf = new byte[MAX_PACKET_SIZE];
    private final byte[] out_buf = new byte[MAX_PACKET_SIZE];
    
    private static class SubNet {
        final int prefix_length;
        final InetAddress address;
        final InetAddress broadcast;
        
        long last_slaves_req_time;
        boolean beacon_ok;
        
        SubNet(int prefix_length, InetAddress address, InetAddress broadcast) {
            this.prefix_length = prefix_length;
            this.address = address;
            this.broadcast = broadcast;
        }
        
        boolean contains(InetAddress addr) {
            if (addr == null) return false;
            byte[] a1 = addr.getAddress();
            byte[] a2 = broadcast.getAddress();
            int i = 0;
            while (i + 8 <= prefix_length) {
                int n = i / 8;
                if (a1[n] != a2[n]) return false;
                i += 8;
            }
            while (i < prefix_length) {
                int n = i / 8;
                int m = 1 << (7 - i % 8);
                if ((a1[n] & m) != (a2[n] & m)) return false;
                i++;
            }
            return true;
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SubNet)) return false;
            SubNet x = (SubNet)o;
            return
                prefix_length == x.prefix_length &&
                broadcast.equals(x.broadcast) &&
                address.equals(x.address);
        }
        
        @Override
        public int hashCode() {
            return broadcast.hashCode();
        }
        
        @Override
        public String toString() {
            return broadcast.getHostAddress() + "/" + prefix_length;
        }
    }
    
    private static class Slave {
        final InetAddress address;
        final int port;
        
        long last_packet_time;
        long last_req_slaves_time;
        
        Slave(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
        
        @Override
        public String toString() {
            return address.getHostAddress() + ":" + port;
        }
    }

    private static boolean old_java_version; 
    private static LocalPeer local_peer;
    
    private DatagramSocket socket;
    private long last_master_packet_time;
    
    private Thread timer_thread = new Thread() {
        public void run() {
            while (true) {
                try {
                    Protocol.invokeAndWait(new Runnable() {
                        public void run() {
                            refresh_timer();
                        }
                    });
                    sleep(DATA_RETENTION_PERIOD / 4);
                }
                catch (IllegalStateException x) {
                    // TCF event dispatch is shut down
                    return;
                }
                catch (Throwable x) {
                    Protocol.log("Unhandled exception in TCF discovery listening thread", x);
                }
            }
        }
    };
    
    private Thread input_thread = new Thread() {
        public void run() {
            for (;;) {
                try {
                    final DatagramPacket p = new DatagramPacket(inp_buf, inp_buf.length);
                    socket.receive(p);
                    Protocol.invokeAndWait(new Runnable() {
                        public void run() {
                            handleDatagramPacket(p);
                        }
                    });
                }
                catch (IllegalStateException x) {
                    // TCF event dispatch is shutdown
                    return;
                }
                catch (Exception x) {
                    Protocol.log("Cannot read from datagram socket", x);
                }
            }
        }
    };
    
    public LocatorService() {
        locator = this;
        try {
            out_buf[0] = 'T';
            out_buf[1] = 'C';
            out_buf[2] = 'F';
            out_buf[3] = '1';
            out_buf[4] = 0;
            out_buf[5] = 0;
            out_buf[6] = 0;
            out_buf[7] = 0;
            try {
                socket = new DatagramSocket(DISCOVEY_PORT);
            }
            catch (BindException x) {
                socket = new DatagramSocket();
            }
            socket.setBroadcast(true);
            input_thread.setName("TCF Locator Receiver");
            timer_thread.setName("TCF Locator Timer");
            input_thread.start();
            timer_thread.start();
            listeners.add(new LocatorListener() {

                public void peerAdded(IPeer peer) {
                    sendPeerInfo(peer, null, 0);
                }

                public void peerChanged(IPeer peer) {
                    sendPeerInfo(peer, null, 0);
                }

                public void peerHeartBeat(String id) {
                }

                public void peerRemoved(String id) {
                }
            });
            refreshSubNetList();
            sendPeerRequest(null, 0);
        }
        catch (Exception x) {
            Protocol.log("Cannot open UDP socket for TCF discovery protocol", x);
        }
    }

    public static LocalPeer getLocalPeer() {
        return local_peer;
    }
    
    public static Collection<LocatorListener> getListeners() {
        return listeners;
    }

    public static void addPeer(AbstractPeer peer) {
        assert peers.get(peer.getID()) == null;
        if (peer instanceof LocalPeer) local_peer = (LocalPeer)peer;
        peers.put(peer.getID(), peer);
        peer.sendPeerAddedEvent();
    }

    public static void removePeer(AbstractPeer peer) {
        String id = peer.getID();
        assert peers.get(id) == peer;
        peers.remove(id);
        peer.sendPeerRemovedEvent();
    }

    public static void channelStarted(final AbstractChannel channel) {
        channel.addCommandServer(locator, new IChannel.ICommandServer() {
            public void command(IToken token, String name, byte[] data) {
                locator.command(channel, token, name, data);
            }
        });
    }

    private void command(AbstractChannel channel, IToken token, String name, byte[] data) {
        try {
            if (name.equals("redirect")) {
                // String peer_id = (String)JSON.parseSequence(data)[0];
                // TODO: perform local ILocator.redirect
                channel.sendResult(token, JSON.toJSONSequence(new Object[]{ null }));
            }
            else if (name.equals("sync")) {
                channel.sendResult(token, null);
            }
            else if (name.equals("getPeers")) {
                channel.sendResult(token, JSON.toJSONSequence(new Object[]{ null, peers.values().toArray()}));
            }
            else {
                channel.terminate(new Exception("Illegal command: " + name));
            }
        }
        catch (Throwable x) {
            channel.terminate(x);
        }
    }
    
    private void refresh_timer() {
        long time = System.currentTimeMillis();
        /* Cleanup slave table */
        if (slaves.size() > 0) {
            int i = 0;
            while (i < slaves.size()) {
                Slave s = slaves.get(i);
                if (s.last_packet_time + DATA_RETENTION_PERIOD < time) {
                    slaves.remove(i);
                }
                else {
                    i++;
                }
            }
        }
        /* Cleanup peers table */
        ArrayList<RemotePeer> l = null;
        for (IPeer p : peers.values()) {
            if (p instanceof RemotePeer) {
                RemotePeer r = (RemotePeer)p;
                if (r.getLastUpdateTime() + DATA_RETENTION_PERIOD < time) {
                    if (l == null) l = new ArrayList<RemotePeer>();
                    l.add(r);
                }
            }
        }
        if (l != null) {
            for (RemotePeer p : l) p.dispose();
        }
        /* Try to become a master */
        if (socket.getLocalPort() != DISCOVEY_PORT && last_master_packet_time + DATA_RETENTION_PERIOD / 2 <= time) {
            DatagramSocket s0 = socket;
            DatagramSocket s1 = null;
            try {
                s1 = new DatagramSocket(DISCOVEY_PORT);
                s1.setBroadcast(true);
                socket = s1;
                s0.close();
            }
            catch (Throwable x) {
            }
        }
        refreshSubNetList();
        for (SubNet subnet : subnets) subnet.beacon_ok = false;
        for (IPeer peer : peers.values()) sendPeerInfo(peer, null, 0);
        if (socket.getLocalPort() != DISCOVEY_PORT) sendSlavesBeacon();
    }
    
    private Slave addSlave(InetAddress addr, int port) {
        long time = System.currentTimeMillis();
        for (Slave s : slaves) {
            if (s.port == port && s.address.equals(addr)) {
                s.last_packet_time = time;
                return s;
            }
        }
        Slave s = new Slave(addr, port);
        s.last_packet_time = time;
        sendPeerRequest(addr, port);
        sendSlavesInfo(s);
        slaves.add(s);
        return s;
    }
    
    private void refreshSubNetList() {
        HashSet<SubNet> set = new HashSet<SubNet>(); 
        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
                NetworkInterface f = e.nextElement();
                /* TODO: Class InterfaceAddress does not exists in Java versions before 1.6.
                 * Fix the code below when support for old Java versions is not needed any more.
                 */
                try {
                    Method m0 = f.getClass().getMethod("getInterfaceAddresses");
                    for (Object ia : (List<?>)m0.invoke(f)) {
                        Method m1 = ia.getClass().getMethod("getNetworkPrefixLength");
                        Method m2 = ia.getClass().getMethod("getAddress");
                        Method m3 = ia.getClass().getMethod("getBroadcast");
                        set.add(new SubNet((Short)m1.invoke(ia), (InetAddress)m2.invoke(ia), (InetAddress)m3.invoke(ia)));
                    }
                }
                catch (Exception x) {
                    if (!old_java_version) {
                        Protocol.log("TCF dicovery needs Java 6 or better", x);
                        old_java_version = true;
                    }
                    Enumeration<InetAddress> n = f.getInetAddresses();
                    while (n.hasMoreElements()) {
                        InetAddress addr = n.nextElement();
                        byte[] buf = addr.getAddress();
                        if (buf.length != 4) {
                            // TODO: Support IPv6
                            continue;
                        }
                        buf[3] = (byte)255;
                        try {
                            set.add(new SubNet(24, addr, InetAddress.getByAddress(buf)));
                        }
                        catch (UnknownHostException y) {
                        }
                    }
                }
            }
        }
        catch (SocketException x) {
            Protocol.log("Cannot get list of network interfaces", x);
        }
        for (Iterator<SubNet> i = subnets.iterator(); i.hasNext();) {
            SubNet s = i.next();
            if (set.contains(s)) continue;
            i.remove();
        }
        for (Iterator<SubNet> i = set.iterator(); i.hasNext();) {
            SubNet s = i.next();
            if (subnets.contains(s)) continue;
            subnets.add(s);
        }
    }
    
    private void sendPeerRequest(InetAddress addr, int port) {
        try {
            out_buf[4] = CONF_REQ_INFO;
            if (addr == null) {
                for (SubNet subnet : subnets) {
                    socket.send(new DatagramPacket(out_buf, 8, subnet.broadcast, DISCOVEY_PORT));
                }
                for (Slave slave : slaves) {
                    socket.send(new DatagramPacket(out_buf, 8, slave.address, slave.port));
                }
            }
            else {
                socket.send(new DatagramPacket(out_buf, 8, addr, port));
            }
        }
        catch (Exception x) {
            Protocol.log("Cannot send datagram packet", x);
        }
    }
    
    private void sendPeerInfo(IPeer peer, InetAddress addr, int port) {
        if (peer instanceof RemotePeer) return;
        Map<String,String> attrs = peer.getAttributes();
        if (attrs.get(IPeer.ATTR_IP_HOST) == null) return;
        if (attrs.get(IPeer.ATTR_IP_PORT) == null) return;
        try {
            out_buf[4] = CONF_PEER_INFO;
            int i = 8;
            for (String key : attrs.keySet()) {
                String s = key + "=" + attrs.get(key);
                byte[] bt = s.getBytes("UTF-8");
                if (i + bt.length + 1 > out_buf.length) break; 
                System.arraycopy(bt, 0, out_buf, i, bt.length);
                i += bt.length;
                out_buf[i++] = 0;
            }
            
            InetAddress peer_addr = InetAddress.getByName(attrs.get(IPeer.ATTR_IP_HOST));
            for (SubNet subnet : subnets) {
                if (addr != null && !subnet.contains(addr)) continue;
                if (!subnet.contains(peer_addr)) continue;
                if (addr == null) {
                    socket.send(new DatagramPacket(out_buf, i, subnet.broadcast, DISCOVEY_PORT));
                    for (Slave slave : slaves) {
                        if (!subnet.contains(slave.address)) continue;
                        socket.send(new DatagramPacket(out_buf, out_buf.length, slave.address, slave.port));
                    }
                    subnet.beacon_ok = true;
                }
                else {
                    socket.send(new DatagramPacket(out_buf, i, addr, port));
                }
            }
        }
        catch (Exception x) {
            Protocol.log("Cannot send datagram packet", x);
        }
    }
    
    private void sendSlavesRequest(InetAddress addr, int port) {
        try {
            out_buf[4] = CONF_REQ_SLAVES;
            socket.send(new DatagramPacket(out_buf, 8, addr, port));
        }
        catch (Exception x) {
            Protocol.log("Cannot send datagram packet", x);
        }
    }
    
    private void sendSlavesInfo(Slave x) {
        try {
            long time = System.currentTimeMillis();
            out_buf[4] = CONF_SLAVES_INFO;
            int i = 8;
            if (x != null) {
                String s = x.port + ":" + x.address.getHostAddress();
                byte[] bt = s.getBytes("UTF-8");
                System.arraycopy(bt, 0, out_buf, i, bt.length);
                i += bt.length;
                out_buf[i++] = 0;
            }
            for (Slave y : slaves) {
                if (y.last_req_slaves_time + DATA_RETENTION_PERIOD < time) continue;
                socket.send(new DatagramPacket(out_buf, i, y.address, y.port));
            }
        }
        catch (Exception z) {
            Protocol.log("Cannot send datagram packet", z);
        }
    }
    
    private void sendSlavesInfo(InetAddress addr, int port) {
        try {
            out_buf[4] = CONF_SLAVES_INFO;
            int i = 8;
            for (SubNet n : subnets) {
                if (!n.contains(addr)) continue;
                for (Slave x : slaves) {
                    if (x.port == port && x.address.equals(addr)) continue;
                    if (!n.contains(x.address)) continue;
                    String s = x.port + ":" + x.address.getHostAddress();
                    byte[] bt = s.getBytes("UTF-8");
                    if (i + bt.length >= out_buf.length) {
                        socket.send(new DatagramPacket(out_buf, i, addr, port));
                        i = 8;
                    }
                    System.arraycopy(bt, 0, out_buf, i, bt.length);
                    i += bt.length;
                    out_buf[i++] = 0;
                }
            }
            if (i > 8) socket.send(new DatagramPacket(out_buf, i, addr, port));
        }
        catch (Exception x) {
            Protocol.log("Cannot send datagram packet", x);
        }
    }
    
    private void sendSlavesBeacon() {
        for (SubNet subnet : subnets) {
            if (subnet.beacon_ok) continue;
            try {
                out_buf[4] = CONF_SLAVES_INFO;
                socket.send(new DatagramPacket(out_buf, 8, subnet.broadcast, DISCOVEY_PORT));
            }
            catch (Exception x) {
                Protocol.log("Cannot send datagram packet", x);
            }
        }
    }
    
    private boolean isRemote(DatagramPacket p) {
        if (p.getPort() != socket.getLocalPort()) return true;
        InetAddress addr = p.getAddress();
        for (SubNet s : subnets) {
            if (s.address.equals(addr)) return false;
        }
        return true;
    }
    
    private void handleDatagramPacket(DatagramPacket p) {
        try {
            long time = System.currentTimeMillis();
            byte[] buf = p.getData();
            int len = p.getLength();
            if (len < 8) return;
            if (buf[0] != 'T') return;
            if (buf[1] != 'C') return;
            if (buf[2] != 'F') return;
            if (buf[3] != '1') return;
            if (isRemote(p)) {
                Slave sl = null;
                if (p.getPort() != DISCOVEY_PORT) {
                    sl = addSlave(p.getAddress(), p.getPort());
                }
                switch (buf[4]) {
                case CONF_PEER_INFO:
                    handlePeerInfoPacket(p);
                    break;
                case CONF_REQ_INFO:
                    handleReqInfoPacket(p);
                    break;
                case CONF_SLAVES_INFO:
                    handleSlavesInfoPacket(p);
                    break;
                case CONF_REQ_SLAVES:
                    sl.last_req_slaves_time = sl.last_packet_time;
                    handleReqSlavesPacket(p);
                    break;
                }
                if (socket.getLocalPort() != DISCOVEY_PORT) {
                    for (SubNet s : subnets) {
                        if (!s.contains(p.getAddress())) continue;
                        if (s.last_slaves_req_time + DATA_RETENTION_PERIOD / 3 <= time) {
                            sendSlavesRequest(p.getAddress(), p.getPort());
                            s.last_slaves_req_time = time;
                        }
                        if (s.address.equals(p.getAddress())) last_master_packet_time = time;
                    }
                }
            }
        }
        catch (Throwable x) {
            Protocol.log("Invalid datagram packet received", x);
        }
    }
    
    private void handlePeerInfoPacket(DatagramPacket p) {
        try {
            Map<String,String> map = new HashMap<String,String>();
            String s = new String(p.getData(), 8, p.getLength() - 8, "UTF-8");
            int l = s.length();
            int i = 0;
            while (i < l) {
                int i0 = i;
                while (i < l && s.charAt(i) != '=' && s.charAt(i) != 0) i++;
                int i1 = i;
                if (i < l && s.charAt(i) == '=') i++;
                int i2 = i;
                while (i < l && s.charAt(i) != 0) i++;
                int i3 = i;
                if (i < l && s.charAt(i) == 0) i++;
                String key = s.substring(i0, i1);
                String val = s.substring(i2, i3);
                map.put(key, val);
            }
            String id = map.get(IPeer.ATTR_ID);
            if (id == null) throw new Exception("Invalid peer info: no ID");
            IPeer peer = peers.get(id);
            if (peer instanceof RemotePeer) {
                ((RemotePeer)peer).updateAttributes(map);
            }
            else if (peer == null) {
                new RemotePeer(map);
            }
        }
        catch (Exception x) {
            Protocol.log("Invalid datagram packet received", x);
        }
    }

    private void handleReqInfoPacket(DatagramPacket p) {
        InetAddress addr = p.getAddress();
        int port = p.getPort();
        for (IPeer peer : peers.values()) {
            sendPeerInfo(peer, addr, port);
        }
    }

    private void handleSlavesInfoPacket(DatagramPacket p) {
        try {
            String s = new String(p.getData(), 8, p.getLength() - 8, "UTF-8");
            int l = s.length();
            int i = 0;
            while (i < l) {
                int i0 = i;
                while (i < l && s.charAt(i) != ':' && s.charAt(i) != 0) i++;
                int i1 = i;
                if (i < l && s.charAt(i) == ':') i++;
                int i2 = i;
                while (i < l && s.charAt(i) != 0) i++;
                int i3 = i;
                if (i < l && s.charAt(i) == 0) i++;
                int port = Integer.parseInt(s.substring(i0, i1));
                if (port != DISCOVEY_PORT) {
                    String host = s.substring(i2, i3);
                    addSlave(InetAddress.getByName(host), port);
                }
            }
        }
        catch (Exception x) {
            Protocol.log("Invalid datagram packet received", x);
        }
    }
    
    private void handleReqSlavesPacket(DatagramPacket p) {
        sendSlavesInfo(p.getAddress(), p.getPort());
    }
    
    /*----------------------------------------------------------------------------------*/

    public static LocatorService getLocator() {
        return locator;
    }

    public String getName() {
        return NAME;
    }

    public Map<String,IPeer> getPeers() {
        return peers;
    }

    public IToken redirect(String peer_id, DoneRedirect done) {
        throw new Error("Channel redirect cannot be done on local peer");
    }

    public IToken sync(DoneSync done) {
        throw new Error("Channel sync cannot be done on local peer");
    }

    public void addListener(LocatorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LocatorListener listener) {
        listeners.remove(listener);
    }
}
