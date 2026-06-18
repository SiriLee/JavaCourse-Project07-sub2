/*
 * Copyright (c) 1994, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.io;

// ============================================================
// 继承关系分析 (Inheritance Hierarchy)
// ============================================================
//
// java.lang.Object
//   └── java.io.InputStream          (abstract: provides base read() methods)
//         └── java.io.FilterInputStream  (wraps an underlying InputStream,
//                                          delegates all reads to it via the
//                                          protected 'in' field)
//               └── java.io.DataInputStream  (THIS CLASS: reads primitive Java
//                                              types in machine-independent
//                                              big-endian format)
//                   implements DataInput    (interface defining readBoolean(),
//                                            readInt(), readUTF(), etc.)
//
// 关键设计模式: Decorator (装饰器) / Wrapper 模式
//   - FilterInputStream 持有底层 InputStream（字段 `in`）
//   - DataInputStream 在底层流的基础上增加了"按类型读取"能力
//   - 可以任意嵌套: DataInputStream → BufferedInputStream → FileInputStream
//

/**
 * A data input stream lets an application read primitive Java data
 * types from an underlying input stream in a machine-independent
 * way. An application uses a data output stream to write data that
 * can later be read by a data input stream.
 * <p>
 * DataInputStream is not necessarily safe for multithreaded access.
 * Thread safety is optional and is the responsibility of users of
 * methods in this class.
 *
 * @author  Arthur van Hoff
 * @see     java.io.DataOutputStream
 * @since   JDK1.0
 */
public
class DataInputStream extends FilterInputStream implements DataInput {

    /**
     * Creates a DataInputStream that uses the specified
     * underlying InputStream.
     *
     * @param  in   the specified input stream
     */
    public DataInputStream(InputStream in) {
        super(in);  // 调用 FilterInputStream 构造器，保存底层流引用到 this.in
    }

    /**
     * working arrays initialized on demand by readUTF
     */
    private byte bytearr[] = new byte[80];   // readUTF 解码用的字节缓冲区
    private char chararr[] = new char[80];   // readUTF 解码用的字符缓冲区

    // ============================================================
    // read(byte[] b)  — 委托给底层流 in.read(b, 0, b.length)
    // ============================================================
    public final int read(byte b[]) throws IOException {
        return in.read(b, 0, b.length);
    }

    public final int read(byte b[], int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    // ============================================================
    // readFully — 循环读取直到填满指定长度，否则抛 EOFException
    // ============================================================
    public final void readFully(byte b[]) throws IOException {
        readFully(b, 0, b.length);
    }

    public final void readFully(byte b[], int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {                          // 循环读取直到填满 len 字节
            int count = in.read(b, off + n, len - n); // 从底层流读剩余部分
            if (count < 0)                         // 未读到足够数据就遇到 EOF
                throw new EOFException();
            n += count;                            // 累加已读字节数
        }
    }

    // ============================================================
    // skipBytes — 循环跳过 n 个字节，返回实际跳过的字节数
    // ============================================================
    public final int skipBytes(int n) throws IOException {
        int total = 0;
        int cur = 0;

        while ((total<n) && ((cur = (int) in.skip(n-total)) > 0)) {
            total += cur;                          // skip() 可能不跳过全部，需循环
        }

        return total;
    }

    // ============================================================
    // readBoolean() — 逐行注释与分析
    // ============================================================
    /**
     * See the general contract of the <code>readBoolean</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained
     * input stream.
     *
     * @return     the <code>boolean</code> value read.
     * @exception  EOFException  if this input stream has reached the end.
     * @exception  IOException   the stream has been closed and the contained
     *             input stream does not support reading after close, or
     *             another I/O error occurs.
     * @see        java.io.FilterInputStream#in
     */
    public final boolean readBoolean() throws IOException {
        // 第1行: 从底层 InputStream 读取 1 个字节
        //   InputStream.read() 返回 int 类型:
        //     - 正常数据: 0~255 (无符号字节值)
        //     - 流结束:   -1
        int ch = in.read();

        // 第2-3行: 检查是否到达流末尾 (EOF)
        //   如果 ch == -1 (即 ch < 0)，说明在读取到数据之前流就结束了
        //   根据 DataInput 接口规范，此时必须抛出 EOFException
        if (ch < 0)
            throw new EOFException();

        // 第4行: 将字节值转换为 boolean
        //   规则 (来自 DataInput 接口规范):
        //     - 字节值为 0  →  false
        //     - 字节值非 0  →  true
        //   对应 DataOutputStream.writeBoolean():
        //     - true  → 写入 (byte)1
        //     - false → 写入 (byte)0
        //   表达式 (ch != 0) 精确实现了这个映射
        return (ch != 0);
    }
    // ============================================================
    // readBoolean() 算法说明:
    // ============================================================
    // 1. 从底层输入流读取 1 个字节 (8 bits)
    // 2. 如果读到流末尾 (-1)，抛出 EOFException
    // 3. 根据 DataInput 协议: 0 表示 false，非 0 表示 true
    // 4. DataOutputStream.writeBoolean 写入 true 时写 1，false 时写 0
    // 5. 因此 (ch != 0) 正确还原 boolean 值
    //
    // 字节布局 (协议格式):
    //   +--------+
    //   | 1 byte |  ← 0 or 1 (非零也视为 true)
    //   +--------+

    public final byte readByte() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (byte)(ch);     // 强转为 byte，保留符号位
    }

