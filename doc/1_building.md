# Documentation

## Building

In order to build the Performance Test Harness you will need to run [Apache Maven](http://maven.apache.org/run-maven/index.html). The Performance Test Harness is setup intp three sub projects. The ```performance-test-harness``` project builds runnable jar which are used on the command line to execute performance tests. The ```performance-test-harness-ui``` project is a [JavaFX](http://docs.oracle.com/javase/8/javafx/get-started-tutorial/jfx-overview.htm) project which also builds a runnable jar to launch the application.

### Instructions
1. Navigate to the root of the project.
2. Open a command prompt 
3. Execute the following command ```mvn clean install```
4. Done.

After the build completes successfully, binary jar files will be build in the following directories:

  - `performance-test-harness/target/main/app/Performance-Test-Harness-{binary.version}.jar`
  - `performance-test-harness-ui/target/jfx/app/Performance-Test-Harness-UI-{binary.version}.jar`

Each build directory will have the following structure:

     - main/app/
		+ lib/ 			(all library dependencies are stored and referenced here)
		+ binary.jar 	(binary jar file)
		

After the binaries have been builyt successfully, you can now move the `lib` and `binary.jar` file to any location as long as the directory structure stays the same. You may also want to create the following directory structure to contain the other necessary files together:

     - root/
		+ config/ 		(contains GeoEvent configuration files)
		+ fixtures/		(contains all of your test fixture configuration files)
		+ lib/ 			(all library dependencies are stored and referenced here)
		+ reports/		(contains all of the generated reports)
		+ simulation/	(contains all of your simulation files)
		+ binary.jar 	(binary jar file)

[Next - Configuration](2_configuration.md)
