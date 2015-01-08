# 小米手环蓝牙通讯SDK简要说明文档

## 简介：
小米手环蓝牙通讯SDK是在Android标准BLE通讯的基础上做的封装，主要是为了便于第三方开发者能快速接入，省去熟悉Android蓝牙API的过程，缩短开发周期。SDK未引用第三方库，也未包含任何界面逻辑，代码中有部分示例，根据具体使用场景可按需求适当删减。SDK仅对蓝牙扫描逻辑和连接读写逻辑进行了封装，并未对其他相关操作做任何处理，开发人员仍然需要对设备是否BLE-capible以及是否打开了蓝牙进行检查，并在相应的情况下做适当的处理。此外SDK仍在完善中，如有问题请直接联系维护人员。

## 权限相关：
在manifest文件中添加如下权限
```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
```

## 蓝牙检查与判断相关：
在代码中动态判断硬件是否支持蓝牙
```java
getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
```

获取BluetoothAdapter对象，该对象用来操作蓝牙控制器。
```java
BluetoothAdapter.getDefaultAdapter()
```

通过BluetoothAdapter的isEnabled()可以判断当前蓝牙是否打开。
如果蓝牙为关闭状态，有两种方式可以将其打开。
1. 推荐方式，引导用户去打开蓝牙。
```java
private BluetoothAdapter mBluetoothAdapter;
...
// Ensures Bluetooth is available on the device and it is enabled. If not,
// displays a dialog requesting user permission to enable Bluetooth.
if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
}
```
2. 隐式打开蓝牙。
```java
mBluetoothAdapter.enable()
```

## 扫描相关api：
扫描相关操作由ScanCallback类完成，该类主要有两个方法，startScan(int timeout)和stopScan()。前者的timeout参数用来指定扫描时间，在timeout时间到达后扫描会停止，也可以在timeout之前调用stopScan()来提前停止扫描。该类的构造函数带有两个参数ScanCallback(DeviceFilter[] filters, IDeviceFoundCallback deviceFoundCB)，前者是对扫描到的设备进行过滤，可以为null，表示不过滤任何设备。后者是扫描到设备时的回调函数，具有如下接口：
```java
public interface IDeviceFoundCallback {
    public void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord);
}
```
其中device是蓝牙设备对象，建立连接时需要；rssi为信号强度标识，通常在-20到-100的范围，越接近0表示该蓝牙设备离扫描端（手机）越近；scanRecord包含扫描到的广播包内容，对于仅需要扫描到设备进行连接的场景，rssi和scanRecord可忽略。

## 连接相关api：
在得到蓝牙设备device对象后，就可以建立连接了。SDK提供了一些便捷的封装，建立连接可以基于特定的profile，这样相应的操作也会简单很多。

以认证功能为例。认证功能对应的profile是AuthProfile，该对象的构造函数包含三个参数
```java
AuthProfile(Context context, BluetoothDevice device, IConnectionStateChangeCallback connStateChangeCB)
```
其中device是scan过程中返回的device参数，connStateChangeCB接口包含三个回调函数，onConnected/onDisconnected/onConnectionFailed，分别对应已连接，已断开和连接失败。其中连接断开包括主动调用disconnect函数的断开和由于距离过远或通讯失败等原因导致的断开。连接失败通常是由于建立连接过程中蓝牙设备超出有效范围或受到干扰等原因。

profile对象创建后就可以调用connect(boolean autoConnect)函数来进行连接了。连接的状态变化会通过回调通知应用。其中参数autoConnect表示连接断开后是否需要自动重新建立连接。对于已经建立的连接或者正在建立的连接都可以通过调用disconnect(boolean autoConnect)来断开连接或取消建立连接，其参数表示连接断开后是否自动重新建立连接。此外，profile在使用完毕后需要调用close()方法来释放相应的资源，close会断开当前连接，建议放在onDestroy或类似的逻辑中处理。

连接建立（onConnected）后在执行读写命令之前必须执行两个初始化命令，profile.discoverServices()和profile.init()。由于这两个命令是其他蓝牙命令的前置条件，所以建议在onConnected回调后立即执行。这两个命令的返回值在成功时为true，如果返回false则表示有异常发生，具体异常原因需要根据系统蓝牙日志进行诊断。

## 蓝牙读写操作相关api：
profile连接成功后可以通过获取相应的service进行蓝牙读写操作。

仍以认证功能为例。认证功能的service可以通过profile.getAuthService()接口获取。AuthService对应的接口有3个，authorize/authenticate/confirm。三个方法中均包含appid这一参数，这个appid由华米统一分配，目前无公开申请接口，每个合作方需联系华米相关人员获取。三个方法的返回值表示命令的执行结果，成功为true，否则为false。其中，authorize和authenticate使用共同的key来授权与认证。key为128位（16字节），应用需妥善保管。

authorize用来对手环进行授权，命令执行时手环会闪蓝色跑马灯，进入等待敲击状态，用户双击（或多次敲击）手环后手环三个灯会同时点亮并轻震，authorize方法此时返回true，其他情况下返回false。authorize方法仅需执行一次即可，多次授权同一手环会重复触发敲击确认流程，并用新的key覆盖旧的key，使得旧的key失效。

authenticate用来对手环进行认证，命令执行结果如为true表明该手环是之前授权过的，认证成功；若为false则该手环可能是未授权过的（也可能是蓝牙命令出错导致返回false），认证失败。

confirm用来触发用户进入敲击确认状态，命令执行时手环闪蓝色跑马灯，进入等待敲击状态，用户双击（或多次敲击）手环后手环三个灯会同时点亮并轻震，confirm方法此时返回true，其他情况下返回false。

## 补充说明：
1. 除了连接相关的api外，其他api均为阻塞式，虽然阻塞时间可能有长有短，但是强烈建议不要在ui线程中调用。
2. 蓝牙连接不是稳定可靠的，所以对连接失败以及异常断开等错误需要进行相应的处理，以免引起用户的困惑。SDK中蓝牙读写操作都带有boolean类型的返回值表示命令执行的结果，开发人员应尽可能检查命令的返回值，不过目前还尚无统一通用的错误处理方式，尤其是针对众多复杂的手机平台硬件和rom版本，关于这一点我们目前也在积极寻找解决办法。
3. SDK本身仅仅是对Android标准蓝牙操作的封装，对于有蓝牙开发经验的开发人员也可以直接通过标准接口进行操作，后期我们会提供通讯协议的相关文档。目前还缺少正式的文档或javadoc，但是demo代码内注释详尽，应该可以满足大部分开发人员的需要。
