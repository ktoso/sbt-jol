# sbt-jol

[ ![Download](https://api.bintray.com/packages/ktosopl/sbt-plugins/sbt-jol/images/download.svg) ](https://bintray.com/ktosopl/sbt-plugins/sbt-jol/_latestVersion)

Trivial way to inspect OpenJDK's [*Java Object Layout*](http://openjdk.java.net/projects/code-tools/jol/) of your Scala classes.

Get the latest via:

 ```
 // project/plugins.sbt
 addSbtPlugin("pl.project13.sbt" % "sbt-jol" % pluginVersionHere)
 ```
 
 Which allows you to (note auto-completion works nicely):
 
 ```
 > jol:internals example.Entry
 
[info] # Running 64-bit HotSpot VM.
[info] # Using compressed oop with 3-bit shift.
[info] # Using compressed klass with 3-bit shift.
[info] # WARNING | Compressed references base/shifts are guessed by the experiment!
[info] # WARNING | Therefore, computed addresses are just guesses, and ARE NOT RELIABLE.
[info] # WARNING | Make sure to attach Serviceability Agent to get the reliable addresses.
[info] # Objects are 8 bytes aligned.
[info] # Field sizes by type: 4, 1, 1, 2, 2, 4, 4, 8, 8 [bytes]
[info] # Array element sizes: 4, 1, 1, 2, 2, 4, 4, 8, 8 [bytes]
[info]
[info] VM fails to invoke the default constructor, falling back to class-only introspection.
[info]
[info] example.Entry object internals:
[info]  OFFSET  SIZE   TYPE DESCRIPTION                    VALUE
[info]       0    12        (object header)                N/A
[info]      12     4    int Entry.value                    N/A
[info]      16     4 String Entry.key                      N/A
[info]      20     4        (loss due to the next object alignment)
[info] Instance size: 24 bytes
[info] Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
 ```
 
  Currently uses jol `0.5`.

Contribute!
-----------
Please note that this plugin is mostly developed on a "on demand" basis by and for myself, contributions are very (!) welcome 
since I most likely will not focus much on it unless I need more features (and for now estimates and internal are all I needed).
 
 License
 -------
 
 Apache v2
