# Javaee_javaparser

## F0 a

### 1. 运行方法

使用idea配置好项目后，运行MethodCallAnalyzer.java即可。

### 2. 类设计介绍

#### 2.1 `Analyzable`接口设计分析

##### 2.1.1 接口的职责

`Analyzable` 是一个泛型接口，表示一个可分析的实体。它定义了一个实体应该如何进行分析，但没有提供具体的实现。这样的设计可以使得任何实现此接口的类都必须提供对应的分析逻辑。

##### 2.1.2 泛型参数

- `T`: 表示返回的分析结果的类型。这使得该接口具有高度的通用性，可以用于分析并返回各种类型的结果。

##### 2.1.3 方法

- `List<T> analyze()`: 该方法定义了如何进行分析，并返回一个包含分析结果的列表。返回的列表类型为泛型参数`T`，这意味着具体的实现类可以返回任意类型的分析结果。

##### 2.1.4 使用场景

接口的设计是为了提供一个统一的方法来定义如何进行分析。任何类只要实现了这个接口，就表示它是可分析的，并且需要提供具体的分析方法。在之前提供的代码中，`ClassInfo` 类就实现了这个接口，表示它可以分析并返回方法的信息。

总体来说，`Analyzable`接口为实现它的类提供了一个标准化的方式来进行分析，并返回分析结果。这种设计提供了良好的扩展性和灵活性，允许添加更多的可分析的实体，而不需要对现有的代码进行大量的修改。

#### 2.2 `MethodCallAnalyzer`类设计分析

##### 2.2.1 类的职责

`MethodCallAnalyzer` 类主要负责获取用户输入的方法信息，并调用相应的分析器进行分析。它接收用户提供的方法名、类名和深度等信息，并利用这些信息进行方法调用的分析。

##### 2.2.2 属性

- `methodName`: 一个字符串，存储方法的名称。
- `packageName`: 一个字符串，存储方法所在的包名。
- `className`: 一个字符串，存储方法所在的类名。
- `depth`: 一个整数，表示分析的深度。

##### 2.2.3 主方法

- `public static void main(String[] args)`: 是程序的入口方法，创建Scanner对象获取用户的输入，并调用`analyzeMethodCall`方法进行分析。

##### 2.2.4 方法

- `public void analyzeMethodCall(String userInput)`: 主要负责处理用户的输入。它分割输入的字符串，从中提取方法名、类名、包名和深度等信息，然后使用`ProjectAnalyzer`进行方法调用的分析。

##### 2.2.5 用户输入处理

在程序的主方法中，首先使用Scanner对象来获取用户的输入。然后去除用户输入中的所有空白字符，并传递给`analyzeMethodCall`方法。

在`analyzeMethodCall`方法中，首先将用户的输入分割成不同的部分。对于合适的输入格式，该方法将提取方法名、完整的类名和深度信息。它进一步将完整的类名分割成包名和类名。最后，它验证深度信息的格式，并提取深度值。

如果用户的输入格式不正确，程序将给出相应的错误提示。

##### 2.2.6 方法调用分析

在提取了所有必要的信息之后，`MethodCallAnalyzer` 创建一个`ProjectAnalyzer`对象并调用其`analyzeSpecificMethod`方法，进行具体的方法调用分析。

总的来说，`MethodCallAnalyzer`类提供了一个简单的用户界面，允许用户输入方法调用的相关信息，并调用相应的分析器进行分析。

#### 2.3 `ProjectAnalyzer`类设计分析

##### 2.3.1 类的职责

`ProjectAnalyzer` 类主要负责分析Java项目中的类和方法。它可以扫描指定包内的所有Java文件，对其进行解析，并提取出类和方法的相关信息。

##### 2.3.2 属性

- `javaFiles`: 一个列表，存储项目中的所有Java文件。
- `packageName`: 一个字符串，代表项目的包名。
- `CONFIGURED_PARSER`: 是一个静态的 `JavaParser` 实例，已经被配置，用于解析Java文件。

##### 2.3.3 构造函数

