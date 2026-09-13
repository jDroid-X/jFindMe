# jFindMe - Extension Recommendations

## Essential Extensions

### Kotlin & Android
- **Kotlin** (ms-kotlin) - Kotlin language support
- **Android Studio** (optional) - For full Android development experience

### Firebase & Backend
- **Firebase Extension for VS Code** - Manage Firebase projects
- **REST Client** - Test API endpoints

### Code Quality
- **Error Lens** - Inline error/warning display
- **Prettier** - Code formatting
- **ESLint** - JavaScript/TypeScript linting

### Productivity
- **GitLens** - Enhanced Git capabilities
- **Project Manager** - Quick workspace switching
- **Todo Tree** - Highlight TODO/FIXME comments

### Python (for Chaquopy)
- **Python** (ms-python.python) - Python language support
- **Pylance** - Python language server

### Documentation
- **Markdown All in One** - Markdown editing
- **Draw.io Integration** - Diagrams and flowcharts

## Project-Specific Configuration

```json
{
    "editor.formatOnSave": true,
    "editor.defaultFormatter": "ms-kotlin.kotlinter",
    "[kotlin]": {
        "editor.tabSize": 4
    },
    "kotlin.lint.onSave": true
}
```

## Workspace Symbols Setup
Enable these folders in search:
- `app/src/main/java/` - Main source code
- `app/src/main/python/` - Python GIS functions
- `.agents/` - Agent configurations
- `requirement/` - Task requirements
