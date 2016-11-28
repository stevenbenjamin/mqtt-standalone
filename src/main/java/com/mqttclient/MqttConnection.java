package com.mqttclient;

import java.net.URI;
import java.net.URISyntaxException;

import com.sun.corba.se.spi.ior.ObjectId;

/**
 * Represents all the data to connect to an external MQTT broker as either a
 * publisher or a subscriber. This includes information on what is required to
 * set up the MQTT connection (broker url, QOS, broker username/password, client
 * id, topic) as well as whether or not this represents a publisher or a
 * subscriber.
 * 
 * In our current implementation we are not supporting QOS 2.
 */
public class MqttConnection {
  /** Correspond to Mqtt QOS levels. */
  public static final int QOS_AT_MOST_ONCE = 0;
  public static final int QOS_AT_LEAST_ONCE = 1;
  public static final int QOS_EXACTLY_ONCE = 2;
  public static final int MINIMUM_REPORT_FREQUENCY = 300_000; // 5 minutes
  public static final int DEFAULT_REPORT_FREQUENCY = 3_600_000; // 1 hour
  public static final int MAXIMUM_REPORT_FREQUENCY = 86_400_000; // 1 day
  public static final String DEFAULT_BYTE_WRITER = "JsonFull";
  public static final String DEFAULT_BYTE_READER = "DropFormat";
  public ObjectId _id;
  public String _uri;
  public String _topic;
  public String _username;
  public String _password;
  public String _clientId;
  /**
   * Mqtt broker version. Supports "3.1", "3.1.1". Defaults to 3.1
   */
  public String _version;
  /** default is 1, available is 0, 1, or 2. */
  public Integer _qos;

  public ObjectId getId() {
    return _id;
  }

  public Integer getQos() {
    return _qos;
  }

  public String getVersion() {
    return _version;
  }

  public void setVersion(String _version) {
    this._version = _version;
  }

  public void setQos(Integer qos) {
    this._qos = qos;
  }

  public String getClientId() {
    return _clientId;
  }

  public void setClientId(String _clientId) {
    this._clientId = _clientId;
  }

  public String getUri() {
    return _uri;
  }

  public void setId(ObjectId _id) {
    this._id = _id;
  }

  public void setUri(String _uri) {
    this._uri = _uri;
  }

  public String getUsername() {
    return _username;
  }

  public void setUsername(String username) {
    this._username = username;
  }

  public String getPassword() {
    return _password;
  }

  public void setPassword(String password) {
    this._password = password;
  }

  public URI asURI() throws URISyntaxException {
    return new URI(_uri);
  }

  public String getTopic() {
    return _topic;
  }

  public void setTopic(String topic) {
    this._topic = topic;
  }
}