- `ProjectAnalyzer(String packageName)`: 构造函数接收一个包名作为参数。在构造函数中，它初始化了Java文件列表和JavaParser的配置。

##### 2.3.4 方法

- `getJavaFiles(File directory)`: 这是一个静态方法，递归地从指定目录中获取所有Java文件。
- `analyze()`: 分析项目中的类，并返回一个类信息列表。这个方法主要遍历Java文件列表，解析每个Java文件并提取其中的类声明。
- `analyzeSpecificMethod(String methodName, String className, int depth)`: 分析指定类中的特定方法，并显示它调用的方法和被哪些方法调用。它首先调用 `analyze()` 方法来获取类信息列表，然后对这些类信息中的方法进行进一步分析。

##### 2.3.5 JavaParser和符号解析

类中使用了 `JavaParser` 和 `JavaSymbolSolver` 来解析Java代码。这两者都是JavaParser库的组成部分。其中：

- `JavaParser`: 用于解析Java源代码。
- `JavaSymbolSolver`: 用于解决Java代码中的符号。

为了支持符号解析，类中配置了 `CombinedTypeSolver`，它结合了 `ReflectionTypeSolver` 和 `JavaParserTypeSolver`。其中：

- `ReflectionTypeSolver`: 使用Java的反射API来解决类型。
- `JavaParserTypeSolver`: 使用JavaParser来解决类型。

#### 2.4 `ClassInfo`类设计分析

##### 2.4.1 类的职责

`ClassInfo` 类主要负责存储和分析关于Java类的信息，如类的名称、包名、以及类中定义的所有方法。

##### 2.4.2 属性

- `unit`: 类型为`CompilationUnit`，存储了类的所有源代码信息。
- `methods`: 是一个列表，存储类中的所有`MethodInfo`对象。

##### 2.4.3 构造函数

- `ClassInfo(CompilationUnit unit)`: 构造函数接收一个`CompilationUnit`对象作为参数。它初始化了类的源代码信息和`methods`列表。

##### 2.4.4 方法

- `analyze()`: 分析`CompilationUnit`中定义的所有类和接口，提取出其中的所有方法，并为每个方法创建一个`MethodInfo`对象，然后添加到`methods`列表中。
- `getClassName()`: 获取`CompilationUnit`中的第一个类或接口声明的名称。
- `getPackageName()`: 获取`CompilationUnit`中的包声明。如果包声明存在，则返回包名，否则返回一个空字符串。
- `getMethods()`: 返回类中的所有`MethodInfo`对象。

##### 2.4.5 JavaParser与类信息解析

在类中，使用了`JavaParser`的`CompilationUnit`对象来获取类的所有源代码信息。此外，通过调用`JavaParser`提供的方法，如`findAll`、`getMethods`、`getClassByName`和`getPackageDeclaration`等，来提取和解析类的相关信息。

##### 2.4.6 代码结构

在`analyze`方法中，首先找到`CompilationUnit`中所有的类和接口声明，然后对每个类或接口声明，提取出其中定义的所有方法。为每个方法创建一个`MethodInfo`对象，并添加到`methods`列表中。这样，`ClassInfo`类可以为外部提供有关类的所有方法的详细信息。

总的来说，`ClassInfo`类为外部提供了一个方便的方式，来获取和分析Java类的基本信息和方法信息。

#### 2.5 `MethodInfo`类设计分析

##### 2.5.1 类的职责

`MethodInfo` 类主要负责存储与分析关于Java方法的信息，如方法的声明、被当前方法调用的其他方法、以及调用当前方法的其他方法等。

##### 2.5.2 属性

- `declaration`: 类型为`MethodDeclaration`，存储Java方法的声明。
- `calledMethods`: 是一个列表，存储被当前方法调用的方法信息。
- `methodsCallingThis`: 是一个列表，存储调用当前方法的方法信息。

##### 2.5.3 构造函数

- `MethodInfo(MethodDeclaration declaration)`: 接收一个方法声明作为参数，并对`calledMethods`和`methodsCallingThis`进行初始化。

##### 2.5.4 方法

