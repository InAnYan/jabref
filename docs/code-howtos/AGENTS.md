# Code How-To Guides

This guide outlines the process for creating, maintaining, and contributing code how-to guides in the JabRef documentation.

## Purpose of Code How-Tos

Code how-tos provide practical, actionable guidance for developers working on specific aspects of JabRef. They focus on "how to implement" rather than "how to use" features.

## When to Create a How-To

Create a new how-to when:

- Implementing a feature requires specific technical knowledge
- There's a common pattern or convention that should be documented
- New contributors need guidance for a specific area
- Complex setup or configuration is required
- Integration with external services is non-trivial

**Don't create how-tos for:**
- Basic programming concepts
- Standard library usage
- Generic development practices
- User-facing features (document in user docs instead)

## How-To Creation Process

### 1. Planning

- Identify the specific problem or task
- Check if existing how-tos cover the topic
- Determine the target audience (new contributors vs experienced developers)
- Outline key steps and prerequisites

### 2. Writing

- Use clear, actionable language
- Include code examples with proper syntax highlighting
- Provide step-by-step instructions
- Document prerequisites and assumptions
- Include troubleshooting sections

### 3. Review

- Technical accuracy review by subject matter experts
- Editorial review for clarity and structure
- Testing of instructions by another developer
- Ensure cross-references to related documentation

### 4. Publishing

- Add to appropriate category in `docs/code-howtos/`
- Update `index.md` with new entry
- Announce in relevant channels if widely applicable

## Content Structure

### Required Sections

#### Title and Introduction

```markdown
# [Specific Task] in JabRef

Brief description of what this guide covers and why it's needed.
```

#### Prerequisites

- Required tools, libraries, or knowledge
- System requirements
- Access permissions needed

#### Step-by-Step Instructions

- Numbered steps for complex procedures
- Code blocks with syntax highlighting
- Expected outputs or results
- Alternative approaches where applicable

#### Examples

- Complete, working code examples
- Before/after comparisons
- Common usage patterns

#### Troubleshooting

- Common errors and solutions
- Debugging tips
- Alternative approaches for edge cases

### Optional Sections

#### Related Documentation

- Links to related how-tos
- References to official documentation
- External resources

#### Advanced Usage

- Performance optimizations
- Alternative implementations
- Integration patterns

## Categories and Organization

### Current Categories

- **bibtex.md**: BibTeX format handling
- **testing.md**: Testing practices and frameworks
- **fetchers.md**: External data source integration
- **ui-recommendations.md**: User interface guidelines
- **tools.md**: Development tools and setup

### Naming Convention

- Use lowercase with hyphens: `feature-name.md`
- Be specific but concise
- Include main technology if relevant: `gradle-plugins.md`

## Writing Guidelines

### Code Examples

- Use syntax highlighting: ```java
- Include imports for non-obvious classes
- Use realistic, copy-pasteable examples
- Comment complex sections
- Follow JabRef coding standards

### Language and Tone

- Write in active voice: "Create a class" not "A class should be created"
- Use imperative mood for instructions
- Be concise but complete
- Assume reader has basic programming knowledge

### Technical Accuracy

- Test all code examples
- Verify API calls and method signatures
- Include version information for tools/frameworks
- Update when APIs change

## Maintenance Process

### Regular Reviews

- Review how-tos annually for accuracy
- Update for new JabRef versions
- Verify code examples still work
- Check links and references

### When APIs Change

- Update affected how-tos within 2 weeks
- Mark outdated sections as deprecated
- Create new versions if changes are breaking
- Communicate changes to dependent teams

### Deprecation

- Mark deprecated how-tos with clear warnings
- Provide links to replacement documentation
- Keep deprecated docs available for 1 year
- Archive permanently after 1 year

## Quality Checklist

### Before Publishing

- [ ] Clear, specific title that describes the task
- [ ] Prerequisites clearly listed
- [ ] Step-by-step instructions are complete
- [ ] Code examples are tested and working
- [ ] Troubleshooting section covers common issues
- [ ] Links to related documentation included
- [ ] Reviewed by at least one subject matter expert
- [ ] Follows markdown formatting standards

### Content Quality

- [ ] Instructions are actionable and unambiguous
- [ ] Examples demonstrate real-world usage
- [ ] Assumptions are stated explicitly
- [ ] Error conditions are handled
- [ ] Performance implications mentioned where relevant

## Tools and Resources

### Writing Tools

- **Markdown Preview**: Use VSCode or similar for live preview
- **Markdown Linting**: Run `markdownlint` on all files
- **Link Checking**: Use `lychee` to verify all links

### Testing Examples

- **Manual Testing**: Have another developer follow instructions
- **Automated Testing**: Create test scripts for complex setups
- **Version Testing**: Test on multiple JabRef versions when applicable

### Templates

- Use existing how-tos as templates
- Maintain consistent structure across guides
- Include standard sections unless not applicable

## Common Patterns

### API Integration How-Tos

1. Service overview and purpose
2. Authentication setup
3. Basic usage example
4. Error handling patterns
5. Rate limiting considerations
6. Testing approaches

### UI Component How-Tos

1. Component purpose and usage
2. Implementation steps
3. Styling guidelines
4. Accessibility requirements
5. Testing strategies
6. Integration examples

### Build/Tooling How-Tos

1. Tool purpose and benefits
2. Installation and setup
3. Configuration options
4. Usage examples
5. Troubleshooting
6. Integration with existing workflows
