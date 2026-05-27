"""Shared configuration for the Python Agent Runtime.

All Java backend service URLs derive from BACKEND_SERVICE_URL since the
14 microservices were consolidated into a single agent-application on one port.
"""

import os

BACKEND_SERVICE_URL = os.getenv("BACKEND_SERVICE_URL", "http://localhost:9090")

# Individual service URLs — all point to the same consolidated backend app.
# Kept as separate vars so services can be split out again in the future
# without changing all call sites.
MEMORY_SERVICE_URL = os.getenv("MEMORY_SERVICE_URL", BACKEND_SERVICE_URL)
SKILL_SERVICE_URL = os.getenv("SKILL_SERVICE_URL", BACKEND_SERVICE_URL)
TOOL_SERVICE_URL = os.getenv("TOOL_SERVICE_URL", BACKEND_SERVICE_URL)
