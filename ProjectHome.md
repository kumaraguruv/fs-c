Application to measure and analyze file systems to find the internal and temporal redundancy for file-based chunking and fingerprint-based data de-duplication.

The FS-C tools allow to analyze the internal and temporal redundancy of file system directories that are found by content-defined chunking using Rabin's fingerprinting method and static chunking with different chunk sizes.

The goal is to allow users to provide an estimate of the redundancy found by de-duplication systems for their concrete workload and to provide a basis for further enhancement to the tools and for e.g. application-specific chunking methods.

The tool suite allows the analysis of the trace data in-memory. In addition, tools and scripts for an analysis on a Hadoop cluster are provided.

The FS-C tool suite has been used to gather and analyze the data for the following research papers:
  * "Dirk Meister, Andre Brinkmann, File Recipe Compression in Data Deduplication Systems, FAST 2013 (to appear)"
  * "Dirk Meister, Jürgen Kaiser, Andre Brinkmann, Toni Cortes, Michael Kuhn, Julian Kunkel, A Study on Data Deduplication in HPC Storage Systems, SC 2012"
  * "Dirk Meister, Andre Brinkmann: Multi-Level Comparision of Data Deduplication in a Backup Scenario, SYSTOR 2009".

Overall, the tool has used to analyse multiple PB of data.