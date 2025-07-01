/*
  ProfileDialogController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware.gui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.RowConstraints;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.WidgetFactory;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

/**
 * Eye care dialog controller
 */
@Slf4j
@Getter
public class ProfileDialogController {

    @FXML
    public Spinner<Integer> gpuThreshold;
    @FXML
    public Spinner<Integer> cpuThreshold;
    @FXML
    public ComboBox<String> process1;
    @FXML
    public ComboBox<String> process2;
    @FXML
    public ComboBox<String> process3;
    @FXML
    RowConstraints gpuRow;
    @FXML
    Label gpuLabel;
    @FXML
    RowConstraints cpuRow;
    @FXML
    Label cpuLabel;
    @FXML
    public Button okButton;
    @FXML
    public Button cancelButton;
    // Inject main controller
    @FXML
    private SettingsController settingsController;

    /**
     * Inject main controller containing the TabPane
     *
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {
    }

    /**
     * Set tooltips
     */
    private void setTooltips() {
        GuiManager.createTooltip(Constants.TOOLTIP_GPU_THRESHOLD, gpuThreshold);
        GuiManager.createTooltip(Constants.TOOLTIP_CPU_THRESHOLD, cpuThreshold);
        GuiManager.createTooltip(Constants.TOOLTIP_PROCESS1, process1);
        GuiManager.createTooltip(Constants.TOOLTIP_PROCESS2, process2);
        GuiManager.createTooltip(Constants.TOOLTIP_PROCESS3, process3);
    }

    /**
     * Init default values
     */
    public void initDefaultValues() {
    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        if (!MainSingleton.getInstance().profileArg.equals(Constants.DEFAULT)) {
            StorageManager sm = new StorageManager();
            currentConfig = sm.readProfileConfig(MainSingleton.getInstance().profileArg);
        }
        if (currentConfig.getProfileProcesses() != null && !currentConfig.getProfileProcesses().isEmpty()) {
            for (int i = 0; i < currentConfig.getProfileProcesses().size(); i++) {
                if (i == 0) process1.setValue(currentConfig.getProfileProcesses().getFirst());
                if (i == 1) process2.setValue(currentConfig.getProfileProcesses().get(1));
                if (i == 2) process3.setValue(currentConfig.getProfileProcesses().get(2));
            }
        }
        var processList = ProcessHandle.allProcesses()
                .map(ProcessHandle::info)
                .map(info -> info.command().orElse(""))
                .map(cmd -> {
                    int idx = Math.max(cmd.lastIndexOf('/'), cmd.lastIndexOf('\\'));
                    return idx >= 0 ? cmd.substring(idx + 1) : cmd;
                })
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        process1.getItems().addAll(processList);
        process2.getItems().addAll(processList);
        process3.getItems().addAll(processList);
        WidgetFactory widgetFactory = new WidgetFactory();
        gpuThreshold.setValueFactory(widgetFactory.gpuValueFactory());
        cpuThreshold.setValueFactory(widgetFactory.cpuValueFactory());
        gpuThreshold.getValueFactory().setValue(currentConfig.getGpuThreshold());
        cpuThreshold.getValueFactory().setValue(currentConfig.getCpuThreshold());
        setTooltips();
    }

    /**
     * Close dialog
     *
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save and close dialog
     *
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        apply(e);
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Apply settings
     *
     * @param e event
     */
    @FXML
    public void apply(InputEvent e) {
        process1.commitValue();
        process2.commitValue();
        process3.commitValue();
        settingsController.injectProfileController(this);
        settingsController.miscTabController.addProfile(e);
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    @SuppressWarnings("All")
    public void save(Configuration config) {
    }

}
