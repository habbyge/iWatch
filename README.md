# iWatch
## 热补丁方案-Native(C/C++)层方案：
1. 支持Android5.0 ~ 11.x ~ 目前master主线代码
2. 支持修改method，新增class、field、method，以及支持部分inline方法的修复
3. 支持即时修复，即补丁下发即可生效
4. 方案稳定可靠
5. 配合[apkpatchplus](https://github.com/habbyge/apkpatchplus)项目，其生成补丁，在iWatch中加载、使用补丁

## 本地测试方法
adb push 补丁.apatch 到手机这个目录: /storage/emulated/0/Android/data/com.habbyge.iwatch/files/Music/
