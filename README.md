# Linker Lab
By Jason Yao, CSCI-UA 202 Operating Systems, Instructor: Allen Gottlieb, Lab 1,
[Github](https://www.github.com/JasonYao/Operating-Systems-Linker-Lab)

## Description

The purposes of this lab is to implement a one- or two-pass linker, in C, C++, or Java.
In order to familiarise myself with C more, C was chosen as the language to build this linker.

A linker is the second step after the source code is compiled into an object module.
This linker will take in that object module, and output an executable for the system,
by linking together the references to external libraries, along with rearranging each
individual module address to the correct absolute value.

This linker will follows the guidelines outlined in the [project description](lab1.pdf),
and is thus considered a linker specific for this class's usage only.

## Compilation & Running

### Making sure gcc is on your system

In order to compile this program, you'll need to make sure that `gcc` is installed.
type `gcc -v`, and if the output dislays some configuration options, you're good to go.

If gcc is NOT installed, you can do so in unix environments with your package manager of choice.
Normally the following commands will get you gcc:

```
apt-get update
apt-get install build-essentials
```

### Compiling
`gcc linker.c -o linker.out`

### Running
`./linker.out testInput_1 testInput_2 ... testInput_n` # This is assuming that you have the object modules as files.

## Licensing
This repo is under the auspice of the license file, located [here](LICENSE.md)
