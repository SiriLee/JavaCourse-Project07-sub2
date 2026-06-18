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

源码注释文件：[DataInputStream_annotated.java](docs/DataInputStream_annotated.java)

### 继承关系

```
java.lang.Object
  └── java.io.InputStream           (abstract read())
        └── java.io.FilterInputStream (holds underlying 'in' stream)
              └── java.io.DataInputStream  ← THIS CLASS
                  implements DataInput
```

设计模式: **Decorator / Wrapper** — `FilterInputStream` 持有底层 `InputStream`，`DataInputStream` 在此基础上增加按类型读取的方法。

### readBoolean() — 逐行注释

```java
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
```

**算法说明:** 读 1 字节，按 DataInput 协议映射：0→false，非 0→true。对应 `DataOutputStream.writeBoolean()` 写入 true 时写 1，false 时写 0。

### readInt() — 逐行注释

```java
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
```

**算法说明:** 连续读 4 字节，按 Big-Endian 字节序拼接为 int。EOF 检测利用 `-1 = 0xFFFFFFFF` 二进制全 1 特性，一次 `|` 运算替代四次 `ch < 0` 逐个判断。移位后低位为 0，加法等价于按位或。