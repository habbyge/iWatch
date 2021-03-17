# iWatch
## 热补丁方案-Native(C/C++)层方案：
1. 支持Android5.0 ~ 11.x
2. 支持修改method，新增class、field、method，以及支持部分inline方法的修复
3. 支持即时修复，即补丁下发即可生效
4. 方案稳定可靠
5. 配合[apkpatchplus](https://github.com/habbyge/apkpatchplus)项目，其生成补丁，在iWatch中加载、使用补丁

## 测试样例
1. 非内部类的方法中修改字段、方法 ------ 验证成功(通过hellhound修改)；
2. 非内部类新增的方法(编译期自动生成的ok，主动添加的不行)， ------ ing......
3. 非内部类新增的字段， ------ ing...... 需要修改 apkpatch才能生效
4. 内部类修改方法和字段，要求public才行 ------ 验证成功(通过hellhound修改)
5. 内部类中新增方法
6. 内部类新增字段
7. 内部类新增字段
8. 新增类
9. ......

## 问题和限制(todo......有待突破)
1. 对 ***内部类*** 有限制，在内部类中需要新增调用方法时，则需要直接新建一个class然后调用该class，直接在内部类中调用其外部类则容易造成方法地址和index错乱，找不到方法，发生NoSuchMethodError crash。但是非内部类场景没有这个限制。不过还是建议新建class的方式来搞。
2. 内部类中修改需要访问外部类时，其访问的method、field、都需要设置为public，是为了阻止编译器自动合成synthric方法，造成方法地址或index错乱。
3. inline的目标函数，一般不能fix。
4. ......

### 内部类的坑
内部类引用外部类的哪个方法，就会找不到该方法。得出结论，一定是内部类改变了该方法。todo
java.lang.NoSuchMethodError: No virtual method b(Ljava/lang/String;)V in class Lcom/habbyge/sample/MainActivity_CF; or its super classes (declaration of 'com.habbyge.sample.MainActivity_CF' appears in /storage/emulated/0/Android/data/com.habbyge.iwatch/files/Music/app-release-2-cfff1f13df8681ea51edb3ea824af973.apatch)