/*
 * Copyright (c) 2019. Mr.Indescribable (https://github.com/Mr-indescribable).
 * Copyright (c) 2010. Stephen Connolly.
 * Copyright (c) 2006-2007. loopy project (http://loopy.sourceforge.net).
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.github.stephenc.javaisotools.loopfs.iso9660.rrip;

import java.util.List;
import java.util.ArrayList;

import com.github.stephenc.javaisotools.loopfs.iso9660.Util;
import com.github.stephenc.javaisotools.loopfs.iso9660.Constants;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.SuspField;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.exceptions.InvalidSuspField;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.exceptions.UnknownSuspFieldId;


class RripSLComponent
{
	private int flags = 0;
	private int length = 0;
	private String text = null;

	public RripSLComponent(int flags, int length, String text) {
		this.flags = flags;
		this.length = length;
		this.text = text;
	}

	public boolean isNormalLink() {
		return (this.flags & ~1) == 0;
	}

	public boolean linksToCurrentDir() {
		return (this.flags & ~1) == 2;
	}

	public boolean linksToParentDir() {
		return (this.flags & ~1) == 4;
	}

	public boolean linksToRootDir() {
		return (this.flags & ~1) == 8;
	}

	public int getLength() {
		return this.length;
	}

	public String getText() {
		return this.text;
	}

	public static List<RripSLComponent> fromComponentArea(byte[] bytes)
			throws InvalidSuspField {
		int offset = 0;
		byte[] subArray;
		RripSLComponent comp;
		List<RripSLComponent> comps = new ArrayList<RripSLComponent>();

		subArray = bytes;

		while (bytes.length - offset >= 2) {
			comp = RripSLComponent.fromByte(subArray);
			comps.add(comp);

			offset += comp.length + 2;
			subArray = Util.getBytes(bytes, offset, bytes.length - offset);
		}

		if (bytes.length - offset != 0) {
			throw new InvalidSuspField("SLComponent field has redundant bytes");
		}

		return comps;
	}

	public static RripSLComponent fromByte(byte[] bytes)
			throws InvalidSuspField {
		int flags;
		int length;
		String text;

		flags = Util.getUInt8(bytes, 1);
		length = Util.getUInt8(bytes, 2);
		text = Util.getString(bytes, 3, length);

		return new RripSLComponent(flags, length, text);
	}
}


/**
 * The SL field. (RRIP specification section 4.1)
 */
public class RripFieldSL extends SuspField
{
	private int flags;
	private String pathname;

	public RripFieldSL() {
	}

	public RripFieldSL(byte[] bytes, int bp) throws InvalidSuspField {
		this.deserialize(bytes, bp);
	}

	public String getPathname() {
		return this.pathname;
	}

	public boolean isContinuous() {
		return (this.flags & 1) != 0;
	}

	@Override
	public int getExpectedId() {
		return Constants.RR_FIELD_ID_SL;
	}

	@Override
	public void deserialize(byte[] block, int bp) throws InvalidSuspField {
		this.deserializeHeader(block, bp);
		this.verifyId();

		this.flags = Util.getUInt8(block, bp + 4);

		// the second argument is not "bp" but offset
		byte[] compArea = Util.getBytes(block, bp + 4, this.length - 5);
		List<RripSLComponent> comps = RripSLComponent.fromComponentArea(compArea);
		this.pathname = this.extracPathname(comps);
	}

	private String extracPathname(List<RripSLComponent> comps)
			throws InvalidSuspField {
		String pathname = "";
		boolean firstTime = true;

		for (RripSLComponent comp : comps) {
			if (!firstTime) {
				pathname += "/";
			}

			if ( comp.isNormalLink() ) {
				pathname += comp.getText();
			}

			if ( comp.linksToCurrentDir() ) {
				pathname += ".";
			}

			if ( comp.linksToParentDir() ) {
				pathname += "..";
			}

			if ( comp.linksToRootDir() ) {
				if (firstTime) {
					// nothing to do
				} else {
					throw new InvalidSuspField("unexpected root symlink");
				}
			}

			firstTime = false;
		}

		return pathname;
	}
}
