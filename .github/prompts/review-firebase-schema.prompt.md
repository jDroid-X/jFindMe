---
name: review-firebase-schema
argument-hint: "<collection-name>"
description: "Review and optimize Firebase database schema for a collection. Checks indexing, query patterns, security rules, and data normalization."
---

# Firebase Schema Review Prompt

Use this prompt when designing new collections or optimizing existing ones for better query performance and security.

## Input Parameter
- `collectionName`: Name of the Firebase collection to review

## Review Checklist

### 1. Data Structure Analysis
```javascript
// Sample document structure for review
const sampleDoc = {
  // Required fields
  // Optional fields
  // Nested objects (check normalization)
  // Timestamps (check update patterns)
};
```

### 2. Query Pattern Audit
- [ ] Identify all read queries on this collection
- [ ] Check for expensive client-side filtering
- [ ] Verify index usage for common queries
- [ ] Look for n+1 query patterns

### 3. Security Rules Validation
```json
{
  "rules": {
    "collectionName": {
      ".read": "auth != null && ...",
      ".write": "auth != null && ...",
      ".validate": "newData.isValid()"
    }
  }
}
```

### 4. Index Definition
```json
// firebase.json
{
  "database": {
    "indexes": [
      {
        "collectionGroup": "collectionName",
        "queryScope": "COLLECTION",
        "fields": [
          { "fieldPath": "ownerId", "order": "ASCENDING" },
          { "fieldPath": "lastUpdate", "order": "DESCENDING" }
        ]
      }
    ]
  }
}
```

## Optimization Recommendations

### Normalization
- Keep documents <1MB
- Flatten nested arrays where possible
- Use separate collections for historical data

### Denormalization
- Duplicate frequently accessed data
- Cache computed values (e.g., bounds, center point)
- Maintain via Cloud Functions

### Query Optimization
- Use compound indexes for multi-field filters
- Limit real-time listener scope
- Implement pagination for large datasets

## Output Format
Return review report:
```markdown
## Firebase Schema Review: {collectionName}
- Structure: [GOOD/WARNING/ISSUE]
- Queries: [Analysis]
- Security: [Status]
- Indexes: [Recommendations]
- Optimization: [Action Items]
```
