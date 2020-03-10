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

import com.github.stephenc.javaisotools.loopfs.iso9660.Util;
import com.github.stephenc.javaisotools.loopfs.iso9660.Constants;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.SuspField;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.exceptions.InvalidSuspField;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.exceptions.UnknownSuspFieldId;


/**
 * The PX field. (RRIP specification section 4.1)
 */
public class RripFieldPX extends SuspField
{
	// currently, I only need this one attribute, maybe I'll add others later
	private long posixFileMode;

	public RripFieldPX() {
	}

	public RripFieldPX(byte[] bytes, int bp) throws InvalidSuspField {
		this.deserialize(bytes, bp);
	}

	@Override
	public int getExpectedId() {
		return Constants.RR_FIELD_ID_PX;
	}

	@Override
	public void deserialize(byte[] block, int bp) throws InvalidSuspField {
		this.deserializeHeader(block, bp);
		this.verifyId();

		this.posixFileMode = Util.getUInt32LE(block, bp + 4);
	}

	public boolean isSymlink() {
		return (this.posixFileMode & Constants.POSIX_FM_S_IFLNK)
				== Constants.POSIX_FM_S_IFLNK;
	}
}
