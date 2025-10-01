Importing the API package gives you access to important classes and interfaces you'll need to start developing a Reactive Music plugin, or to hook into some of Reactive Music's core features as a mod developer.

```java
import circuitlord.reactivemusic.api;
```

# [[ReactiveMusicAPI]]
This class is the main entrypoint for Reactive Music developers. You'll find many useful methods and object references that you can implement to unlock the full immersive audio potential of your mod or plugin!

# [[ReactiveMusicPlugin]]
This is the service provider interface surface for the final class of a plugin, which should be configured to be imported by the service loader. It includes various `default` method calls meant to be overriden that are called at various points throughout Reactive Music's main flow.

## ⚠️ _Setting up your plugin for import!_
---
_For Reactive Music to recognize your plugin, you must declare it in the `resources` folder of your project. Create the directory `resources/META-INF` and create the file `circuitlord.reactivemusic.api.ReactiveMusicPlugin`<br>
<br>
Declare the package of your final plugin class extending `ReactiveMusicPlugin` like so:_
```
yourname.yourmod.someplace.YourPluginClass
```



