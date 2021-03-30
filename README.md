# iWatch
## 热补丁方案-Native(C/C++)层方案：
1. 支持Android5.0 ~ 11.x ~ 目前master主线代码
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
9. 需要fix的方法调用需要fix的方法
10. ......

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

### 3. Class.forName()与dexClassLoader.load()有何区别，前者会导致内部类illaccesserror.

### 4. 内部类和非内部类中如果新增field、method，都建议使用新增类的方式，防止破坏旧的类结构(字段索引、地址).


测试样例：~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
## 1) AndroidManifest.xml
```xml
<application
    android:name="com.habbyge.sample.IWatchApplication"
    android:allowBackup="false"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true">

    <activity android:name="com.habbyge.sample.MainActivity">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```
## 2) IWatchApplication.java
```java
public class IWatchApplication extends Application {
    private static final String TAG = "iWatch.IWatchApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // 必须最先执行的初始化，加载所有补丁
        boolean initRet = PatchManager.getInstance().init(getApplicationContext(), "0.1", "1", true);
        if (!initRet) {
            Log.e(TAG, "onCreate, init failure !");
        }
    }
}
```
## 3) MainActivity.java 测试增、改
```java
public class MainActivity extends Activity {
    private static final String TAG = "iWatch.MainActivity";

    // 测试用例: private字段是否可访问
    public static int ix = 10;
    public int ix_HOOK = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "onResume-1, ix_HOOK=" + ix_HOOK + ", ix=" + ix + ", " + ix);

        findViewById(R.id.method).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Fix-2, btnHookMethod, onClick !"); // 测试用例：验证内部类改变

                // 测试用例：内部类访问外部现有private字段(static or not)会失败，因为编译期会为private字段合成
                // 一个访问方法（类型flag：Synthetic），该访问方法的形参是旧的修复前的类型(对象)，但是函数中是修复
                // 类名，因此发生failed to verify的crash，因此必须修改成public才能阻止其生成合成方法。
                ix = 1;
                ix_HOOK = 10;
                Log.d(TAG, "__XYX__ ix=" + ix + ", ix_HOOK=" + ix_HOOK);

                // 测试用例：测试(匿名)内部类访问新增的class(包括新增字段、方法)
//                Test test = new Test();
//                test.print("i love my family ! new !!!!!!");

                // 测试用例：测试(匿名)内部类访问现有方法(public/private)
//                printf("onClick");
            }
        });

        printf2("onCreate-End", 1000);
    }

    // 测试用例: 验证新增方法、访问类中字段......
    public void printf2(String text, int x1) { // 通过修改
        Log.w(TAG, "printf2-begin: " + text + ", ix=" + ix + "， strX_Added=");
        for (int i = 0; i < 100; ++i) {
            ++ix_HOOK;
            ++ix;
            ++x1;
//        printf(text + x1);
        }
        printf(text + x1);
        Log.w(TAG, "printf2-end: " + text + ", ix=" + ix);
    }

    // 测试用例: 验证新增方法、访问类中字段......
    public void printf(String text) { // 通过修改
//        Log.w(TAG, "printf-bengin-2: " + text + ", ix=" + ix + "，beg !!!!!!");
        for (int i = 0; i < 100; ++i) {
            ++ix_HOOK;
            ++ix;
        }

        Log.d(TAG, "printf-Mid1-2: " + text + "x, ix_HOOK2="
                + ix_HOOK + ", " /*+ test("ix_HOOK_ix")*/
                + ", x = " + text + ", xx=" + text);

        Log.w(TAG, "printf-Mid0-2: " + text + ", ix=" + ix + ", x1=" + "，end !!!!!!");
        int x = ix * ix_HOOK;
        int x2 = ix * ix_HOOK;
        Log.w(TAG, "printf-End-2: " + text + ", ix=" + ix + ", x2=" + x2 + "，end !!!!!!");
    }
}
```
## 4) Test.java 测试新增类
```java
public class Test {
    public static final String TAG = "iWatch.Test";

    public static String family = "I love my family !!!!!!";

    public void print(String txt) {
        Log.d(TAG, "print: " + txt + ", family=" + family);
    }

    @Keep // TODO: 2021/3/19 这里是必须加的
    public void printf2(String text, int x1) {
        Log.d(TAG, "print: " + text + ", " + x1 + ", " + family);
        String value = getFamily();
        Log.d(TAG, "print: " + text + ", " + x1 + ", " + family);
    }

    public static String getFamily() {
        return family;
    }
}
```
