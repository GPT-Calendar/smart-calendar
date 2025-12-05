# Design Document

## Overview

The Ollama Bridge Desktop Application is a Python-based Windows application that serves as a proxy bridge between an Android mobile app and a locally running Ollama instance. The application features a Tkinter-based GUI for configuration and monitoring, and runs an HTTP proxy server that forwards requests from the mobile app to Ollama. The application will be packaged as a standalone executable using PyInstaller, requiring no Python installation from end users.

## Architecture

### High-Level Architecture

```
┌─────────────────┐         ┌──────────────────────────┐         ┌─────────────┐
│                 │  HTTP   │  Ollama Bridge Desktop   │  HTTP   │             │
│  Mobile App     │────────▶│  ┌────────────────────┐  │────────▶│   Ollama    │
│  (Android)      │         │  │  Proxy Server      │  │         │  Instance   │
│                 │◀────────│  │  (Flask/HTTP)      │  │◀────────│  (Port      │
└─────────────────┘         │  └────────────────────┘  │         │   11434)    │
                            │           │               │         └─────────────┘
                            │           ▼               │
                            │  ┌────────────────────┐  │
                            │  │  Tkinter UI        │  │
                            │  │  - Connection Info │  │
                            │  │  - Request Logs    │  │
                            │  │  - Settings        │  │
                            │  │  - System Tray     │  │
                            │  └────────────────────┘  │
                            └──────────────────────────┘
```

### Component Architecture

The application follows a modular architecture with clear separation of concerns:

1. **UI Layer (Tkinter)**: Handles all user interactions and display
2. **Proxy Layer (Flask)**: HTTP server that forwards requests
3. **Network Layer**: Handles IP detection and network utilities
4. **Configuration Layer**: Manages settings persistence
5. **System Integration Layer**: Handles Windows-specific features (tray, startup)

## Components and Interfaces

### 1. Main Application (`main.py`)

**Responsibility**: Application entry point and lifecycle management

**Interface**:
```python
class OllamaBridgeApp:
    def __init__(self):
        """Initialize application components"""
        
    def start(self):
        """Start the proxy server and UI"""
        
    def stop(self):
        """Gracefully shutdown the application"""
```

### 2. Proxy Server (`proxy_server.py`)

**Responsibility**: HTTP proxy server that forwards requests between mobile app and Ollama

**Interface**:
```python
class ProxyServer:
    def __init__(self, port: int, ollama_url: str, log_callback: Callable):
        """Initialize proxy server with configuration"""
        
    def start(self):
        """Start the Flask server in a separate thread"""
        
    def stop(self):
        """Stop the Flask server"""
        
    def forward_request(self, path: str, method: str, headers: dict, body: bytes) -> Response:
        """Forward HTTP request to Ollama and return response"""
        
    def check_ollama_health(self) -> bool:
        """Check if Ollama instance is reachable"""
```

**Implementation Details**:
- Uses Flask as the HTTP server framework
- Runs in a separate daemon thread to avoid blocking the UI
- Forwards all HTTP methods (GET, POST, PUT, DELETE)
- Preserves headers and body content
- Implements CORS headers for mobile app compatibility
- Logs all requests/responses via callback

### 3. UI Manager (`ui_manager.py`)

**Responsibility**: Tkinter-based graphical user interface

**Interface**:
```python
class UIManager:
    def __init__(self, app_controller: AppController):
        """Initialize UI with reference to app controller"""
        
    def create_main_window(self):
        """Create the main application window"""
        
    def update_connection_info(self, ip_addresses: List[str], port: int):
        """Update displayed connection information"""
        
    def add_log_entry(self, timestamp: str, method: str, path: str, status: int, duration: float):
        """Add entry to request log display"""
        
    def show_error(self, title: str, message: str):
        """Display error dialog"""
        
    def show_settings_dialog(self):
        """Open settings configuration dialog"""
```

