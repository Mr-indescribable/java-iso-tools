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

package com.github.stephenc.javaisotools.loopfs.iso9660;

public interface Constants {

    /**
     * ISO sector size. This does not include the 288 bytes reserved for synchronization, header, and EC on CD-ROMs
     * because this information is not used in .iso files.
     */
    int DEFAULT_BLOCK_SIZE = 2 * 1024;

    /**
     * The number of reserved sectors at the beginning of the file.
     */
    int RESERVED_SECTORS = 16;

    /**
     * The number of reserved bytes at the beginning of the file.
     */
    int RESERVED_BYTES = RESERVED_SECTORS * DEFAULT_BLOCK_SIZE;

    /**
     * Default character encoding.
     */
    String DEFAULT_ENCODING = "US-ASCII";

    /**
     * Signature Words of System Use fields. (SUSP specification section 5)
     */
    int SU_FIELD_ID_CE = 0x4345;
    int SU_FIELD_ID_PD = 0x5044;
    int SU_FIELD_ID_SP = 0x5350;
    int SU_FIELD_ID_ST = 0x5354;
    int SU_FIELD_ID_ER = 0x4552;

    /**
     * Signature Words of RRIP fields. (RRIP specification section 4.1)
     */
    int RR_FIELD_ID_PX = 0x5058;
    int RR_FIELD_ID_PN = 0x504E;
    int RR_FIELD_ID_SL = 0x534C;
    int RR_FIELD_ID_NM = 0x4E4D;
    int RR_FIELD_ID_CL = 0x434C;
    int RR_FIELD_ID_PL = 0x504C;
    int RR_FIELD_ID_RE = 0x5245;
    int RR_FIELD_ID_TF = 0x5446;
    int RR_FIELD_ID_SF = 0x5346;

    /**
     * The RR field in RRIP which has been dropped after RRIP 1.09.
     */
    int RR_FIELD_ID_RR = 0x5252;

    /**
     * Expected value of the check_bytes field in SP fields.
     * (SUSP specification section 5.3 [4])
     */
    int SUSP_CHECK_BYTES = 0xBEEF;

	/**
	 * POSIX file modes, see RRIP specification section 4.1.1
	 */
	long POSIX_FM_S_IRUSR  = 0000400;
	long POSIX_FM_S_IWUSR  = 0000200;
	long POSIX_FM_S_IXUSR  = 0000100;
	long POSIX_FM_S_IRGRP  = 0000040;
	long POSIX_FM_S_IWGRP  = 0000020;
	long POSIX_FM_S_IXGRP  = 0000010;
	long POSIX_FM_S_IROTH  = 0000004;
	long POSIX_FM_S_IWOTH  = 0000002;
	long POSIX_FM_S_IXOTH  = 0000001;
	long POSIX_FM_S_ISUID  = 0004000;
	long POSIX_FM_S_ISGID  = 0002000;
	long POSIX_FM_S_ENFMT  = 0002000;
	long POSIX_FM_S_ISVTX  = 0001000;
	long POSIX_FM_S_IFSOCK = 0140000;
	long POSIX_FM_S_IFLNK  = 0120000;
	long POSIX_FM_S_IFREG  = 0100000;
	long POSIX_FM_S_IFBLK  = 0060000;
	long POSIX_FM_S_IFCHR  = 0020000;
	long POSIX_FM_S_IFDIR  = 0040000;
	long POSIX_FM_S_IFIFO  = 0010000;
}
