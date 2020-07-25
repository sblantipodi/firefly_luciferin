module org.dpsoftware {

    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.desktop;
    requires com.sun.jna.platform;
    requires com.sun.jna;
    requires org.freedesktop.gstreamer;
    requires nrjavaserial;
    requires org.eclipse.paho.client.mqttv3;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires slf4j.api;

    opens org.dpsoftware to javafx.fxml;
    opens org.dpsoftware.gui to javafx.fxml;
    opens org.dpsoftware.grabber to javafx.fxml;
    exports org.dpsoftware;
    exports org.dpsoftware.gui;
    exports org.dpsoftware.grabber;

}