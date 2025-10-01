In this section I will do my best to provide instruction, as well as recommendations, in how to start developing for Reactive Music from scratch.

# Getting Started
To start developing for Reactive Music, you will need a few things.

1. Your preferred text editor or IDE.
2. The Reactive Music `.jar` files.
3. A valid Fabric Minecraft project setup.
4. An idea for your plugin.
5. (Optionally) The `.jar` files for any other mods of which you will be accessing their API's.

## Setting up your IDE
Let's start by getting your IDE set up!

IDE stands for Integrated Development Environment.

It’s a software application that provides developers with a comprehensive set of tools to write, test, and debug code all in one place. Instead of using separate tools for editing, compiling, debugging, and version control, an IDE integrates them into a single interface.

There are many different IDE's out there - for today, I will recommend using VSCode. It's well adopted, feature full, and relatively easy to learn - in part because it's so widely used.

Go ahead and install an IDE then move on to the next step.

## Obtaining a Fabric mod template
Next we'll need to load up a project template onto your system. This is where the code for your plugin will live. You can select and download an official Fabric project template [here](https://fabricmc.net/develop/template/). Make sure to select the correct version of minecraft.

At this point, it is recommended to also get familiar with and setup a version control system such as [git](https://git-scm.com).

## Importing the builds into your environment
With a project template loaded, and your IDE fired up, you're almost ready to start developing.

Download the `.jar` files for the version of Reactive Music you want to develop for and copy them into a new folder in your project's root directory.

From here, you'll need to update your `build.gradle` file to include the `.jar` files. Find a block that looks like this:
```gradle
dependencies {
	// To change the versions see the gradle.properties file
	minecraft "com.mojang:minecraft:${project.minecraft_version}"
	mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
	modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}
```
Add the paths to the `.jar` files you copied - for example, I like to keep my `.jar` files in a folder labeled `libs/`:
```gradle
// ReactiveMusic API jar in libs/
modCompileOnly files("libs/reactivemusic-2.0.0-alpha.0.1+1.21.jar")

// Immersive Aircraft jar in libs/
modCompileOnly files("libs/immersive_aircraft-1.3.3+1.21.1-fabric.jar")
```

To keep it simple, if you're using VSCode - let's just close VSCode from here and reopen your project. This will get everything fired back up again so that VSCode will give you autocomplete suggestions - and will also let you compile! This is very important!

## Developing your plugin
If you've made it to this point - congratulations! You're almost officially a Minecraft modder 😎 Take a look at some other plugins for examples on how to structure your codebase, and different features you can add or expand on. This is where it gets really complicated - so at this point you're past being a beginner if you can make it to the next step - Good luck, and have fun 💃

Make sure to read [[this overview|Overview of the API]] to get an idea of how Reactive Music's systems work and what you need to do to have Reactive Music import your plugin.

## Building your plugin
Assuming you've done everything right, and your plugin's code is valid - go into the terminal portion of your IDE and use the command `gradlew build`
You'll be able to find your fresh `.jar` in `build/libs/` if it compiles. Install that into your Minecraft `mods` folder and test out your plugin!