**UI Layout**:
```
┌─────────────────────────────────────────────┐
│  Ollama Bridge                          [_][□][X]│
├─────────────────────────────────────────────┤
│  Status: ● Connected to Ollama              │
│                                             │
│  Connection Information:                    │
│  ┌───────────────────────────────────────┐ │
│  │  http://192.168.1.100:8080           │ │
│  │                                       │ │
│  │  [Copy to Clipboard]                 │ │
│  └───────────────────────────────────────┘ │
│                                             │
│  Available IP Addresses:                    │
│  • 192.168.1.100 (Recommended - WiFi)      │
│  • 127.0.0.1 (Localhost)                   │
│                                             │
│  Request Log:                               │
│  ┌───────────────────────────────────────┐ │
│  │ 14:23:45 POST /api/generate 200 1.2s │ │
│  │ 14:23:42 POST /api/chat 200 0.8s     │ │
│  │ 14:23:40 GET /api/tags 200 0.1s      │ │
│  └───────────────────────────────────────┘ │
│                                             │
│  [Settings] [Test Connection] [Minimize]   │
└─────────────────────────────────────────────┘
```

### 4. Network Utilities (`network_utils.py`)

**Responsibility**: Network-related operations and IP detection

**Interface**:
```python
class NetworkUtils:
    @staticmethod
    def get_local_ip_addresses() -> List[Tuple[str, str]]:
        """Get all local IP addresses with interface names
        Returns: List of (ip_address, interface_name) tuples"""
        
    @staticmethod
    def get_recommended_ip() -> str:
        """Get the recommended IP address for mobile connection"""
        
    @staticmethod
    def is_port_available(port: int) -> bool:
        """Check if a port is available for binding"""
        
    @staticmethod
    def test_ollama_connection(url: str) -> Tuple[bool, str]:
        """Test connection to Ollama instance
        Returns: (success, message)"""
```

### 5. Configuration Manager (`config_manager.py`)

**Responsibility**: Persist and load application settings

**Interface**:
```python
class ConfigManager:
    def __init__(self, config_file: str = "config.json"):
        """Initialize configuration manager"""
        
    def load_config(self) -> dict:
        """Load configuration from file"""
        
    def save_config(self, config: dict):
        """Save configuration to file"""
        
    def get(self, key: str, default=None):
        """Get configuration value"""
        
    def set(self, key: str, value):
        """Set configuration value"""
```

**Configuration Schema**:
```json
{
    "proxy_port": 8080,
    "ollama_url": "http://localhost:11434",
    "auto_start": false,
    "minimize_to_tray": true,
    "log_requests": true,
    "theme": "light"
}
```

### 6. System Tray Manager (`tray_manager.py`)

**Responsibility**: System tray integration for Windows

**Interface**:
```python
class TrayManager:
    def __init__(self, app_controller: AppController):
        """Initialize system tray icon"""
        
    def create_tray_icon(self):
        """Create and display system tray icon"""
        
    def show_window(self):
        """Restore main window from tray"""
        
    def hide_window(self):
        """Hide main window to tray"""
        
    def update_tooltip(self, text: str):
        """Update tray icon tooltip"""
```

### 7. Startup Manager (`startup_manager.py`)

**Responsibility**: Windows startup integration

**Interface**:
```python
class StartupManager:
    @staticmethod
    def is_enabled() -> bool:
        """Check if auto-start is enabled"""
        
    @staticmethod
    def enable():
        """Add application to Windows startup"""
        
    @staticmethod
    def disable():
        """Remove application from Windows startup"""
```

### 8. Application Controller (`app_controller.py`)

**Responsibility**: Coordinates between all components

**Interface**:
```python
class AppController:
    def __init__(self):
        """Initialize all application components"""
        
    def start_proxy(self):
        """Start the proxy server"""
        
    def stop_proxy(self):
        """Stop the proxy server"""
        
    def restart_proxy(self, new_port: int = None):
        """Restart proxy with new configuration"""
        
    def on_request_logged(self, log_entry: dict):
        """Callback for proxy request logging"""
        
    def test_ollama_connection(self) -> Tuple[bool, str]:
        """Test connection to Ollama"""
        
    def update_settings(self, settings: dict):
        """Update application settings"""
```

## Data Models

### LogEntry

```python
@dataclass
class LogEntry:
    timestamp: datetime
    method: str
    path: str
    status_code: int
    duration_ms: float
    client_ip: str
    error: Optional[str] = None
```

### ConnectionInfo

```python
@dataclass
class ConnectionInfo:
    ip_addresses: List[Tuple[str, str]]  # (ip, interface_name)
    recommended_ip: str
    port: int
    full_url: str
```

### AppConfig

```python
@dataclass
class AppConfig:
    proxy_port: int = 8080
    ollama_url: str = "http://localhost:11434"
    auto_start: bool = False
    minimize_to_tray: bool = True
    log_requests: bool = True
    max_log_entries: int = 100
```

