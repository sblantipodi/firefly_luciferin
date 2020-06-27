/*
  MQTTManager.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/

package org.dpsoftware;

import lombok.NoArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;

@NoArgsConstructor
public class MQTTManager implements MqttCallback {

    MqttClient client;
    Configuration config;
    FastScreenCapture fastScreenCapture;

    public MQTTManager(Configuration config, FastScreenCapture fastScreenCapture) {

        try {
            this.config = config;
            attemptReconnect(config, fastScreenCapture);
            this.fastScreenCapture = fastScreenCapture;
        } catch (MqttException e) {
            System.out.println("Can't connect to MQTT Server");
        }

    }

    void attemptReconnect(Configuration config, FastScreenCapture fastScreenCapture) throws MqttException {

        client = new MqttClient(config.getMqttServer(), "JavaFastScreenCapture");
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setAutomaticReconnect(true);
        connOpts.setCleanSession(true);
        connOpts.setConnectionTimeout(10);
        connOpts.setUserName(config.getMqttUsername());
        connOpts.setPassword(config.getMqttPwd().toCharArray());
        client.connect(connOpts);
        client.setCallback(this);
        client.subscribe(config.getMqttTopic());
        System.out.println("Connected to MQTT Server");
        
    }

    public void publishToTopic(String msg) {

        MqttMessage message = new MqttMessage();
        message.setPayload(msg.getBytes());
        try {
            client.publish(config.getMqttTopic(), message);
        } catch (MqttException e) {
            System.out.println("Cant't send MQTT msg");
        }

    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection Lost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {

        System.out.println(message);
        if (message.toString().contains("START")) {
            fastScreenCapture.getTim().startCapturingThreads();
        } else if (message.toString().contains("STOP")) {
            fastScreenCapture.getTim().stopCapturingThreads(config);
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("delivered");
    }

}
