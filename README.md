# react-native-contacts-picker
react-native兼容iOS8的通讯录选择器

### 使用方式(Android)
- 安装模块
```
npm i react-native-contacts-picker -s
```
- 关联模块
```
react-native link react-native-contacts-picker
```
- 添加权限
```
<uses-permission android:name="android.permission.READ_CONTACTS" />
```
- 引入模块
```
import ContactPickerBridge from 'react-native-contacts-picker';
```

### 使用方式(iOS)
- 安装模块
```
npm i react-native-contacts-picker -s
```
- 关联模块
```
react-native link react-native-contacts-picker
```
- 添加权限

    在 `Info.plist` 中添加 `Privacy - Contacts Usage Description` 权限
- 引入模块
```
import ContactPickerBridge from 'react-native-contacts-picker';
```

### 接口说明

|接口名|接口成功返回|接口失败返回|注意点|
| --- | --- | --- | --- |
|openContactPicker|返回 {"data": {"phone":"(555) 564-8583","name":"Bell Kate"}, "code": 0} 格式数据|{"msg": "失败原因", "code": 1/2}||
|getAllContact|返回 {"data": [{"phoneArray":["(555) 766-4823","(707) 555-1854"],"name":"ZakroffHank"}], "code": 0} 格式数据|{"msg": "失败原因", "code": 1/2}||
|checkContactPermissions|{"status": true/false}|无|由于Android 6.0以下没有原生权限管理，因此目标版本在23以下时，返回的永远是true。不过可以使用 `getAllContact` 方法看返回的数据是否为空来**粗略**判断权限。|