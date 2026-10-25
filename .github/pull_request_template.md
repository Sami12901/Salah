## What changed

## Why

## Verification (quote real output — do not estimate)
```
$ ./gradlew :app:testDebugUnitTest
...
```

## Checklist
- [ ] Unit tests added/updated for any pure logic (calculation, formatting, scheduling, content)
- [ ] `PRIVACY.md` updated if a permission or network host was added
- [ ] `API_SOURCES.md` updated if an endpoint was added
- [ ] `CONTENT_LICENSES.md` updated if content/assets were added
- [ ] No analytic, ad, tracking or account code introduced
- [ ] User-visible strings go through `t(...)` and have a Bengali translation
