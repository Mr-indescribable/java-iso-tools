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

package com.github.stephenc.javaisotools.loopfs.udf;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.stephenc.javaisotools.loopfs.api.FileEntry;
import com.github.stephenc.javaisotools.loopfs.udf.exceptions.InvalidDescriptor;
import com.github.stephenc.javaisotools.loopfs.udf.Constants;
import com.github.stephenc.javaisotools.loopfs.udf.UDFFileSystem;
import com.github.stephenc.javaisotools.loopfs.udf.descriptor.FileEntryDescriptor;
import com.github.stephenc.javaisotools.loopfs.udf.descriptor.FileIdentifierDescriptor;
import com.github.stephenc.javaisotools.loopfs.udf.descriptor.element.ExtentAD;


public class UDFFileEntry implements FileEntry
{
	private UDFFileSystem fs;
	private FileEntryDescriptor icb;
	private String parentPath;
	private String entryName;
	private Boolean isRoot;

	public UDFFileEntry (
		UDFFileSystem fs,
		FileEntryDescriptor icb,
		String parentPath,
		String entryName
	) {
		this(fs, icb, parentPath, entryName, false);
	}

	public UDFFileEntry(
		UDFFileSystem fs,
		FileEntryDescriptor icb,
		String parentPath,
		String entryName,
		Boolean isRoot
	) {
		this.fs = fs;
		this.parentPath = parentPath;
		this.icb = icb;
		this.entryName = entryName;
		this.isRoot = isRoot;
	}

	public String getName() {
		if (this.isRoot) {
			return "/";
		} else {
			return this.entryName;
		}
	}

	public String getPath() {
		if (this.isRoot) {
			return "/";
		}

		Path path = Paths.get(this.parentPath, this.entryName);
		return path.toString();
	}

	public long getLastModifiedTime() {
		return 0;
	}

	public boolean isLink() {
		return this.icb.icbTag.fileType == Constants.FE_TYPE_LINK;
	}

	public boolean isSymlink() {
		// Not implemented
		return false;
	}

	public boolean isDirectory() {
		return this.icb.icbTag.fileType == Constants.FE_TYPE_DIRECTORY;
	}

	public long getSize() {
		// If only we have BEEEEEP unsigned types in Java.
		return this.icb.infoLength.longValue();
	}

	/**
	 * get Allocation Descriptors
	 */
	public List<ExtentAD> getADs() {
		return this.icb.allocDescriptors;
	}

	/**
	 * Load files in the entry of a directory
	 */
	public void loadFiles() throws IOException {
		if ( this.isDirectory() ){
			this.icb.loadChildren(this.fs);
		}
	}

	/**
	 * Get files in the entry of a directory
	 */
	public List<UDFFileEntry> getFiles() throws IOException {
		if ( !this.isDirectory() ){
			return new ArrayList<UDFFileEntry>();
		}

		String currentPath = this.getPath();
		List<UDFFileEntry> files = new ArrayList<UDFFileEntry>();

		for (FileIdentifierDescriptor fid : this.icb.fids){
			Long relativeSectorNum = fid.icb.location.blockNumber;
			Long absSectorNum = relativeSectorNum + this.fs.getPDStartPos();

			byte[] buffer = new byte[Constants.DEFAULT_BLOCK_SIZE];
			this.fs.readBlock(absSectorNum, buffer);

			FileEntryDescriptor fed;
			try {
				fed = new FileEntryDescriptor(buffer);
			} catch (InvalidDescriptor ex) {
				throw new IOException(
					"Invalid descriptor found at sector " + absSectorNum.toString()
				);
			}

			String fileName = fid.fileId.toString();
			if (fileName.equals("")){
				continue;
			}

			UDFFileEntry file = new UDFFileEntry(
				this.fs, fed, currentPath, fileName
			);
			files.add(file);
		}

		return files;
	}
}