### OllamaStatus

```python
@dataclass
class OllamaStatus:
    is_reachable: bool
    version: Optional[str]
    last_check: datetime
    error_message: Optional[str] = None
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property 1: Connection string format validity

*For any* application state where the proxy server is running, the displayed connection string should be a valid HTTP URL containing an IP address and port number.
**Validates: Requirements 1.5, 2.2**

### Property 2: Network interface completeness

*For any* set of available network interfaces on the system, all interfaces with valid IP addresses should be displayed in the UI.
**Validates: Requirements 2.5**

### Property 3: Request forwarding preservation

*For any* HTTP request received by the proxy server, the forwarded request to Ollama should contain identical headers and body content.
**Validates: Requirements 3.1, 3.3**

### Property 4: Response forwarding preservation

*For any* HTTP response received from Ollama, the forwarded response to the mobile app should contain identical headers and body content.
**Validates: Requirements 3.2, 3.4**

### Property 5: Error response completeness

*For any* failed request to Ollama, the proxy server should return an HTTP error response containing both a status code and an error message.
**Validates: Requirements 3.5**

### Property 6: Request logging completeness

*For any* request processed by the proxy server, a log entry should be created containing timestamp, method, path, status code, and processing time.
**Validates: Requirements 4.1, 4.2**

### Property 7: Log size invariant

*For any* state of the request log, the number of displayed entries should never exceed 100.
**Validates: Requirements 4.3**

### Property 8: Client IP logging

*For any* incoming connection to the proxy server, the client IP address should be captured and displayed in the log.
**Validates: Requirements 4.5**

### Property 9: Port validation

*For any* port number input by the user, values outside the range 1024-65535 should be rejected with a validation error.
**Validates: Requirements 5.2**

### Property 10: Proxy availability invariant

*For any* UI state (visible or minimized to tray), the proxy server should continue accepting and processing requests.
**Validates: Requirements 6.4**

### Property 11: Settings persistence

*For any* configuration change saved by the user, loading the configuration after restart should return the same values.
**Validates: Requirements 7.4**

## Error Handling

### Error Categories

1. **Network Errors**
   - Ollama instance unreachable
   - Port binding failures
   - Network interface detection failures
   - Mobile app connection timeouts

2. **Configuration Errors**
   - Invalid port numbers
   - Invalid Ollama URL format
   - Configuration file corruption
   - Registry access failures (Windows startup)

3. **Runtime Errors**
   - Request forwarding failures
   - Response parsing errors
   - Memory constraints (log overflow)
   - Thread synchronization issues

### Error Handling Strategy

**Graceful Degradation**:
- If Ollama is unreachable, display clear status and continue running
- If port binding fails, suggest alternative ports
- If configuration is corrupted, fall back to defaults

**User Communication**:
- All errors displayed in non-technical language
- Provide actionable troubleshooting steps
- Include "Test Connection" button for verification

**Logging**:
- Log all errors to a file for debugging
- Include timestamp, error type, and stack trace
- Rotate log files to prevent disk space issues

**Recovery**:
- Automatic retry for transient network errors (with exponential backoff)
- Configuration reset option in settings
- Proxy server auto-restart on configuration changes

### Error Response Format

```python
@dataclass
class ErrorResponse:
    status_code: int
    error_type: str
    message: str
    troubleshooting_steps: List[str]
    timestamp: datetime
```

## Testing Strategy

### Unit Testing

The application will use **pytest** as the testing framework for Python unit tests.

**Unit Test Coverage**:
- Network utility functions (IP detection, port availability)
- Configuration manager (load, save, validation)
- Request/response forwarding logic
- Log entry management and size limiting
- Port validation logic
- URL format validation
- Startup registry management

**Test Organization**:
```
tests/
├── unit/
│   ├── test_network_utils.py
│   ├── test_config_manager.py
│   ├── test_proxy_server.py
│   ├── test_startup_manager.py
│   └── test_app_controller.py
├── integration/
│   ├── test_proxy_forwarding.py
│   └── test_ui_integration.py
└── property/
    ├── test_forwarding_properties.py
    ├── test_logging_properties.py
    └── test_validation_properties.py
