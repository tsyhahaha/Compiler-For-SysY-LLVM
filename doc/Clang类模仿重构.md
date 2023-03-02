# Clang类模仿重构

为了更为结构化的生成中间代码，本文将SysY文法与Clang类一一对应，以重构出节点类，重新构建AST（之前的树顶多算是Parse tree，而非AST）

## 一、声明

### 1、变量声明

先从最简单的情况开始：

```C
const int a = 1;
int b = 2;
```

AST对应结构：

```c
VarDecl 0x26b281623d8 <.\main.c:1:1, col:15> col:11 a 'const int' cinit
| `-IntegerLiteral 0x26b283b7958 <col:15> 'int' 1
`-VarDecl 0x26b283b7998 <line:2:1, col:9> col:5 b 'int' cinit
  `-IntegerLiteral 0x26b283b7a00 <col:9> 'int' 2
```

可见简单的变量声明，表观来看是由同一个 VarDecl 表征的。

再看一下更为复杂的定义：

```C
int b = 2 + 3 * 2;
```

```C
VarDecl 0x18bde6123d8 <.\main.c:1:1, col:17> col:5 b 'int' cinit
  `-BinaryOperator 0x18bde8a9a10 <col:9, col:17> 'int' '+'
    |-IntegerLiteral 0x18bde8a9978 <col:9> 'int' 2
    `-BinaryOperator 0x18bde8a99f0 <col:13, col:17> 'int' '*'
      |-IntegerLiteral 0x18bde8a99a0 <col:13> 'int' 3
      `-IntegerLiteral 0x18bde8a99c8 <col:17> 'int' 2
```

可见定义中的表达式计算，是由 BinaryOperator 类主导的。

最后看一下更为复杂的变量引用：

```C
const int a = 2;
int b = 2 + 3 * a;
```

```C
`-VarDecl 0x1baf8b38178 <line:2:1, col:17> col:5 b 'int' cinit
  `-BinaryOperator 0x1baf8b38288 <col:9, col:17> 'int' '+'
    |-IntegerLiteral 0x1baf8b381e0 <col:9> 'int' 2
    `-BinaryOperator 0x1baf8b38268 <col:13, col:17> 'int' '*'
      |-IntegerLiteral 0x1baf8b38208 <col:13> 'int' 3
      `-ImplicitCastExpr 0x1baf8b38250 <col:17> 'int' <LValueToRValue>
        `-DeclRefExpr 0x1baf8b38230 <col:17> 'const int' lvalue Var 0x1baf8876be8 'a' 'const int'
```

看得出，这种变量引用，是DeclRefExpr表征的，外层ImplicitCastExpr是CastExpr的子类，而后者是用于类型转换，易知我们的实验不涉及这一点。

### 2、函数声明

#### main函数

```C
int main() {
	return 0;
}
```

```C
FunctionDecl 0x276074f9160 <.\main.c:1:1, line:3:1> line:1:5 main 'int ()'
  `-CompoundStmt 0x276074f9280 <col:12, line:3:1>
    `-ReturnStmt 0x276074f9270 <line:2:2, col:9>
      `-IntegerLiteral 0x276074f9248 <col:9> 'int' 0
```

#### 自定义函数

```C
int f1(int a) { }
void f2 () { }
```

```C
|-FunctionDecl 0x24ea4d23f88 <.\main.c:1:1, col:17> col:5 f1 'int (int)'
| |-ParmVarDecl 0x24ea4c545d8 <col:8, col:12> col:12 a 'int'
| `-CompoundStmt 0x24ea4d24078 <col:15, col:17>
`-FunctionDecl 0x24ea4d240e0 <line:2:1, col:14> col:6 f2 'void ()'
  `-CompoundStmt 0x24ea4d24180 <col:12, col:14>
