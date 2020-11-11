package com.xboson.artemis.plugin;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;


public class Auth implements IConst {

  private static final Base64.Decoder b64d = Base64.getDecoder();
  private static final int PASS_BIND_TIME = 24 * 3600;

  private MongoCollection<Document> user;
  private MongoCollection<Document> passsafe;


  Auth(Database db) {
    user = db.open().getCollection(TabUser);
    passsafe = db.open().getCollection(TabPasssafe);

    Document ps = new Document("crt", 1);
    ps.put("expireAfterSeconds", PASS_BIND_TIME);
    passsafe.createIndex(ps);
  }


  public boolean check(String name, String pass, String bindAddr) {
    Document d = userInfo(name);
    if (d == null) return false;
    return check_password(name, d.getString("password"), pass, bindAddr);
  }


  private Document userInfo(String name) {
    Document where = new Document("_id", name);
    Document ret = user.find(where).first();
    if (! ret.getBoolean("enb")) {
      return null;
    }
    return ret;
  }


  private boolean check_password(String un, String realps, String inpass, String bind) {
    if (realps == null) return false;
    PassState st = is_safe_password(un, inpass, bind);
    if (st == PassState.UNSAFE) return false;

    byte[] b = b64d.decode(inpass);
    MessageDigest md = md5();
    md.update(b, 0, 16);
    md.update(un.getBytes());
    md.update(realps.getBytes());

    byte[] ck = md.digest();
    if (ck.length != b.length - 16) {
      return false;
    }

    for (int i=0; i<ck.length; ++i) {
      if (ck[i] != b[i + 16]) {
        return false;
      }
    }

    if (st != PassState.BINDED) {
      bind_safe_password(un, inpass, bind);
    }
    return true;
  }


  private void bind_safe_password(String un, String inpass, String bind) {
    Document doc = createDoc(un, inpass);
    doc.put("crt", new Date());
    doc.put("bind", bind);
    passsafe.insertOne(doc);
  }


  private PassState is_safe_password(String un, String inpass, String bind) {
    Document doc = createDoc(un, inpass);
    doc = passsafe.find(doc).first();
    if (doc == null) {
      return PassState.SAFE;
    }
    if (bind.equals( doc.getString("bind") )) {
      return PassState.BINDED;
    }
    return PassState.UNSAFE;
  }


  private Document createDoc(String un, String ps) {
    return new Document("_id", un +" "+ ps);
  }


  private static MessageDigest md5() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }


  enum PassState {
    SAFE, UNSAFE, BINDED,
  }
}
