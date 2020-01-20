# 实验一：基于 JoeQ 的数据流分析

计63 陈晟祺 2016010981

## 数据流方程求解

求解过程主要在 `MySolver` 类的 `visitCFG` 方法中实现，其中我添加的代码主要进行以下工作：

1. 初始化：除了调用 analysis 模块本身的 `preprocess` 之外，还需要进行通用的初始化。首先找到所有转向出口（即 successor 为 `null`) 的 Quad 和进入的 Quad，前者可能有多个，后者有且仅有一个。而后，根据方向，设置方程初始条件（如前向迭代将所有的 `OUT` 初始化为 Top 元素，后向则初始化 `IN`）。
2. 迭代求解：这一步即为求解数据流方程的主要工作。我们以前向迭代为例，只需要用 `QuadIterator` 对于每个 Quad，获取其所有前驱的 `OUT` 并通过 `meetWith` 方法得到其新的 `IN`，而后作用调用 `processQuad` 来得到其新的 `OUT`。如果所有 Quad 的 `OUT` 都没有变化，那么停止迭代。对于反向迭代这一过程也是类似的，不再赘述。
3. 后处理：在主体迭代完成后，还需要进行最后的处理。对于前向迭代，其 `EXIT` 应该是所有转向出口的 Quad 的 `OUT` 经过 `meetWith` 作用后的结果；对于后向迭代，其 `ENTRY` 就是入口 Quad 的 `IN` 的值。最后，调用 analysis模块本身的 `postprocess` 输出结果即可。

需要注意的是，每次调用 analysis 的 `newTempVar` 后，在与任何已有的值进行 meet 运算之前，都需要将其初始化为 Top 元素（即调用 `setToTop` 方法），否则行为可能是不正确的。

## ReachingDef 求解

ReachingDef 问题是一个前向数据流方程，其 Top 为空集，Bottom 为所有的定义语句编号，而 meet 操作则为求并集。

我们在 analysis 类中定义的数据流对象称为 `ReachingDefs`，每一个对象均包含一个 `TreeSet<Integer>`，表示能够到达此处的定义语句编号（由于是 `TreeSet`，所以天然是有序的）。

对于所有给寄存器 `a` 赋值的语句，我们定义非恒等的转移函数，其将会从输入中移除所有对 `a` 的定义语句编号（见 `killDefs` 函数），并添加当前的语句编号（见 `addDef` 函数）。为了方便进行这些操作，我们维护了全局所有定义语句与变量名称的对应关系。

在初始化时，需要遍历 Quads 向上述全局对应关系中添加条目。而 `ENTRY` 保持为空。还有一个需要注意的细节是不能输出其他求解器都会输出的 `Initialization completed.`。

## Faintness 求解

Faintness 问题与 Liveness 问题类似，是一个后向数据流方程。其 Top 为所有变量，Bottom 为空集，而 meet 操作为求交集（因为只有所有后继都 faint，才能安全地在某处标记为 faint）。

我们沿用了 Liveness 分析的部分代码（包括数据流对象 `VarSet`），按照上述对方程的描述修改了部分定义。由于只要求对二元运算和 MOVE 操作传播 faintness 信息，所以定义 `TransferFunction` 时，需要重载 `visitMove` 和 `visitBinary` 方法，它们都会考察语句的目标寄存器，如果不在当前 faint 集合中，则将语句的所有源寄存器从 faint 集合中移除（如果存在）。考虑到副作用，需要重载 `visitQuad` 方法，对于其他所有使用了源寄存器的语句，都强制将所有使用到的寄存器从 faint 集合中移除。我们实现了 `wakeVar` 函数，用于更方便地进行这些操作。

由于此时对于不同类型的语句需要调用不同的方法（用到了  Visitor 设计模式），在 `processQuad` 方法中不可直接调用 `TransferFunction` 的方法。与常数传播框架中的实现类似，我们需要使用  `Helper.runPass(q, transferfn)` 来正确地访问各种语句。

在初始化时，需要向全集中添加所有 Quad 中用到的变量，并且将 `EXIT` 设置为全集（因为所有变量在整个程序执行完后显然全部不活跃）。

## Faintness 测试

为了测试 Faintness 算法的正确性，我添加了以下的测例：

1. `testReturn` ：修改自 `test1`，测试简单情况
2. `testSideEffect`：测试是否能够正确处理具有副作用的语句
3. `testOptimization`：测试经过编译器优化后的情形
4. `testControlFlow`：测试在控制流分支时的情形
5. `testComplexControlFlow`：测试在控制流有多重分支时的情形

具体的说明在 `TestFaintness.java` 代码中给出。需要说明的是测试 3 来自于一个观察，即如果直接编写测试，Java 编译器会进行常数折叠，因此基本上分析得不到什么有意义的结果。所以我们在其它测试中均使用了 `Integer.valueOf` 来强迫编译器不进行这些优化（事实上将某些变量作为函数参数等 NAC 也可以达到这个效果）；而在测试 3 中，我们有意让编译器进行优化，从而体现实际的不同。

最后的测试效果均是预期中的，说明 Faintness 的求解应当是没有问题的。

## 总结

本实验中，我们实现了一个简单的数据流方程求解算法，进行 ReachingDef 和 Faintness 两个数据流问题的求解，并测试其正确性。总体来说，实验要求比较清晰，难度也并不大。然而，受到原框架的限制和 Java 语法的约束，整体的代码显得不甚清晰，甚至存在冗余。希望今后的实验在这一方面能够有所改善。