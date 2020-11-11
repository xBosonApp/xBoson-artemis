package com.xboson.artemis.plugin;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.bson.Document;


public class Device implements IConst {

  private MongoCollection<Document> dev;


  Device(Database db) {
    dev = db.open().getCollection(TabDev);
  }


  public boolean createDevice(SimpleString address) {
    Address.Info info = Address.parse(address);
    if (info == null) return false;

    Document where = new Document("_id", info.devicePrefix());

    Document set = new Document();
    set.put("devid", info.dev);
    set.put("state", "");
    set.put("meta", new Document());

    Document doc = new Document();
    doc.put("$setOnInsert", set);

    dev.updateOne(where, doc, new UpdateOptions().upsert(true));
    return true;
  }
}
