package com.xboson.artemis.plugin;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Device implements IConst {

  private static final Logger log = LoggerFactory.getLogger(Device.class);

  private MongoCollection<Document> dev;
  private UpdateOptions upsert;


  Device(Database db) {
    dev = db.open().getCollection(TabDev);
    upsert = new UpdateOptions().upsert(true);
    dev.createIndex(new Document("product", 1));
  }


  // TODO; 设备默认 meta
  public boolean createDevice(SimpleString address) {
    Address.Info info = Address.parse(address);
    if (info == null) return false;

    String dev_id = info.devicePrefix();
    Document where = new Document("_id", dev_id);

    Document set = new Document();
    set.put("devid", info.dev);
    set.put("product", info.prod);
    set.put("scenes", info.scene);
    set.put("state", "");
    set.put("dc", 0);
    set.put("dd", 0);
    set.put("meta", new Document());

    Document doc = new Document();
    doc.put("$setOnInsert", set);

    dev.updateOne(where, doc, upsert);
    log.debug("Created device {}", dev_id);
    return true;
  }
}
