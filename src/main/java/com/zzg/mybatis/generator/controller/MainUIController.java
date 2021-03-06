package com.zzg.mybatis.generator.controller;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.zzg.mybatis.generator.bridge.MybatisGeneratorBridge;
import com.zzg.mybatis.generator.model.DatabaseConfig;
import com.zzg.mybatis.generator.model.GeneratorConfig;
import com.zzg.mybatis.generator.model.UITableColumnVO;
import com.zzg.mybatis.generator.util.ConfigHelper;
import com.zzg.mybatis.generator.util.DbUtil;
import com.zzg.mybatis.generator.util.StringUtils;
import com.zzg.mybatis.generator.view.AlertUtil;
import com.zzg.mybatis.generator.view.UIProgressCallback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mybatis.generator.config.ColumnOverride;
import org.mybatis.generator.config.IgnoredColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.junit.Assert.assertNotNull;

public class MainUIController extends BaseFXController {

    private static final Logger _LOG = LoggerFactory.getLogger(MainUIController.class);
    private static final String PROJECT_FOLDER_NOT_SELECT = "Please choose project folder!";
    private static final String PROJECT_FOLDER_NOT_EXIST = "Project folder not exist，create?";
    private static final String MODEL_FOLDER_NOT_INPUT = "Please input model folder!";
    private static final String MODEL_FOLDER_NOT_EXIST = "Model target folder not exist, create?";
    private static final String DAO_FOLDER_NOT_INPUT = "Please input dao folder!";
    private static final String DAO_FOLDER_NOT_EXIST = "Dao target folder not exist, create?";
    private static final String MAPPER_FOLDER_NOT_INPUT = "Please input mapping folder!";
    private static final String MAPPER_FOLDER_NOT_EXIST = "Mapping target folder not exist, create?";

    // tool bar buttons
    @FXML
    private Label connectionLabel;
    @FXML
    private Label configsLabel;
    @FXML
    private TextField connectorPathField;
    @FXML
    private TextField modelTargetPackage;
    @FXML
    private TextField mapperTargetPackage;
    @FXML
    private TextField daoTargetPackage;
    @FXML
    private TextField tableNameField;
    @FXML
    private TextField domainObjectNameField;
    @FXML
    private TextField generateKeysField;	//添加输入框
    @FXML
    private TextField modelTargetProject;
    @FXML
    private TextField mappingTargetProject;
    @FXML
    private TextField daoTargetProject;
    @FXML
    private TextField mapperName;
    @FXML
    private TextField projectFolderField;
    @FXML
    private CheckBox offsetLimitCheckBox;
    @FXML
    private CheckBox commentCheckBox;
    @FXML
    private CheckBox annotationCheckBox;
    @FXML
    private CheckBox isCache;
    @FXML
    private TextField cacheType;
    @FXML
    private TextField flashInterval;

    @FXML
    private TreeView<String> leftDBTree;
    @FXML
    private TextArea consoleTextArea;
    // Current selected databaseConfig
    private DatabaseConfig selectedDatabaseConfig;
    // Current selected tableName
    private String tableName;

    private List<IgnoredColumn> ignoredColumns;

    private List<ColumnOverride> columnOverrides;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ImageView dbImage = new ImageView("icons/computer.png");
        dbImage.setFitHeight(40);
        dbImage.setFitWidth(40);
        connectionLabel.setGraphic(dbImage);
        connectionLabel.setOnMouseClicked(event -> {
            NewConnectionController controller = (NewConnectionController) loadFXMLPage("New Connection", FXMLPage.NEW_CONNECTION, false);
            controller.setMainUIController(this);
            controller.showDialogStage();
        });
        ImageView configImage = new ImageView("icons/config-list.png");
        configImage.setFitHeight(40);
        configImage.setFitWidth(40);
        configsLabel.setGraphic(configImage);
        configsLabel.setOnMouseClicked(event -> {
            GeneratorConfigController controller = (GeneratorConfigController) loadFXMLPage("Generator Config", FXMLPage.GENERATOR_CONFIG, false);
            controller.setMainUIController(this);
            controller.showDialogStage();
        });

