![solrex logo](https://i.ibb.co/L8fDFsW/final-Solr-1-cropped.png)

# Solrex
An administration toolkit and interface for managing large scale Apache Solr™ clusters. 

## Features
### Reindexer
- Support for variable sized sharding configurations between source and destination collections.
- Automatic progress reporting and abortion capability.
- Optional support for NAT networking configurations (eg. running Solr on an isolated network via Docker, Kubernetes...)
## Feature tracking
[See the following Trello board](https://trello.com/b/FrMiMvTp/solrex)

## Technologies
- **Project Reactor along with Solrs and SolrJ** for asynchronous communication with Solr
- **Spring Boot with Spring Webflux** as a backend wrapper
