package com.comply.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "comply.bootstrap")
public class BootstrapProperties {

  private boolean enabled;
  private User admin = new User();
  private User ocos = new User();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public User getAdmin() {
    return admin;
  }

  public void setAdmin(User admin) {
    this.admin = admin;
  }

  public User getOcos() {
    return ocos;
  }

  public void setOcos(User ocos) {
    this.ocos = ocos;
  }

  public static class User {
    private String fullName;
    private String email;
    private String password;

    public String getFullName() {
      return fullName;
    }

    public void setFullName(String fullName) {
      this.fullName = fullName;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
