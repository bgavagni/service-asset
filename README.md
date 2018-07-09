### Summary:
This example project provides a set of REST APIs that facilitate uploading, 
storing and retrieving assets. The project hosted on Google App Engine and utilizes Google Cloud Storage for asset storage.


### Addressed Requirements:
###### REST API: 
###### POST /api/asset/upload
###### Parameters:
multipart/form-data
###### file - the uploaded asset to store
###### Description:
Provides a HTTP endpoint to upload assets and returns a response with JSON
content containing a unique identifier for the uploaded file.

###### REST API:
###### GET /api/asset/retrieve/<id>
###### Parameters:
###### id - the unique identifier of the stored asset
###### Description:
Provides an HTTP endpoint to download an asset by its unique identifier with
the original filename in the response.


### Additional Completed Requirements:
##### ● Add an endpoint that returns a list of all files in the system, their identifier, original filename, and the byte size of the file.
###### REST API:
###### GET /api/asset/list
###### Parameters:
###### none
###### Description:
Provides an HTTP endpoint that returns a response with a JSON array containing
all files in the stored in the system with their unique identifier, original
filename, and the byte size of the file in each entry.

##### ● Build a web page/app that provides a browser-based mechanism for using your upload and download endpoints.
`https://bgavagni-service-asset.appspot.com/index.html`


### Building:
###### NOTE: Maven is prerequisite and for building the project.

###### 1. Download the project archive and unzip.
###### 2. Change directory to the unzipped archive containing the pom.xml build file.
###### 3. Execute the following build targets:
`mvn clean verify`
##### Example:
    [INFO] --- maven-war-plugin:2.2:war (default-war) @ service-asset ---
    [INFO] Packaging webapp
    [INFO] Assembling webapp [service-asset] in [/Users/bgavagni/projects/rest-est/workspace/service-asset/target/service-asset-0.1.0-SNAPSHOT]
    [INFO] Processing war project
    [INFO] Copying webapp resources [/Users/bgavagni/projects/rest-test/workspace/service-asset/src/main/webapp]
    [INFO] Webapp assembled in [438 msecs]`
    [INFO] Building war: /Users/bgavagni/projects/rest-test/workspace/service-asset/target/service-asset-0.1.0-SNAPSHOT.war`
    [INFO] WEB-INF/web.xml already added, skipping
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS`
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 11.469 s
    [INFO] Finished at: 2018-07-08T14:44:32-04:00
    [INFO] ------------------------------------------------------------------------


### Running:
The Google App Engine can be run locally for development. The following are links for reference on how to setup a Google App Engine development environment.
`https://cloud.google.com/tools/intellij/docs/deploy-local`
`https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server`
##### Example:
`appengine-java-sdk/bin/dev_appserver --port=8080 service-asset/target/service-asset-0.1.0-SNAPSHOT`


### Testing:
The deployed version of the project can be tested at:
`https://bgavagni-service-asset.appspot.com`


### Design / Architectural / Technical Decisions:
The project was designed with JAX-RS as the REST API framework and
implemented on Google App Engine platform and Google Cloud Storage (Storage)
for asset storage.

The lightweight and flexibility of JAX-RS were key contributors for its
selection as the Java EE web framework of the REST API implementation.

Google App Engine was selected as it is a convenient Java supported PaaS 
that facilitates rapid prototyping of solutions which can scale from
small to large enterprise workloads.  Google App Engine offers a free tier
for small workloads making it an ideal option for quick projects.

Google Cloud Storage was selected as the asset storage facility for its
scalable, and robust handling of object storage.

Finally, the JAX-RS REST API implementation can be easily ported to 
AWSElastic BeanStalk or AWS Lambda for future REST API hosting of the
solution. Additionally, AWS S3 could be substituted for asset storage.
New Document