```

### Property-Based Testing

The application will use **Hypothesis** as the property-based testing library for Python.

**Property Test Configuration**:
- Each property test should run a minimum of 100 iterations
- Each property-based test must include a comment tag referencing the correctness property from this design document
- Tag format: `# Feature: ollama-bridge-desktop, Property {number}: {property_text}`

**Property Test Coverage**:
- Request/response preservation across random HTTP methods, headers, and bodies
- Log size invariant across random sequences of requests
- Port validation across random integer inputs
- Network interface detection across various network configurations
- Configuration persistence across random setting combinations

**Example Property Test Structure**:
```python
from hypothesis import given, strategies as st

# Feature: ollama-bridge-desktop, Property 3: Request forwarding preservation
@given(
    method=st.sampled_from(['GET', 'POST', 'PUT', 'DELETE']),
    headers=st.dictionaries(st.text(), st.text()),
    body=st.binary()
)
def test_request_forwarding_preserves_content(method, headers, body):
    """For any HTTP request, forwarded request should preserve headers and body"""
    # Test implementation
    pass
```

### Integration Testing

**Integration Test Scenarios**:
- End-to-end request flow: Mobile app → Proxy → Ollama → Proxy → Mobile app
- UI updates in response to proxy events
- Configuration changes triggering proxy restart
- System tray interactions
- Windows startup integration

### Manual Testing Checklist

**Installation & Startup**:
- [ ] Single .exe file runs without Python installed
- [ ] Application starts within 5 seconds
- [ ] Ollama detection works correctly
- [ ] UI displays connection information

**Core Functionality**:
- [ ] Mobile app can connect and send requests
- [ ] Requests are forwarded to Ollama correctly
- [ ] Responses are returned to mobile app
- [ ] Request logs display correctly

**Configuration**:
- [ ] Port changes work correctly
- [ ] Custom Ollama URL can be set
- [ ] Settings persist across restarts
- [ ] Invalid inputs are rejected with clear errors

**System Integration**:
- [ ] Minimize to system tray works
- [ ] Restore from tray works
- [ ] Tray context menu functions correctly
- [ ] Auto-start can be enabled/disabled
- [ ] Application starts minimized when auto-start is enabled

**Error Handling**:
- [ ] Ollama unreachable error displays correctly
- [ ] Port conflict error displays correctly
- [ ] Network errors are handled gracefully
- [ ] Troubleshooting guide is helpful

## Deployment

### Build Process

**PyInstaller Configuration** (`build.spec`):
```python
# -*- mode: python ; coding: utf-8 -*-

block_cipher = None

a = Analysis(
    ['main.py'],
    pathex=[],
    binaries=[],
    datas=[('icon.ico', '.'), ('config_default.json', '.')],
    hiddenimports=['pystray._win32'],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='OllamaBridge',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon='icon.ico',
    version='version_info.txt'
)
```

**Build Command**:
```bash
pyinstaller build.spec --clean --onefile
```

**Version Information** (`version_info.txt`):
```
VSVersionInfo(
  ffi=FixedFileInfo(
    filevers=(1, 0, 0, 0),
    prodvers=(1, 0, 0, 0),
    mask=0x3f,
    flags=0x0,
    OS=0x40004,
    fileType=0x1,
    subtype=0x0,
    date=(0, 0)
  ),
  kids=[
    StringFileInfo(
      [
      StringTable(
        u'040904B0',
        [StringStruct(u'CompanyName', u'Ollama Bridge'),
        StringStruct(u'FileDescription', u'Ollama Bridge Desktop Application'),
        StringStruct(u'FileVersion', u'1.0.0.0'),
        StringStruct(u'InternalName', u'OllamaBridge'),
        StringStruct(u'LegalCopyright', u'Copyright (c) 2025'),
        StringStruct(u'OriginalFilename', u'OllamaBridge.exe'),
        StringStruct(u'ProductName', u'Ollama Bridge'),
        StringStruct(u'ProductVersion', u'1.0.0.0')])
      ]),
    VarFileInfo([VarStruct(u'Translation', [1033, 1200])])
  ]
)
```

### Dependencies

**Core Dependencies** (`requirements.txt`):
```
flask==3.0.0
requests==2.31.0
pystray==0.19.5
Pillow==10.1.0
pywin32==306
```

**Development Dependencies** (`requirements-dev.txt`):
```
pytest==7.4.3
hypothesis==6.92.1
pyinstaller==6.3.0
black==23.12.1
flake8==7.0.0
mypy==1.7.1
```

