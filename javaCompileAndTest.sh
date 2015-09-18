
# Builds the program
javac -d bin/ src/linker/*.java

# Runs the program against a testfile
echo 'Outputting file to Linker.output'
java -cp bin linker.OSLinker < testing/input-1
