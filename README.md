# vStore Framework
## Authors

**Project leader:** 
[[Julien Gedeon]](https://www.informatik.tu-darmstadt.de/telekooperation/telecooperation_group/staff_1/staff_1_details_23872.en.jsp)

**Developers:**
Nicolas Himmelmann


## Contents

[What is it?](#what-is-it)

[Workflow](#workflow)

[Library dependencies](#library-dependencies)

[Paper](#paper)

## What is it?

vStore is an approach to enable context-aware storage decisions for applications. It provides a storage abstraction layer with the key focus lying on the network edge. 
This means that the underlying algorithm stores files on a different storage node based on the given context.

## Workflow

### Initialization

First, we need to provide a base level directory to the framework, where it has full read and write access.
```java
VStore.initialize(<path>);
```
This command initializes the framework instance and the framework can now be used.

### Creating a configuration

Next, there is the possibility to download a configuration file from a defined location.
This configuration can contain the following information:
- A pre-defined set of storage nodes and information about them (location, network speed, type)
- A set of matching rules, with context and detail scores

The following comand tells the framework to download this file:
```java
ConfigManager confMgr = VStore.getInstance().getConfigManager();
confMgr.download(<URL_TO_File>, <true> or <false>);
```
The second parameter determines, if the download of the configuration file should block (true)
or if it should continue in the background (false).


### Managing context and rules

To actually use the framework for file storage, we need to provide some context information.
Furthermore, more rules can be added in addition to the rules that are already contained in the
configuration file we have downloaded in the last step.

To provide the framework with new context information or new rules, the following methods can be used:

```java
VStore vstore = VStore.getInstance();
vstore.provideContext(<ContextDescription>);
vstore.provideRules(<RuleDescription>);
```

For the structure of a ContextDescription instance, you can look at the documentation of
```vstore.framework.context.ContextDescription``` .

#### Persisting context information

The provided context information does not persist by default, when the current framework instance is destroyed.
This means, the next time the framework is initialized, it will start without any context.
If you want the information to be persistent, you need to call the following method and set the parameter
to ```true```. If you set the parameter to ```false```, the persistent context will be deleted.
```java
vstore.persistContext(<true> or <false>);
```

The information will be kept until you update it with the ```provideContext``` method or until you clear it using
```java
vstore.clearCurrentContext();
```

You can also chain these commands by doing the following:
```java
vstore.clearContext()
      .provideContext(<ContextDescription>)
      .persistContext();
```
This makes things a bit easier.

Additionally, there is the possibility to do the same thing using the ContextManager:
```java
ContextManager ctxMgr = ContextManager.get();
ctxMgr.provideContext(<ContextDescription>);
ctxMgr.persistContext(<true> or <false>);
ctxMgr.clearCurrentContext();
ctxMgr.getCurrentContext();
```


#### Handling matching-rules

For manually adding and deleting matching rules, you can use the Rule Manager.
```java
RuleManager ruleMgr = RuleManager.get();
ruleMgr.getRules();
ruleMgr.storeNewRule(<VStoreRule>);
ruleMgr.deleteRule(<RuleId>);
ruleMgr.updateRule(<VStoreRule>);
ruleMgr.clearRules();
```


### Storage of a file

To store a file, the framework needs

- a path to the file
- a flag which determines if the file should be stored for public access or only for private access
- an identifier of the source device.

Then you need to call ```vstore.store();``` with these parameters.

Internally, the framework will perform the following actions:

- Derive the MIME type of the data
- Copy the file into the local framework folder
- Compute a hash for the file (currently MD5)
- Check if the same file was uploaded previously
- Perform the matching algorithm to find a target node for the file
- Upload the file in the background.

If any of these steps fails, the method will throw a ```StoreException```.  Possible error codes
can be found in the enum ```vstore.framework.error.ErrorCode```.

#### Keeping track of the upload state
Since the framework uses the GreenRobot EventBus (Pub/Sub paradigm) for providing
information to the application, you can subscribe to the following events to keep track of the
upload state:

```java
UploadBeginEvent - Published once the upload to a storage node starts
UploadStateEvent - Published regularly during the upload, containing the progress in percent.
SingleUploadDoneEvent - Published once the upload to the storage node has finished.
UploadFailedEvent - Published when an upload attempt failed.
UploadFailedPermanentlyEvent - Published, when the upload failed permanently (after 3 attempts).
```



## Library dependencies

- https://code.google.com/archive/p/json-simple/
- OkHTTP3
- GreenRobot EventBus


For further information, please see the documentation created by JavaDoc, or refer to the sample application for Android (vstore-android-filebox).

## Paper
Our research on *vStore* was published and awarded best paper at the 2018 MobiCASE conference. If you use our framework, please consider citing the following paper: 

J. Gedeon, N. Himmelmann, P. Felka, F. Herrlich, M. Stein, M. Mühlhäuser. "vStore: A Context-Aware Framework for Mobile Micro-Storage at the Edge" [[PDF]](https://fileserver.tk.informatik.tu-darmstadt.de/JG/vstore/gedeon_vstore.pdf)  [[BibTeX]](https://fileserver.tk.informatik.tu-darmstadt.de/JG/vstore/vstore.bib)