```

容易看出，main函数与自定函数的定义形式一致，因此可以选择合并，这一点也可以从llvm ir 中看出：

```lisp
; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @f1(i32 %0) #0 {
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  store i32 %0, i32* %3, align 4
  %4 = load i32, i32* %2, align 4
  ret i32 %4
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @f2() #0 {
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @main() #0 {
  %1 = call i32 @f1()
  call void @f2()
  ret i32 0
}
```

毕竟main函数也是一种函数，所以合并无可厚非。

除此之外，还可看出，函数体内部，是由一个叫做 CompoundStmt 类包裹的，其本质是表示SysY文法中的Block概念，即只要是花括号包裹的区域，都是一个CompoundStmt，这一点可以通过clang命令验证。

上面的例子是带有函数传参的，对于传参所定义的参数，需要用 ParmVarDecl 表征。

## 二、表达式

对于表达式的表示，直接用最复杂的表达式测试即可，这样才能看到表征一个表示的所有类。

```C
int f1() {
	return 2*3 + -4* (-5) + ( 0 || !8 && (1==1) && (2 > 3))
}
```

```C
FunctionDecl 0x209d9a0a190 <.\main.c:1:1, line:3:1> line:1:5 f1 'int ()'
  `-CompoundStmt 0x209d9a0a600 <col:10, line:3:1>
    `-ReturnStmt 0x209d9a0a5f0 <line:2:2, col:56>
      `-BinaryOperator 0x209d9a0a5d0 <col:9, col:56> 'int' '+'
        |-BinaryOperator 0x209d9a0a3a8 <col:9, col:22> 'int' '+'
        | |-BinaryOperator 0x209d9a0a2c8 <col:9, col:11> 'int' '*'
        | | |-IntegerLiteral 0x209d9a0a278 <col:9> 'int' 2
        | | `-IntegerLiteral 0x209d9a0a2a0 <col:11> 'int' 3
        | `-BinaryOperator 0x209d9a0a388 <col:15, col:22> 'int' '*'
        |   |-UnaryOperator 0x209d9a0a310 <col:15, col:16> 'int' prefix '-'
        |   | `-IntegerLiteral 0x209d9a0a2e8 <col:16> 'int' 4
        |   `-ParenExpr 0x209d9a0a368 <col:19, col:22> 'int'
        |     `-UnaryOperator 0x209d9a0a350 <col:20, col:21> 'int' prefix '-'
        |       `-IntegerLiteral 0x209d9a0a328 <col:21> 'int' 5
        `-ParenExpr 0x209d9a0a5b0 <col:26, col:56> 'int'
          `-BinaryOperator 0x209d9a0a590 <col:28, col:55> 'int' '||'
            |-IntegerLiteral 0x209d9a0a3c8 <col:28> 'int' 0
            `-BinaryOperator 0x209d9a0a570 <col:33, col:55> 'int' '&&'
              |-BinaryOperator 0x209d9a0a4c0 <col:33, col:44> 'int' '&&'
              | |-UnaryOperator 0x209d9a0a418 <col:33, col:34> 'int' prefix '!' cannot overflow
              | | `-IntegerLiteral 0x209d9a0a3f0 <col:34> 'int' 8
              | `-ParenExpr 0x209d9a0a4a0 <col:39, col:44> 'int'
              |   `-BinaryOperator 0x209d9a0a480 <col:40, col:43> 'int' '=='
              |     |-IntegerLiteral 0x209d9a0a430 <col:40> 'int' 1
              |     `-IntegerLiteral 0x209d9a0a458 <col:43> 'int' 1
              `-ParenExpr 0x209d9a0a550 <col:49, col:55> 'int'
                `-BinaryOperator 0x209d9a0a530 <col:50, col:54> 'int' '>'
                  |-IntegerLiteral 0x209d9a0a4e0 <col:50> 'int' 2
                  `-IntegerLiteral 0x209d9a0a508 <col:54> 'int' 3
```

可看出，除了 BinaryOperator 外，对于类似 `-4` 此类，含有一元表达式，需要用 UnaryOperator 表征；对于带括号的表达式，需要用 ParenExpr 表征。

## 三、控制流

### 1、函数调用

```C
int f1() { return 2*3 + -4* (-5) + ( 0 || !8 && (1==1) && (2 > 3)); }

void f2() { }

int main() {
	int a;
	a = f1();
	f2();
	return 0;
}
```

```C
FunctionDecl 0x16de3042978 <line:10:1, line:15:1> line:10:5 main 'int ()'
  `-CompoundStmt 0x16de3042c40 <col:12, line:15:1>
    |-DeclStmt 0x16de3042a98 <line:11:2, col:7>
    | `-VarDecl 0x16de3042a30 <col:2, col:6> col:6 used a 'int'
    |-BinaryOperator 0x16de3042b58 <line:12:2, col:9> 'int' '='
    | |-DeclRefExpr 0x16de3042ab0 <col:2> 'int' lvalue Var 0x16de3042a30 'a' 'int'
    | `-CallExpr 0x16de3042b38 <col:6, col:9> 'int'
    |   `-ImplicitCastExpr 0x16de3042b20 <col:6> 'int (*)()' <FunctionToPointerDecay>
    |     `-DeclRefExpr 0x16de3042ad0 <col:6> 'int ()' Function 0x16de30423c0 'f1' 'int ()'
    |-CallExpr 0x16de3042be8 <line:13:2, col:5> 'void'
    | `-ImplicitCastExpr 0x16de3042bd0 <col:2> 'void (*)()' <FunctionToPointerDecay>
    |   `-DeclRefExpr 0x16de3042b78 <col:2> 'void ()' Function 0x16de30428a0 'f2' 'void ()'
    `-ReturnStmt 0x16de3042c30 <line:14:2, col:9>
      `-IntegerLiteral 0x16de3042c08 <col:9> 'int' 0
