# Implementation Plan

[Overview]
Add internationalization (i18n) support to the project through message properties files and a global exception handler, without modifying any existing controller code.

Spring Boot provides built-in `Accept-Language` header resolution for resolving locales. We will provide English defaults in `messages.properties` and Traditional Chinese translations in `messages_zh_TW.properties`. Additionally, we will implement a global `@RestControllerAdvice` to catch unhandled exceptions across the application and return translated error messages. Since we are not modifying existing controllers, this global handler will catch exceptions that are not internally caught by controllers (like `ReplayController` exceptions or missing parameters).

[Types]
Introduction of a custom exception type to facilitate throwing errors with i18n message keys.

- `AppException`: A `RuntimeException` that accepts a `messageKey` (String) and `args` (Object[]) to be translated by the global exception handler.
- `ErrorResponse`: A standard record or class representing the API error payload, containing fields like `timestamp`, `status`, `error`, `message`, and `path`.

[Files]
Changes to create i18n properties and custom exception handling structure.

- Create `src/main/resources/messages.properties` (Default English data data for general application text and errors)
- Create `src/main/resources/messages_zh_TW.properties` (Traditional Chinese translations)
- Create `src/main/java/com/ben/nat_jetstream_demo/exception/AppException.java`
- Create `src/main/java/com/ben/nat_jetstream_demo/exception/ErrorResponse.java`
- Create `src/main/java/com/ben/nat_jetstream_demo/exception/GlobalExceptionHandler.java`
- Update `src/main/resources/application.properties` (Add `spring.messages.basename` configuration if needed, although `messages` is default)

[Functions]
Methods for capturing exceptions and formatting error payloads using the MessageSource.

- `GlobalExceptionHandler#handleAppException(AppException, WebRequest, Locale)`: Catches `AppException`, resolves its `messageKey` through `MessageSource` with the request `Locale`, and constructs an `ErrorResponse`.
- `GlobalExceptionHandler#handleException(Exception, WebRequest, Locale)`: Catches global unhandled `Exception`, resolves a default internal error key in the `Locale`, and returns an `ErrorResponse`.
- `GlobalExceptionHandler#handleMissingParams(...)`: Catches specific Spring MVC exceptions like `MissingServletRequestParameterException` to show translated bad request errors.

[Classes]
Creation of a global advice layer.

- New class `GlobalExceptionHandler` with `@RestControllerAdvice`. It will have a dependency injected `MessageSource`.
- New class `AppException` extending `RuntimeException` to be thrown anywhere in service layers, carrying i18n context.

[Dependencies]
No new dependencies are required.

Utilizing existing `spring-boot-starter-web` features.

[Implementation Order]
The sequence of creating resources, types, and the global handler.

1. Create `messages.properties` and `messages_zh_TW.properties` with sample values (e.g. `error.internal`, `error.missing_param`).
2. Implement `AppException` and `ErrorResponse` data structures.
3. Implement `GlobalExceptionHandler` with `@RestControllerAdvice` and inject `MessageSource`.
4. Configure application properties to explicitly set the messages basename, and character encoding to UTF-8.
