package com.xboson.artemis.plugin;

import com.mongodb.client.MongoCollection;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.bson.Document;


public class Address implements IConst {

  private static final char DELIMCH = '.';
  private static final String DELIM_REG = "\\"+ DELIMCH;

  private MongoCollection<Document> addr;
  private final Document exists;


  Address(Database db) {
    addr = db.open().getCollection(TabAddr);
    exists = new Document("$exists", true);
  }


  private boolean op(String user, String address, String op) {
    Info info = parse(address);
    if (info == null) return false;

    Document where = new Document("_id", info.addrPrefix());
    where.put(op +"."+ user, exists);
    return addr.count(where) == 1;
  }


  public boolean canSend(String user, String address) {
    return op(user, address, "send");
  }


  public boolean canRecv(String user, String address) {
    return op(user, address, "recv");
  }


  /**
   * 解析地址错误返回 null
   */
  public static Info parse(String address) {
    String[] p = address.split(DELIM_REG, 5);
    if (p.length < 5) return null;
    return new Info(p[1], p[2], p[3], p[4]);
  }


  /**
   * 解析地址错误返回 null
   */
  public static Info parse(SimpleString addr) {
    SimpleString[] p = addr.split(DELIMCH);
    if (p.length < 5) return null;
    return new Info(p[1].toString(),
            p[2].toString(), p[3].toString(), p[4].toString());
  }


  public static class Info {
    public final String scene;
    public final String prod;
    public final String dev;
    public final String chan;

    Info(String s, String p, String d, String c) {
      this.scene = s;
      this.prod  = p;
      this.dev   = d;
      this.chan  = c;
    }

    String addrPrefix() {
      return DELIMCH + scene + DELIMCH + prod;
    }

    String devicePrefix() {
      return addrPrefix() + DELIMCH + dev;
    }
  }
}
