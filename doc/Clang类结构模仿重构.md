# Clang类结构模仿重构

通过上面的讨论，形成了以下类：

```python
VarDecl
ParmVarDecl
FunctionDecl

BreakStmt
CompoundStmt
ContinueStmt
IfStmt
ReturnStmt
WhileStmt
DeclStmt
BinaryOperator
CallExpr
DeclRefExpr
ParenExpr
UnaryOperator
OpaqueValueExpr

IntegerLiteral
StringLiteral
```

接下来需要分别讨论之间的关系以及 llvm ir 的生成策略

## 一、类间关系

### 1、顶层类：Decl

![Inheritance graph](https://clang.llvm.org/doxygen/classclang_1_1DeclaratorDecl__inherit__graph.png)

Decl 类包含了所有表征定义的类，自顶向下顺序来说：

* NamedDecl：表征声明是有名变量，有一些变量的声明可以不需要声明名字，这是 C++ 中的某些特性。在我们的实验中不需要考虑。
* ValueDecl：Represent the declaration of a variable (in which case it is an lvalue) a function (in which case it is a function designator) or an enum。简单来说，是含有实际内容的定义，包括变量、函数的定义，这也是本次实验的限定，所以不需要考虑。
* DeclaratorDecl：待定，反正没啥用

我们所需要的声明（无论是之后的数组还是函数）DeclaratorDecl层已经够了，即保留一个顶层即可（直接就是Decl）。

![image-20221102205614815](C:\Users\DELL\AppData\Roaming\Typora\typora-user-images\image-20221102205614815.png)

### 2、顶层类：Stmt

Stmt 是最基本的结构，表征一个语句，一般带有分号。我们涉及的 Stmt 如下图所示：

![Stmt](C:\Users\DELL\Desktop\Stmt.png)

在Expr上层其实还存在一个ValueStmt，但上述一样可以省略。上文讨论到的用于类型转换类的CastExpr，此处并没用到。其中DeclStmt的实质内容，是Decl。

下面综述一下 Expr 各部分的含义：

* BinaryOperator：二元运算符，包含 =
* CallExpr：函数调用，如printf，getint等
* DeclRefExpr：变量的引用，如 b = a*2，则 a 就是DeclRefExpr类。
* ParenExpr：带括号的表达式
* UnaryOperator：一元运算符表达式

### 3、重构前后信息量

![Root](C:\Users\DELL\Desktop\Root.png)

按照SysY文法，构建等价语法结构如上图所示，既然能够构建得出，说明此种分类方式的表现力足够，因此可以用这些类搭建AST。

## 二、LLVM生成策略

总的来说，对于上面重构的AST形式，我期望的生成方式是在各类内通过统一的方法进行 IR 生成。对此我们需分析上述各个类的 IR 耦合度是否支持我们这样做。当然，在此之前，必须对LLVM IR 的语法进行简单陈述。

### 1、LLVM IR

#### Instructions

* opcode：add，sub
* operands
* one or zero result values
* Explicitly typed

#### Instruction classes

* Arithmetic instructions

  ```C
  %sum = add i32 %a, 10
  ```

* Compare Instructions

  ```c
  %cond = icmp eq i32 %sum, 99
  ```

* Control Flow instructions (terminator instructions)

  ```C
  br i1 %cond, label %bb1, label %bb2
  ret i32 %sum
  ```

* Call instructions

  ```C
  call void @use(i32 %sum)
  ```

* Load / Store

  ```C
  %a = load i32, i32* %ptr
  store i32 10, i32* %ptr
  ```

### 2、LLVM IR耦合性测试（声明与控制流部分）

#### 1、main函数结构

```C
int main() {
	return 0;
}
```

```lisp
define dso_local i32 @main() #0 {
  %1 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  ret i32 0
}
```

对于main函数而言，其实可以设置一个标识位（作为一个普遍意义上的函数而言），从而上面 LLVM IR 的前两行可以略去（这两行本是为函数返回值留的）。

```lisp
define dso_local i32 @main() #0 {
  ret i32 0
}
```

#### 2、DeclStmt 结构测试

**变量常量声明**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     

```C
const int m;
int a;
int main() {
	int i = 2;
	const int b = 3;
	return 0;
}
```

```lisp
@m = dso_local constant i32 0, align 4
@a = dso_local global i32 0, align 4

define dso_local i32 @main() #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4                                                                             
  store i32 0, i32* %1, align 4
  store i32 2, i32* %2, align 4
  store i32 3, i32* %3, align 4
  ret i32 0
}
```

可见，对于**常量，变量**的定义，LLVM仅做存储记录的操作，更多要做的是符号表中信息的记录。除此之外，全局变量声明更具有独特意义，因此可以单独作为一类。

而对于函数的定义，其实 main 本身就是一个函数，可以看出其结构。由于我们实验的程序没有涉及多个 C 文件交互，所以都是dso_local，唯一需要注意的是类型（ i32 or void）和名称 `@name`。由于变量函数的两种声明区分明显，因此可以略作划分：VarDeclStmt 与 FuncDeclStmt。

**带参函数声明**

```C
void f1(int a, int b) {}
```

```lisp
define dso_local void @f1(i32 %0, i32 %1) #0 {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  store i32 %1, i32* %3, align 4
  store i32 %0, i32* %4, align 4
  ret void
}
```

可见只需为参数分配内存并存入即可，即使是 void 类型，最终也要返回 void。

#### 3、ReturnStmt 结构测试

除了上面 main 函数直接返回 0 以外，下面测试一下自定义函数返回值。

```C
int f1(int a, int b) {
	return a + b;
}
```

```lisp
define dso_local i32 @f1(i32 %0, i32 %1) #0 {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  store i32 %1, i32* %3, align 4
  store i32 %0, i32* %4, align 4
-  %5 = load i32, i32* %4, align 4
-  %6 = load i32, i32* %3, align 4
-  %7 = add nsw i32 %5, %6
  ret i32 %7
}
```

标注的三行是 return 语句对应的 IR。可见，其是通过表达式计算，得到最终结果然后 ret 这个结果的。

> ps：标记nsw指出这个加法操作没有有符号回绕（no signed wrap），这表示指令已知没有溢出，允许某些优化。如果你对nsw标记背后的历史感兴趣，Dan Gohman在http://lists.cs.uiuc.edu/pepermail/llvmdev/2011-December/045924.html的文章值得一读。
>
> 在我们的实验中，nsw可以不必要考虑。

#### 4、IfStmt 结构测试

```C
int main() {
	int a = 1, b = 2;
	if(a) {} 
	else {}
	return 0;
}
```

```lisp
define dso_local i32 @main() #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  store i32 1, i32* %2, align 4
  store i32 2, i32* %3, align 4
  %4 = load i32, i32* %2, align 4
  %5 = icmp ne i32 %4, 0
  br i1 %5, label %6, label %7
6:                                                ; preds = %0
  br label %8
7:                                                ; preds = %0
  br label %8
8:                                                ; preds = %7, %6
  ret i32 0
}
```

从上面的结构可以看出，if 所包含的三部分，也都是有迹可循的。

* Cond 部分，该部分需要在 if 开始时就计算清楚，并且要判断 Expr 结果类型，如果是 i1 类型，就直接 br；否则要增加一个变量来存储 `icmp ne i32 var, 0` 的结果，转换成 bool 类型再 br。
* then 和 else 部分，都是作为一个标签引出的，如果没有 else 部分，则只有 then 部分的标签
* 结束标签，if 的结尾需要一个 end （上例中的 label %8），then 和 else 语句末尾都需要 br 到这个结束标签。

#### 5 、WhileStmt 结构测试

```C
int main() {
	int i = 2;
	while(i > 0) {
		i = i - 1;
	}
	return 0;
}
```

```lisp
define dso_local i32 @main() #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  store i32 2, i32* %2, align 4
  br label %3
3:                                                ; preds = %6, %0
  %4 = load i32, i32* %2, align 4
  %5 = icmp sgt i32 %4, 0
  br i1 %5, label %6, label %9
6:                                                ; preds = %3
  %7 = load i32, i32* %2, align 4
  %8 = sub nsw i32 %7, 1
  store i32 %8, i32* %2, align 4
  br label %3
9:                                                ; preds = %3
  ret i32 0
}
```

容易看出，while是必然包含三个标签的（不考虑优化）

* 入口标签：上例中的 `label %3` 直接 br 进入，后续循环也需要重新跳到这个地方。
* Cond：然后立即计算条件，与 IfStmt 的Cond 一致。根据计算结果判断，是br到下面的 CompoundStmt 部分还是结束标签。
* 循环体：本质是一个 CompoundStmt，不过最后需要 br 到入口标签

### 3、LLVM IR 耦合性测试（表达式部分）

这部分单独分出来是有所考虑的，因为 Expr 是一个相对复杂的结构，种类上包含诸多差别较大的 Expr，而且 BinaryOperator 有重构的可能，下面将一一陈述。

#### 1、BinarOperator 结构测试

从我们的文法中可以看出，Exp 其实是分两类的，一个是逻辑Exp（Cond），一种是算术Exp（Exp），在此基础上我们进行一下测试：

```C
int f(int a, int b) {
	if((a>4)&&(b<5)) {
		return 1*a + b / 4;
	}
	return 0;
}
```

```lisp
define dso_local i32 @f(i32 %0, i32 %1) #0 {
  %3 = alloca i32, align 4						; 给返回值留的空，下面 store 部分可以看到很多次
  %4 = alloca i32, align 4
  %5 = alloca i32, align 4
  store i32 %1, i32* %4, align 4
  store i32 %0, i32* %5, align 4
  %6 = load i32, i32* %5, align 4				; 符号表记录了参数的位置，用的时候查表可得
  %7 = icmp sgt i32 %6, 4
  br i1 %7, label %8, label %17

8:                                                ; preds = %2
  %9 = load i32, i32* %4, align 4
  %10 = icmp slt i32 %9, 5
  br i1 %10, label %11, label %17

11:                                               ; preds = %8
  %12 = load i32, i32* %5, align 4
  %13 = mul nsw i32 1, %12
  %14 = load i32, i32* %4, align 4
  %15 = sdiv i32 %14, 4
  %16 = add nsw i32 %13, %15
  store i32 %16, i32* %3, align 4
  br label %18

17:                                               ; preds = %8, %2
  store i32 0, i32* %3, align 4
  br label %18

18:                                               ; preds = %17, %11
  %19 = load i32, i32* %3, align 4
  ret i32 %19
}
```

对于 Cond 部分： `(a>4)&&(b<5)`，涉及短路求值，即如果 $a>4$ 不满足，直接全不满足，从而能够减少计算量。所以这里设有跳转符号，这与算术 Exp 计算大大不同。而对于算术 Exp 来说（上例的 `label %11` 部分），只需要自底向上进行计算与存储即可，然后把结果存到要返回的地方（这个应当告诉算术 Exp）。

所以 Exp 起码可以分成两部分，CondExp 和 ArithExp。

除此之外，还有一种格格不入的 Exp，即赋值表达式，在我们文法中形式为 Lval = Exp，把该形式的表达式并入二元表达式中，是很抽象的，下面进行测试。

```C
void f(int a, int b) {
	int c;
	c = a + b;
}
```

```lisp
define dso_local void @f(i32 %0, i32 %1) #0 {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca i32, align 4
  store i32 %1, i32* %3, align 4
  store i32 %0, i32* %4, align 4
  %6 = load i32, i32* %4, align 4
  %7 = load i32, i32* %3, align 4
  %8 = add nsw i32 %6, %7
  store i32 %8, i32* %5, align 4
  ret void
}
```

其实也比较简单，这主要是符号表的功劳，对于赋值类语句，左边的符号必定是被定义过的，在Exp计算结果出来后（存在一个新的变量里），将该结果重新 store 给之前为左边符号 alloca 的位置。所以如果要重新分出一个 `AssignStmt` 来的话，就是先计算Exp的值（结果存在Exp自定义的位置，这个应该有两种情况，一种是自定义位置存结果，一种是父级指定存的位置）。然后查表（必定查得到，不然报错），将结果存到表中记录的位置。

#### 2、CallExpr结构测试

前面的例子偷偷测试过了

```lisp
%1 = call i32 @f1()
```

形如以上结构，当然也可能没返回值的，那就不需要一个赋值表达式包裹。

#### 3、DeclRefExpr结构测试

其本质是查表，查符号表找到变量的位置（其实是变量号：`%1`,`%2`......，`%2*` 就是地址），然后用就对了。

#### 4、ParenExpr结构测试

这个。。。语法分析已经搞过了，形成的语法树里面好像可以没有，因为语法树本身就确定了计算顺序，括号有无都没有区别。所以分析到这里，ParenExpr 这个类都可以不需要了。

#### 5、UnaryOperator

这个类其实是对单个数的包裹，测试如下：

```C
void f(int a, int b) {
	int c;
	if(!(a>b) && (b>0)) {
		c = -a + - +b;
	}
}
```

```lisp
define dso_local void @f(i32 %0, i32 %1) #0 {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca i32, align 4
  store i32 %1, i32* %3, align 4  b
  store i32 %0, i32* %4, align 4  a
  %6 = load i32, i32* %4, align 4 a
  %7 = load i32, i32* %3, align 4 b
  %8 = icmp sgt i32 %6, %7
  br i1 %8, label %18, label %9

9:                                                ; preds = %2
  %10 = load i32, i32* %3, align 4
  %11 = icmp sgt i32 %10, 0
  br i1 %11, label %12, label %18

12:                                               ; preds = %9
  %13 = load i32, i32* %4, align 4
  %14 = sub nsw i32 0, %13
  %15 = load i32, i32* %3, align 4
  %16 = sub nsw i32 0, %15
  %17 = add nsw i32 %14, %16
  store i32 %17, i32* %5, align 4
  br label %18

18:                                               ; preds = %12, %9, %2
  ret void
}
```

对此我的评价是，LLVM 真狡猾，可以看出它实现Cond中的 `!` 符号是通过翻转条件的，个人认为这一点可以作为Cond的一个标识符，从而能对自身的 op 进行调整，但是这一点对于摩根律比较棘手，通过标识符的传递（自顶向下）可能可以解决。且对于前面谈到的，如果 Cond 是一个 ArithExp 我们可以直接进行更改（改为Cond），可以通过增加 `==` op做到。

对于 ArithExp 中的 UnaryOperator 来说，LLVM IR 的解决方法也是转化为 ArithExp，即 `-x` 的全部改为 `sub i32 0, x`. 

从上面讨论可以得知，其实连 UnaryOperator 类也不需要了（通过 CondExp 和 ArithExp）进行了统一。

---

通过上面的耦合性测试结果可知，最终形成的类为下图所述：

![image-20221102202148500](C:\Users\DELL\AppData\Roaming\Typora\typora-user-images\image-20221102202148500.png)

在此基础上，可以开始构建AST，并且在各成分内部进行代码生成，当然在此之前，需要维护全局的符号表等，这将在下一章进行讨论。，