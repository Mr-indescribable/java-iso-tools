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

import java.util.List;
import java.util.ArrayList;

import com.github.stephenc.javaisotools.loopfs.api.FileEntry;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.exceptions.SuspFieldEnds;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.exceptions.InvalidSuspField;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.exceptions.UnknownSuspFieldId;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.SuspField;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.SuspFieldCE;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.SuspFieldER;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.SuspFieldPD;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.SuspFieldSP;
import com.github.stephenc.javaisotools.loopfs.iso9660.susp.SuspFieldST;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldCL;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldNM;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldPL;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldPN;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldPX;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldRE;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldRR;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldSF;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldSL;
import com.github.stephenc.javaisotools.loopfs.iso9660.rrip.RripFieldTF;


/**
 * Represents a file in an ISO9660 file system.
 */
public final class Iso9660FileEntry implements FileEntry {

    public static final String ID_SEPARATOR1 = ".;1";
    public static final String ID_SEPARATOR2 = ";1";

    private Iso9660FileSystem fileSystem;
    private String parentPath;
    private final int entryLength;
    private final long startSector;
    private final int dataLength;
    private final long lastModifiedTime;
    private final int flags;
    private final int fidLength;
    private final String identifier;

    private final boolean suParsed;

    // This list contains all fields in the system use block,
    // including fields not implemented.
    //
    // Currently, this list will make some redundancy.
    private final List<SuspField> suspFields = new ArrayList<SuspField>();

    // This list contains all RRIP NM fields
    private final List<RripFieldNM> rrnmFields = new ArrayList<RripFieldNM>();

    // This list contains all RRIP SL fields
    private final List<RripFieldSL> rrslFields = new ArrayList<RripFieldSL>();

    private RripFieldPX rrpxField = null;

    //private final int extAttributeLength;
    //private final int fileUnitSize;
    //private final int interleaveSize;

    public Iso9660FileEntry(final Iso9660FileSystem fileSystem, final byte[] block, final int pos) {
        this(fileSystem, null, block, pos, true);
    }

    public Iso9660FileEntry(final Iso9660FileSystem fileSystem, final String parentPath,
                            final byte[] block, final int startPos) {
        this(fileSystem, parentPath, block, startPos, true);
    }

    /**
     * Initialize this instance.
     *
     * @param fileSystem the parent file system
     * @param parentPath the path of the parent directory
     * @param block      the bytes of the sector containing this file entry
     * @param startPos   the starting position of this file entry
     * @param parseSu    whether the System Use block needs to be parsed
     */
    public Iso9660FileEntry(final Iso9660FileSystem fileSystem, final String parentPath,
                            final byte[] block, final int startPos,
                            final boolean parseSu) {

        this.fileSystem = fileSystem;
        this.parentPath = parentPath;

        final int offset = startPos - 1;
        this.entryLength = Util.getUInt8(block, offset + 1);
        //this.extAttributeLength = Util.getUInt8(block, offset+2);
        this.startSector = Util.getUInt32LE(block, offset + 3);
        this.dataLength = (int) Util.getUInt32LE(block, offset + 11);
        this.lastModifiedTime = Util.getDateTime(block, offset + 19);
        this.flags = Util.getUInt8(block, offset + 26);
        //this.fileUnitSize = Util.getUInt8(block, offset+27);
        //this.interleaveSize = Util.getUInt8(block, offset+28);
        this.fidLength = Util.getUInt8(block, offset + 33);
        this.identifier = getFileIdentifier(block, offset, isDirectory());

        this.suParsed = parseSu;

        if (parseSu) {
            // Util.getBytes will not minus startPos by 1.
            int suStartPos = offset + 33 + this.fidLength;

            // ECMA-119 9.1.12
            if (this.fidLength % 2 == 0) {
                suStartPos += 1;
            }

            byte[] suBlock = Util.getBytes(
                    block,
                    suStartPos,
                    this.entryLength - (33 + this.fidLength)
            );

            try {
                this.deserializeSuBlock(suBlock);
            } catch (InvalidSuspField ex) {
                throw new RuntimeException(ex);
            } catch (SuspFieldEnds ex) {
                // Nothing to do.
            }
        }
    }

