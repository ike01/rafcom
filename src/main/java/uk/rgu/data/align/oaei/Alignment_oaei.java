/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.rgu.data.align.oaei;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import uk.rgu.data.utilities.FileOps;

/**
 *
 * @author aikay
 */
public class Alignment_oaei {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    // TODO code application logic here
    generateBencAlignTrainTest().forEach(System.out::println);
  }

//  public static List<URI> getOntologyFiles() throws URISyntaxException {
//    List<URI> fileUris = new ArrayList();
//    fileUris.add(new URI("data/2016_conference/cmt.owl"));
//    fileUris.add(new URI("data/2016_conference/Cocus.owl"));
//    fileUris.add(new URI("data/2016_conference/Conference.owl"));
//    fileUris.add(new URI("data/2016_conference/confious.owl"));
//    fileUris.add(new URI("data/2016_conference/confOf.owl"));
//    fileUris.add(new URI("data/2016_conference/crs_dr.owl"));
//    fileUris.add(new URI("data/2016_conference/edas.owl"));
//    fileUris.add(new URI("data/2016_conference/ekaw.owl"));
//    fileUris.add(new URI("data/2016_conference/iasted.owl"));
//    fileUris.add(new URI("data/2016_conference/linklings.owl"));
//    fileUris.add(new URI("data/2016_conference/MICRO.owl"));
//    fileUris.add(new URI("data/2016_conference/MyReview.owl"));
//    fileUris.add(new URI("data/2016_conference/OpenConf.owl"));
//    fileUris.add(new URI("data/2016_conference/paperdyne.owl"));
//    fileUris.add(new URI("data/2016_conference/PCS.owl"));
//    fileUris.add(new URI("data/2016_conference/sigkdd.owl"));
//
//    return fileUris;
//  }
  public static int expectedClasses(String refAlignName) {
    int count;
    switch (refAlignName.toLowerCase()) {
      case "cmt-conference.rdf":
        count = 12;
        break;
      case "cmt-confof.rdf":
        count = 10;
        break;
      case "cmt-edas.rdf":
        count = 8;
        break;
      case "cmt-ekaw.rdf":
        count = 8;
        break;
      case "cmt-iasted.rdf":
        count = 4;
        break;
      case "cmt-sigkdd.rdf":
        count = 10;
        break;
      case "conference-confof.rdf":
        count = 11;
        break;
      case "conference-edas.rdf":
        count = 14;
        break;
      case "conference-ekaw.rdf":
        count = 23;
        break;
      case "conference-iasted.rdf":
        count = 13;
        break;
      case "conference-sigkdd.rdf":
        count = 12;
        break;
      case "confof-edas.rdf":
        count = 14;
        break;
      case "confof-ekaw.rdf":
        count = 20;
        break;
      case "confof-iasted.rdf":
        count = 9;
        break;
      case "confof-sigkdd.rdf":
        count = 6;
        break;
      case "edas-ekaw.rdf":
        count = 19;
        break;
      case "edas-iasted.rdf":
        count = 19;
        break;
      case "edas-sigkdd.rdf":
        count = 11;
        break;
      case "ekaw-iasted.rdf":
        count = 10;
        break;
      case "ekaw-sigkdd.rdf":
        count = 11;
        break;
      case "iasted-sigkdd.rdf":
        count = 15;
        break;
      case "301_refalign.rdf":
        count = 22;
        break;
      case "302_refalign.rdf":
        count = 23;
        break;
      case "303_refalign.rdf":
        count = 18;
        break;
      case "304_refalign.rdf":
        count = 31;
        break;
      default:
        count = 0;
        break;
    }

    return count;
  }

  public static List<AlignTrainTest> generateConfAlignTrainTest() {
    List<AlignTrainTest> allTestcaseData = new ArrayList<AlignTrainTest>();
    String ontoFilesBase = "data/2016_conference/7/";
    List<File> refAlignmentFiles = FileOps.listFiles("data/2016_conference/reference-alignment", new ArrayList());
    for (File file : refAlignmentFiles) {
      String fileName = FilenameUtils.removeExtension(file.getName());
      String[] nameSplit = fileName.split("-");
      File sourceOnto = new File(ontoFilesBase + nameSplit[0] + ".owl");
      File targetOnto = new File(ontoFilesBase + nameSplit[1] + ".owl");
      AlignTrainTest alignTrainTest = new AlignTrainTest(sourceOnto, targetOnto, file, "");
      alignTrainTest.setExpectedClassCount(expectedClasses(file.getName()));
      allTestcaseData.add(alignTrainTest);
//      System.out.println(alignTrainTest);
    }

    return allTestcaseData;
  }

  public static List<AlignTrainTest> generateBencAlignTrainTest() {
    List<AlignTrainTest> allTestcaseData = new ArrayList<AlignTrainTest>();
    String ontoFilesBase = "data/2016_benchmark/";
    List<File> refAlignmentFiles = FileOps.listFiles("data/2016_benchmark/reference-alignment", new ArrayList());
    for (File file : refAlignmentFiles) {
      String fileName = FilenameUtils.removeExtension(file.getName());
      File sourceOnto = new File(ontoFilesBase + "101_onto.rdf");
      File targetOnto = new File(ontoFilesBase + fileName.split("_")[0] + "_onto.rdf");
      AlignTrainTest alignTrainTest = new AlignTrainTest(sourceOnto, targetOnto, file, "");
      alignTrainTest.setExpectedClassCount(expectedClasses(file.getName()));
      allTestcaseData.add(alignTrainTest);
//      System.out.println(alignTrainTest);
    }

    return allTestcaseData;
  }

}
