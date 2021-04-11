/*
	File: FileShareApp.java
	Copyright 2007 by Nadeem Abdul Hamid, Patrick Valencia

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
	Feb 07 2007	Nadeem Abdul Hamid	Add to project (from source by P. Valencia)
 */


package peerbase.sample;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.logging.Level;

import javax.swing.*;

import peerbase.*;
import peerbase.util.SimplePingStabilizer;


/**
 * The GUI for a simple peer-to-peer file sharing
 * application. 
 * 
 * @author Nadeem Abdul Hamid
 */
@SuppressWarnings("serial")
public class FileShareApp extends JFrame
{

	private static final int FRAME_WIDTH = 565, FRAME_HEIGHT = 265;

	private JPanel filesPanel, peersPanel;
	private JPanel lowerFilesPanel, lowerPeersPanel;
	private DefaultListModel filesModel, peersModel;
	private JList filesList, peersList;


	private JButton fetchFilesButton, addFilesButton, searchFilesButton;
	private JButton removePeersButton, refreshPeersButton, rebuildPeersButton;

	private JTextField addTextField, searchTextField;
	private JTextField rebuildTextField;

	private FileShareNode peer;


	private FileShareApp(String initialhost, int initialport, int maxpeers, PeerInfo mypd)
	{
		peer = new FileShareNode(maxpeers, mypd);
		peer.buildPeers(initialhost, initialport, 2);

		fetchFilesButton = new JButton("Fetch");
		fetchFilesButton.addActionListener(new FetchListener());
		addFilesButton = new JButton("Add");
		addFilesButton.addActionListener(new AddListener());
		searchFilesButton = new JButton("Search");
		searchFilesButton.addActionListener(new SearchListener());
		removePeersButton = new JButton("Remove");
		removePeersButton.addActionListener(new RemoveListener());
		refreshPeersButton = new JButton("Refresh");
		refreshPeersButton.addActionListener(new RefreshListener());
		rebuildPeersButton = new JButton("Rebuild");
		rebuildPeersButton.addActionListener(new RebuildListener());

		addTextField = new JTextField(15);
		searchTextField = new JTextField(15);
		rebuildTextField = new JTextField(15);

		setupFrame(this);

		(new Thread() { public void run() { peer.mainLoop(); }}).start();

		/*
		  Swing is not threadsafe, so can't update GUI component
		  from a thread other than the event thread
		 */
		/*
		(new Thread() { public void run() { 
			while (true) {

				new RefreshListener().actionPerformed(null);
				try { Thread.sleep(1000); } catch (InterruptedException e) { }
			}
		}}).start();
		 */
		new javax.swing.Timer(3000, new RefreshListener()).start();

		peer.startStabilizer(new SimplePingStabilizer(peer), 3000);
	}

	
	private void setupFrame(JFrame frame)
	{
		/* fixes the overlapping problem by using
		   a BorderLayout on the whole frame
		   and GridLayouts on the upper/lower panels*/

		frame = new JFrame("FileShareNode ID: <" + peer.getId() + ">");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setLayout(new BorderLayout());


		JPanel upperPanel = new JPanel();
		JPanel lowerPanel = new JPanel();
		upperPanel.setLayout(new GridLayout(1, 2));
		// allots the upper panel 2/3 of the frame height
		upperPanel.setPreferredSize(new Dimension(FRAME_WIDTH, (FRAME_HEIGHT * 2 / 3)));
		lowerPanel.setLayout(new GridLayout(1, 2));


		frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);

		filesModel = new DefaultListModel();
		filesList = new JList(filesModel);
		peersModel = new DefaultListModel();
		peersList = new JList(peersModel);
		filesPanel = initPanel(new JLabel("Available Files"), filesList);
		peersPanel = initPanel(new JLabel("Peer List"), peersList);
		lowerFilesPanel = new JPanel();
		lowerPeersPanel = new JPanel();

		filesPanel.add(fetchFilesButton);
		peersPanel.add(removePeersButton);
		peersPanel.add(refreshPeersButton);

		lowerFilesPanel.add(addTextField);
		lowerFilesPanel.add(addFilesButton);
		lowerFilesPanel.add(searchTextField);
		lowerFilesPanel.add(searchFilesButton);	

		lowerPeersPanel.add(rebuildTextField);
		lowerPeersPanel.add(rebuildPeersButton);

