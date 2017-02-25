AutoValueHelper
============

IntelliJ IDEA plugin that adds a 'Builder' and `Create` action to the Generate menu (Alt+Insert)
which generates a Builder or Create method for AutoValue class. Works with IntelliJ IDEA 12.x, 13.x and 14.x.

#### Manual installation

Download the plugin jar `AutoValueHelper.jar` and select "Install Plugin From Disk" in IntelliJ's plugin preferences.

### Usage

Use `Shift+Alt+B` for Builder `Shift+Alt+c` for Create method or `Alt+Insert` for options.
When generating a builder when a builder already exists, the plugin will try to update it.
It will add missing methods and parameters and remove unused ones.

### TODO

Release to plugins repo

### Thanks
Based on [InnerBuilder plugin](https://github.com/analytically/innerbuilder).
Thanks to [Mathias Bogaert](https://github.com/analytically) for his work!

### License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Copyright 2013-2015 [Rustam Sinukov](mailto:rxsinukov@gmail.com).
