# LuceneWebSearch 
application, at first, provides the possibility to enter web page address for indexing, then, after a few minutes, provides the possibility to enter keywords to seacrh in recursively indexed pages, result list can be sorted alphabetically or by relevance. 

Spring, Lucene packages are used. 

Unfortunately, because of some write locks, I could not Lucene on hard disk, only RAMDirectory is propertly used.
Commented lines of code could be used to degrade to Java 7 and to consider some possible forks of code. 
