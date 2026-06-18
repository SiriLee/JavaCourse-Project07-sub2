/*
 * Copyright (c) 1994, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * ...
 */

package java.io;

/*
 * Inheritance hierarchy:
 *   java.lang.Object
 *     └── java.io.InputStream           (abstract read())
 *           └── java.io.FilterInputStream (holds underlying 'in' stream)
 *                 └── java.io.DataInputStream  ← THIS CLASS
 *                     implements DataInput
 *
 * Design pattern: Decorator / Wrapper
 *   FilterInputStream wraps an InputStream; DataInputStream adds typed read methods.
 */

public class DataInputStream extends FilterInputStream implements DataInput {

    public DataInputStream(InputStream in) {
        super(in);
    }

    private byte bytearr[] = new byte[80];
    private char chararr[] = new char[80];

    public final int read(byte b[]) throws IOException {
        return in.read(b, 0, b.length);
    }

    public final int read(byte b[], int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    public final void readFully(byte b[]) throws IOException {
        readFully(b, 0, b.length);
    }

    public final void readFully(byte b[], int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
    }

    public final int skipBytes(int n) throws IOException {
        int total = 0;
        int cur = 0;
        while ((total < n) && ((cur = (int) in.skip(n - total)) > 0)) {
            total += cur;
        }
        return total;
    }

    /*
     * readBoolean — reads 1 byte, returns false if 0, true otherwise.
     *
     * Byte layout:   +--------+
     *                | 1 byte |  0→false, nonzero→true
     *                +--------+
     */
    public final boolean readBoolean() throws IOException {
        int ch = in.read();                 // read 1 byte (0..255, or -1 for EOF)
        if (ch < 0)
            throw new EOFException();       // premature EOF → exception
        return (ch != 0);                   // 0→false, nonzero→true (per DataInput spec)
    }

    public final byte readByte() throws IOException {
        int ch = in.read();
        if (ch < 0) throw new EOFException();
        return (byte) ch;
    }

    public final int readUnsignedByte() throws IOException {
        int ch = in.read();
        if (ch < 0) throw new EOFException();
        return ch;
    }

    public final short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) throw new EOFException();
        return (short) ((ch1 << 8) + ch2);
    }

    public final int readUnsignedShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) throw new EOFException();
        return (ch1 << 8) + ch2;
    }

    public final char readChar() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) throw new EOFException();
        return (char) ((ch1 << 8) + ch2);
    }

    /*
     * readInt — reads 4 bytes, assembles into int via big-endian.
     *
     * EOF idiom: (ch1|ch2|ch3|ch4) < 0
     *   Each valid byte is 0x00..0xFF (bit 7 clear); -1 (EOF) is 0xFFFFFFFF.
     *   If any byte is -1, the OR result has bit 7 set → negative.
     *   This replaces four separate (ch < 0) checks with a single comparison.
     *
     * Assembly: (ch1<<24) + (ch2<<16) + (ch3<<8) + ch4
     *   Uses + rather than | because shifting already leaves low bits zero,
     *   making addition equivalent. HotSpot JIT optimizes this pattern.
     *
     * Byte layout:   +--------+--------+--------+--------+
     *                | byte 0 | byte 1 | byte 2 | byte 3 |
     *                |  MSB   |        |        |  LSB   |
     *                +--------+--------+--------+--------+
     *                bit 31-24  23-16    15-8     7-0
     *
     * Example: bytes [0x12, 0x34, 0x56, 0x78] → 0x12345678 (305419896)
     */
    public final int readInt() throws IOException {
        int ch1 = in.read();                 // MSB (bits 31-24)
        int ch2 = in.read();                 // bits 23-16
        int ch3 = in.read();                 // bits 15-8
        int ch4 = in.read();                 // LSB (bits 7-0)

        if ((ch1 | ch2 | ch3 | ch4) < 0)     // any byte == -1 → EOF
            throw new EOFException();

        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);  // big-endian assembly
    }

    private byte readBuffer[] = new byte[8];

    public final long readLong() throws IOException {
        readFully(readBuffer, 0, 8);
        return (((long) readBuffer[0] << 56)
                + ((long) (readBuffer[1] & 255) << 48)
                + ((long) (readBuffer[2] & 255) << 40)
                + ((long) (readBuffer[3] & 255) << 32)
                + ((long) (readBuffer[4] & 255) << 24)
                + ((readBuffer[5] & 255) << 16)
                + ((readBuffer[6] & 255) << 8)
                + (readBuffer[7] & 255));
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    // ... (readLine, readUTF — not required for this experiment)
}
