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