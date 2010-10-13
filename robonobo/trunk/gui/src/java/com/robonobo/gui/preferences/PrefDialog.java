package com.robonobo.gui.preferences;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.*;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.util.NetUtil;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class PrefDialog extends JDialog {
	Dimension sz = new Dimension(500, 400);
	List<PrefPanel> prefPanels = new ArrayList<PrefPanel>();
	private RobonoboFrame frame;

	public PrefDialog(RobonoboFrame frame) {
		super(frame, true);
		this.frame = frame;
		setTitle("robonobo preferences");
		setSize(sz);
		setPreferredSize(sz);
		double[][] cellSizen = { { 5, TableLayout.FILL, 80, 5, 80, 5 }, { 5, TableLayout.FILL, 5, 25, 5 } };
		setLayout(new TableLayout(cellSizen));

		JTabbedPane tabPane = new JTabbedPane();
		System.out.println("flarp b0");
		tabPane.addTab("Basic", new JScrollPane(createBasicPanel()));
		System.out.println("flarp b1");
		tabPane.addTab("Advanced", new JScrollPane(createAdvancedPanel()));
		System.out.println("flarp b2");
		tabPane.setSelectedIndex(0);
		add(tabPane, "1,1,4,1");

		JButton saveBtn = new JButton("Save");
		saveBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doSave();
			}
		});
		add(saveBtn, "2,3");

		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doCancel();
			}
		});
		add(cancelBtn, "4,3");
	}

	private JPanel createBasicPanel() {
		JPanel bp = new JPanel();
		bp.setLayout(new BoxLayout(bp, BoxLayout.Y_AXIS));

		FilePrefPanel dfPanel = new FilePrefPanel(frame, "robo.downloadDirectory", "Downloads Folder", true);
		prefPanels.add(dfPanel);
		bp.add(dfPanel);
		bp.add(vertSpacer());

		IntPrefPanel mrdPanel = new IntPrefPanel(frame, "robo.maxRunningDownloads", "Max Simultaneous Downloads", false);
		prefPanels.add(mrdPanel);
		bp.add(mrdPanel);
		bp.add(vertSpacer());

		System.out.println("flarp c0");
		Set<InetAddress> localIps = NetUtil.getLocalInetAddresses(frame.getController().getConfig().getAllowLoopbackAddress());
		System.out.println("flarp c1");
		String[] ipArr = new String[localIps.size()];
		int i = 0;
		for (InetAddress addr : localIps) {
			ipArr[i++] = addr.getHostAddress();
		}
		System.out.println("flarp c2");
		ChoicePrefPanel lipPanel = new ChoicePrefPanel(frame, "mina.localAddress", "Local IP Address", ipArr);
		prefPanels.add(lipPanel);
		bp.add(lipPanel);
		bp.add(vertSpacer());

		System.out.println("flarp c3");
		GatewayPrefPanel gPanel = new GatewayPrefPanel(frame);
		prefPanels.add(gPanel);
		bp.add(gPanel);
		bp.add(Box.createVerticalGlue());

		System.out.println("flarp c4");
		return bp;
	}

	private JPanel createAdvancedPanel() {
		JPanel ap = new JPanel();
		ap.setLayout(new BoxLayout(ap, BoxLayout.Y_AXIS));

		StringPrefPanel suPanel = new StringPrefPanel(frame, "robo.sonarServerUrl", "Node Locator URL");
		prefPanels.add(suPanel);
		ap.add(suPanel);
		ap.add(vertSpacer());

		StringPrefPanel muPanel = new StringPrefPanel(frame, "robo.metadataServerUrl", "Metadata Server URL");
		prefPanels.add(muPanel);
		ap.add(muPanel);
		ap.add(vertSpacer());

		StringPrefPanel buPanel = new StringPrefPanel(frame, "wang.bankUrl", "Bank URL");
		prefPanels.add(buPanel);
		ap.add(buPanel);
		ap.add(vertSpacer());

		BoolPrefPanel llPanel = new BoolPrefPanel(frame, "mina.locateLocalNodes", "Locate Local Nodes");
		prefPanels.add(llPanel);
		ap.add(llPanel);
		ap.add(vertSpacer());

		BoolPrefPanel lrPanel = new BoolPrefPanel(frame, "mina.locateRemoteNodes", "Locate Remote Nodes");
		prefPanels.add(lrPanel);
		ap.add(lrPanel);
		ap.add(vertSpacer());

		ap.add(Box.createVerticalGlue());
		
		return ap;
	}

	private Component vertSpacer() {
		return Box.createRigidArea(new Dimension(0, 5));
	}

	private void doCancel() {
		for (PrefPanel pp : prefPanels) {
			pp.resetValue();
		}
		setVisible(false);
	}

	private void doSave() {
		List<PrefPanel> changedPrefs = new ArrayList<PrefPanel>();
		for (PrefPanel pp : prefPanels) {
			if (pp.hasChanged())
				changedPrefs.add(pp);
		}
		if (changedPrefs.size() == 0)
			setVisible(false);
		else {
			int retVal = JOptionPane.showConfirmDialog(this, "Changing preferences requires robonobo to be restarted.  Are you ready to restart now?", "Restart robonobo?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(retVal == JOptionPane.YES_OPTION) {
				for (PrefPanel pp : changedPrefs) {
					pp.applyChanges();
				}
				frame.getController().saveConfig();
				frame.restart();
			}
		}
	}
}
