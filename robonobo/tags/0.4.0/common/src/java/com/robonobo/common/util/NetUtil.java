package com.robonobo.common.util;
/*
 * Robonobo Common Utils
 * Copyright (C) 2008 Will Morton (macavity@well.com) & Ray Hilton (ray@wirestorm.net)

 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/


import java.util.*;
import java.net.*;

import com.twmacinta.util.MD5;

public class NetUtil {
	public static Set<InetAddress> getLocalInetAddresses(boolean includeLoopback) {
		Set<InetAddress> retSet = new HashSet<InetAddress>();
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch(SocketException e) {
			return retSet;
		}

		while(interfaces.hasMoreElements()) {
			NetworkInterface thisInterface = interfaces.nextElement();
			Enumeration<InetAddress> addrs = thisInterface.getInetAddresses();
			while(addrs.hasMoreElements()) {
				InetAddress addr = addrs.nextElement();
				if(!includeLoopback && addr.isLoopbackAddress())
					continue;
				retSet.add(addr);
			}
		}

		return retSet;
	}

	public static Inet4Address getFirstPublicInet4Address() {
		return getFirstPublicInet4Address(new PublicNetworkDefinition());
	}
	
	public static Inet4Address getFirstPublicInet4Address(PublicNetworkDefinition def) {
		Set<InetAddress> localAddrs = getLocalInetAddresses(true);
		Iterator<InetAddress> rator = localAddrs.iterator();
		while(rator.hasNext()) {
			InetAddress addr = (InetAddress)rator.next();
			if(addr.isLoopbackAddress())
				continue;
			if(addr instanceof Inet4Address && def.addrIsPublic(addr))
				return (Inet4Address)addr;
		}
		return null;
	}		

	public static Inet4Address getFirstNonLoopbackInet4Address() {
		Set<InetAddress> localAddrs = getLocalInetAddresses(true);
		Iterator<InetAddress> rator = localAddrs.iterator();
		while(rator.hasNext()) {
			InetAddress thisAddr = (InetAddress)rator.next();
			if(thisAddr instanceof Inet4Address && (!thisAddr.isLoopbackAddress()))
				return (Inet4Address)thisAddr;
		}
		return null;
	}
}
