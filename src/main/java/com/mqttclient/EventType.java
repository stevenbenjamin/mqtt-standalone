package com.mqttclient;

public enum EventType {
  connect, disconnect, kill, sent, subscribe, unsubscribe, received, rejected, connectionLost, parseError, sendError
}