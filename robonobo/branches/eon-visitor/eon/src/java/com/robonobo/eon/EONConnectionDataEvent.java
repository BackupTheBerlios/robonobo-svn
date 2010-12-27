package com.robonobo.eon;
/*
 * Eye-Of-Needle
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

public class EONConnectionDataEvent extends EONConnectionEvent {
	EonSocketAddress sourceEndPoint;
	byte[] data;
	int offset;
	int count;
	long syncNum;

	public EONConnectionDataEvent(EONConnection conn, EonSocketAddress sourceEndPoint, byte[] data, int offset, int count, long syncNum) {
		super(conn);
		this.sourceEndPoint = sourceEndPoint;
		this.data = data;
		this.offset = offset;
		this.count = count;
		this.syncNum = syncNum;
	}

	public int getCount() {
		return count;
	}

	public byte[] getData() {
		return data;
	}

	public int getOffset() {
		return offset;
	}

	public EonSocketAddress getSourceSocketAddress() {
		return sourceEndPoint;
	}

	public long getSyncNum() {
		return syncNum;
	}
}