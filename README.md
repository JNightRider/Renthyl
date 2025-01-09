Renthyl is a modular FrameGraph-based rendering pipeline designed with efficiency in mind.

[![](https://jitpack.io/v/codex128/Renthyl.svg)](https://jitpack.io/#codex128/Renthyl)

## Features

* Allows for the use of modern rendering paradigms, such as deferred and forward++.
* Code-first design makes constructing and manipulating FrameGraphs incredibly easy.
* Easily control the layout of the FrameGraph using game logic.
* Designed to be totally savable, so FrameGraphs can be saved and loaded from files.
* Culling algorithm ensures unnecessary operations are skipped.
* Resource manager minimizes the creation and binding of resources by reallocating resources where possible.
* Allows for stress-free multithreading.
* Tree-like Structure allows individual groups of passes to be exported and shared.

## Get Started

1. Add Renthyl to your Gradle build script.

```groovy
dependencies {
   implementation "com.github.codex128:Renthyl:1.2.3"
}
```
Make sure you add Jitpack as a repository:
```groovy
repositories {
   maven {
      url "https://jitpack.io"
   }
}
```

2. Initialize Renthyl in your JMonkeyEngine application.
   
```java
Renthyl.initialize(application);
FrameGraph fg = Renthyl.forward(assetManager);
viewPort.setPipeline(fg);
```

3. Run the project.

## Learn Renthyl

A full guide on how to use Renthyl can be found at this repository's [wiki](https://github.com/codex128/FrameGraph/wiki).
