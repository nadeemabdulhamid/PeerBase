/*
	File: FileShareNode.java
	Copyright 2007 by Nadeem Abdul Hamid
	
	Permission to use, copy, modify, and distribute this software and its
	documentation for any purpose and without fee is hereby granted, provided
	that the above copyright notice appear in all copies and that both the
	copyright notice and this permission notice and warranty disclaimer appear
	in supporting documentation, and that the names of the authors or their
	employers not be used in advertising or publicity pertaining to distri-
	bution of the software without specific, written prior permission.

	The authors and their employers disclaim all warranties with regard to
	this software, including all implied warranties of merchantability and
	fitness. In no event shall the authors or their employers be liable for 
	any special, indirect or consequential damages or any damages whatsoever 
	resulting from loss of use, data or profits, whether in an action of 
	contract, negligence or other tortious action, arising out of or in 
	connection with the use or performance of this software, even if 
	advised of the possibility of such damage.

	Date		Author				Changes
	Feb 07 2007	Nadeem Abdul Hamid	Created
 */


package peerbase.sample;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import peerbase.*;

/**
 * The backend implementation of a simple peer-to-peer file sharing
 * application. This class mostly implements a simple protocol by defining
 * the appropriate handlers (as inner classes) for the inherited methods 
 * of the Node class from the PeerBase system. 
 * 
 * @author Nadeem Abdul Hamid
 */
public class FileShareNode extends Node {

	/* MESSAGE TYPES */
	public static final String INSERTPEER = "JOIN";
	public static final String LISTPEER = "LIST";
	public static final String PEERNAME = "NAME";
	public static final String QUERY = "QUER";
	public static final String QRESPONSE = "RESP";
	public static final String FILEGET = "FGET";
	public static final String PEERQUIT = "QUIT";
	
	public static final String REPLY = "REPL";
	public static final String ERROR = "ERRO";

	
	/* CLASS MEMBERS */
	// mapping from filenames to peer-ids
	private Hashtable<String,String> files;
	
	
	public FileShareNode(int maxPeers, PeerInfo myInfo) {
		super(maxPeers, myInfo);
		files = new Hashtable<String,String>();
		
		this.addRouter(new Router(this));
		
		this.addHandler(INSERTPEER, new JoinHandler(this));
		this.addHandler(LISTPEER, new ListHandler(this));
		this.addHandler(PEERNAME, new NameHandler(this));
		this.addHandler(QUERY, new QueryHandler(this));
		this.addHandler(QRESPONSE, new QResponseHandler(this));
		this.addHandler(FILEGET, new FileGetHandler(this));
		this.addHandler(PEERQUIT, new QuitHandler(this));
	}
	
	
	/**
	 * Register the specified file as being locally available.
	 * @param filename the name of the file
	 */
	public void addLocalFile(String filename) {
		// delete existing entry for filename (that may be non-local)
		if (files.containsKey(filename))
			files.remove(filename);
		files.put(filename, getId());
	}
	
	
	public String[] getFileNames() {
		return files.keySet().toArray(new String[files.size()]);
	}
	
	
	public String getFileOwner(String filename) {
		return files.get(filename);
	}
	

	public void buildPeers(String host, int port, int hops) {
		LoggerUtil.getLogger().fine("build peers");
		
		if (this.maxPeersReached() || hops <= 0)
			return;
		PeerInfo pd = new PeerInfo(host, port);
		List<PeerMessage> resplist = this.connectAndSend(pd, PEERNAME, "", true);
		if (resplist == null || resplist.size() == 0)
			return;
		String peerid = resplist.get(0).getMsgData();
		LoggerUtil.getLogger().fine("contacted " + peerid);
		pd.setId(peerid);
		
		String resp = this.connectAndSend(pd, INSERTPEER,
				String.format("%s %s %d", this.getId(), this.getHost(), this.getPort()), true).get(0).getMsgType();
		if (!resp.equals(REPLY) || this.getPeerKeys().contains(peerid))
			return;
		
		this.addPeer(pd);
		
		// do recursive depth first search to add more peers
		resplist = this.connectAndSend(pd, LISTPEER, "", true);
		
		if (resplist.size() > 1) {
			resplist.remove(0);
			for (PeerMessage pm : resplist) {
				String[] data = pm.getMsgData().split("\\s");
				String nextpid = data[0];
				String nexthost = data[1];
				int nextport = Integer.parseInt(data[2]);
				if (!nextpid.equals(this.getId()))
					buildPeers(nexthost, nextport, hops - 1);
			}
		}
	}
	
	
	
	
	/* INNER CLASSES */
	
