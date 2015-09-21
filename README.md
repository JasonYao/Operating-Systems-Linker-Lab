# Linker Lab
By Jason Yao, CSCI-UA 202 Operating Systems, Instructor: Allen Gottlieb, Lab 1,
[Github](https://www.github.com/JasonYao/Operating-Systems-Linker-Lab)

## Description

The purposes of this lab is to implement a one- or two-pass linker, in C, C++, or Java.

C was originally chosen, but more time was spent developing the underlying structures than to
developing the linker code - things such as hashtables or linked lists all while dealing with
pointer allocation.

Thus, the decision to switch back to Java (A breath of fresh air after toiling in the bowels of
computer science known as C programming)

A linker is the second step after the source code is compiled into an object module.
This linker will take in that object module, and resolve all references to libraries and module loading,
by linking together the references to external libraries, along with rearranging each
individual module address to the correct absolute value.

This linker will follows the guidelines outlined in the [project description](lab1.pdf),
and is thus considered a linker specific for this class's usage only.

**THIS IS NOT MEANT TO BE NOR WILL IT EVER BE A LINKER TO BE USED ON ACTUAL OBJECT MODULES **

This is simply a proof of concept of how a linker is built, what it does, and why it is important in operating system usages.

## Compilation & Running

### Making sure java is on your system

In order to compile this program, you'll need to make sure that `java` is installed.
type `java -version`, and if the output dislays a java runtime version, you're good to go.

If `java` is NOT installed, you can do so in unix environments with your package manager of choice.
Normally the following commands will get you `java`:

```
apt-get update
apt-get install build-essentials
apt-get install default-jdk
```

### Compiling
`mkdir bin` # Creates the output folder to hold compiled programs
`javac -d bin/ src/linker/*.java` # Builds the program

### Running
`java -cp bin linker.OSLinker < INSERT_TEST_FILE_PATH_HERE` # Runs the program against a testfile sent in via standard input

#### Example:

`java -cp bin linker.OSLinker < testing/input-1` # Runs the program against a testfile sent in via standard input

## Licensing
This repo is under the auspice of the license file, located [here](LICENSE.md)
