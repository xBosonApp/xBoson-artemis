package com.xboson.artemis.plugin;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.bson.BsonArray;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;


public class Device implements IConst {

  private static final Logger log = LoggerFactory.getLogger(Device.class);

  private MongoCollection<Document> dev;
  private UpdateOptions upsert;


  Device(Database db) {
    dev = db.open().getCollection(TabDev);
    upsert = new UpdateOptions().upsert(true);
    dev.createIndex(new Document("product", 1));
  }


  /**
   * TODO; 设备默认 meta
   */
  public boolean createDevice(SimpleString address) {
    log.debug("Before create device", address);
    Address.Info info = Address.parse(address);
    if (info == null) return false;
    // 如果消费者用匹配模式创建地址, 只由一个匹配地址被创建, 该方法失效
    if (info.dev.equals("*")) return false;

    String dev_id = info.devicePrefix();
    Document where = new Document("_id", dev_id);
    Date now = new Date();

    Document meta = new Document();

    Document set = new Document();
    set.put("devid", info.dev);
    set.put("product", info.prod);
    set.put("scenes", info.scene);
    set.put("state", "auto created");
    set.put("dc", 0);
    set.put("dd", 0);
    set.put("cd", now);
    set.put("md", now);
    set.put("meta", meta);

    Document doc = new Document();
    doc.put("$setOnInsert", set);

    dev.updateOne(where, doc, upsert);
    log.debug("Created device {}", dev_id);
    return true;
  }
}
