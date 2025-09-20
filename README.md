# Command-line PDF merger.
## Constraints supported:
- Accept multiple PDF files as inputs.
- Accept directories; when a directory is specified, recursively collect all PDFs under it.
- Allow specifying output filename with -o option.
- If a single directory is specified without -o, use the directory name as the output file name
  (created in the current working directory).
## Usage examples:
```shell
- java -jar target/MergePDF-1.0-SNAPSHOT-shaded.jar -o merged.pdf a.pdf b.pdf
- java -jar target/MergePDF-1.0-SNAPSHOT-shaded.jar /path/to/dir
```