- `addCalledMethod(MethodInfo method)`: 添加一个被当前方法调用的方法到`calledMethods`列表中。
- `addMethodCallingThis(MethodInfo method)`: 添加一个调用当前方法的方法到`methodsCallingThis`列表中。
- `getMethodName()`: 获取当前方法的名称。
- `getClassName()`: 获取当前方法所在的类名。
- `analyze(List<MethodInfo> allMethods)`: 分析当前方法，找出它调用了哪些方法，并更新`calledMethods`和`methodsCallingThis`列表。
- `getInvokes(int depth, int currentDepth, String packageName)`: 获取此方法调用的所有方法，并以特定格式输出。
- `getInvokedBy(int depth, int currentDepth, String packageName)`: 获取调用此方法的所有方法，并以特定格式输出。

##### 2.5.5 方法调用和解析

类中使用了`JavaParser`的方法来解析方法的调用和被调用信息。它搜索所有的方法调用表达式，并尝试解析它们，然后对比它们与所有已知的方法，以确定调用关系。此外，类还提供了方法来生成关于方法调用关系的输出，以供外部使用。

##### 2.5.6 异常处理

在`analyze`方法中，当遇到无法解析的方法调用时，类会捕获异常并打印出相关信息，确保程序不会因为单个方法的问题而崩溃。



## F2

### 分析报告：MethodCallResolver 工具

#### 目的：
MethodCallResolver 是一个使用 JavaParser 库构建的工具，旨在解析 Java 源文件中的方法调用并确定方法调用的实际声明。这个工具能够处理 Java 代码中的动态绑定情况，即在运行时调用的方法取决于对象的实际类型，而不仅仅是其声明类型。

#### 功能：
- 解析 Java 源文件，提取方法调用表达式。
- 识别和记录对象创建表达式中的类型信息。
- 确定变量的实际类型，并通过映射跟踪变量名与实际类型的关系。
- 对每个方法调用，解析其实际引用的方法声明，即使方法在超类中定义但在子类中被重写。
- 输出方法调用表达式及其对应的完全限定方法签名。

#### 实现细节：
- **对象创建分析**：工具首先遍历源文件中的所有`ObjectCreationExpr`，这表示类似`new ClassName()`的表达式。通过 JavaParserFacade，每个这样的表达式都被转换为使用上下文，这样就可以确定对象的实际类型。然后，该类型与创建该对象的变量名相关联并存储在`typeMap`中。
  
- **方法调用解析**：接着，源文件中的每个`MethodCallExpr`都被分析。对于每个方法调用，如果它的作用域（即调用方法的对象）是一个变量表达式，工具会查找该变量的实际类型。基于实际类型，工具尝试找到对应的方法声明。如果找到，它将输出该方法的完全限定签名。

- **错误处理**：代码包含了异常处理逻辑，确保在解析过程中出现的任何错误（如类型解析失败）都会被捕获并报告。

#### 结果输出：
输出包括方法调用的文本表示和解析后得到的方法声明的完全限定签名。如果解析成功，这将准确地显示哪个类的哪个方法被调用，无论是在父类中定义还是在子类中重写。

#### 限制和假设：
- **代码环境依赖**：工具假设所有相关的源代码文件都可以访问，并且通过`JavaParserTypeSolver`正确指向了它们的路径。如果在路径下没有找到相应的源文件，解析将失败。
- **反射限制**：使用`ReflectionTypeSolver`能够解析标准库中的类型，但自定义类需要源文件路径正确设置。
- **类型解析的精确性**：类型解析的准确性取决于 JavaParser 和 Java Symbol Solver 的能力。这意味着复杂的继承结构或泛型可能会增加解析的复杂性和失败的可能性。
- **类型推断**：对于类型推断（如 Java 8 的 lambdas 或方法引用），这个工具的准确性可能会降低，因为类型推断可能需要更多的上下文信息。
- **多态性**：工具试图处理多态性，但它的成功取决于`typeMap`中信息的完整性和准确性。如果在对象创建和方法调用之间存在控制流复杂性，工具可能无法准确跟踪类型。
