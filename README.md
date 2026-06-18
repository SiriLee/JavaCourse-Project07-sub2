# I/O

## 实验一
源代码：[DirReader](src/main/java/com/example/tools/DirReader.java)

运行测试：
```java
public static void main(String[] args) {
    DirReader dirReader = new DirReader();
    dirReader.readDir(".\\src");
    System.out.print(dirReader.getResult());
}
```
编译运行：
```bash
mvn compile

java -cp target/classes com.example.Main
```
运行结果
```powershell
main:
        |- java:
                |- com:
                        |- example:
                                |- Main.java
                                |- tools:
                                        |- DirReader.java
        |- resources:
                |- .gitkeep
test:
        |- java:
                |- com:
                        |- example:
                                |- .gitkeep
```

## 实验二

源代码：[IOBenchmark](src/main/java/com/example/tools/IOBenchmark.java)

### 实验目的
比较带缓冲的流和不带缓冲的流在读写性能上的差距。

### 实验方法
- 生成 10MB 测试文件
- 分别使用 `FileInputStream`（无缓冲）和 `BufferedInputStream`（有缓冲）逐字节读取，计时对比
- 分别使用 `FileOutputStream`（无缓冲）和 `BufferedOutputStream`（有缓冲）逐字节写入，计时对比

### 编译运行
```bash
mvn compile
java -cp target/classes com.example.Main
```

### 运行结果
```
=== I/O Benchmark: Buffered vs Unbuffered (10 MB, byte-by-byte) ===

[READ]  Unbuffered (byte-by-byte):   8963 ms
[READ]  Buffered   (byte-by-byte):   172 ms
[READ]  Buffered is ~52x faster

[WRITE] Unbuffered (byte-by-byte):   13043 ms
[WRITE] Buffered   (byte-by-byte):   168 ms
[WRITE] Buffered is ~78x faster
```

### 结论
逐字节 I/O 时，无缓冲流每次 `read()`/`write()` 都触发一次系统调用，开销极大。缓冲流通过内部 8192 字节缓冲区将系统调用次数减少约 8000 倍，因此在 10MB 数据量下性能差距达到 50-80 倍。

## 实验三

完整注释文件：[DataInputStream_annotated.java](docs/DataInputStream_annotated.java)

### 继承关系

```
java.lang.Object
  └── java.io.InputStream              (abstract read())
        └── java.io.FilterInputStream   (wraps underlying InputStream via 'in' field)
              └── java.io.DataInputStream  ← THIS CLASS
                  implements DataInput
```

设计模式: **Decorator / Wrapper** — `FilterInputStream` 持有底层 `InputStream`，`DataInputStream` 在底层流之上增加类型化读取能力。可嵌套 `DataInputStream → BufferedInputStream → FileInputStream`。

### readBoolean() — 逐行注释

```java
/**
 * See the general contract of the readBoolean method of DataInput.
 * Bytes for this operation are read from the contained input stream.
 *
 * @return     the boolean value read.
 * @exception  EOFException  if this input stream has reached the end.
 * @exception  IOException   the stream has been closed and the contained
 *             input stream does not support reading after close, or
 *             another I/O error occurs.
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
```

**算法说明:**
1. 从底层输入流读取 1 个字节 (8 bits)
2. 如果读到流末尾 (-1)，抛出 EOFException
3. 根据 DataInput 协议: 0 表示 false，非 0 表示 true
4. DataOutputStream.writeBoolean 写入 true 时写 1，false 时写 0
5. 因此 `(ch != 0)` 正确还原 boolean 值

**字节布局:**
```
+--------+
| 1 byte |  ← 0 or 1 (非零也视为 true)
+--------+
```

### readInt() — 逐行注释

```java
/**
 * See the general contract of the readInt method of DataInput.
 * Bytes for this operation are read from the contained input stream.
 *
 * @return     the next four bytes of this input stream, interpreted as an int.
 * @exception  EOFException  if this input stream reaches the end before
 *               reading four bytes.
 * @exception  IOException   the stream has been closed and the contained
 *             input stream does not support reading after close, or
 *             another I/O error occurs.
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
```

**算法说明:**
1. 从底层流连续读取 4 个字节
2. 使用 Big-Endian 字节序重建 int 值 (MSB first)
3. 如果任何 read() 返回 -1，表示数据不足 4 字节，抛出 EOFException
4. 算法正确性保证:
   - 每个字节为 0~255，左移后不溢出
   - 4 个值相加 ≤ 0xFFFFFFFF，在 int 有符号范围内
   - Big-Endian 与 DataOutputStream.writeInt() 配套: writeInt 将 int 拆为 `(v>>>24), (v>>>16), (v>>>8), (v>>>0)`

**字节布局:**
```
+--------+--------+--------+--------+
| byte 0 | byte 1 | byte 2 | byte 3 |
| MSB    |        |        | LSB    |
+--------+--------+--------+--------+
bit 31-24  23-16    15-8     7-0
```

**EOF 检测技巧:**
利用 `-1` (0xFFFFFFFF) 的位或特性代替逐个判断：
```java
// 等价但更紧凑的写法，一次位或即检测4个字节:
if ((ch1 | ch2 | ch3 | ch4) < 0)
    throw new EOFException();

// 等价于:
// if (ch1 < 0 || ch2 < 0 || ch3 < 0 || ch4 < 0)
//     throw new EOFException();
```