package com.xboson.artemis.plugin;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;


public class Security implements ActiveMQSecurityManager4 {

  private final Logger log = LoggerFactory.getLogger(Security.class);
  private Database db;
  private Auth auth;
  private Address addr;


  public ActiveMQSecurityManager init(Map<String, String> properties) {
    db = new Database(properties);
    log.info("Connected to MongoDB {} {}", db.host, db.db_name);
    auth = new Auth(db);
    addr = new Address(db);
    return this;
  }


  @Override
  public boolean validateUser(String user, String password) {
    throw new RuntimeException("not support");
  }


  @Override
  public boolean validateUserAndRole(String user,
                                     String password,
                                     Set<Role> roles,
                                     CheckType checkType) {
    throw new RuntimeException("not support");
  }


  @Override
  public String validateUser(String user,
                             String password,
                             RemotingConnection conn,
                             String securityDomain) {
    if (auth.check(user, password, getBind(conn)))
      return user;
    return null;
  }


  @Override
  public String validateUserAndRole(String user,
                                    String password,
                                    Set<Role> roles,
                                    CheckType checkType,
                                    String address,
                                    RemotingConnection remotingConnection,
                                    String securityDomain) {
    switch (checkType) {
      case SEND:
        return addr.canSend(user, address) ? user : null;

      case CONSUME:
        return addr.canRecv(user, address) ? user : null;

      default:
        return null;
    }
  }


  private String getBind(RemotingConnection conn) {
    // 删掉端口
    String b = conn.getRemoteAddress();
    String[] r = b.split(":", 2);
    if (r.length > 1) {
      return r[0];
    }
    return b;
  }
}
