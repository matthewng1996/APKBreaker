package APKBreaker;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class APKBreakerGUI extends Application {

    public static final String FILE_EXTENSION = "zip";

    String logo = "\n" +
            "  _______ _______ ___ ___  _______                  __               \n" +
            " |   _   |   _   |   Y   )|   _   .----.-----.---.-|  |--.-----.----.\n" +
            " |.  1   |.  1   |.  1  / |.  1   |   _|  -__|  _  |    <|  -__|   _|\n" +
            " |.  _   |.  ____|.  _  \\ |.  _   |__| |_____|___._|__|__|_____|__|  \n" +
            " |:  |   |:  |   |:  |   \\|:  1   \\                                 \n" +
            " |::.|:. |::.|   |::.| .  |::.. .  /                                 \n" +
            " `--- ---`---'   `--- ---'`-------'                                  \n" +
            "                                                                     \n";

    // Create the TextArea for the Output
    private TextArea outputArea = new TextArea();

    BorderPane gui;
    SplitPane splitVertical;
    SplitPane splitHorizontal;

    BorderPane left;
    BorderPane center;
    BorderPane right;

    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.setTitle("APKBreaker");

        //Set initial window size
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());

        //Create MenuBar
        MenuBar menuBar = createMenuBar(primaryStage);
        VBox vBox = new VBox(menuBar);

        splitVertical = new SplitPane();
        splitVertical.setOrientation(Orientation.VERTICAL);

        splitHorizontal = new SplitPane();
        splitHorizontal.setOrientation(Orientation.HORIZONTAL);

        left = new BorderPane(new Label("Left"));
        center = new BorderPane(new Label("MainScreen"));
        right = new BorderPane(new Label("Right"));

        splitHorizontal.getItems().addAll(left, center, right);
        splitVertical.getItems().addAll(splitHorizontal, outputArea);
        splitVertical.setDividerPositions(0.75);
        splitHorizontal.setDividerPositions(0.15, 0.85);

        gui = new BorderPane();
        gui.setTop(vBox);
        gui.setCenter(splitVertical);

        //Set log console properties
        outputArea.setStyle("-fx-control-inner-background:#000000; -fx-font-family: Consolas; -fx-highlight-fill: #00ff00; -fx-highlight-text-fill: #000000; -fx-text-fill: #00ff00;");
        outputArea.setEditable(false);

        writeOutput(logo);
        writeOutput("[ * ] Welcome to APKBreaker! To begin, simply upload your APK in the 'File' settings. File > Upload new APK");

        Scene scene = new Scene(gui, 1000, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(APKBreakerGUI.class, args);
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu menu_file = new Menu("File");
        MenuItem file_upload = new MenuItem("Upload new APK");
        MenuItem file_save = new MenuItem("Save Project");
        MenuItem file_load = new MenuItem("Load Project");
        MenuItem file_exit = new MenuItem("Quit/Exit");

        menu_file.getItems().addAll(file_upload, file_save, file_load, file_exit);

        file_upload.setOnAction(e -> {
            FileUpload(stage);
        });

        file_save.setOnAction(e -> {
            SaveProject();
        });

        file_load.setOnAction(e -> {
            LoadProject();
        });

        file_exit.setOnAction(e -> {
            ExitApplication();
        });

        Menu menu_edit = new Menu("Edit");

        Menu menu_options = new Menu("Options");
        MenuItem options_Themes = new MenuItem("Themes");

        menu_options.getItems().addAll(options_Themes);

        menuBar.getMenus().add(menu_file);
        menuBar.getMenus().add(menu_edit);
        menuBar.getMenus().add(menu_options);

        return menuBar;
    }

    // Method to log the Message to the Output-Area
    private void writeOutput(String msg)
    {
        this.outputArea.appendText(msg + "\n");
    }

    private void FileUpload(Stage stage) {
        final FileChooser fileChooser = new FileChooser();
        configureFileChooser(fileChooser);
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            //Do something to file
            Path path = Paths.get(file.getPath());
            FileProcesses fileProcesses = new FileProcesses();
            Path newPath;

            String[] newDirectoryNames = file.getPath().split(Pattern.quote(File.separator));
            String newDirectoryName = newDirectoryNames[newDirectoryNames.length - 1];
            newDirectoryName = newDirectoryName.replace("com.", "");
            newDirectoryName = newDirectoryName.replace(".apk", "");

            if(path.getParent() != null) {
                newPath = Paths.get(path.getParent().toString(), newDirectoryName);
            } else {
                newPath = Paths.get(newDirectoryName);
            }

            fileProcesses.CreateNewDirectory(newPath);

            Path newZipFilePath = fileProcesses.zipFile(path, file, FILE_EXTENSION, newDirectoryName);

            try {
                ZipFile zip = new ZipFile(newZipFilePath.toFile());
                ZipEntry manifest = zip.getEntry("AndroidManifest.xml");
                InputStream inputStream = zip.getInputStream(manifest);
                System.out.println("[ * ] AndroidManifest.xml retrieved...");

                byte[] buf = new byte[100000];
                inputStream.read(buf);
                inputStream.close();

                XMLDecompressor xmlDecompressor = new XMLDecompressor();
                writeOutput("[ * ]" + xmlDecompressor);
                String androidManifestXML = xmlDecompressor.decompressXML(buf);
                writeOutput("[ * ]" + "Decompression successful");
                fileProcesses.writeTextFile(androidManifestXML, "AndroidManifest.xml");
                writeOutput("[ * ] AndroidManifest.xml decompressed.");

                System.out.println("[ * ] Retrieving classes.dex file(s)...");
                int totalNumOfDexFiles = fileProcesses.getClassesDex(zip, newZipFilePath);
                writeOutput("[ * ] A total of " + totalNumOfDexFiles + " Classes.dex file(s) are extracted");
                fileProcesses.readDexFiles(totalNumOfDexFiles, newPath);
            } catch (Exception e) {
                writeOutput("[ !! ] File is not a valid zip file");
            }
        }
    }

    private void SaveProject() {

    }

    private void LoadProject() {

    }

    private void ExitApplication() {
        System.exit(0);
    }

    private static void configureFileChooser(final FileChooser fileChooser){
        fileChooser.setTitle("View APKs");
        fileChooser.setInitialDirectory(
                new File(System.getProperty("user.dir"))
        );

        //Set extension filters
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                new FileChooser.ExtensionFilter("APK", "*.apk")
        );
    }
}
