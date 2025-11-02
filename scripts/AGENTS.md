# Creating and Maintaining Automation Scripts

This guide outlines the process for creating, maintaining, and contributing automation scripts in the JabRef project, including CI/CD integration, error handling, and testing.

## Script Categories

JabRef uses various types of automation scripts:

### Build and Development Scripts

- **Build automation**: Gradle task wrappers, build verification
- **Development setup**: Environment configuration, tool installation
- **Code generation**: Template processing, metadata generation

### CI/CD Scripts

- **Test execution**: Parallel test running, result aggregation
- **Deployment**: Release automation, artifact publishing
- **Quality checks**: Linting, formatting, security scanning

### Maintenance Scripts

- **Database operations**: Schema updates, data migration
- **Documentation**: API doc generation, changelog updates
- **Repository management**: Branch cleanup, issue management

## Script Implementation Process

### 1. Script Planning

- Define the automation need and scope
- Identify the target environment (local vs CI/CD)
- Determine input parameters and expected outputs
- Plan error handling and logging strategy

### 2. Language Selection

Choose appropriate scripting language based on requirements:

```bash
# Shell scripts for simple automation
#!/bin/bash
set -euo pipefail

# Input validation
if [ $# -ne 1 ]; then
    echo "Usage: $0 <parameter>"
    exit 1
fi

# Main logic
echo "Processing: $1"
```

```python
#!/usr/bin/env python3
# Python for complex data processing
import sys
import argparse
from pathlib import Path

def main():
    parser = argparse.ArgumentParser(description='Process bibliography data')
    parser.add_argument('input', help='Input file path')
    parser.add_argument('--output', '-o', help='Output file path')

    args = parser.parse_args()

    # Script logic here
    process_file(Path(args.input), Path(args.output) if args.output else None)

if __name__ == '__main__':
    main()
```

```ruby
#!/usr/bin/env ruby
# Ruby for text processing and file manipulation
require 'optparse'
require 'pathname'

options = {}
OptionParser.new do |opts|
  opts.banner = "Usage: #{$0} [options]"

  opts.on("-i", "--input FILE", "Input file") do |file|
    options[:input] = Pathname.new(file)
  end

  opts.on("-o", "--output FILE", "Output file") do |file|
    options[:output] = Pathname.new(file)
  end
end.parse!

# Script logic
process_files(options)
```

### 3. Script Structure

Follow consistent structure across all scripts:

```bash
#!/bin/bash
# =============================================================================
# Script Name: descriptive_script_name.sh
# Description: Brief description of what the script does
# Author: Your Name
# Created: YYYY-MM-DD
# =============================================================================

set -euo pipefail  # Exit on error, undefined vars, pipe failures

# Constants
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_NAME="$(basename "$0")"

# Configuration
LOG_LEVEL="${LOG_LEVEL:-INFO}"
DRY_RUN="${DRY_RUN:-false}"

# Functions
log() {
    local level="$1"
    shift
    echo "[$level] $*" >&2
}

error() {
    log "ERROR" "$*"
    exit 1
}

warning() {
    log "WARNING" "$*"
}

info() {
    log "INFO" "$*"
}

debug() {
    [ "$LOG_LEVEL" = "DEBUG" ] && log "DEBUG" "$*"
}

usage() {
    cat << EOF
Usage: $SCRIPT_NAME [OPTIONS] [ARGS...]

Description of script purpose and usage.

OPTIONS:
    -h, --help          Show this help message
    -v, --verbose       Enable verbose output
    -d, --dry-run       Show what would be done without executing
    --log-level LEVEL   Set log level (DEBUG, INFO, WARNING, ERROR)

EXAMPLES:
    $SCRIPT_NAME --dry-run input.txt
    $SCRIPT_NAME --verbose --log-level DEBUG input.txt output.txt

EOF
}

# Argument parsing
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                usage
                exit 0
                ;;
            -v|--verbose)
                LOG_LEVEL="DEBUG"
                shift
                ;;
            -d|--dry-run)
                DRY_RUN=true
                shift
                ;;
            --log-level)
                LOG_LEVEL="$2"
                shift 2
                ;;
            -*)
                error "Unknown option: $1"
                ;;
            *)
                # Positional arguments
                break
                ;;
        esac
    done

    # Validate required arguments
    if [ $# -lt 1 ]; then
        error "Missing required argument. Use -h for help."
    fi
}

# Main logic functions
validate_environment() {
    # Check required tools
    command -v git >/dev/null 2>&1 || error "git is required"
    command -v java >/dev/null 2>&1 || error "java is required"

    # Check environment variables
    [ -n "${JAVA_HOME:-}" ] || warning "JAVA_HOME not set"
}

perform_operation() {
    local input_file="$1"
    local output_file="${2:-}"

    if [ "$DRY_RUN" = true ]; then
        info "Would process: $input_file -> ${output_file:-stdout}"
        return
    fi

    info "Processing: $input_file"

    # Main script logic here
    if [ -n "$output_file" ]; then
        # Process to file
        process_file "$input_file" > "$output_file"
        info "Output written to: $output_file"
    else
        # Process to stdout
        process_file "$input_file"
    fi
}

process_file() {
    local file="$1"
    # File processing logic
    cat "$file"
}

# Main execution
main() {
    parse_args "$@"
    validate_environment

    info "Starting $SCRIPT_NAME"

    # Execute main logic
    perform_operation "$@"

    info "Completed successfully"
}

# Run main function with all arguments
main "$@"
```

