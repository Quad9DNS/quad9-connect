package com.quad9.aegis.Model;

import android.util.Log;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;

class MyHandshakeListener implements HandshakeCompletedListener {
    private String ciphersuite;

    public void handshakeCompleted(HandshakeCompletedEvent e) {
        Log.d("MyHandshakeListener", "Handshake succesful!");
        Log.d("MyHandshakeListener", "Using cipher suite: " + e.getCipherSuite());
        this.ciphersuite = e.getCipherSuite();
    }

    public String getCipherSuite() {
        return this.ciphersuite;
    }
}