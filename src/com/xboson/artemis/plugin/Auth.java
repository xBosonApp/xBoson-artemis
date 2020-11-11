package com.xboson.artemis.plugin;

import com.mongodb.client.MongoCollection;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerSessionPlugin;
import org.bson.Document;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class Auth implements IConst, ActiveMQServerSessionPlugin {

  private static final Base64.Decoder b64d = Base64.getDecoder();
  private MongoCollection<Document> user;


  Auth(Database db) {
    user = db.open().getCollection(TabUser);
  }


  public void afterCreateSession(ServerSession session) throws ActiveMQException {
    String un = session.getUsername();
    Document info = userInfo(un);
    if (!check_password(un, info.getString("password"), session.getPassword())) {
      throw new ActiveMQException("bad auth",
              ActiveMQExceptionType.SECURITY_EXCEPTION);
    }
  }


  public boolean check(String name, String pass) {
    Document d = userInfo(name);
    if (d == null) return false;
    return check_password(name, d.getString("password"), pass);
  }


  private Document userInfo(String name) {
    Document where = new Document("_id", name);
    Document ret = user.find(where).first();
    if (! ret.getBoolean("enb")) {
      return null;
    }
    return ret;
  }


  private boolean check_password(String un, String ps, String inpass) {
    if (ps == null) return false;

    byte[] b = b64d.decode(inpass);
    MessageDigest md = md5();
    md.update(b, 0, 16);
    md.update(un.getBytes());
    md.update(ps.getBytes());

    byte[] ck = md.digest();
    if (ck.length != b.length - 16) {
      return false;
    }

    for (int i=0; i<ck.length; ++i) {
      if (ck[i] != b[i + 16]) {
        return false;
      }
    }
    return true;
  }


  private static MessageDigest md5() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
