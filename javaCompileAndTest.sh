
# Builds the program
mkdir bin
javac -d bin/ src/linker/*.java

# Runs the program against a testfile
java -cp bin linker.RobustOSLinker < testing/input-9