## Error Handling and Logging

### Comprehensive Error Handling

```bash
# Trap errors and cleanup
trap 'error "Script failed at line $LINENO"' ERR
trap 'cleanup' EXIT

cleanup() {
    # Clean up temporary files, connections, etc.
    [ -n "${temp_file:-}" ] && rm -f "$temp_file"
    debug "Cleanup completed"
}
```

### Structured Logging

```bash
# Log levels with consistent formatting
log_error() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $*" >&2; }
log_warning() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $*" >&2; }
log_info() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] INFO: $*" >&2; }
log_debug() { [ "${DEBUG:-false}" = true ] && echo "[$(date +'%Y-%m-%d %H:%M:%S')] DEBUG: $*" >&2; }
```

### Exit Codes

```bash
# Define standard exit codes
readonly EXIT_SUCCESS=0
readonly EXIT_FAILURE=1
readonly EXIT_INVALID_ARGS=2
readonly EXIT_MISSING_DEPS=3
readonly EXIT_PERMISSION_DENIED=4

# Use them consistently
error_invalid_args() {
    log_error "Invalid arguments: $*"
    echo "Use -h for help" >&2
    exit $EXIT_INVALID_ARGS
}
```

## Testing Scripts

### Unit Testing Scripts

```bash
#!/bin/bash
# Test framework for bash scripts

test_example_function() {
    local result
    result=$(example_function "test input")

    assert_equals "expected output" "$result" "example_function should return expected output"
}

assert_equals() {
    local expected="$1"
    local actual="$2"
    local message="${3:-Assertion failed}"

    if [ "$expected" != "$actual" ]; then
        echo "FAIL: $message"
        echo "Expected: '$expected'"
        echo "Actual: '$actual'"
        return 1
    else
        echo "PASS: $message"
        return 0
    fi
}

run_tests() {
    local test_functions=$(declare -F | grep -E '^declare -f test_' | sed 's/declare -f //')

    local passed=0
    local failed=0

    for test_func in $test_functions; do
        echo "Running $test_func..."
        if $test_func; then
            ((passed++))
        else
            ((failed++))
        fi
        echo
    done

    echo "Tests completed: $passed passed, $failed failed"
    [ $failed -eq 0 ]
}
```

### Integration Testing

```bash
# Test script in CI environment
test_script_integration() {
    # Create test data
    echo "test data" > test_input.txt

    # Run script
    if bash script.sh test_input.txt test_output.txt; then
        # Verify output
        if grep -q "expected content" test_output.txt; then
            echo "Integration test passed"
            return 0
        fi
    fi

    echo "Integration test failed"
    return 1
}
```

## CI/CD Integration

### GitHub Actions Integration

