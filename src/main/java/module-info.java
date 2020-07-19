module org.dpsoftware {

    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.desktop;
    requires com.sun.jna.platform;
    requires com.sun.jna;
    requires org.freedesktop.gstreamer;
    requires nrjavaserial;
    requires slf4j.api;
    requires org.eclipse.paho.client.mqttv3;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;

    opens org.dpsoftware to javafx.fxml;
    exports org.dpsoftware;

}