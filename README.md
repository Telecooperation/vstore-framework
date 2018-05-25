# vStore Framework
## Authors

Nicolas Himmelmann, Telecooperation Lab, TU Darmstadt

Julien Gedeon, Telecooperation Lab, TU Darmstadt

## Contents

[What is it?](#what-is-it)

[Workflow](#workflow)

[Extending the framework](#extending-the-framework)

[Library dependencies](#library-dependencies)

[Paper](#paper)

## What is it?

vStore is an approach to enable context-aware storage decisions for applications. It provides a storage abstraction layer with the key focus lying on the network edge. 
This means that the underlying algorithm stores files on a different storage node based on the given context.

## Workflow

### Create a configuration

First, we need to define a configuration for the framework. This includes
- an absolute path to a directory for framework-related data (with full read/write access!)
- the desired types of context we want to use, together with their score weight
- a set of matching rules

```java
VStoreConfig initialConfig = VStoreConfig.getInstance();
initialConfig.setDirectory(<path>);
```

Then we create an instance of the framework for further actions and pass it the initial configuration. 
To provide the framework with new context information or new rules, the following methods can be used.

```java
VStore vstore = VStore.getInstance(initialConfig);
vstore.newContext(<parameters>);
vstore.newRules(<parameters>);
``` 

The provided information, such as context or rules, do not persist once the current instance is destroyed. This means, the next time the framework is initialized, it will start without context and without matching-rules.
If you want the information to be persistent, you need to call
```java
vstore.persistContext();
vstore.persistRules();
```

In this case, the information will be kept until you update it with the above methods or clear it using
```java
vstore.clearContext();
vstore.clearRules();
```

### Storage of a file

To store a file, the framework needs a path to the file, an identifier of the source device and optionally some usage context from this device.
If no extra context is given, the context provided previously by ```vstore.newContext(<parameters>);``` is used.

Then you need to call ```vstore.storeFile();``` with the following parameters:
TODO
 
## Extending the framework

### Adding more context
Adding new context types to the framework is very easy. The following steps show you how it is done:
- First: Clearly define the properties of your new context type, since the framework requires you to provide a method which outputs the context as JSON.
- Create a new class with the name of your context type which extends the parent class `VContextType`. It must implement the required methods

   - `public JSONObject getJson();`

   - `public boolean matches(VContextType other);`

 

## Queue

- A queue (FIFO) is used for temporary storage of upload jobs
- Upload thread works on the queue until it is empty


## Library dependencies

- https://code.google.com/archive/p/json-simple/
- OkHTTP3
- GreenRobot EventBus


For further information, please see the documentation created by JavaDoc, or refer to the examples.

## Paper

J. Gedeon, N. Himmelmann, P. Felka, F. Herrlich, M. Stein, M. Mühlhäuser. "vStore: A Context-Aware Framework for Mobile Micro-Storage at the Edge" [[Download here]](https://www.tk.informatik.tu-darmstadt.de/fileadmin/user_upload/Group_TK/filesDownload/Published_Papers/JG/gedeon_vstore.pdf)


