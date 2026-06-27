"""
Sonar — Recording Conversation Analysis Tool.

Record audio conversations, transcribe them with Whisper, analyze with LLM,
search for solutions, and generate structured Markdown reports.
"""
from __future__ import annotations

from .config import Config, load_config, get_config_path
from .models import AnalysisResult, Solution
from .recorder import record, list_devices
from .stt import transcribe
from .analyzer import analyze
from .searcher import search
from .formatter import format_report, save_report

__version__ = "0.1.0"
__all__ = [
    "Config",
    "AnalysisResult",
    "Solution",
    "load_config",
    "get_config_path",
    "record",
    "list_devices",
    "transcribe",
    "analyze",
    "search",
    "format_report",
    "save_report",
]
