
# Renthyl

[![](https://jitpack.io/v/codex128/Renthyl.svg)](https://jitpack.io/#codex128/Renthyl)

![VXGI-demo](resources/VXGI-demo.png)

Renthyl is a modular, code-first, and completely customizable rendering pipeline for JMonkeyEngine suitable for any game. It is designed to be as fast as possible by culling unnecessary render operations and minimizing resource creation.

Renthyl is currently in alpha status: there may be bugs. If one is encountered, please open an issue, and include stacktraces, example code, and screenshots if applicable. Also note that many features within the RenthylPlus subproject are work-in-progress.

Contributors are welcome and wanted! If you have the know-how, consider implementing a rendering technique in the RenthylPlus subproject.

## Get Started

1. Add Renthyl to your Gradle build script. Renthyl depends on Java 8 and JMonkeyEngine 3.8 minimum, along with some other libraries.

```groovy
repositories {
    maven {
        url "https://jitpack.io"
    }
}
dependencies {
    implementation "com.github.codex128:RenthylCore:v1.2.5"
    implementation "com.github.codex128:RenthylCore:v1.2.5:sources"
    implementation "com.github.codex128:RenthylCore:v1.2.5:javadoc"
}
```

2. Initialize Renthyl in your JMonkeyEngine application.
   
```java
Renthyl.initialize(application);
```

3. Create a FrameGraph and add it to the main ViewPort.

```java
FrameGraph fg = Renthyl.forward(assetManager);
viewPort.setPipeline(fg);
```

### Wiki (under construction)

* [Understanding Renthyl](Wiki/UnderstandingRenthyl.md)
