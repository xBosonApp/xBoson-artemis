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

> 如果 MongoDBurl 中没有明确指定数据库则使用默认数据库.


# iot 设计:

mqtt 主题前缀:

* /场景id/产品id/设备id
  * /data  设备发送, 设备原始数据, 任意类型
  * /event 设备发送, 消息格式
  * /state 设备发送, 状态, 字符串
  * /cmd   设备接收, 设备原始数据, 用于发送控制命令 
  
> 消费者可用 '+' 通配符匹配产品下的所有设备.

  
mqtt 参数:
 
* 用户名: 设备用户 - 为一种设备创建一个访问用户
* 密码: 加密算法(密钥)


密码加密算法:

1. 生成 16 字节随机数 (增强安全)
2. 按顺序执行 md5: 随机数, 用户名, 密码原文
3. 将随机数和 md5 签名连接到一个字节数组中
4. 该字节数组执行 base64, 结果为密码

> 运行 `node pass 用户名 密码明文` 生成一个可登录密码, 该密码在一段时间内与设备 ip 地址绑定.


## 表结构

默认库 `xboson-artemis-iot`

```
{
  "user" : {
    "_id"      : "用户名",
    "password" : "密码明文",
    "info"     : "说明",
    "owner"    : "平台用户名, 可以修改该条数据的平台用户",
    "enb"      : true // true 启用
  },
  
  "address" : {
    "_id" : ". 分割的地址, 格式: '.场景id.产品id', 该格式来自 fmt 的转换",
    "fmt" : "/ 分割的地址, 原始 mqtt 地址",
    "send": {"可发送数据用户" : 1}, 
    "recv": {"可接受数据用户" : 1}
  },
  
  "passsafe" : {
    "_id" : "用户名+加密密码",
    "crt" : "登录时间",
    "bind": "绑定客户端地址"
  },
  
  "device" : {
    "_id"     : "格式: '.场景id.产品id.设备id'",
    "devid"   : "设备id",
    "product" : "产品id, 索引",
    "scenes"  : "场景id",
    "state"   : "状态",
    "dc"      : 0, // 数据量
    "dd"(time): 最后数据时间
    "meta"    : {}
  },
  
  "product" : {
    "_id"     : "格式: '.场景id.产品id'",
    "scenes"  : "场景id",
    
    desc(string)   : "说明",
    cd(time)       : 创建时间
    md(time)       : 修改时间
    script(string) : '脚本, 用于处理虚拟数据, 有接口标准'
  
    "meta" : [ // 属性信息列表
      { name     : '属性名'
        desc     : '说明'
        type     : DevAttrType '类型索引'
        notnull  : bool 不能空 
        defval   : '默认值'
        dict     : '只在字典类型时有效'
        max      : 最大值
        min      : 最小值
      }
    ],
  
    "datas" : 输入数据列表 [
      { name     : '数据名'
        desc     : '说明'
        type     : DevDataType '数据类型'
      }
    ],
    
    "cmd" : 输出命令列表 [
      { name : "名称"
        desc : '说明'
        type : DevDataType
      }
    ]
  },
  
  "scenes" : {
    "_id" : "场景id",
    "owner" : "归属, 平台用户名",
    "share" : ["共享用户名列表"],
    "name"  : "场景名",
    "desc"  : "详细说明",
    cd(time)       : 创建时间
    md(time)       : 修改时间
  },
  
  "dev-data" : {
    所有年份数据:  {
      _id : !yr~[device-id]$[data-name] (数据名)
      dev : 设备ID - 需要索引
      l : 最后插入的数据
      v : (数据 map) {
        Y : n年数据, (数字类型)
        ...
      }
    }
  
    当年所有月数据: {
      _id : !mo~[device-id]$[data-name]@Y
      dev : 设备ID
      l : 最后插入的数据
      v : {
        1 : 1月数据 (月份是数字类型)
        ...
        12 : 12月数据
      }
    }
  
    日数据: {
      _id : !dy~[device-id]$[data-name]@Y-M
      dev : 设备ID
      l : 最后插入的数据
      v : {
        1 : x月1日数据
        ...
        31 : 31日数据
      }
    }
  
    小时数据: {
      _id : !hr~[device-id]$[data-name]@Y-M-D
      dev : 设备ID
      l : 最后插入的数据
      v : {
        0 : 0点数据
        ...
        23 : 23点数据
      }
    }
  
    分钟数据:  {
      _id : !mi~[device-id]$[data-name]@Y-M-D_h
      dev : 设备ID
      l : 最后插入的数据
      v : {
        0 : 0分数据
        ...
        59: 59分数据
      }
    }
  
    秒数据:  {
      _id : !se~[device-id]$[data-name]@Y-M-D_h:m
      dev : 设备ID
      l : 最后插入的数据
      v : {
        0 : 0秒数据
        ...
        59: 59秒数据
      }
    }
  }
}
```

数据类型 DevDataType:

* DAT_string    DevAttrType = 100 // 字符串
* DAT_number    DevAttrType = 101 // 数字
* DAT_dict      DevAttrType = 102 // 字典
* DAT_date      DevAttrType = 103