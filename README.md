# Artemis 与 xBoson 系统集成

Artemis 插件; 制定一个设备接入规则, 对设备进行登录认证, 把数据传入 xBoson平台运算核心. 


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

> mqtt 默认规则: MULTICAST 模式相同 clientid 登录, 前一个客户端被踢掉; 
  不同 clientid 连接同一个地址生成自己的队列, 都会接收到同一条消息(连接到自己的队列上);
  ANYCAST 模式任何不同 clientid 客户端连接到同一个队列, 分配消息;
  


# iot 设计:

mqtt 主题前缀:

* /场景id/产品id/设备id
  * /data  设备发送, 设备原始数据, 任意类型
  * /event 设备发送, 消息格式
  * /state 设备发送, 状态, 字符串
  * /cmd   设备接收, 设备原始数据, 用于发送控制命令 
  * /save  平台接收, json 格式, 平台保存的数据
  
> 消费者可用 '+' 通配符匹配产品下的所有设备.
  只有消费者和生产者使用 QoS1 时, 才能保留离线消息.

## data 设备数据主题格式

该主题由设备推送, 由设备定义数据格式; 由 SAAS 应用或 PAAS 脚本订阅,
数据接收后必须对数据进行解析, 解析后的数据推送到 save 主题中进行保存;
SAAS应用可在数据处理期间产生控制命令或事件.

如果设备可以直接推送 json 格式, 则可以掠过 data 主题, 将数据直接推送
到 save 主题中.

发送到该主题的数据不算做设备数据, 只有推送到 save 中才算做设备数据.


## save 保存数据主题格式

通常由 SAAS 应用或 PAAS 产品处理脚本发送, 发送的数据会被 PAAS 平台处理保存;
也可以由设备直接推送.

必须是 JSON 数据格式, 字段与类型必须和 `产品` 定义中的 `数据列表` 匹配.
当消息中的字段没有在 `数据列表` 中定义, 该数据不会被保存.

```
{
  time  数据发送时间, UNIX 时间
  data  {
    数据, 与 `产品` 定义中的 `数据列表` 匹配
  } 
}
```

## state 状态主题

一条可阅读的字符串, 该主题用于更新设备状态, 状态不保留历史, 设备仅保存最新状态.


## event 事件主题格式

该主题由 SAAS 应用或 PAAS 产品处理脚本发送, PAAS 平台订阅,
事件历史会被保留, 并等待用户处理.

JSON 数据格式, 必须包含的固定字段, 规则如下:

```
{
  code  消息代码
  msg   可阅读的信息文本
  cd    消息发送的时间, 自1970开始的毫秒, UNIX 时间 (消息在 mqtt 中中转会有延迟)
  level 消息级别, 定义在字典 `IOT.001` / `EVENT_LEVEL` 中 参考: 
        1. 调试消息可忽略 
        2. 普通消息 
        3. 警告, 可能需要进行处理
        4. 一般损害, 应立即处理
        5. 严重损害, 设施即将失去功能
        6. 严重损害, 有可能造成人员伤亡
        7. 非常严重损害, 自然灾害, 应动员全部资源进行处理
  data  {
    消息数据, 可定义任意字段, 字段会被 `产品/消息字典解读`
  }
}
```

## cmd 命令主题格式

该主题由 SAAS 应用或 PAAS 产品处理脚本发送, 由设备订阅,
设备收到命令后进行对应的处理, 格式为设备可理解的自定义格式,

在平台上定义的 JSON 命令格式在经过 SAAS 应用或 PAAS 产品处理脚本后将被转换为
对应的命令格式.

只有 PAAS 产品脚本发送的命令产生历史记录, SAAS应用发送的命令数据没有历史记录.

  
## mqtt 

登录:
 
* 用户名: 设备用户 - 为一种设备创建一个访问用户
* 密码: 加密算法(密钥)


密码加密算法:

1. 生成 16 字节随机数 (增强安全)
2. 按顺序执行 md5: 随机数, 用户名, 密码原文 (TODO: 与当前时间关联, 1小时内)
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
    cd(time)  : 创建时间
    md(time)  : 修改时间
    "meta"    : {}
  },
  
  "address" : {
    "_id" : ". 分割的地址, 格式: '.场景id.产品id', 该格式来自 fmt 的转换",
    "fmt" : "/ 分割的地址, 原始 mqtt 地址",
    "send": {"可发送数据用户" : 1}, 
    "recv": {"可接受数据用户" : 1},
    
    auto_restart : boolean 随系统自动启动
    auto_auth : string 自动启动使用的平台账户
    
    data : data 主题 {
      count : '线程数',
      user  : '登录用户',
      script: '处理脚本',
      qos   : 0 数据质量,
    }
    
    state : {
      count : 0,
      user  : '登录用户',
      qos   : 0,
    }
    
    event : {
      count : 0,
      user  : '登录用户',
      qos   : 0,
    }
    
    save : {
      count  : 1,
      user   : "登录用户",
      qos   : 0,
    }
  },
  
  "product" : {
    "_id"     : "格式: '.场景id.产品id', 该格式来自 fmt 的转换",
    "scenes"  : "场景id",
    "pid"     : "产品id",
    "name"    : "产品名称",
    "desc"    : "产品描述",
    
    cd(time)  : 创建时间
    md(time)  : 修改时间
  
    "meta" : [ // 属性信息列表
      { name     : '属性名'
        desc     : '说明'
        type     : DevAttrType '类型索引'
        notnull  : bool 不能空 
        defval   : '默认值'
        dict     : '只在字典类型时有效'
      }
    ],
  
    "data" : 输入数据列表 [
      { name     : '数据名'
        desc     : '说明'
        type     : DevDataType '数据类型'
        unit     : '计量单位'
      }
    ],
    
    "cmd" : 输出命令列表 [
      { name : "名称"
        desc : '说明'
        type : DevDataType
      }
    ],
    
    "event" : 事件字典 {
      事件字典 : 对应的说明
    }
  },
  
  "scenes" : {
    "_id" : "场景id",
    "owner" : "归属, 平台用户名", (索引)
    "share" : ["共享用户名列表"], (索引)
    "name"  : "场景名", (索引)
    "desc"  : "详细说明",
    cd(time)  : 创建时间
    md(time)  : 修改时间
  },
  
  "script" : {
    _id   : 脚本名
    desc  : 说明
    owner : 归属, 平台用户名   (索引)
    share : ["共享用户名列表"] (索引)
    code  : 脚本, 加密
    cd(time)  : 创建时间
    md(time)  : 修改时间
  },
  
  "dev-data" : {
    所有年份数据:  {
      _id : !yr~[device-id]$[data-name] (数据名)
      dev : 设备ID - (索引)
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
  },
  
  cmd_his : {
    _id     : mongo 自动生成
    devid   : 设备完整id (索引)
    scenes  : "场景id", (索引)
    product : "产品id", (索引)
    cd      : 命令生成时间
    data    : {} 命令的数据
    payload : 发送的原始数据
  }
  
  event_his : {
    _id     : mongo 自动生成
    msg     : 事件文本
    code    : 消息代码
    devid   : 设备完整id (索引)
    product : "产品id", (索引)
    scenes  : "场景id", (索引)
    cd      : 事件生成时间
    level   : 事件级别
    repwho  : 对事件进行响应的用户
    repmsg  : 响应事件的对策文本
    reptime : 响应时间
    data    : {} 事件数据
  }
}
```

数据类型 DevAttrType:

* DAT_string    DevAttrType = 100 // 字符串
* DAT_number    DevAttrType = 101 // 数字
* DAT_dict      DevAttrType = 102 // 字典
* DAT_date      DevAttrType = 103

数据类型 DevDataType:

* DDT_int       DevDataType = 1 // 整数类型
* DDT_float     DevDataType = 2 // 浮点类型
* DDT_virtual   DevDataType = 3 // 虚拟数据
* DDT_sw        DevDataType = 4 // 开关类型
* DDT_string    DevDataType = 5

# 其他

* [xBoson平台运算核心](https://github.com/yanmingsohu/xBoson-core)
* [MQTT 标准](https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html)
* [artemis 文档](https://activemq.apache.org/components/artemis/documentation/latest/mqtt.html)