        leftDBTree.setShowRoot(false);
        leftDBTree.setRoot(new TreeItem<>());
        Callback<TreeView<String>, TreeCell<String>> defaultCellFactory = TextFieldTreeCell.forTreeView();
        leftDBTree.setCellFactory((TreeView<String> tv) -> {
            TreeCell<String> cell = defaultCellFactory.call(tv);
            cell.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                int level = leftDBTree.getTreeItemLevel(cell.getTreeItem());
                TreeCell<String> treeCell = (TreeCell<String>) event.getSource();
                TreeItem<String> treeItem = treeCell.getTreeItem();
                if (level == 1) {
                    final ContextMenu contextMenu = new ContextMenu();
                    MenuItem item1 = new MenuItem("新建连接");
                    item1.setOnAction(event1 -> {
                        treeItem.getChildren().clear();
                    });
                    MenuItem item2 = new MenuItem("删除连接");
                    item2.setOnAction(event1 -> {
                        DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
                        try {
                            ConfigHelper.deleteDatabaseConfig(selectedConfig.getName());
                            this.loadLeftDBTree();
                        } catch (Exception e) {
                            AlertUtil.showErrorAlert("Delete connection failed! Reason: " + e.getMessage());
                        }
                    });
                    contextMenu.getItems().addAll(item1, item2);
                    cell.setContextMenu(contextMenu);
                }
                if (event.getClickCount() == 2) {
                    treeItem.setExpanded(true);
                    if (level == 1) {
                        System.out.println("index: " + leftDBTree.getSelectionModel().getSelectedIndex());
                        DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
                        try {
                            List<String> tables = DbUtil.getTableNames(selectedConfig);
                            if (tables != null && tables.size() > 0) {
                                ObservableList<TreeItem<String>> children = cell.getTreeItem().getChildren();
                                children.clear();
                                for (String tableName : tables) {
                                    TreeItem<String> newTreeItem = new TreeItem<>();
                                    ImageView imageView = new ImageView("icons/table.png");
                                    imageView.setFitHeight(16);
                                    imageView.setFitWidth(16);
                                    newTreeItem.setGraphic(imageView);
                                    newTreeItem.setValue(tableName);
                                    children.add(newTreeItem);
                                }
                            }
                        } catch (CommunicationsException e) {
                            _LOG.error(e.getMessage(), e);
                            AlertUtil.showErrorAlert("Connection timeout");
                        } catch (Exception e) {
                            _LOG.error(e.getMessage(), e);
                            AlertUtil.showErrorAlert(e.getMessage());
                        }
                    } else if (level == 2) { // left DB tree level3
                        String tableName = treeCell.getTreeItem().getValue();
                        selectedDatabaseConfig = (DatabaseConfig) treeItem.getParent().getGraphic().getUserData();
                        this.tableName = tableName;
                        tableNameField.setText(tableName);
                        domainObjectNameField.setText(StringUtils.dbStringToCamelStyle(tableName));
                    }
                }
            });
            return cell;
        });
        loadLeftDBTree();
    }

    void loadLeftDBTree() {
        TreeItem rootTreeItem = leftDBTree.getRoot();
        rootTreeItem.getChildren().clear();
        List<DatabaseConfig> dbConfigs = null;
        try {
            dbConfigs = ConfigHelper.loadDatabaseConfig();
            for (DatabaseConfig dbConfig : dbConfigs) {
                TreeItem<String> treeItem = new TreeItem<>();
                treeItem.setValue(dbConfig.getName());
                ImageView dbImage = new ImageView("icons/computer.png");
                dbImage.setFitHeight(16);
                dbImage.setFitWidth(16);
                dbImage.setUserData(dbConfig);
                treeItem.setGraphic(dbImage);
                rootTreeItem.getChildren().add(treeItem);
            }
        } catch (Exception e) {
            _LOG.error("connect db failed, reason: {}", e);
            AlertUtil.showErrorAlert(e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
        }
    }

    @FXML
    public void chooseConnectorFile() {
        FileChooser directoryChooser = new FileChooser();
        File selectedFolder = directoryChooser.showOpenDialog(getPrimaryStage());
        if (selectedFolder != null) {
            connectorPathField.setText(selectedFolder.getAbsolutePath());
        }
    }

    @FXML
    public void chooseProjectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedFolder = directoryChooser.showDialog(getPrimaryStage());
        if (selectedFolder != null) {
            projectFolderField.setText(selectedFolder.getAbsolutePath());
        }
    }

    @FXML
    public void generateCode() {
        if (tableName == null) {
            AlertUtil.showErrorAlert("Please select table from left DB treee first");
            return;
        }
        GeneratorConfig generatorConfig = getGeneratorConfigFromUI();
        if (!checkDirs(generatorConfig)) {
            return;
        }

        MybatisGeneratorBridge bridge = new MybatisGeneratorBridge();
        bridge.setGeneratorConfig(generatorConfig);
        bridge.setDatabaseConfig(selectedDatabaseConfig);
        bridge.setIgnoredColumns(ignoredColumns);
        bridge.setColumnOverrides(columnOverrides);
        bridge.setProgressCallback(new UIProgressCallback(consoleTextArea));
        try {
            bridge.generate();
        } catch (Exception e) {
            AlertUtil.showErrorAlert(e.getMessage());
        }
    }

    @FXML
    public void saveGeneratorConfig() {
        TextInputDialog dialog = new TextInputDialog("保存配置");
        dialog.setTitle("保存当前配置");
        dialog.setContentText("请输入配置名称");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String name = result.get();
            _LOG.info("user choose name: {}", name);
            try {
                GeneratorConfig generatorConfig = getGeneratorConfigFromUI();
                generatorConfig.setName(name);
                ConfigHelper.saveGeneratorConfig(generatorConfig);
            } catch (Exception e) {
                AlertUtil.showErrorAlert("删除配置失败");
            }
        }
    }

    public GeneratorConfig getGeneratorConfigFromUI() {
        GeneratorConfig generatorConfig = new GeneratorConfig();
        generatorConfig.setConnectorJarPath(connectorPathField.getText());
        generatorConfig.setProjectFolder(projectFolderField.getText());
        generatorConfig.setModelPackage(modelTargetPackage.getText());
        generatorConfig.setGenerateKeys(generateKeysField.getText());
        generatorConfig.setModelPackageTargetFolder(modelTargetProject.getText());
        generatorConfig.setDaoPackage(daoTargetPackage.getText());
        generatorConfig.setDaoTargetFolder(daoTargetProject.getText());
        generatorConfig.setMapperName(mapperName.getText());
        generatorConfig.setMappingXMLPackage(mapperTargetPackage.getText());
        generatorConfig.setMappingXMLTargetFolder(mappingTargetProject.getText());
        generatorConfig.setTableName(tableNameField.getText());
        generatorConfig.setDomainObjectName(domainObjectNameField.getText());
        generatorConfig.setOffsetLimit(offsetLimitCheckBox.isSelected());
        generatorConfig.setComment(commentCheckBox.isSelected());
        generatorConfig.setAnnotation(annotationCheckBox.isSelected());
        generatorConfig.setCache(isCache.isSelected());
        generatorConfig.setCacheType(cacheType.getText());
        generatorConfig.setFlashInterval(flashInterval.getText());
        return generatorConfig;
    }

    public void setGeneratorConfigIntoUI(GeneratorConfig generatorConfig) {
        connectorPathField.setText(generatorConfig.getConnectorJarPath());
        projectFolderField.setText(generatorConfig.getProjectFolder());
        modelTargetPackage.setText(generatorConfig.getModelPackage());
        generateKeysField.setText(generatorConfig.getGenerateKeys());
        modelTargetProject.setText(generatorConfig.getModelPackageTargetFolder());
        daoTargetPackage.setText(generatorConfig.getDaoPackage());
        daoTargetProject.setText(generatorConfig.getDaoTargetFolder());
        mapperTargetPackage.setText(generatorConfig.getMappingXMLPackage());
        mappingTargetProject.setText(generatorConfig.getMappingXMLTargetFolder());
        cacheType.setText(generatorConfig.getCacheType());
        flashInterval.setText(generatorConfig.getFlashInterval());
    }

    @FXML
    public void openTableColumnCustomizationPage() {
        if (tableName == null) {
            AlertUtil.showErrorAlert("Please select table from left DB treee first");
            return;
        }
        SelectTableColumnController controller = (SelectTableColumnController) loadFXMLPage("Select Columns", FXMLPage.SELECT_TABLE_COLUMN, true);
        controller.setMainUIController(this);
        try {
            // If select same schema and another table, update table data
            if (!tableName.equals(controller.getTableName())) {
                List<UITableColumnVO> tableColumns = DbUtil.getTableColumns(selectedDatabaseConfig, tableName);
                controller.setColumnList(FXCollections.observableList(tableColumns));
                controller.setTableName(tableName);
            }
            controller.showDialogStage();
        } catch (Exception e) {
            _LOG.error(e.getMessage(), e);
            AlertUtil.showErrorAlert(e.getMessage());
        }
    }

    public void setIgnoredColumns(List<IgnoredColumn> ignoredColumns) {
        this.ignoredColumns = ignoredColumns;
    }

    public void setColumnOverrides(List<ColumnOverride> columnOverrides) {
        this.columnOverrides = columnOverrides;
    }

    /**
     * check dirs exist
     *
     * @return
     */
    private boolean checkDirs(GeneratorConfig config) {
        assertNotNull(config);

        /* check project folder */
        String projectFolder = config.getProjectFolder();
        if (StringUtils.isBlank(projectFolder)) {
            AlertUtil.showInfoAlert(PROJECT_FOLDER_NOT_SELECT);
            return false;
        }

        if(!accept(projectFolder, PROJECT_FOLDER_NOT_EXIST)) {
            return false;
        }

        if(!projectFolder.endsWith(File.separator)) {
            projectFolder = projectFolder.concat(File.separator);
        }

        /* check model target folder */
        String modelTargetFolder = config.getModelPackageTargetFolder();
        if (StringUtils.isBlank(modelTargetFolder)) {
            AlertUtil.showInfoAlert(MODEL_FOLDER_NOT_INPUT);
            return false;
        }

        if(!accept(projectFolder.concat(modelTargetFolder), MODEL_FOLDER_NOT_EXIST)) {
            return false;
        }

        /* check dao target folder */
        String daoTargetFolder = config.getDaoTargetFolder();
        if (StringUtils.isBlank(daoTargetFolder)) {
            AlertUtil.showInfoAlert(DAO_FOLDER_NOT_INPUT);
            return false;
        }

        if(!accept(projectFolder.concat(daoTargetFolder), DAO_FOLDER_NOT_EXIST)) {
            return false;
        }

        /* check mapper target folder */
        String mapperTargetFolder = config.getMappingXMLTargetFolder();
        if (StringUtils.isBlank(mapperTargetFolder)) {
            AlertUtil.showInfoAlert(MAPPER_FOLDER_NOT_INPUT);
            return false;
        }

        if(!accept(projectFolder.concat(mapperTargetFolder), MAPPER_FOLDER_NOT_EXIST)) {
            return false;
        }

        return true;
    }

    /**
     * check dir exist && confirm mkdir
     *
     * @param dirPath
     * @param message
     * @return
     */
    private boolean accept(String dirPath, String message) {
        File file = new File(dirPath);
        if (file.exists()) {
            return true;
        }

        Alert confirmationAlert = AlertUtil.buildConfirmationAlert(message);
        Optional<ButtonType> optional = confirmationAlert.showAndWait();
        if (optional.isPresent()) {
            if (ButtonType.OK == optional.get()) {
                try {
                    FileUtils.forceMkdir(new File(dirPath));
                    return true;
                } catch (Exception e) {
                    AlertUtil.showErrorAlert("创建失败");
                }
            }
        }

        return false;
    }

}
