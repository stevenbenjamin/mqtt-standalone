package com.mqttclient.netty;

import com.mqttclient.protocol.ClientMessage;
import com.mqttclient.protocol.MessageType;
import com.mqttclient.protocol.ServerMessage;

public abstract class MessageTracer {
  public final String label;

  public MessageTracer(String label) {
    this.label = label;
  }

  public abstract void traceOutgoing(ClientMessage m);

  public abstract void traceIncoming(ServerMessage m);

  public boolean sent(MessageType t) {
    return true;
  }

  public boolean received(MessageType t) {
    return true;
  }

  public static final MessageTracer NO_OP = new MessageTracer("NO_OP") {
    @Override
    public void traceOutgoing(ClientMessage m) {
    }

    @Override
    public void traceIncoming(ServerMessage m) {
    }
  };

  public static MessageTracer createDebugTracer(final String label) {
    return new MessageTracer(label) {
      @Override
      public void traceOutgoing(ClientMessage m) {
        System.out.println("=>" + label + " : " + m);
      }

      @Override
      public void traceIncoming(ServerMessage m) {
        System.out.println("<=" + label + " : " + m);
      }
    };
  }
}