		upperPanel.add(filesPanel);
		upperPanel.add(peersPanel);
		lowerPanel.add(lowerFilesPanel);
		lowerPanel.add(lowerPeersPanel);

		/* by using a CENTER BorderLayout, the 
		   overlapping problem is fixed:
		   http://forum.java.sun.com/thread.jspa?threadID=551544&messageID=2698227 */

		frame.add(upperPanel, BorderLayout.NORTH);
		frame.add(lowerPanel, BorderLayout.CENTER);

		frame.setVisible(true);

	}

	
	private JPanel initPanel(JLabel textField,
			JList list)
	{
		JPanel panel = new JPanel();
		panel.add(textField);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(200, 105));
		panel.add(scrollPane);
		return panel;
	}

	
	private void updateFileList() {
		filesModel.removeAllElements();
		for (String filename : peer.getFileNames()) {
			String pid = peer.getFileOwner(filename);
			if (pid.equals(peer.getId()))
				filesModel.addElement(filename + ":(local)");
			else
				filesModel.addElement(filename + ":" + pid);
		}
	}


	private void updatePeerList(){
		peersModel.removeAllElements();
		for (String pid : peer.getPeerKeys()) {
			peersModel.addElement(pid);
		}
	}

	
	class FetchListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if(filesList.getSelectedValue() != null)
			{
				String selected = filesList.getSelectedValue().toString();
				String filename = selected.substring(0, selected.indexOf(':'));
				String pid = peer.getFileOwner(filename);
				String[] ownerData = pid.split(":");
				String host = ownerData[0];
				int port = Integer.parseInt(ownerData[1]);
				LoggerUtil.getLogger().fine("Fetching " + filename + " from " + host + ":" + port);
				PeerInfo pd = new PeerInfo(host, port);
				List<PeerMessage> resplist = peer.connectAndSend(pd, FileShareNode.FILEGET, filename, true);
				LoggerUtil.getLogger().fine("FETCH RESPONSE TYPE: " + resplist.get(0).getMsgType());
				if (resplist.size() > 0 && resplist.get(0).getMsgType().equals(FileShareNode.REPLY)) {
					try {
						FileOutputStream outfile = new FileOutputStream(filename);
						outfile.write(resplist.get(0).getMsgDataBytes());
						outfile.close();
						peer.addLocalFile(filename);
					} catch (IOException ex) {
						LoggerUtil.getLogger().warning("Fetch error: " + ex);
					}
				}

			}
		}
	}

	class AddListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String filename = addTextField.getText().trim();
			if (!filename.equals("")) {
				peer.addLocalFile(filename);
			}
			addTextField.requestFocusInWindow();
			addTextField.setText("");
			updateFileList();
		}
	}

	class SearchListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String key = searchTextField.getText().trim();
			for (String pid : peer.getPeerKeys()) {
				peer.sendToPeer(pid, FileShareNode.QUERY,
						peer.getId() + " " + key + " 4",
						true);
			}

			searchTextField.requestFocusInWindow();
			searchTextField.setText("");
		}
	}

	class RemoveListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (peersList.getSelectedValue() != null) {
				String pid = peersList.getSelectedValue().toString();
				peer.sendToPeer(pid, FileShareNode.PEERQUIT, peer.getId(), true);
				peer.removePeer(pid);
			}
		}
	}

	class RefreshListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			updateFileList();
			updatePeerList();
		}
	}

	class RebuildListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String peerid = rebuildTextField.getText().trim();
			if (!peer.maxPeersReached() && !peerid.equals("")) {
				try {
					String[] data = peerid.split(":");
					String host = data[0];
					int port = Integer.parseInt(data[1]);
					peer.buildPeers(host, port, 3);
				}
				catch (Exception ex) {
					LoggerUtil.getLogger().warning("FileShareApp: rebuild: " + ex);
				}
			}
			rebuildTextField.requestFocusInWindow();
			rebuildTextField.setText("");
		}
	}


	public static void main(String[] args) throws IOException
	{
		int port = 9000;
		if (args.length != 1) {
			System.out.println("Usage: java ... peerbase.sample.FileShareApp <host-port>");
		}
		else {
			port = Integer.parseInt(args[0]);
		}

		LoggerUtil.setHandlersLevel(Level.FINE);
		new FileShareApp("localhost", 9001, 5, new PeerInfo("localhost", port));

		/*	FileShareApp goo2 = new FileShareApp("localhost:8000", 
		 5, new PeerData("localhost", 8001)); */
	}

}