```yaml
# .github/workflows/script-test.yml
name: Script Tests
on: [push, pull_request]

jobs:
  test-scripts:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Test Bash Scripts
        run: |
          find scripts -name "*.sh" -exec bash -n {} \;

      - name: Test Python Scripts
        run: |
          find scripts -name "*.py" -exec python -m py_compile {} \;

      - name: Run Script Tests
        run: |
          cd scripts
          bash run_tests.sh
```

### Pre-commit Hooks

```bash
#!/bin/bash
# pre-commit hook to validate scripts

# Check shell scripts
find scripts -name "*.sh" -exec bash -n {} \; || exit 1

# Check Python scripts
find scripts -name "*.py" -exec python -m py_compile {} \; || exit 1

echo "All scripts validated successfully"
```

## Performance Considerations

### Efficient Operations

```bash
# Use efficient text processing
process_large_file() {
    local file="$1"

    # Avoid loading entire file into memory
    while IFS= read -r line; do
        process_line "$line"
    done < "$file"
}

# Use parallel processing when appropriate
parallel_process() {
    local files=("$@")

    printf '%s\n' "${files[@]}" | parallel --no-notice process_file
}
```

### Resource Management

```bash
# Clean up resources
cleanup_resources() {
    # Close file descriptors
    exec 3>&-
    exec 4<&-

    # Remove temporary files
    rm -f "${temp_files[@]}"

    # Kill background processes
    kill "${pids[@]}" 2>/dev/null || true
}
```

## Security Considerations

### Input Validation

```bash
validate_input_file() {
    local file="$1"

    # Check file exists and is readable
    [ -f "$file" ] || error "File does not exist: $file"
    [ -r "$file" ] || error "File is not readable: $file"

    # Check file size limits
    local size
    size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file")
    [ "$size" -gt 100000000 ] && error "File too large: $file"

    # Check for malicious content
    if grep -q '[[:cntrl:]]' "$file"; then
        warning "File contains control characters: $file"
    fi
}
```

### Safe Command Execution

```bash
# Use arrays for safe command construction
execute_safely() {
    local cmd=("$@")

    if [ "${DRY_RUN:-false}" = true ]; then
        echo "Would execute: ${cmd[*]}"
        return
    fi

    debug "Executing: ${cmd[*]}"
    "${cmd[@]}"
}
```

## Documentation Standards

### Script Documentation

```bash
# Every script must include header documentation
# =============================================================================
# Script Name: script_name.sh
# Description: What the script does in one line
# Author: Author Name
# Created: YYYY-MM-DD
# Modified: YYYY-MM-DD - Brief change description
# Dependencies: List of required tools/commands
# Usage: script_name.sh [options] [arguments]
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Invalid arguments
#   3 - Missing dependencies
# =============================================================================
```

### README Files

Each script directory should have a README.md:

```markdown
# Scripts Directory

This directory contains automation scripts for the JabRef project.

## Scripts Overview

| Script | Purpose | Language | CI/CD |
|--------|---------|----------|-------|
| `build.sh` | Build automation | Bash | Yes |
| `test.py` | Test execution | Python | Yes |
| `deploy.rb` | Deployment | Ruby | No |

## Usage

See individual script headers for detailed usage information.

## Development

- Follow the script template in `TEMPLATE.sh`
- Add tests in `test/` directory
- Update this README when adding new scripts
```

## Maintenance Guidelines

### Regular Reviews

- Review scripts annually for security and efficiency
- Update dependencies and language versions
- Remove obsolete scripts with deprecation warnings

### Version Management

```bash
# Include version information
readonly SCRIPT_VERSION="1.2.0"
readonly SCRIPT_DATE="2024-01-15"

show_version() {
    echo "$SCRIPT_NAME version $SCRIPT_VERSION ($SCRIPT_DATE)"
}
```

### Deprecation Process

```bash
# Mark deprecated scripts
deprecated_script() {
    warning "This script is deprecated. Use 'new_script.sh' instead."
    warning "This script will be removed in version X.Y.Z"

    # Still execute but show warnings
    main "$@"
}
```
