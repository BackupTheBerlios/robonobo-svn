package com.robonobo.gui;

import static com.robonobo.common.util.TextUtil.isNonEmpty;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.robonobo.core.api.model.Stream;

/**
 * Defines default order for streams.  Sorted first by artist, then album, then track number, then title, then stream id
 */
public class StreamComparator implements Comparator<Stream> {
	protected static Pattern trackNumPat = Pattern.compile("^(\\d+)");

	public StreamComparator() {
	}
	
	public int compare(Stream s1, Stream s2) {
		int result = compareByArtist(s1, s2);
		if(result != 0)
			return result;
		result = compareByAlbum(s1, s2);
		if(result != 0)
			return result;
		result = compareByTrackNum(s1, s2);
		if(result != 0)
			return result;
		result = compareByTitle(s1, s2);
		if(result != 0)
			return result;
		return compareByStreamId(s1, s2);
	}
	
	private static int compareByArtist(Stream s1, Stream s2) {
		return compareStrings(s1.getAttrValue("artist"), s2.getAttrValue("artist"));		
	}

	private static int compareByAlbum(Stream s1, Stream s2) {
		return compareStrings(s1.getAttrValue("album"), s2.getAttrValue("album"));
	}
	
	private static int compareByTitle(Stream s1, Stream s2) {
		return compareStrings(s1.getTitle(), s2.getTitle());
	}
	
	private static int compareByStreamId(Stream s1, Stream s2) {
		return compareStrings(s1.getStreamId(), s2.getStreamId());
	}
	
	private static int compareStrings(String s1, String s2) {
		if(isNonEmpty(s1)) {
			if(isNonEmpty(s2))
				return s1.compareTo(s2);
			else
				return 1;
		} else {
			if(isNonEmpty(s2))
				return -1;
			else
				return 0;
		}
	}
	
	private static int compareByTrackNum(Stream s1, Stream s2) {
		String s1TrackStr = s1.getAttrValue("track");
		String s2TrackStr = s2.getAttrValue("track");
		if(isNonEmpty(s1TrackStr)) {
			if(isNonEmpty(s2TrackStr))
				return compareTrackNums(s1TrackStr, s2TrackStr);
			else
				return 1;
		} else {
			if(isNonEmpty(s2TrackStr))
				return -1;
			else
				return 0;
		}
	}
	
	private static int compareTrackNums(String s1, String s2) {
		Matcher m = trackNumPat.matcher(s1);
		Integer i1 = m.find() ? Integer.parseInt(m.group()) : null;
		m = trackNumPat.matcher(s2);
		Integer i2 = m.find() ? Integer.parseInt(m.group()) : null;
		if(i1 != null) {
			if(i2 != null)
				return i1.compareTo(i2);
			else
				return 1;
		} else {
			if(i2 != null)
				return -1;
			else
				return 0;
		}
	}
}
