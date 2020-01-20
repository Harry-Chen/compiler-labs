# 实验二：基于 JoeQ 的程序优化

计63 陈晟祺 2016010981

## 冗余 `NULL_CHECK` 分析

实验要求差找所有完全冗余的 `NULL_CHECK`，即如果某个变量在之前进行过 `NULL_CHECK` 并且没有被更改，则后续对其的 `NULL_CHECK` 就是完全冗余的。它事实上与 Must Reaching Def 问题是类似的，只需要检查一定能够到达某一个 `NULL_CHECK` 处的被检查的变量即可。因此，可以简单地使用 `Solver` 框架求解一个前向数据流问题（实现为 `submit.RedundantNullChecks` 类）。这个问题的求解对象为变量（寄存器），meet 操作是交，`ENTRY` 是空，top 是全集（所有出现的变量），bottom 为空集，转移函数为：
$$
f_q(B)=
\begin{cases}
B \cup \{R\}, &\text{q checks nullity of R} \\
B \backslash \{R\}, &\text{q defines R}, \\
B, &\text{otherwise}
\end{cases}
$$
然后，在 `postprocess` 中，对于每一个类型为 `NULL_CHECK` 的 quad，如果它检查的变量在其 `IN` 集合中，则说明这次检查时完全冗余的，将其打印出即可。这一实现在 `NullTest` 和 `SkipList` 类中都取得了正确的结果。

## 冗余 `NULL_CHECK` 移除

当计算出完全冗余的 quad 之后，可以简单地将其移除。具体地，我为 `RedundantNullChecks` 这个类增加了一个 `mode` 变量指示工作方式，默认为 `PRINT`，此时只进行分析。而在 `Optimize` 类中，我使用同样的方法调用数据流分析，而将 `mode` 置为 `REMOVE`，这样，在 `postprocess` 中，根据 `mode` 判断是否在发现冗余时直接进行移除即可。

经过测试，提供的 `NullTest`、`QuickSort` 和 `SkipList` 三个类在进行了移除后依旧可以正常工作，输出正确的结果。

| 程序      | NULL_CHECK数量（移除前） | NULL_CHECK数量（移除后） | 移除数量 |
| --------- | ------------------------ | ------------------------ | -------- |
| NullTest  | 328                      | 326                      | 2        |
| QuickSort | 1800                     | 1724                     | 76       |
| SkipList  | 5393                     | 4104                     | 1289     |

## 基于比较的 `NULL_CHECK` 移除

如果一个 `NULL_CHECK` 紧跟在一个 `IFCMP_A` 之后并且这一比较是某个变量与 `null` 进行的，则根据分支结果，可以直接移除一部分的 `NULL_CHECK`，如：

```
7   IFCMP_A                 R1 Integer,	AConst: null,	EQ,	BB4

BB3	(in: BB2, out: BB4)
9   NULL_CHECK              T-1 <g>,	R1 Integer
8   INVOKEVIRTUAL_A%        T2 String,	java.lang.Integer.toString ()Ljava/lang/String;,	(R1 Integer)

BB4	(in: BB2, BB3, out: BB5, BB6)
10  IFCMP_A                 R5 Integer,	R1 Integer,	NE,	BB6
```

此处的 Quad 9 就可以直接移除，因为由 Quad 7 跳转来时，`R1` 必定不是 `null`。

我们同样在 `RedundantNullChecks` 中实现这一检查，在 `postprocess` 中，执行完上面的完全冗余移除后，再次扫描所有的 Quads，对于所有形式为 `IFCMP_A Rx, null, EQ/NE, BBy` 的语句，根据其检查的结果（相等/不等）对应找到可能被移除的后继 Quad，并检查其是否为 `NULL_CHECK`，如果是并且其检查的变量为上面比较过的，则直接移除这一 `NULL_CHECK`。

根据测试，三个类在进行这一额外的优化后依旧能正常工作。事实上，只有 `NullTest` 类中含有这样的情况（如上面的例子所示），剩余两个类没有发生实际的代码移除。

