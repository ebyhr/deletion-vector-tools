# deletion-vector-tools

# Build
This project is a standard Maven-based application. To build the project, run the 
following command from the root directory:
```sh
./mvnw clean install -DskipTests
```

# Run
Once built, you can execute the tool using the provided executable JAR. The following options are required:
- `--path`: Path to the Deletion Vector file.	
- `--offset`: Offset value in the file.	
- `--size`: Byte size of the section to process.

```sh
java -jar target/deletion-vector-tools-*-executable.jar --path path-to-deletion-vector --offset 123 --size 456
```

When executed, the tool will parse the specified Deletion Vector file and output 
its information in the following format:
```sh
0: {564851,1081346}
```
