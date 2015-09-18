
# Builds the program
javac -d Linker/bin/ Linker/src/linker/*.java
java /Linker/bin/linker/OSLinker.class < testing/input-1
