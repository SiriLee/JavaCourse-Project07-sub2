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