### Distribution

**Package Contents**:
- `OllamaBridge.exe` - Main executable
- `README.txt` - Quick start guide
- `LICENSE.txt` - License information

**Distribution Channels**:
- GitHub Releases
- Direct download from project website
- Optional: Microsoft Store (future consideration)

### Installation Instructions (for users)

1. Download `OllamaBridge.exe`
2. Double-click to run (no installation needed)
3. Allow firewall access when prompted
4. Copy the displayed connection URL
5. Enter the URL in your mobile app settings

### System Requirements

- Windows 10 or later (64-bit)
- 100 MB free disk space
- Network connection (WiFi or Ethernet)
- Ollama installed and running on the same PC

## Security Considerations

### Network Security

- Proxy server binds to all interfaces (0.0.0.0) to allow mobile connections
- No authentication implemented (assumes trusted local network)
- CORS headers allow cross-origin requests from mobile app
- Recommendation: Use on private networks only

### Data Privacy

- No data is stored or logged permanently (except optional request logs)
- Request logs are kept in memory only (cleared on restart)
- Configuration file stores only non-sensitive settings
- No telemetry or external communication

### Windows Security

- Application requires firewall exception for incoming connections
- Registry modifications limited to HKEY_CURRENT_USER (no admin required)
- No system-level changes or driver installations

### Future Security Enhancements

- Optional API key authentication
- HTTPS support with self-signed certificates
- Request rate limiting
- IP whitelist/blacklist

## Performance Considerations

### Resource Usage

**Memory**:
- Base application: ~50 MB
- Per request: ~1-5 MB (depending on payload size)
- Log buffer: ~10 MB (100 entries)
- Total expected: <100 MB under normal use

**CPU**:
- Idle: <1%
- During request forwarding: 5-10%
- UI updates: <5%

**Network**:
- Bandwidth: Depends on Ollama request/response sizes
- Typical: 1-10 MB per AI request
- Latency overhead: <10ms for local forwarding

### Optimization Strategies

- Use daemon threads for proxy server (non-blocking)
- Implement request/response streaming for large payloads
- Limit log buffer size to prevent memory growth
- Use connection pooling for Ollama requests
- Minimize UI updates (batch log entries)

### Scalability

- Designed for single-user, single-device scenarios
- Can handle multiple concurrent requests from one mobile device
- Not designed for multi-user or production use
- Maximum recommended: 10 requests per second

## Future Enhancements

### Phase 2 Features

- macOS and Linux support
- Dark mode theme
- Request/response inspection (debugging view)
- Export logs to file
- QR code generation for easy mobile setup
- Automatic Ollama installation detection and guidance

### Phase 3 Features

- Multi-device support (multiple mobile apps)
- User authentication and API keys
- HTTPS/TLS support
- Cloud relay option (for remote access)
- Model management UI (download, switch models)
- Performance metrics and analytics

## Appendix

### Technology Stack Summary

- **Language**: Python 3.10+
- **UI Framework**: Tkinter (built-in)
- **HTTP Server**: Flask
- **HTTP Client**: requests
- **System Tray**: pystray
- **Windows Integration**: pywin32
- **Packaging**: PyInstaller
- **Testing**: pytest, Hypothesis

### File Structure

```
ollama-bridge/
├── main.py                 # Application entry point
├── app_controller.py       # Main application controller
├── proxy_server.py         # HTTP proxy server
├── ui_manager.py           # Tkinter UI
├── network_utils.py        # Network utilities
├── config_manager.py       # Configuration management
├── tray_manager.py         # System tray integration
├── startup_manager.py      # Windows startup integration
├── models.py               # Data models
├── icon.ico                # Application icon
├── config_default.json     # Default configuration
├── build.spec              # PyInstaller build spec
├── version_info.txt        # Version information
├── requirements.txt        # Dependencies
├── requirements-dev.txt    # Development dependencies
├── README.md               # Developer documentation
├── tests/                  # Test suite
│   ├── unit/
│   ├── integration/
│   └── property/
└── dist/                   # Build output
    └── OllamaBridge.exe
```

### Development Setup

```bash
# Clone repository
git clone <repository-url>
cd ollama-bridge

# Create virtual environment
python -m venv venv
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
pip install -r requirements-dev.txt

# Run tests
pytest

# Run application (development)
python main.py

# Build executable
pyinstaller build.spec --clean --onefile
```
