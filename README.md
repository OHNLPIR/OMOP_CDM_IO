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

## Runtime Arguments
### Required Arguments
- -Dctakes.umlsuser=\[your_umls_username]
- -Dctakes.umlspw=\[your_umls_password]
- -DdictionaryPath=\[path_relative_to_umls_dictionary] (Default: resources/org/apache/ctakes/dictionary/lookup/fast/sno_rx_16ab/sno_rx_16ab.script)
### Optional Arguments
- -Dpipeline.threads=\[number_of_threads_to_use_on_nlp] (Default: Available CPU Cores/2)
- -Dindexing.threads=\[number_of_threads_to_use_for_indexing] (Default: 1)
