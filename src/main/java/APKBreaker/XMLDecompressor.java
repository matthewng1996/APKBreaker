package APKBreaker;

/*
    Credits to https://gist.github.com/seymores/2425692
 */

import java.util.List;

public class XMLDecompressor {

    public static int endDocTag = 0x00100101;
    public static int startTag = 0x00100102;
    public static int endTag = 0x00100103;

    public String decompressXML(byte[] buf) {

        StringBuilder retString  = new StringBuilder();

        // Compressed XML file/bytes starts with 24x bytes of data,
        // 9 32 bit words in little endian order (LSB first):
        // 0th word is 03 00 08 00
        // 3rd word SEEMS TO BE: Offset at then of StringTable
        // 4th word is: Number of strings in string table
        // little endian storage format, or in integer format (ie MSB first).
        int numbStrings = LittleEndian32Word(buf, 4 * 4);

        // StringIndexTable starts at offset 24x, an array of 32 bit LE offsets
        // of the length/string data in the StringTable.
        int sitOff = 0x24; // Offset of start of StringIndexTable

        // StringTable, each string is represented with a 16 bit little endian
        // character count, followed by that number of 16 bit (LE) (Unicode)
        // chars.
        int stOff = sitOff + numbStrings * 4; // StringTable follows

        // XMLTags, The XML tag tree starts after some unknown content after the
        // StringTable. There is some unknown data after the StringTable, scan
        // forward from this point to the flag for the start of an XML start
        // tag.
        int xmlTagOff = LittleEndian32Word(buf, 3 * 4); // Start from the offset in the 3rd

        // Scan forward until we find the bytes: 0x02011000(x00100102 in normal
        // int)
        for (int ii = xmlTagOff; ii < buf.length - 4; ii += 4) {
            if (LittleEndian32Word(buf, ii) == startTag) {
                xmlTagOff = ii;
                break;
            }
        }

        // Step through the XML tree element tags and attributes
        int off = xmlTagOff;
        int indent = 0;
        int startTagLineNo = -2;
        while (off < buf.length) {
            int tag0 = LittleEndian32Word(buf, off);
            // int tag1 = LEW(xml, off+1*4);
            int lineNo = LittleEndian32Word(buf, off + 2 * 4);
            // int tag3 = LEW(xml, off+3*4);
            int nameNsSi = LittleEndian32Word(buf, off + 4 * 4);
            int nameSi = LittleEndian32Word(buf, off + 5 * 4);

            if (tag0 == startTag) { // XML START TAG
                int tag6 = LittleEndian32Word(buf, off + 6 * 4); // Expected to be 14001400
                int numbAttrs = LittleEndian32Word(buf, off + 7 * 4); // Number of Attributes
                // to follow
                // int tag8 = LEW(xml, off+8*4); // Expected to be 00000000
                off += 9 * 4; // Skip over 6+3 words of startTag data
                String name = compXmlString(buf, sitOff, stOff, nameSi);
                // tr.addSelect(name, null);
                startTagLineNo = lineNo;

                // Look for the Attributes
                StringBuffer sb = new StringBuffer();
                for (int ii = 0; ii < numbAttrs; ii++) {
                    int attrNameNsSi = LittleEndian32Word(buf, off); // AttrName Namespace Str
                    // Ind, or FFFFFFFF
                    int attrNameSi = LittleEndian32Word(buf, off + 1 * 4); // AttrName String
                    // Index
                    int attrValueSi = LittleEndian32Word(buf, off + 2 * 4); // AttrValue Str
                    // Ind, or
                    // FFFFFFFF
                    int attrFlags = LittleEndian32Word(buf, off + 3 * 4);
                    int attrResId = LittleEndian32Word(buf, off + 4 * 4); // AttrValue
                    // ResourceId or dup
                    // AttrValue StrInd
                    off += 5 * 4; // Skip over the 5 words of an attribute

                    String attrName = compXmlString(buf, sitOff, stOff,
                            attrNameSi);

                    String attrValue = "";
                    if (attrValueSi != -1) {
                        attrValue =  compXmlString(buf, sitOff, stOff, attrValueSi);
                    } else {
                        if (attrResId == -1)
                            attrValue = "resourceID 0x" + Integer.toHexString(attrResId);
                        else
                            attrValue = String.valueOf(Integer.valueOf(Integer.toHexString(attrResId), 16).intValue());
                        //System.out.println(attrName + " >>> " + attrValue.split("0x")[1]);
                    }
                    // attrValue = Integer.valueOf(Integer.toHexString(attrResId), 16).intValue();

                    sb.append(" " + attrName + "=\"" + attrValue + "\"");
                    // tr.add(attrName, attrValue);
                }
                retString.append("<" + name + sb + ">");
                prtIndent(indent, "<" + name + sb + ">");
                indent++;

            } else if (tag0 == endTag) { // XML END TAG
                indent--;
                off += 6 * 4; // Skip over 6 words of endTag data
                String name = compXmlString(buf, sitOff, stOff, nameSi);
                retString.append("</" + name + ">");
                prtIndent(indent, "</" + name + "> (line " + startTagLineNo
                        + "-" + lineNo + ")");
                // tr.parent(); // Step back up the NobTree

            } else if (tag0 == endDocTag) { // END OF XML DOC TAG
                break;

            } else {
                prt("  Unrecognized tag code '" + Integer.toHexString(tag0) + "' at offset " + off);
                break;
            }
        } // end of while loop scanning tags and attributes of XML tree
        //prt("    end at offset " + off);

        return retString.toString();
    }

    // Return value of a Little Endian 32 bit word from the byte array at offset off.
    public static int LittleEndian32Word(byte[] arr, int off) {
        return arr[off + 3] << 24 & 0xff000000 | arr[off + 2] << 16 & 0xff0000 | arr[off + 1] << 8 & 0xff00 | arr[off] & 0xFF;
    }

    public static String compXmlString(byte[] xml, int sitOff, int stOff, int strInd) {
        if (strInd < 0)
            return null;
        int strOff = stOff + LittleEndian32Word(xml, sitOff + strInd * 4);
        return compXmlStringAt(xml, strOff);
    }

    // Return the string stored in StringTable format at
    // offset strOff. This offset points to the 16 bit string length, which is followed by that number of 16 bit (Unicode) chars.
    public static String compXmlStringAt(byte[] arr, int strOff) {
        int strLen = arr[strOff + 1] << 8 & 0xff00 | arr[strOff] & 0xff;
        byte[] chars = new byte[strLen];
        for (int ii = 0; ii < strLen; ii++) {
            chars[ii] = arr[strOff + 2 + ii * 2];
        }
        return new String(chars); // Hack, just use 8 byte chars
    }

    public static String spaces = "                                             \n";

    public static void prtIndent(int indent, String str) {
        prt(spaces.substring(0, Math.min(indent * 2, spaces.length())) + str);
    }

    static void prt(String str) {
        //System.err.print(str);
    }
}
