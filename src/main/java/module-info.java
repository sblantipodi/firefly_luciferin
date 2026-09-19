/**
 * Module declaration for the org.dpsoftware module.
 * <p>
 * This module defines dependencies, packages to be exported, and packages to be opened for reflection.
 * <p>
 * Features:
 * - Declares dependencies on various required modules such as JavaFX, Lombok, JNA, Jackson, GStreamer, and others.
 * - Opens specific packages to certain modules for runtime reflection (e.g., for JavaFX and Jackson usage).
 * - Exports packages that are publicly available for use by other modules.
 * <p>
 * Dependencies:
 * - `javafx.controls`, `javafx.fxml`, `javafx.web`: Provides JavaFX functionality for building GUI applications.
 * - `static lombok`: Enables the use of Lombok annotations for reducing boilerplate code.
 * - `java.desktop`: Provides AWT and Swing features.
 * - `com.sun.jna`, `com.sun.jna.platform`: Supports JNA (Java Native Access) usage for native code integration.
 * - `xt.audio`: Adds audio processing capabilities.
 * - `org.freedesktop.gstreamer`: Integrates GStreamer for media streaming.
 * - `com.fazecast.jSerialComm`: Library for serial communication.
 * - `org.eclipse.paho.client.mqttv3`: MQTT client for connecting to MQTT brokers.
 * - `com.fasterxml.jackson.databind`, `com.fasterxml.jackson.dataformat.yaml`: Provides JSON and YAML data format parsing and processing.
 * - `java.net.http`: Enables HTTP client functionality.
 * - `jdk.httpserver`: Built-in HTTP server.
 * - `ch.qos.logback.classic`: Provides logging capabilities.
 * - `org.freedesktop.dbus`: For D-Bus communication.
 * - `jdk.incubator.vector`: For vector computation through the incubator module.
 * - `java.management`, `jdk.management`: Provides APIs for Java runtime and management operations.
 * - `jdk.compiler`: Enables programmatic access to the Java compiler.
 * - `javafx.graphics`: For advanced JavaFX graphic capabilities.
 * <p>
 * Opened Packages:
 * - `org.dpsoftware`, `org.dpsoftware.gui`, `org.dpsoftware.gui.controllers`: Allows these packages to be accessible for JavaFX and reflection.
 * - `org.dpsoftware.managers.dto`, `org.dpsoftware.managers.dto.mqttdiscovery`: Opened for Jackson deserialization purposes.
 * - `org.dpsoftware.audio`, `org.dpsoftware.grabber`, `org.dpsoftware.gui.trayicon`: Opened for JavaFX-related functionality.
 * <p>
 * Exported Packages:
 * - `org.dpsoftware`: The core package of the module.
 * - `org.dpsoftware.audio`: Contains functionality related to audio handling.
 * - `org.dpsoftware.config`: Manages configuration and settings.
 * - `org.dpsoftware.grabber`: Provides grabbing-related features and utilities.
 * - `org.dpsoftware.gui`: Defines GUI components and logic.
 * - `org.dpsoftware.gui.controllers`: Contains controllers for handling GUI events.
 * - `org.dpsoftware.gui.elements`: Hosts custom GUI components and elements.
 * - `org.dpsoftware.gui.bindings.appindicator`: Handles app indicator integration for GUI.
 * - `org.dpsoftware.gui.bindings.notify`: Provides GUI notification functionality.
 * - `org.dpsoftware.managers`: Includes manager classes for controlling various systems.
 * - `org.dpsoftware.managers.dto`: Contains Data Transfer Objects (DTOs) for data handling.
 * - `org.dpsoftware.managers.dto.mqttdiscovery`: Defines DTOs for MQTT discovery functionality.
 * - `org.dpsoftware.utilities`: Provides general-purpose utilities and helpers.
 * - `org.dpsoftware.network`: Includes network-related functionality.
 * - `org.dpsoftware.gui.trayicon`: Supports the system tray icon feature.
 */module org.dpsoftware {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires static lombok;
    requires java.desktop;
    requires com.sun.jna.platform;
    requires com.sun.jna;
    requires xt.audio;
    requires org.freedesktop.gstreamer;
    requires com.fazecast.jSerialComm;
    requires org.eclipse.paho.client.mqttv3;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires java.net.http;
    requires jdk.httpserver;
    requires ch.qos.logback.classic;
    requires org.freedesktop.dbus;
    requires jdk.incubator.vector;
    requires java.management;
    requires jdk.management;
    requires jdk.compiler;
    requires javafx.graphics;
    requires jakarta.websocket;
    requires org.glassfish.tyrus.server;
    requires org.glassfish.tyrus.container.grizzly.server;

    opens org.dpsoftware to javafx.fxml, javafx.web;
    opens org.dpsoftware.gui to javafx.fxml, javafx.web;
    opens org.dpsoftware.gui.controllers to javafx.fxml;
    opens org.dpsoftware.managers.dto to com.fasterxml.jackson.databind;
    opens org.dpsoftware.managers.dto.mqttdiscovery to com.fasterxml.jackson.databind;

    exports org.dpsoftware;
    exports org.dpsoftware.audio;
    exports org.dpsoftware.config;
    exports org.dpsoftware.grabber;
    exports org.dpsoftware.gui;
    exports org.dpsoftware.gui.controllers;
    exports org.dpsoftware.gui.elements;
    exports org.dpsoftware.gui.bindings.appindicator;
    exports org.dpsoftware.gui.bindings.notify;
    exports org.dpsoftware.managers;
    exports org.dpsoftware.managers.dto;
    exports org.dpsoftware.managers.dto.mqttdiscovery;
    exports org.dpsoftware.utilities;
    exports org.dpsoftware.lut;
    exports org.dpsoftware.network;
    exports org.dpsoftware.network.web;
    exports org.dpsoftware.gui.tc;

    opens org.dpsoftware.audio to javafx.fxml, javafx.web;
    opens org.dpsoftware.grabber to javafx.fxml, javafx.web;
    exports org.dpsoftware.gui.trayicon;
    opens org.dpsoftware.gui.trayicon to javafx.fxml, javafx.web;
    opens org.dpsoftware.network.web;
    opens org.dpsoftware.lut;
}
