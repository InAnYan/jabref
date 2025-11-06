
import os
import re

def adjust_header_levels(content):
    lines = content.split("\n")
    new_lines = []
    for line in lines:
        if line.startswith("#"):
            new_lines.append("#" + line)
        else:
            new_lines.append(line)
    return "\n".join(new_lines)

def main():
    docs_path = "docs/code-howtos"
    output_file = "AGENTS.md"
    
    with open(output_file, "w") as outfile:
        for filename in sorted(os.listdir(docs_path)):
            if filename.endswith(".md") and filename != "index.md":
                filepath = os.path.join(docs_path, filename)
                with open(filepath, "r") as infile:
                    content = infile.read()
                    
                    # Skip frontmatter
                    if content.startswith("---"):
                        parts = content.split("---")
                        if len(parts) > 2:
                            content = "---".join(parts[2:])
                    
                    # Add section header
                    title = os.path.splitext(filename)[0].replace("-", " ").title()
                    outfile.write(f"## {title}\n\n")
                    
                    # Adjust header levels and write content
                    adjusted_content = adjust_header_levels(content)
                    outfile.write(adjusted_content)
                    outfile.write("\n\n")

if __name__ == "__main__":
    main()
