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

    public MQTTManager(Configuration config) {

        try {
            this.config = config;
            client = new MqttClient(config.getMqttServer(), "JavaFastScreenCapture");
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setAutomaticReconnect(true);
            connOpts.setUserName(config.getMqttUsername());
            connOpts.setPassword(config.getMqttPwd().toCharArray());
            client.connect(connOpts);
            client.setCallback(this);
            client.subscribe(config.getMqttTopic());

//            publishToTopic("{\"state\": \"ON\", \"effect\": \"AmbiLight\"}\n");
        } catch (MqttException e) {
            System.out.println("Can't connect to MQTT Server");
        }

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
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println(message);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("delivered");
    }

}
