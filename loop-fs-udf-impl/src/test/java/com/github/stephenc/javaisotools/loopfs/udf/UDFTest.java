/*
 * Copyright (c) 2019. Mr.Indescribable (https://github.com/Mr-indescribable).
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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import com.github.stephenc.javaisotools.loopfs.spi.VolumeDescriptorSet;
import com.github.stephenc.javaisotools.loopfs.udf.UDFFileEntry;
import com.github.stephenc.javaisotools.loopfs.udf.UDFFileSystem;
import com.github.stephenc.javaisotools.loopfs.udf.UDFVolumeDescriptorSet;
import com.github.stephenc.javaisotools.loopfs.udf.descriptor.AnchorDescriptor;
import com.github.stephenc.javaisotools.loopfs.udf.descriptor.FileIdentifierDescriptor;


/**
 * Tests the UDF implementation.
 */
public class UDFTest {

	private static Properties testProperties;
	private static String discPath;
	private static File discFile;

	@BeforeClass
	public static void loadConfiguration() throws Exception {
		testProperties = new Properties();
		InputStream is = null;
		is = UDFTest.class.getClassLoader().getResourceAsStream("test.properties");
		testProperties.load(is);
		is.close();

		discPath = testProperties.getProperty("source-image");
		discFile = new File(discPath);
	}

	@Test
	public void smokes() throws Exception {
		UDFFileSystem img = new UDFFileSystem(discFile, true);
		this.runCheck(img);
	}

	/**
	 * File structure of the image for test:
	 *
	 *       /
	 *       └── a.txt  (17733 bytes)
	 */
	private void runCheck(UDFFileSystem img) throws Exception {
		for (UDFFileEntry fe : img) {
			if (fe.isDirectory()){
				Assert.assertEquals(fe.getName(), "/");
				Assert.assertEquals(fe.getPath(), "/");
			} else {
				InputStream inStream = img.getInputStream(fe);
				String content = IOUtils.toString(inStream, StandardCharsets.UTF_8); 

				Assert.assertEquals(fe.getPath(), "/a.txt");
				Assert.assertEquals(fe.getName(), "a.txt");
				Assert.assertEquals(content.length(), 17733);
			}
		}
	}
}
