package com.xboson.artemis.plugin;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

import java.util.Map;


public class Database implements IConst {

  private MongoClient cli;
  public final String db_name;
  public final String host;


  public Database(Map<String, String> setting) {
    MongoClientURI uri = new MongoClientURI(setting.get(ConfURL));
    cli = new MongoClient(uri);
    String db_name = uri.getDatabase();
    if (db_name == null || "".equals(db_name)) {
      db_name = ConfDBName;
    }
    this.db_name = db_name;
    this.host = uri.getHosts().toString();
  }


  public MongoDatabase open() {
    return cli.getDatabase(db_name);
  }


  public void close() {
    cli.close();
  }
}
