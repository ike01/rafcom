# README #

### What is this repository for? ###

Java maven application for implementing Rafcom ontology alignment approach. It can be executed using maven commands or through an IDE. To use maven for execution, pom.xml has to be updated accordingly.

### Installation ###

Some libraries used in the project were not found on maven. The .jar files of such libraries are included in the /lib directory for installation into local maven.

### Description ###

This classes in this repository are specific to experiments described in a research paper. Choice of class names were informed accrdingly.

1. RafcomOA_Benc: implements selection of candidate alignments and generates features of the benchmark dataset.
2. RafcomOA_Conf: similar to RafcomOA_Benc but for the conference dataset.

Both classes require a word embedding model. The GoogleNews Negative300 model is publicly available and was one of the models used. The classes above generate alignment features as .csv files which are used for subsequent machine classification.
The classes for machine classifcation are in the align.ml.eval package. The [Weka Java API] (https://www.cs.waikato.ac.nz/ml/weka/) was used for machine learning. For this purpose, generated .csv files were converted to .arff format. This can be modified to directly work with .csv.

1. ApproachesBenchmark: implements alternative alignment approaches for the benchmark dataset.
2. ApproachesConference: implements alternative alignment approaches for the conference dataset.