```

可见，函数调用是通过 CallExpr 类表征的，同时从中也可以发现，若在函数体内部定义变量，需包裹在DeclStmt中。

### 2、if 语句

```C
int main() {
	if() {
		
	} else {
		
	}
	return 0;
}
```

```C
FunctionDecl 0x18ef75bb130 <.\main.c:1:1, line:8:1> line:1:5 main 'int ()'
  `-CompoundStmt 0x18ef75bb2b0 <col:12, line:8:1>
    |-IfStmt 0x18ef75bb250 <line:2:2, line:6:2> has_else
    | |-OpaqueValueExpr 0x18ef75bb238 <<invalid sloc>> '_Bool'
    | |-CompoundStmt 0x18ef75bb218 <line:2:7, line:4:2>
    | `-CompoundStmt 0x18ef75bb228 <col:9, line:6:2>
    `-ReturnStmt 0x18ef75bb2a0 <line:7:2, col:9>
      `-IntegerLiteral 0x18ef75bb278 <col:9> 'int' 0
```

可见，if 控制流是由 IfStmt 表征，且由三部分构成（如果没有else则只有两部分：

* Expr：这里的OpaqueValueExpr其实是因为 if 的 cond 中没有写东西，所以解析为“不透明（Opaque）指针”（Clang误认为我不想透露这里的数据类型）。
* CompoundStmt

### 3、循环语句与break和continue

```C
int main() {
	while(2>3) {
		break;
		continue;
	}
	return 0;
}
```

```C
FunctionDecl 0x1be18d628c0 <.\main.c:1:1, line:7:1> line:1:5 main 'int ()'
  `-CompoundStmt 0x1be18d62aa0 <col:12, line:7:1>
    |-WhileStmt 0x1be18d62a48 <line:2:2, line:5:2>
    | |-BinaryOperator 0x1be18d629f8 <line:2:8, col:10> 'int' '>'
    | | |-IntegerLiteral 0x1be18d629a8 <col:8> 'int' 2
    | | `-IntegerLiteral 0x1be18d629d0 <col:10> 'int' 3
    | `-CompoundStmt 0x1be18d62a28 <col:13, line:5:2>
    |   |-BreakStmt 0x1be18d62a18 <line:3:3>
    |   `-ContinueStmt 0x1be18d62a20 <line:4:3>
    `-ReturnStmt 0x1be18d62a90 <line:6:2, col:9>
      `-IntegerLiteral 0x1be18d62a68 <col:9> 'int' 0
```

循环语句倒没有OpaqueValueExpr类作为判断，这个存疑。对于 break 和 continue 都有独自的 Stmt 对应。

### 4、return语句

从前文可见，是由一个 ReturnStmt 表征。

### 5、getint 和 printf

```C
int main() {
	int a;
	a = getint();
	printf("123");
	return 0;
}
```

```C
-FunctionDecl 0x23c332d6ca0 <.\main.c:1:1, line:6:1> line:1:5 main 'int ()'
| `-CompoundStmt 0x23c332d7748 <col:12, line:6:1>
|   |-DeclStmt 0x23c332d6e08 <line:2:2, col:7>
|   | `-VarDecl 0x23c332d6da0 <col:2, col:6> col:6 used a 'int'
|   |-BinaryOperator 0x23c332d6fa8 <line:3:2, col:13> 'int' '='
|   | |-DeclRefExpr 0x23c332d6e20 <col:2> 'int' lvalue Var 0x23c332d6da0 'a' 'int'
|   | `-CallExpr 0x23c332d6f88 <col:6, col:13> 'int'
|   |   `-ImplicitCastExpr 0x23c332d6f70 <col:6> 'int (*)()' <FunctionToPointerDecay>
|   |     `-DeclRefExpr 0x23c332d6f18 <col:6> 'int ()' Function 0x23c332d6e68 'getint' 'int ()'
|   |-CallExpr 0x23c332d76b8 <line:4:2, col:14> 'int'
|   | |-ImplicitCastExpr 0x23c332d76a0 <col:2> 'int (*)(const char *, ...)' <FunctionToPointerDecay>
|   | | `-DeclRefExpr 0x23c332d75d0 <col:2> 'int (const char *, ...)' Function 0x23c332d7458 'printf' 'int (const char *, ...)'
|   | `-ImplicitCastExpr 0x23c332d76f8 <col:9> 'const char *' <NoOp>
|   |   `-ImplicitCastExpr 0x23c332d76e0 <col:9> 'char *' <ArrayToPointerDecay>
|   |     `-StringLiteral 0x23c332d7630 <col:9> 'char [4]' lvalue "123"
|   `-ReturnStmt 0x23c332d7738 <line:5:2, col:9>
|     `-IntegerLiteral 0x23c332d7710 <col:9> 'int' 0
`-FunctionDecl 0x23c332d7458 <line:4:2> col:2 implicit used printf 'int (const char *, ...)' extern
  |-ParmVarDecl 0x23c332d74f8 <<invalid sloc>> <invalid sloc> 'const char *'
  `-FormatAttr 0x23c332d7568 <col:2> Implicit printf 1 2
```

可以看出，作为内置函数，printf在调用上与一般函数没有区别（除了需要 StringLiteral 来表征字符串）。但 printf 其实自动在代码末尾进行了注册，我们的实验其实也要求这样做，不过是在文首进行声明。