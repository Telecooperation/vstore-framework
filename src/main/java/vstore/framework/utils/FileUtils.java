package vstore.framework.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import vstore.framework.file.VFileType;

/**
 * Some utilities for working with files on the filesystem.
 */
public class FileUtils {

    /**
     * Prevent instantiation.
     */
    private FileUtils() {}

    /**
     * Moves the given file to the output path.
     * @param inputFile The file to move.
     * @param outputPath The path where to put the file.
     * @param outputName The name of the output file (can be null, then the original name is used).
     * @return true if the move was successful
     */
    public static File moveFile(File inputFile, File outputPath, String outputName) {
        InputStream in;
        OutputStream out;

        try {
            //Create output directory if it doesn't exist
            if (!outputPath.exists())
            {
                outputPath.mkdirs();
            }
            in = new FileInputStream(inputFile.getAbsolutePath());
            if (outputName == null || "".equals(outputName)) {
                outputName = outputPath.getAbsolutePath() + "/" + inputFile.getName();
            } else {
                outputName = outputPath.getAbsolutePath() + "/" + outputName;
            }
            out = new FileOutputStream(outputName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();

            // write the output file
            out.flush();
            out.close();

            // delete the original file
            inputFile.delete();
            return new File(outputName);
        }
        catch (FileNotFoundException fnfe1) {
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Copies the given file to the given output path.
     *
     * @param inputFile The file to copy.
     * @param outputPath The path to which to save the file.
     * @param outputName The name of the output file (can be null, then the original name is used).
     * @return The object of the new file if successful. If not, null.
     */
    public static File copyFile(File inputFile, File outputPath, String outputName) {
        try {
            FileInputStream in = new FileInputStream(inputFile.getAbsolutePath());
            File copiedFile = copyFileFromInputStream(in, inputFile.getName(), outputPath, outputName);
            in.close();

            return copiedFile;
        }  catch (FileNotFoundException fnfe1) {
            return null;
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
     * Copies a file fro the input stream to the given output path with the given output name.
     * The given file input stream will not be closed.
     * 
     * @param in The file input stream to copy data from.
     * @param inputFileName The filename of the input file.
     * @param outputPath The output path where to save the new file.
     * @param outputName The name of the new file.
     * @return A File object containing a reference to the path of the created file.
     */
    private static File copyFileFromInputStream(FileInputStream in, String inputFileName,
                                                File outputPath, String outputName) {
        OutputStream out;
        try {
            //Create output directory if it doesn't exist
            if (!outputPath.exists()) {
                outputPath.mkdirs();
            }
            if (outputName == null || "".equals(outputName)) {
                outputName = outputPath.getAbsolutePath() + "/" + inputFileName;
            } else {
                outputName = outputPath.getAbsolutePath() + "/" + outputName;
            }
            out = new FileOutputStream(outputName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();

            //Write the output file (file has now been copied)
            out.flush();
            out.close();
            return new File(outputName);

        }  catch (FileNotFoundException fnfe1) {
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Writes the given String to the given file in the given output path. Creates necessary
     * directories on the way to output path if they do not exist.
     * @param s The string to write to the file.
     * @param outputPath The path where the output file should be created.
     * @param outputName The name of the output file.
     * @return The file object referencing the created file.
     */
    public static File writeStringToFile(String s, File outputPath, String outputName) {
        //Create output directory if it doesn't exist
        if (!outputPath.exists()) {
            outputPath.mkdirs();
        }
        if (outputName == null || "".equals(outputName)) {
            return null;
        } else {
            outputName = outputPath.getAbsolutePath() + "/" + outputName;
        }
        try {
            PrintWriter out = new PrintWriter(outputName);
            out.write(s);
            out.close();
            return new File(outputName);
        } catch(FileNotFoundException e) {
            return null;
        }
    }

    /**
     * Deletes the given file if it exists.
     * @param file The file to delete.
     */
    public static void deleteFile(File file) {
        if(file != null && file.exists()) {
            file.delete();
        }
    }

    /**
     * Returns the MIME type of a file based on the file's extension.
     *
     * @param filename The filename of the file including the file extension.
     * @return Null, if no file extension available. Otherwise, the MIME type corresponding
     *         to the file extension.
     */
    public static String getMimeType(String filename) {
        String filenameArray[] = filename.split("\\.");
        if(filenameArray.length > 0) {
            String extension = filenameArray[filenameArray.length - 1];
            if(VFileType.isExtensionSupported(extension)) {
                String mapResult = VFileType.getMimeTypeFromExtension(extension);
                if(mapResult != null) {
                    return mapResult;
                }
            }
        }
        return VFileType.getMimeTypeFromExtension("vstor");
    }
}
