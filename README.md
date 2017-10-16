#  OMOP CDM Indexer

## Introduction
This application provides a pipeline for the conversion of clinical notes into the OHDSI OMOP Common Data Model.
It also further provides an optional pipeline component that then stores these CDM objects in hierarchical form within 
an ElasticSearch index

## Prerequisites
- Java Runtime Environment v8 or Higher
- A clinical note collection
- A collection reader capable of reading the clinical note collection into UIMA (BioBankCNDeserializer included in this 
repository is specialized for Mayo's BioBank clinical note collection and will not function for anything else)
- A UMLS license
- A multithreaded computing environment
- OHDSI ATHENA csv specifications (can be found at http://athena.ohdsi.org/)
- cTAKES dictionary resources (can be found at http://ctakes.apache.org/)

## Runtime Arguments
### Required Arguments
- -Dctakes.umlsuser=\[your_umls_username]
- -Dctakes.umlspw=\[your_umls_password]
- -DdictionaryPath=\[path_relative_to_umls_dictionary] (Default: resources/org/apache/ctakes/dictionary/lookup/fast/sno_rx_16ab/sno_rx_16ab.script)
### Optional Arguments
- -Dpipeline.threads=\[number_of_threads_to_use_on_nlp] (Default: Available CPU Cores/2)
- -Dindexing.threads=\[number_of_threads_to_use_for_indexing] (Default: 1)

## Generating Required Dictionaries
- The OMOP CDM Indexer contains code that will autonomously generate appropriate sqlite databases from OEM tab-delimited data tables.
Obtain OHDSI ATHENA csv specifications from http://athena.ohdsi.org/ and place in the "OHDSI" folder in your working directory.
- The OMOP CDM Indexer contains code that will autonomously generate appropriate SNOMED and UMLS lookup tables for UMLS 
CUI -> SNOMED Lookup as well as hierarchial checks. A full SNOMEDCT_US download (available from UMLS website) is required
inside the "SNOMEDCT_US" folder, and the MRCONSO.RRF file from a valid UMLS installation containing SNOMEDCT codes is required 
required in the "UMLS" folder
- cTAKES Dictionary Specifications can be converted into a format compatible with the modified dictionary lookup
- OMOP CDM Indexer requires several additional dictionaries. These can be generated using the dictionary creator included 
with the cTAKES binary download (http://ctakes.apache.org/) and a UMLS installation with SNOMED source vocabularies. The dictionary descriptor xml and associated folder for generated dictionaries should then be
placed in the resources/dictionary folder:
    - Ethnicity and Race extraction requires a dictionary with the T098 (Population Group) semantic type selected. The generated
    database should be named "ethnicitiesandraces"
