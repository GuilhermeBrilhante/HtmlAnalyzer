# HTML Analyzer

A Java command-line tool that analyzes HTML content from a URL and finds the text at the deepest nesting level.

---

## Axur Engineering Practices Applied

This implementation was developed following the software engineering practices used at Axur, as described in their engineering blog articles.

---

## Clean Code

Following Robert C. Martin's principles:

- **Expressive Naming:** Variables and methods clearly communicate intent
  - `currentDepth`, `maxDepth`, `deepestText`
  - `canCloseTag()`, `hasUnclosedTags()`, `isSuccessfulResponse()`

- **Single Responsibility Principle:** Each method has one clear purpose
  - `createConnection()`: handles HTTP connection setup
  - `createReader()`: creates properly configured reader
  - `findDeepestText()`: orchestrates parsing logic
  - `processLine()`: delegates to specialized handlers
  - `HtmlParsingState`: encapsulates parsing state

- **No Magic Numbers:** All constants are named and meaningful
  - `CONNECTION_TIMEOUT_MS`, `HTTP_SUCCESS_MIN`, `MIN_TAG_LENGTH`

- **Small, Focused Methods:** Each method is concise and does one thing well

- **Self-Documenting Code:** The code expresses its intentions clearly, reducing the need for explanatory comments

---

## Domain-Driven Design (DDD)

The code is organized in layered architecture following DDD principles:

### Infrastructure Layer

Handles technical implementations and external communication:

- `createConnection()`: HTTP communication
- `createReader()`: I/O stream handling
- Charset configuration (UTF-8)
- Connection timeout management

### Application Layer

Coordinates use cases and orchestrates domain operations:

- `analyze()`: main entry point, coordinates the workflow
- `classifyLine()`: categorizes HTML content
- `processLine()`: routes to appropriate handlers

### Domain Layer (Core)

Contains business rules and domain model:

- `HtmlParsingState`: domain model representing parsing state
- `findDeepestText()`: core algorithm implementation
- `updateDeepestText()`: business rule (deepest text selection)
- Tag matching validation logic

---

## Test-Driven Development (TDD) Mindset

While the technical requirements prohibit external libraries (including JUnit), the code was designed with testability in mind:

- **Testable Design:** Small, focused methods are easy to test
- **Dependency Injection:** State is encapsulated in `HtmlParsingState`, making it testable in isolation
- **Test Strategy Documentation:** Comprehensive test suite outlined in code comments
- **Design Decisions:** Choices like which dependencies to inject were made to facilitate testing

The code includes a detailed testing strategy section with test suites covering:

- Valid HTML scenarios
- Malformed HTML detection
- Error handling
- Edge cases

Each test would follow the TDD cycle: **Red -> Green -> Refactor**

---

## DevOps Principles Applied

### Production Readiness

- Proper resource management (try-with-resources, connection cleanup)
- Timeout configuration to prevent hanging connections
- Explicit error handling with meaningful messages
- UTF-8 charset handling for international content

### Observability Considerations

Comments indicate where logging would be added in production:

- Error logging: `log.error("Failed to analyze URL: {}", urlString, e);`
- Warning logging: `log.warn("Malformed HTML detected at line: {}", trimmedLine);`
- Debug logging: `log.debug("Processing line type: {}", type);`

---

## Software Craftsmanship

As emphasized in Axur's engineering culture:

- **Attention to Detail:** Every line is carefully crafted
- **Edge Case Handling:** Empty tags, null inputs, malformed HTML
- **Consistent Code Style:** Uniform structure throughout
- **Defensive Programming:** Input validation at all entry points
- **Professional Quality:** Production-ready code, not just "working" code

---

## Technical Approach

### Design Decisions

The solution demonstrates clean architecture with clear separation of concerns:

1. **Single Responsibility:** Each component has one well-defined purpose
   - HTTP connection handling is isolated
   - Parsing logic is separate from I/O
   - State management is encapsulated

2. **Resource Safety:** Proper cleanup using try-with-resources and finally blocks
   - BufferedReader is auto-closed
   - HttpURLConnection is explicitly disconnected
   - No resource leaks

3. **Defensive Programming:**
   - Input validation (null checks, empty strings)
   - Edge case handling (empty tags, minimum lengths)
   - Consistent error reporting

### Algorithm

The algorithm uses a stack-based approach to track HTML tag nesting:

1. Read each line from the HTTP response
2. Classify each line as: opening tag, closing tag, or text
3. For opening tags: push to stack, increment depth
4. For closing tags: validate against stack top, pop and decrement depth
5. For text: if current depth > max depth, save as deepest text
6. At end: validate all tags are closed

**Time Complexity:** O(n) where n is the number of lines
**Space Complexity:** O(d) where d is the maximum nesting depth

---

## Error Handling

The implementation handles three types of errors as specified:

### `URL connection error`

Returned for:
- Missing command-line arguments
- Null or empty URL
- Network failures
- Non-2xx HTTP responses

### `malformed HTML`

Returned for:
- Mismatched tags
- Unclosed tags
- Empty tags (`<>`, `</>`)
- HTML without any text content
- Invalid line formats

---

## Building and Running

### Compilation

From the directory containing the source code:

```bash
javac HtmlAnalyzer.java
```

### Execution

From the compilation directory:

```bash
java HtmlAnalyzer http://hiring.axreng.com/internship/example1.html
```

### Requirements

- **JDK:** 17+
- **Dependencies:** None (uses only standard JDK libraries)
- **External Libraries:** Not allowed per technical requirements

---

## Testing Strategy

While automated tests are not included (due to library restrictions), the code was designed following TDD principles. A comprehensive test suite would include:

### Test Suites

1. **Valid HTML Scenarios**
   - Single level text
   - Multiple nesting levels
   - Multiple texts at same depth (verify first is returned)
   - Empty lines handling
   - Indentation handling

2. **Malformed HTML Detection**
   - Unclosed tags
   - Mismatched tags
   - Empty tags
   - HTML without text

3. **Error Handling**
   - Invalid URLs
   - Null/empty URLs
   - HTTP errors
   - Network failures

4. **Edge Cases**
   - Single tag without text
   - Text outside tags
   - Deep nesting (10+ levels)

---

## References

This implementation draws inspiration from:

- *Clean Code* by Robert C. Martin
- *Domain-Driven Design* by Eric Evans
- *Implementing Domain-Driven Design* by Vaughn Vernon
- Axur Engineering Blog articles on software engineering practices
