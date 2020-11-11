package APKBreaker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileProcesses {

    public void CreateNewDirectory(Path currPath) {
        File file = new File(currPath.toString());
        boolean isNewDirMade = file.mkdir();
        if (isNewDirMade) {
            System.out.println("[ * ] New directory made, subsequent files are added to this directory");
        } else {
            System.out.println("[ * ] New directory not made, subsequent files will be added to your current directory");
        }
    }

    public Path zipFile(Path path, File file, String new_extension, String dest) {
        int indexOfExt = file.getName().lastIndexOf('.');
        String name = file.getName().substring(0,indexOfExt);
        try {
            if (path.getParent() != null) {
                Path destPath = Paths.get(path.getParent().toString(), dest, dest);
                path = Files.copy(file.toPath(), destPath.resolveSibling(name + "." + new_extension), StandardCopyOption.REPLACE_EXISTING);
            } else { //Path parent == null when the jar file is executed at the apk directory
                Path destPath = Paths.get(name, dest);
                path = Files.copy(file.toPath(), destPath.resolveSibling(name + "." + new_extension), StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("[ * ] zip file has been created from APK");
        } catch (Exception e) {
            System.out.println("[ !! } Unable to get path");
            System.out.println("[ !! ] Usage: java -jar apkbreaker.jar /path/to/file.apk");
        }

        return path;
    }

    public void writeTextFile(String contents, String fileName) {
        try {
            //TODO: Add dest to AndroidManifest
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(contents);
            fileWriter.close();
            System.out.println("[ * ] AndroidManifest.xml extracted...");
        } catch (Exception e) {
            System.out.println("[ !! ] File write is unsuccessful.");
        }
    }

    public int getClassesDex(ZipFile zipFile, Path zipFilePath) {
        int numOfDexFiles = 0;
        try {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while ( entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                String zipFileContentName = entry.getName();

                if (zipFileContentName.contains("classes")) {
                    try (FileSystem fileSystem = FileSystems.newFileSystem(zipFilePath, null)) {
                        numOfDexFiles++;
                        Path fileToExtract = fileSystem.getPath(zipFileContentName);
                        Path extractToPath = zipFilePath.resolveSibling(zipFileContentName);
                        Files.copy(fileToExtract, extractToPath);
                    }
                }
            }
            System.out.println("[ * ] Classes.dex file(s) are extracted");
            zipFile.close();
        } catch (Exception e) {
            System.out.println("[ !! ] Zip File cannot be opened/read.");
        }
        return numOfDexFiles;
    }

    public void readDexFiles(int numOfDexFiles, Path dexFilePath) {
        System.out.println("[ * ] Reading all classes.dex file...");
        for (int i = 1; i <= numOfDexFiles; i++) {
            try {
                String dexFileName = "";
                if (i == 1) {
                    dexFileName = "classes.dex";
                } else {
                    dexFileName = "classes" + i + ".dex";
                }
                byte[] dexFileContent = Files.readAllBytes(Paths.get(dexFilePath.toString(), dexFileName));
                ByteBuffer byteBuffer = ByteBuffer.wrap(dexFileContent);
            } catch (IOException e) {
                System.out.println("Unable to read classes" + i + ".dex file");
            }
        }
    }
}
