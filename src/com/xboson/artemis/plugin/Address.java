package com.xboson.artemis.plugin;

import com.mongodb.client.MongoCollection;
import org.bson.Document;


public class Address implements IConst {

  private MongoCollection<Document> addr;
  private final Document exists;


  Address(Database db) {
    addr = db.open().getCollection(TabAddr);
    exists = new Document("$exists", true);
  }


  private boolean op(String user, String address, String op) {
    Document where = new Document("_id", address);
    where.put(op +"."+ user, exists);
    return addr.count(where) == 1;
  }


  public boolean canSend(String user, String address) {
    return op(user, address, "send");
  }


  public boolean canRecv(String user, String address) {
    return op(user, address, "recv");
  }
}
