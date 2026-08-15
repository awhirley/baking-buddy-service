# API Error Handling

This describes how the BakingBuddy backend handles request validation, errors,
and response formatting.

## Response shape

Successful responses return the resource directly — no wrapper. Errors return
a consistent envelope so the client can handle failures the same way
regardless of which endpoint produced them:

```json
{
  "error": {
    "code": "validation_error",
    "message": "Request failed validation",
    "field_errors": [
      { "field": "name", "message": "must not be blank" }
    ],
    "details": {}
  }
}
```

- `code` — stable, machine-readable identifier from a fixed enum. Safe for
  the frontend to switch on.
- `message` — human-readable summary, safe to show as a fallback.
- `field_errors` — populated only for validation failures; one entry per
  invalid field, so the client can highlight all of them at once rather than
  failing on the first bad field.
- `details` — free-form context specific to the error (e.g. expected vs.
  actual version on a conflict).

Field names are snake_case on the wire, matching the rest of the API.

## Error codes and status mapping

| Code                   | HTTP Status | Meaning                                                          |
|-------------------------|:-----------:|-------------------------------------------------------------------|
| `validation_error`      | 400         | One or more fields failed validation                              |
| `bad_request`           | 400         | Malformed request that isn't a field failure (e.g. bad path param)|
| `not_found`              | 404         | Requested resource doesn't exist                                  |
| `conflict`               | 409         | Request conflicts with current server state                       |
| `unprocessable_entity`   | 422         | Well-formed and valid shape, but violates a business rule         |
| `data_integrity_error`   | 500         | Server-side data inconsistency (not the client's fault)           |
| `internal_error`         | 500         | Unhandled exception                                                |

**400 vs 422** — `validation_error` is about *shape*: a field is missing,
blank, or the wrong type. `unprocessable_entity` is about *semantics*: the
request is well-formed but violates a rule the server enforces — e.g.
reverting a recipe ingredient to a `best_version` that doesn't exist for
that ingredient.

**409 vs 422** — `conflict` is for competing state (two updates racing each
other); `unprocessable_entity` is for requests that were never valid to
begin with, regardless of timing.

## Exception hierarchy

Route and service code throws typed exceptions rather than building error
responses inline. A single `StatusPages` handler is the only place that
converts an exception into an HTTP response, keeping that mapping in one
place:

- `NotFoundException(resource, id)` → 404
- `ValidationException(fieldErrors)` → 400
- `BadRequestException(message)` → 400
- `ConflictException(message, details)` → 409
- `UnprocessableEntityException(message, details)` → 422
- `DataIntegrityException(message)` → 500

All extend a sealed `ApiException` base, so adding a new error type is a
compile-time-checked decision about status code and error code — not a
convention that can silently drift.

Anything that isn't one of the above (an unexpected exception) is caught by
a catch-all handler, logged server-side, and returned to the client as a
generic `internal_error` with no internal details or stack trace exposed.

## Validation

Field validation runs explicitly at the top of route handlers, before any
database access, and collects every failing field into one
`ValidationException` rather than throwing on the first problem:

```kotlin
validate {
    requireNotBlank(payload.name, "name")
    requireNotBlank(payload.recipeSource, "source")
}
```

This keeps validation logic out of route bodies and gives the client all
invalid fields in a single response.