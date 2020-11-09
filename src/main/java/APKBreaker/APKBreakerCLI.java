/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package APKBreaker;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class APKBreakerCLI {
    public static final String FILE_EXTENSION = "zip";

    public static void main(String[] args) {
        String logo = "\n" +
                "  _______ _______ ___ ___  _______                  __               \n" +
                " |   _   |   _   |   Y   )|   _   .----.-----.---.-|  |--.-----.----.\n" +
                " |.  1   |.  1   |.  1  / |.  1   |   _|  -__|  _  |    <|  -__|   _|\n" +
                " |.  _   |.  ____|.  _  \\ |.  _   |__| |_____|___._|__|__|_____|__|  \n" +
                " |:  |   |:  |   |:  |   \\|:  1    \\                                 \n" +
                " |::.|:. |::.|   |::.| .  |::.. .  /                                 \n" +
                " `--- ---`---'   `--- ---'`-------'                                  \n" +
                "                                                                     \n";
        System.out.println(logo);

        if (args.length  == 1) {
            Path path = Paths.get(args[0]);
            File file = new File(args[0]);
            Path newPath;

            FileProcesses fileProcesses = new FileProcesses();
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
                System.out.println("[ * ]" + xmlDecompressor);
                String androidManifestXML = xmlDecompressor.decompressXML(buf);
                System.out.println("[ * ]" + "Decompression successful");
                fileProcesses.writeTextFile(androidManifestXML, "AndroidManifest.xml");
                System.out.println("[ * ] AndroidManifest.xml decompressed.");

                System.out.println("[ * ] Retrieving classes.dex file(s)...");
                int totalNumOfDexFiles = fileProcesses.getClassesDex(zip, newZipFilePath);
                System.out.println("[ * ] A total of " + totalNumOfDexFiles + " Classes.dex file(s) are extracted");
                fileProcesses.readDexFiles(totalNumOfDexFiles, newPath);
            } catch (Exception e) {
                System.out.println("[ !! ] File is not a valid zip file");
            }
        } else {
            System.out.println("[ !! ] Usage: java -jar apkbreaker.jar /path/to/file.apk");
        }
    }
}
