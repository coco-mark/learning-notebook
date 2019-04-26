### equals

Java SE 中 equals() 用于判断两个对象是否相等，规约如下：

1. 自反性：对象自身与自身相等
2. 交换性：a.equals(b) 与 b.equals(a) 结果相同
3. 传递性
4. 一致性：只有 equals 方法中涉及的参数改变了，equals 的结果才会改变

### 违背 equals 的交换律

有一类反例，会违背 equals 的交换律：子类 Child 比父类 Father 多一个 sex 属性：

```java
Parents p = new Parents("XiaoMing", 10); // name, age
ViolatingChild c = new ViolatingChild("XiaoMing", 10, "boy"); // name, age, sex

System.out.println(p.equals(c)); // true
System.out.println(c.equals(p)); // false
```

在上面的情况下，如果想保证 equals 交换律仍然成立，需要使用组合，而不是继承的方式。由于继承，是的 equals 方法中的类型判断失效了：

```java
@Override
public boolean equals(Object tar) {
    if (this == tar) {
        return true;
    }
    if (!(tar instanceof ViolatingChild)) {
        return false;
    }
    ViolatingChild other = (ViolatingChild) tar;
    boolean b1 = (this.getName() == null && other.getName() == null)
            || (this.getName() != null && this.getName().equals(other.getName()));

    boolean b2 = (this.getSex() == null && other.getSex() == null)
            || (this.getSex() != null && this.getSex().equals(other.getSex()));

    return this.getAge() == other.getAge() && b1 && b2;
}
```

### hashcode

 Java SE 中 hashcode 用于标识一个类的对象，hashCode 方法规约：

1.  内部一致性：当 equals() 方法中的涉及到的参数改变时，hashCode 也应该随之改变。
    因此实现了 equals 后也需要一并实现对应的 hashCode 方法
2.  equals 一致性：equals() 相等的两个对象，hashCode 也应该相等
3.  冲突性：不同的对象 hashcode 可能相同

hashcode 用于基于 hash 的 collection 的唯一性判定条件。在 Team 类中没有实现 hashCode 方法，hashmap.get 的值为 null，尽管 equals 为 true。

```java
/**
 * 仅实现 equals 方法，导致相同的 Team 无法从 hashmap 中获取
 */
@Test
public void equalsWithoutHashcode() {
    Team t1 = new Team("tom", 10);
    Team t2 = new Team("jerry", 4);

    HashMap<Team, String> hashMap = new HashMap<>();
    hashMap.put(t1, "tom");
    hashMap.put(t2, "jerry");

    Team target = new Team("tom", 10);
    System.out.println(hashMap.get(target)); // null
}
```

### hashcode 与冲突

一个"标准"的 hashCode() 可以通过 IDEs 生成，或者使用 Apache Common 或 Guava 工具类。IDEs 生成的示例如下（基于 IDEA 2019.01）：

```java
@Override
public final int hashCode() {
    int result = getName() != null ? getName().hashCode() : 0;
    result = 31 * result + getAge();
    return result;
}
```

上面的代码中，通过质数 31 来作为一个因子，加入到 hashcode 中。这样做的目的有两个：

1.  31 作为质数，可以有效的减少 hashcode 冲突
2.  `31 * i` 可以被 `31 * i = i <<< 5 - i` 的位运算所代替，从而加快 hashcode 生成速度

### 参考资料

-   [Guide to hashCode() in Java](https://www.baeldung.com/java-hashcode)
-   [Java equals() and hashCode() Contracts](https://www.baeldung.com/java-equals-hashcode-contracts)