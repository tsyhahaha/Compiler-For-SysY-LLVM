# Clang类模仿实现

![CompUnit](C:\Users\DELL\Desktop\CompUnit.png)

如果要用Clang的类来取代节点类，第一要求就是表达力要足够。上图是文法所需的结构，我们的实现需能够表征。

## Root

根节点可以单独设置一个类，毕竟这一层具有诸多独特的意义。

```java
public class Root(){
    private List<VarDeclStmt> globalVars;	// 全局变量集合
    private List<DeclStmt> functions;	// 自定义函数集合
    private DeclStmt main;	// main函数
}
```

## VarDecl  and  FuncDecl

```java
public class VarDecl() extends Decl {
	private boolean isGlobal;	// 是否是全局变量，true： IR 需要加 global 标识
    private boolean isConst;	// 是否是
    private String name;
    // private Type type; 		// 我们的实验只有int一种类型，所以可不需要
    
}
```





