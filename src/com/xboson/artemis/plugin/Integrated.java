package com.xboson.artemis.plugin;


import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;


public class Integrated implements ActiveMQServerPlugin, IConst {

  private static final SimpleString XBOSON_WORKER =
          new SimpleString("(xboson-worker)");


  private final Logger log = LoggerFactory.getLogger(Integrated.class);
  private Map<String, String> setting;
  private Database db;
  private Auth auth;
  private Device dev;


  @Override
  public void init(Map<String, String> properties) {
    setting = properties;
  }


  @Override
  public void registered(ActiveMQServer server) {
    db = new Database(setting);
    log.info("Connected to MongoDB {} {}", db.host, db.db_name);
    auth = new Auth(db);
    dev = new Device(db);
  }


  @Override
  public void unregistered(ActiveMQServer server) {
    db.close();
  }


  public void afterAddAddress(AddressInfo addressInfo, boolean reload)
          throws ActiveMQException {
    // 重启服务器时不创建设备
    if (!reload) {
      dev.createDevice(addressInfo.getName());
    }
  }


  /**
   * artemis 地址有多播组和任播组, 任播组中每个队列轮流接收消息, 多播组总是获取所有消息;
   * 地址将每条消息发给任播组和多播组; 一个队列只能连接一个客户端;
   */
  public void beforeAddAddress(AddressInfo addressInfo, boolean reload)
          throws ActiveMQException {
    if (addressInfo.getName().charAt(0) == '.') {
      addressInfo.setRoutingTypes(EnumSet.allOf(RoutingType.class));
    }
  }


  @Override
  public void beforeCreateQueue(QueueConfiguration qc) throws ActiveMQException {
    // 队列名称是 clientid + 地址
    // 带有特殊标记的队列设置为任播组, 用于负载平衡
    if (qc.getName().startsWith(XBOSON_WORKER)) {
      qc.setDurable(true);
      qc.setAutoDelete(false);
      qc.setTransient(false);
      qc.setTemporary(false);
      qc.setRoutingType(RoutingType.ANYCAST);
      log.debug("Create xBoson worker queue {}", qc);
    }
  }
}
