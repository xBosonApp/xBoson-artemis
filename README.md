# Artemis 与 xBoson 系统集成

https://activemq.apache.org/components/artemis/documentation/latest/mqtt.html


# 构建

本机测试时, lib 目录链接到 artemis 实例中的 lib 目录.
在 Artemis 中测试.

`gradle`


# Artemis 测试

修改 artemis 配置文件 `broker.xml` 找到路径 `configuration.core.broker-plugins`
加入如下配置:

```xml
  <broker-plugin class-name="com.xboson.artemis.plugin.Integrated">
    <property key="MongoDBurl" value="mongodb://localhost"/>
  </broker-plugin>
```

修改 `bootstrap.xml` 在 `broker` 下配置或修改, 并删除 `jaas-security`:

```xml
  <security-manager class-name="com.xboson.artemis.plugin.Security">
    <property key="MongoDBurl" value="mongodb://localhost"/>
  </security-manager>
```


# iot 设计:

mqtt 主题前缀:

* /场景id/产品id/设备id
  * /data 数据
  * /alert 报警

  
mqtt 参数:
 
* 用户名: 设备用户 - 为一种设备创建一个访问用户
* 密码: 加密算法(密钥)


密码加密算法:

1. 生成 16 字节随机数 (增强安全)
2. 按顺序执行 md5: 随机数, 用户名, 密码原文
3. 将随机数和 md5 签名连接到一个字节数组中
4. 该字节数组执行 base64, 结果为密码

运行 `node pass 用户名 密码` 生成一个可登录密码, 该密码在一段时间内与设备 ip 地址绑定.


## 表结构

```json
{
  "user" : {
    "_id"      : "用户名",
    "password" : "密码明文",
    "info"     : "说明",
    "owner"    : "平台用户名, 可以修改该条数据的平台用户",
    "enb"      : true // true 启用
  },
  
  "address" : {
    "_id" : ". 分割的地址, 格式: '.场景id.产品id.设备id.data', 该格式来自 fmt 的转换",
    "fmt" : "/ 分割的地址, 原始 mqtt 地址",
    "send": {}, // 可发送数据用户 set
    "recv": {}  // 可接受数据用户 set
  },
  
  "passsafe" : {
    "_id" : "用户名+加密密码",
    "crt" : "登录时间",
    "bind": "绑定客户端地址"
  }
}
```