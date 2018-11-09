package uk.rgu.data.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author 1113938
 */
public class FileOps {

  private static final Logger LOG = Logger.getLogger(FileOps.class.getName());

  /**
   * Writes content of collection to the file in supplied path.
   *
   * @param lines
   * @param fileNameWithPath
   */
  public static void printResults(Collection<String> lines, String fileNameWithPath) {
    // Create file
    File out = new File(fileNameWithPath);
    // Clear entries in file
//    try (PrintWriter writer = new PrintWriter(out)) {
//      writer.print("");
//    } catch (FileNotFoundException ex) {
//      logger.log(Level.SEVERE, null, ex);
//    }
    // Write to output file
    try {
      FileUtils.writeLines(out, lines, false); // false: overwrite (don't append)
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Writes content of collection to the file in supplied path.
   *
   * @param lines
   * @param fileNameWithPath
   * @param append True to append, false to overwrite.
   */
  public static synchronized void printResults(Collection<String> lines, String fileNameWithPath, boolean append) {
    // Create file
    File out = new File(fileNameWithPath);
    // Write to output file
    try {
//      FileUtils.writeStringToFile(out, System.getProperty("line.separator"), append);
      FileUtils.writeLines(out, lines, append); // false: overwrite (don't append)
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Writes content of collection to the file in supplied path.
   *
   * @param content
   * @param filePath
   */
  public static void printResults(String content, String filePath) {
    // Create file
    File out = new File(filePath);
    // Write to output file
    try {
      FileUtils.writeStringToFile(out, content);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Writes content of collection to the file in supplied path (thread-safe).
   *
   * @param content
   * @param filePath
   * @param append
   */
  public static synchronized void printResults(String content, String filePath, boolean append) {
    // Create file
    File out = new File(filePath);
    // Write to output file
    try {
      FileUtils.writeStringToFile(out, content, append);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Write sentences to file with clear separation of end-of-sentence.
   *
   * @param sentences
   * @param outFilePath
   * @throws IOException
   */
  public static void writeSentencesToFile(List<String> sentences, String outFilePath) throws IOException {
    PrintWriter pw = new PrintWriter(new FileWriter(outFilePath, true)); // output writer. true to append text
    for (String sentence : sentences) {
      if (!sentence.trim().equals("")) {
        pw.println(sentence);
      }
    }
    pw.close();
  }

  /**
   * Write list of strings to file in thread-safe manner.
   * Each non-empty entry is written to a new line in file.
   *
   * @param sentences
   * @param outFilePath
   * @throws IOException
   */
  public static synchronized void writeSentencesToFileSync(List<String> sentences, String outFilePath) throws IOException {
    PrintWriter pw = new PrintWriter(new FileWriter(outFilePath, true)); // output writer. true to append text
    for (String sentence : sentences) {
      if (!sentence.trim().equals("")) {
        pw.println(sentence);
      }
    }
    pw.close();
  }

  /**
   * Removes a directory and all its content.
   *
   * @param path
   * @return
   */
  public static boolean clearDirectory(File path) {
    if (path.exists()) {
      File[] files = path.listFiles();
      // Some JVM could return null when the directory is empty.
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            clearDirectory(file);
          } else {
            file.delete();
          }
        }
      }
    } else {
      // File does not exist
      return true;
    }
    return (path.delete());
  }

  /**
   * Creates a directory if it does not exist.
   *
   * @param dirPath
   */
  public static void createDirIfNotExist(String dirPath) {
    File theDir = new File(dirPath);

    // if the directory does not exist, create it
    if (!theDir.exists()) {
      LOG.log(Level.INFO, "Creating directory: {0}", dirPath);
      boolean result = false;

      try {
        result = theDir.mkdirs();
      } catch (SecurityException se) {
        LOG.log(Level.SEVERE, "Could not create the directory!", se);
      }
      if (result) {
        LOG.info("Directory created!");
      }
    }
  }

  /**
   * Retrieves the text in a file (using Apache Tika). Apache Tika enables this
   * to handle a wide variety of MIME.
   *
   * @param filePath
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   * @throws SAXException
   * @throws TikaException
   */
  public static String getFileContent(Path filePath) throws FileNotFoundException, IOException, SAXException, TikaException {
    //detecting the file type
    Parser parser = new AutoDetectParser();
    BodyContentHandler handler = new BodyContentHandler(-1);
    Metadata metadata = new Metadata();
    FileInputStream inputstream = new FileInputStream(filePath.toFile());
    ParseContext pcontext = new ParseContext();

    parser.parse(inputstream, handler, metadata, pcontext);

    return handler.toString();
  }

  /**
   * Retrieves the text of a text (.txt or similar plain format) file. Lucene
   * cannot create the term vectors and tokens for reader class. You have to
   * index its text values to the index. It would be better if this was in
   * another class. (Consider having this in a separate file.)
   *
   * @param f
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static String getAllText(File f) throws FileNotFoundException, IOException {
    String textFileContent;
    try (BufferedReader in = new BufferedReader(
            new InputStreamReader(
                    new FileInputStream(f), "UTF8"))) {
      textFileContent = "";
      String line;
      while ((line = in.readLine()) != null) {
//        textFileContent = textFileContent + " " + line;
        textFileContent = textFileContent + System.getProperty("line.separator") + line;
      }
    }
    return textFileContent.replaceAll("\\s\\s+", " ").trim();
  }

  /**
   * Retrieves the text of a text (.txt or similar plain format) file
   * line-by-line. File is read line-by-line into a list. (This method was used
   * to read file that had been split into a sentence per line).
   *
   * @param f
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static List<String> getTextLines(File f) throws FileNotFoundException, IOException {
    List<String> textFileContent = new ArrayList<String>();
    try (BufferedReader in = new BufferedReader(
            new InputStreamReader(
                    new FileInputStream(f), "UTF8"))) {
      String line;
      while ((line = in.readLine()) != null) {
        textFileContent.add(line);
      }
    }

    return textFileContent;
  }

  /**
   * Reads a text file line-by-line into a list. Terms are read in in lowercase
   * and a set data structure is used to force uniqueness of terms in final
   * list.
   *
   * @param filePath
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static List<String> getList(String filePath) throws FileNotFoundException, IOException {
    Set<String> set = new HashSet<String>();

    try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
      stream.forEach(s -> {
        set.add(s.toLowerCase().trim());
      });
    }

    List<String> list = new ArrayList<String>(set);

    return list;
  }

  /**
   * Retrieves all file paths in a directory and sub-directories. Sub-directory
   * paths are included in returned paths list.
   *
   * @param path
   * @return
   * @throws IOException
   */
  public static List<Path> getFilesInDirectory(Path path) throws IOException {
    List<Path> files = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry)) {
          getFilesInDirectory(entry);
        }
        files.add(entry);
      }
    }

    return files;
  }

  /**
   * Retrieves files in a directory and sub-directories. Sub-directory names are
   * excluded in returned list.
   *
   * @param directoryName
   * @param files List to add files to.
   * @return
   */
  public static List<File> listFiles(String directoryName, List<File> files) {
    File directory = new File(directoryName);

    // get all the files from a directory
    File[] fList = directory.listFiles();
    for (File file : fList) {
      if (file.isFile()) {
        files.add(file);
      } else if (file.isDirectory()) {
        listFiles(file.getAbsolutePath(), files);
      }
    }

    return files;
  }

}
