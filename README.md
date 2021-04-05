# iWatch
## 热补丁方案-Native(C/C++)层方案：
1. 支持Android5.0 ~ 11.x ~ 目前master主线代码
2. 支持修改method，新增class、field、method，以及支持部分inline方法的修复
3. 支持即时修复，即补丁下发即可生效
4. 方案稳定可靠
5. 配合[apkpatchplus](https://github.com/habbyge/apkpatchplus)项目，其生成补丁，在iWatch中加载、使用补丁

## 本地测试方法
adb push 补丁.apatch 到手机这个目录: /storage/emulated/0/Android/data/com.habbyge.iwatch/files/Music/

## 问题和限制(todo......有待突破)
1. 对 ***内部类*** 有限制，在内部类中需要新增调用方法时，则需要直接新建一个class然后调用该class，直接在内部类中调用其外部类则容易
   造成方法地址和index错乱，找不到方法，发生NoSuchMethodError crash。但是非内部类场景没有这个限制。不过还是建议新建class的方式来搞。
2. 内部类中修改需要访问外部类时，其访问的method、field、都需要设置为public，是为了阻止编译器自动合成synthric方法，造成方法地址或index错乱。
3. inline的目标函数，一般不能fix。
4. ......

## 坑(todo)
### 1. 内部类的坑
内部类引用外部类的哪个方法，就会找不到该方法。得出结论，一定是内部类改变了该方法。todo
java.lang.NoSuchMethodError: No virtual method b(Ljava/lang/String;)V in class Lcom/habbyge/sample/MainActivity_CF;
or its super classes (declaration of 'com.habbyge.sample.MainActivity_CF' appears in
/storage/emulated/0/Android/data/com.habbyge.iwatch/files/Music/app-release-2-cfff1f13df8681ea51edb3ea824af973.apatch)

### 2. 在同一个class中修改调用方法的坑
在开启proguard混淆、优化时，非内部类中方法调用一个修改后方法，需要以当前方法为起点，向上回溯，直到遇到keep住的方法停止，然后在这个被keep住
的方法修改一点代码(通常打一个日志)，来保证该keep函数也被修改，即被标记为FixMethodAnno，出现在patch文件中；否则会导致：
NoSuchMethodError exception，具体原因未知，我猜是地址偏移了，后面研究(todo); ------ 得找个方法来阻止地址变更。

### 3. 内部类和非内部类中如果新增field、method，都建议使用新增类的方式，防止破坏旧的类结构(字段索引、地址).
