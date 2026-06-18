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

源码阅读笔记：[DataInputStream_annotated.java](docs/DataInputStream_annotated.java)

### 继承关系

```
java.lang.Object
  └── java.io.InputStream              (abstract read())
        └── java.io.FilterInputStream   (wraps underlying InputStream via 'in' field)
              └── java.io.DataInputStream  ← THIS CLASS
                  implements DataInput
```

设计模式: **Decorator / Wrapper** — `FilterInputStream` 持有底层 `InputStream`，`DataInputStream` 在底层流之上增加类型化读取能力。

### readBoolean() 算法

1. 从底层流读取 1 个字节 (`in.read()`)
2. 若返回 `-1`（EOF），抛 `EOFException`
3. 按 DataInput 协议：`0 → false`，`非0 → true`，即 `(ch != 0)`

```
字节布局:  +--------+
          | 1 byte |  ← 0=false, 非0=true
          +--------+
```

### readInt() 算法

1. 从底层流连续读取 4 个字节 (`ch1`, `ch2`, `ch3`, `ch4`)
2. EOF 检测: `(ch1|ch2|ch3|ch4) < 0` — 利用 `-1` 的二进制全为 1 特性，一次位或即判断是否有字节读取失败
3. 按 Big-Endian 字节序拼接: `(ch1<<24) + (ch2<<16) + (ch3<<8) + (ch4)`

```
字节布局:  +--------+--------+--------+--------+
          | byte 0 | byte 1 | byte 2 | byte 3 |
          | MSB    |        |        | LSB    |
          +--------+--------+--------+--------+
          bit 31-24  23-16    15-8     7-0
```

示例 `[0x12, 0x34, 0x56, 0x78]` → `0x12345678` (305419896)