	/* msg syntax: JOIN pid host port */
	private class JoinHandler implements HandlerInterface {
		private Node peer;
		
		public JoinHandler(Node peer) { this.peer = peer; }
				
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			if (peer.maxPeersReached()) {
				LoggerUtil.getLogger().fine("maxpeers reached " + 
									peer.getMaxPeers());
				peerconn.sendData(new PeerMessage(ERROR, "Join: " +
									"too many peers"));
				return;
			}
			
			// check for correct number of arguments
			String[] data = msg.getMsgData().split("\\s");
			if (data.length != 3) {
				peerconn.sendData(new PeerMessage(ERROR, "Join: " +
									"incorrect arguments"));
				return;
			}
			
			// parse arguments into PeerInfo structure
			PeerInfo info = new PeerInfo(data[0], data[1],
										 Integer.parseInt(data[2]));
			
			if (peer.getPeer(info.getId()) != null) 
				peerconn.sendData(new PeerMessage(ERROR, "Join: " +
										"peer already inserted"));
			else if (info.getId().equals(peer.getId())) 
				peerconn.sendData(new PeerMessage(ERROR, "Join: " +
										"attempt to insert self"));
			else {
				peer.addPeer(info);
				peerconn.sendData(new PeerMessage(REPLY, "Join: " +
										"peer added: " + info.getId()));
			}
		}
	}
	
	/* msg syntax: LIST */
	private class ListHandler implements HandlerInterface {
		private Node peer;
		
		public ListHandler(Node peer) { this.peer = peer; }
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			peerconn.sendData(new PeerMessage(REPLY, 
					String.format("%d", peer.getNumberOfPeers())));
			for (String pid : peer.getPeerKeys()) {
				peerconn.sendData(new PeerMessage(REPLY, 
						String.format("%s %s %d", pid, peer.getPeer(pid).getHost(),
								peer.getPeer(pid).getPort())));
			}
		}
	}
	
	/* msg syntax: NAME */
	private class NameHandler implements HandlerInterface {
		private Node peer;
		
		public NameHandler(Node peer) { this.peer = peer; }
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			peerconn.sendData(new PeerMessage(REPLY, peer.getId()));
		}
	}

	/* msg syntax: QUER return-pid key ttl */
	private class QueryHandler implements HandlerInterface {
		private FileShareNode peer;
		public QueryHandler(FileShareNode peer) { this.peer = peer; }
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			String[] data = msg.getMsgData().split("\\s");
			if (data.length != 3) {
				peerconn.sendData(new PeerMessage(ERROR, "Query: incorrect arguments"));
				return;
			}
			
			String ret_pid = data[0].trim();
			String key = data[1].trim();
			int ttl = Integer.parseInt(data[2].trim());
			peerconn.sendData(new PeerMessage(REPLY, "Query: ACK"));
			/* After acknowledging the query, this connection will be closed. A
			 * separate thread will be started to actually perform the task of the
			 * query...
			 */ 
			
			QueryProcessor qp = new QueryProcessor(peer, ret_pid, key, ttl);
			qp.start();
		}
	}

	private class QueryProcessor extends Thread {
		private FileShareNode peer;
		private String ret_pid;
		private String key;
		private int ttl;
		
		public QueryProcessor(FileShareNode peer, String ret_pid, 
								String key, int ttl) {
			this.peer = peer;
			this.ret_pid = ret_pid;
			this.key = key;
			this.ttl = ttl;
		}
		
		public void run() {
			// search through this node's list of files for a filename 
			// containing the key
			for (String filename : peer.files.keySet()) {
				if (filename.toUpperCase().indexOf(key.toUpperCase()) >= 0) {
					String fpid = peer.files.get(filename);
					String[] data = ret_pid.split(":");
					String host = data[0];
					int port = Integer.parseInt(data[1]);
					peer.connectAndSend(new PeerInfo(ret_pid, host, port), 
							QRESPONSE, filename + " " + fpid, true);
					LoggerUtil.getLogger().fine("Sent QRESP " 
							+ new PeerInfo(ret_pid, host, port) 
							+ " " + filename + " " + fpid);
					return;
				}
			}
			
			// will only reach here if key not found... 
			// in which case propagate query to neighbors, if there is still
			// time-to-live for the query
			if (ttl > 0) {
				String msgdata = String.format("%s %s %d", ret_pid, key, ttl - 1);
				for (String nextpid : peer.getPeerKeys())
					peer.sendToPeer(nextpid, QUERY, msgdata, true);
			}
		}
	}

	/* msg syntax: RESP file-name pid */
	private class QResponseHandler implements HandlerInterface {
		@SuppressWarnings("unused")
		private Node peer;
		
		public QResponseHandler(Node peer) { this.peer = peer; }
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			String[] data = msg.getMsgData().split("\\s");
			if (data.length != 2) {
				peerconn.sendData(new PeerMessage(ERROR, "Resp: "
						+ "incorrect arguments"));
				return;
			}
			
			String filename = data[0];
			String pid = data[1];
			if (files.containsKey(filename)) {
				peerconn.sendData(new PeerMessage(ERROR, "Resp: " 
						+ "can't add duplicate file " + filename));
				return;
			}
			
			files.put(filename, pid);
			peerconn.sendData(new PeerMessage(REPLY, "Resp: "
					+ "file info added " + filename));
		}
	}

	/* msg syntax: FGET file-name */
	private class FileGetHandler implements HandlerInterface {
		@SuppressWarnings("unused")
		private Node peer;
		
		public FileGetHandler(Node peer) { this.peer = peer; }
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			String filename = msg.getMsgData().trim();
			if (!files.containsKey(filename)) {
				peerconn.sendData(new PeerMessage(ERROR, "Fget: "
						+ "file not found " + filename));
				return;
			}
			
			byte[] filedata = null;
			try {
				FileInputStream infile = new FileInputStream(filename);
				int len = infile.available();
				filedata = new byte[len];
				infile.read(filedata);
				infile.close();
			}
			catch (IOException e) {
				LoggerUtil.getLogger().info("Fget: error reading file: " + e);
				peerconn.sendData(new PeerMessage(ERROR, "Fget: "
						+ "error reading file " + filename));
				return;
			}
			
			peerconn.sendData(new PeerMessage(REPLY, filedata));
		}
	}

	/* msg syntax: QUIT pid */
	private class QuitHandler implements HandlerInterface {
		private Node peer;
		
		public QuitHandler(Node peer) { this.peer = peer; }
		
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			String pid = msg.getMsgData().trim();
			if (peer.getPeer(pid) == null) {
				peerconn.sendData(new PeerMessage(ERROR, "Quit: peer not found: " + pid));
			} 
			else {
				peer.removePeer(pid);
				peerconn.sendData(new PeerMessage(REPLY, "Quit: peer removed: " + pid));
			}
		}
	}
	
	private class Router implements RouterInterface {
		private Node peer;
		
		public Router(Node peer) {
			this.peer = peer;
		}
		
		public PeerInfo route(String peerid) {
			if (peer.getPeerKeys().contains(peerid)) return peer.getPeer(peerid);
			else return null;
		}
	}

	
}