    private void deserializeSuBlock(byte[] suBlock)
            throws InvalidSuspField, SuspFieldEnds {

        SuspField field;
        int suBp = 1;

        while (suBp < suBlock.length) {
            int nextId = SuspField.extractFieldId(suBlock, suBp);

            switch (nextId) {
                case Constants.SU_FIELD_ID_CE:
                    field = new SuspFieldCE(suBlock, suBp);
                    break;
                case Constants.SU_FIELD_ID_PD:
                    field = new SuspFieldPD(suBlock, suBp);
                    break;
                case Constants.SU_FIELD_ID_SP:
                    field = new SuspFieldSP(suBlock, suBp);
                    break;
                case Constants.SU_FIELD_ID_ST:
                    field = new SuspFieldST(suBlock, suBp);
                    this.suspFields.add(field);
                    throw new SuspFieldEnds(
                        "SU field sequence ends with the ST field"
                    );
                case Constants.SU_FIELD_ID_ER:
                    field = new SuspFieldER(suBlock, suBp);
                    break;
                case Constants.RR_FIELD_ID_PX:
                    field = new RripFieldPX(suBlock, suBp);
                    this.rrpxField = (RripFieldPX) field;
                    break;
                case Constants.RR_FIELD_ID_PN:
                    field = new RripFieldPN(suBlock, suBp);
                    break;
                case Constants.RR_FIELD_ID_SL:
                    field = new RripFieldSL(suBlock, suBp);
                    this.rrslFields.add( (RripFieldSL) field );
                    break;
                case Constants.RR_FIELD_ID_NM:
                    field = new RripFieldNM(suBlock, suBp);
                    this.rrnmFields.add( (RripFieldNM) field );
                    break;
                case Constants.RR_FIELD_ID_CL:
                    field = new RripFieldCL(suBlock, suBp);
                    break;
                case Constants.RR_FIELD_ID_PL:
                    field = new RripFieldPL(suBlock, suBp);
                    break;
                case Constants.RR_FIELD_ID_RE:
                    field = new RripFieldRE(suBlock, suBp);
                    break;
                case Constants.RR_FIELD_ID_TF:
                    field = new RripFieldTF(suBlock, suBp);
                    break;
                case Constants.RR_FIELD_ID_SF:
                    field = new RripFieldSF(suBlock, suBp);
                    break;
                case Constants.RR_FIELD_ID_RR:
                    field = new RripFieldRR(suBlock, suBp);
                    break;
                default:
                    String hexId = Integer.toHexString(nextId).toUpperCase();
                    throw new SuspFieldEnds(
                        "SU field sequence ends with unknown field id: 0x" + hexId
                    );
            }

            this.suspFields.add(field);
            int length = field.getLength();
            suBp += length;
        }
    }

    private String getFileIdentifier(final byte[] block, final int offset, final boolean isDir) {
        if (isDir) {
            final int buff34 = Util.getUInt8(block, offset + 34);

            if ((this.fidLength == 1) && (buff34 == 0x00)) {
                return ".";
            } else if ((this.fidLength == 1) && (buff34 == 0x01)) {
                return "..";
            }
        }

        final String id;
        if (this.suParsed){
            id = Util.getString(block, offset + 34, this.fidLength);
        } else {
            id = Util.getDChars(
                    block, offset + 34, this.fidLength, this.fileSystem.getEncoding());
        }

        // We should handle both of these separators here.
        final int sep1Idx = id.indexOf(ID_SEPARATOR1);
        final int sep2Idx = id.indexOf(ID_SEPARATOR2);

        if (sep1Idx >= 0) {
            return id.substring(0, sep1Idx);
        } else if (sep2Idx >= 0) {
            return id.substring(0, sep2Idx);
        } else {
            return id;
        }
    }

    public String getName() {
        if (this.fileSystem.suspUsed()) {
            if (this.rrnmFields.toArray().length == 0) {
                return this.identifier;
            }

            String name = "";
            for (RripFieldNM field : this.rrnmFields) {
                name += field.getNameContent();
                if (! field.getFlagContinue()){
                    break;
                }
            }
            return name;
        } else {
            return this.identifier;
        }
    }

    public String getPath() {
        if (".".equals(this.getName())) {
            return "";
        }

        StringBuffer buf = new StringBuffer();

        if (null != this.parentPath) {
            buf.append(this.parentPath);
        }

        buf.append(getName());

        if (isDirectory()) {
            buf.append("/");
        }

        return buf.toString();
    }

    public long getLastModifiedTime() {
        return this.lastModifiedTime;
    }

    public boolean isDirectory() {
        return (this.flags & 0x03) != 0;
    }

    public boolean isSymlink() {
        return this.rrpxField.isSymlink();
    }

    public String getSymlinkPath() {
        if (! this.isSymlink()) {
            throw new RuntimeException("This is not a symbolic link");
        }

        String path = "";
        boolean firstTime = true;

        for (RripFieldSL rrsl: this.rrslFields) {
            if (! firstTime) {
                path += "/";
            }

            path += rrsl.getPathname();

            if (! rrsl.isContinuous()) {
                break;
            }

            firstTime = false;
        }

        return path;
    }

    public long getSize() {
        return this.dataLength;
    }

    /**
     * Returns the block number where this entry starts.
     */
    public long getStartBlock() {
        return this.startSector;
    }

    /**
     * Returns the size this entry takes up in the file table.
     */
    public int getEntryLength() {
        return this.entryLength;
    }

    /**
     * Returns true if this is the last entry in the file system.
     */
    public final boolean isLastEntry() {
        return (this.flags & 0x40) == 0;
    }

    /**
     * Returns SUSP fields contained in this directory record (or entry)
     */
    public List<SuspField> getSuspFields() {
        return this.suspFields;
    }

    /**
     * Returns a list of NM fields
     */
    public List<RripFieldNM> getRRNMFields() {
        return this.rrnmFields;
    }

    public boolean hasNMFields() {
        return this.rrnmFields.toArray().length > 0;
    }
}