    public final int readUnsignedByte() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch;             // 返回 int 0~255，无符号
    }

    public final short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short)((ch1 << 8) + (ch2 << 0));  // big-endian
    }

    public final int readUnsignedShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 8) + (ch2 << 0);
    }

    public final char readChar() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (char)((ch1 << 8) + (ch2 << 0));
    }

    // ============================================================
    // readInt() — 逐行注释与分析
    // ============================================================
    /**
     * See the general contract of the <code>readInt</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained
     * input stream.
     *
     * @return     the next four bytes of this input stream, interpreted as an
     *             <code>int</code>.
     * @exception  EOFException  if this input stream reaches the end before
     *               reading four bytes.
     * @exception  IOException   the stream has been closed and the contained
     *             input stream does not support reading after close, or
     *             another I/O error occurs.
     * @see        java.io.FilterInputStream#in
     */
    public final int readInt() throws IOException {
        // 第1行: 从底层流读取第1个字节 (最高有效字节 MSB, bits 31-24)
        //   InputStream.read() 返回 0~255 或 -1 (EOF)
        int ch1 = in.read();

        // 第2行: 读取第2个字节 (bits 23-16)
        int ch2 = in.read();

        // 第3行: 读取第3个字节 (bits 15-8)
        int ch3 = in.read();

        // 第4行: 读取第4个字节 (最低有效字节 LSB, bits 7-0)
        int ch4 = in.read();

        // 第5-6行: 巧妙的 EOF 检测 —— 位或运算
        //   关键洞察:
        //     - 有效字节值范围: 0x00 ~ 0xFF (bit 7 为 0)
        //     - EOF 标志 -1:      0xFFFFFFFF (所有 bit 为 1)
        //     - 如果 ch1~ch4 全部有效: OR 结果 bit 7 = 0，值 ≥ 0
        //     - 如果任意一个为 -1:   OR 结果 bit 7 = 1，值 < 0
        //   因此只需一次比较 (ch1|ch2|ch3|ch4) < 0 就能判断是否有字节读失败
        //   这比逐个检查 if (ch1<0 || ch2<0 || ch3<0 || ch4<0) 更紧凑
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();

        // 第7行: 按 Big-Endian (大端序，网络字节序) 拼接 4 个字节为 int
        //   ch1 << 24: 第1个字节左移 24 位 → 占据 bit 31~24 (最高位)
        //   ch2 << 16: 第2个字节左移 16 位 → 占据 bit 23~16
        //   ch3 << 8:  第3个字节左移 8 位  → 占据 bit 15~8
        //   ch4 << 0:  第4个字节不移位     → 占据 bit 7~0  (最低位)
        //
        //   示例: 读取字节 [0x12, 0x34, 0x56, 0x78]
        //     ch1<<24 = 0x12000000
        //     ch2<<16 = 0x00340000
        //     ch3<<8  = 0x00005600
        //     ch4<<0  = 0x00000078
        //     return  = 0x12345678  (十进制 305419896)
        //
        //   注意: 使用加法 (+) 而非按位或 (|)
        //   原因: << 保证了低位为 0，加法等价于按位或，且 HotSpot 对此模式有优化
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }
    // ============================================================
    // readInt() 算法说明:
    // ============================================================
    // 1. 从底层流连续读取 4 个字节
    // 2. 使用 Big-Endian 字节序重建 int 值 (MSB first)
    // 3. 如果任何 read() 返回 -1，表示数据不足 4 字节，抛出 EOFException
    // 4. 算法正确性保证:
    //    a. 每个字节为 0~255，左移后不溢出
    //    b. 4 个值相加 <= 0xFFFFFFFF，在 int 有符号范围内 (-2^31 ~ 2^31-1)
    //    c. Big-Endian 与 DataOutputStream.writeInt() 配套:
    //       writeInt 将 int 拆为 (v>>>24), (v>>>16), (v>>>8), (v>>>0)
    //
    // 字节布局 (协议格式):
    //   +--------+--------+--------+--------+
    //   | byte 0 | byte 1 | byte 2 | byte 3 |
    //   | MSB    |        |        | LSB    |
    //   +--------+--------+--------+--------+
    //   对应 bit: 31-24    23-16    15-8     7-0

    // ============================================================
    // readBuffer: readLong() 使用的共享缓冲区，避免每次分配新数组
    // ============================================================
    private byte readBuffer[] = new byte[8];

    // ============================================================
    // readLong — 与 readInt 类似，读 8 字节拼接为 long
    //   注意 readBuffer[0] 不需要 & 255，因为移位时会自动提升为 long
    //   readBuffer[1~7] 需要 & 255 以处理符号扩展问题
    // ============================================================
    public final long readLong() throws IOException {
        readFully(readBuffer, 0, 8);              // 利用 readFully 确定读满 8 字节
        return (((long)readBuffer[0] << 56) +
                ((long)(readBuffer[1] & 255) << 48) +
                ((long)(readBuffer[2] & 255) << 40) +
                ((long)(readBuffer[3] & 255) << 32) +
                ((long)(readBuffer[4] & 255) << 24) +
                ((readBuffer[5] & 255) << 16) +
                ((readBuffer[6] & 255) <<  8) +
                ((readBuffer[7] & 255) <<  0));
    }

    // ============================================================
    // readFloat / readDouble — 委托给 readInt / readLong,
    //   再用 Float.intBitsToFloat / Double.longBitsToDouble 转换
    // ============================================================
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    // ============================================================
    // readLine / readUTF — 字符串读取，略 (实验不要求)
    // ============================================================
    // ... (readLine, readUTF, readUTF(DataInput) 省略)
}
