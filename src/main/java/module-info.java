module org.dpsoftware {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires static lombok;
    requires java.desktop;
    requires com.sun.jna.platform;
    requires com.sun.jna;
    requires xt.audio;
    requires org.freedesktop.gstreamer;
    requires nrjavaserial;
    requires org.eclipse.paho.client.mqttv3;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires java.net.http;
    requires org.slf4j;
    requires logback.classic;

    opens org.dpsoftware to javafx.fxml, javafx.web;
    opens org.dpsoftware.gui to javafx.fxml;
    opens org.dpsoftware.gui.controllers to javafx.fxml;
    opens org.dpsoftware.grabber to javafx.fxml;
    opens org.dpsoftware.managers to javafx.web;
    exports org.dpsoftware;
    exports org.dpsoftware.audio;
    exports org.dpsoftware.config;
    exports org.dpsoftware.grabber;
    exports org.dpsoftware.gui;
    exports org.dpsoftware.gui.controllers;
    exports org.dpsoftware.gui.elements;
    exports org.dpsoftware.managers;
    exports org.dpsoftware.managers.dto;
    exports org.dpsoftware.utilities;
    exports org.dpsoftware.network;

}