package com.robonobo.mina.external.buffer;

import com.robonobo.common.exceptions.SeekInnerCalmException;

/**
 * Wrap this up into a class as it allows us to provide a simultaneous
 * lastContigPage and pageMap - otherwise another page could arrive between
 * calls to getLastContig and getPageMap, giving us erroneous values
 */
public class StreamPosition {
	private long lastContiguousPage;
	/**
	 * A bitmap specifying which pages we have after lastContiguousPage. If
	 * bit n is set, then we have page (lastContig+n)
	 */
	private int pageMap;

	public StreamPosition(long lastContiguousPage, int pageMap) {
		this.lastContiguousPage = lastContiguousPage;
		this.pageMap = pageMap;
	}

	public long getLastContiguousPage() {
		return lastContiguousPage;
	}

	public int getPageMap() {
		return pageMap;
	}
	
	public boolean includesPage(long pageNum) {
		if(pageNum <= lastContiguousPage)
			return true;
		if((pageNum - lastContiguousPage) > 31)
			return false;
		int bitPos = (int) (pageNum - lastContiguousPage - 1L);
		return ((pageMap >>> bitPos) & 0x1) > 0;
	}
	
	public long highestIncludedPage() {
		if(pageMap == 0)
			return lastContiguousPage;
		for(int i=31;i>=0;i--) {
			if(((pageMap >>> i) & 0x1) > 0)
				return lastContiguousPage + i + 1;
		}
		throw new SeekInnerCalmException();
	}
	
	@Override
	public String toString() {
		return "[lc="+lastContiguousPage+",pm="+pageMap+"]";
	}
}