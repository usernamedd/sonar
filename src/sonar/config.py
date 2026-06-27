"""
Configuration module for Sonar.
Loads settings from YAML config file or provides sensible defaults.
"""
from __future__ import annotations

import os
import yaml
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional


DEFAULT_CONFIG_PATH = Path.home() / ".config" / "sonar" / "config.yaml"
DEFAULT_RECORDINGS_DIR = Path.home() / "sonar-recordings"
DEFAULT_OUTPUT_DIR = Path.home() / "sonar-reports"


@dataclass
class Config:
    """Configuration dataclass for Sonar."""

    # STT (Speech-to-Text) settings
    stt_model: str = "base"
    stt_device: str = "cpu"  # "cpu" or "cuda"

    # LLM (Analysis) settings
    llm_provider: str = "mock"
    llm_api_key: str = "mock-key"
    llm_model: str = "gpt-4"

    # Search settings
    search_provider: str = "mock"
    search_api_key: str = "mock-key"

    # Paths
    recordings_dir: str = str(DEFAULT_RECORDINGS_DIR)
    output_dir: str = str(DEFAULT_OUTPUT_DIR)

    @classmethod
    def defaults(cls) -> "Config":
        """Return a Config instance with all default values."""
        return cls()

    def ensure_dirs(self) -> None:
        """Create the recordings and output directories if they don't exist."""
        Path(self.recordings_dir).mkdir(parents=True, exist_ok=True)
        Path(self.output_dir).mkdir(parents=True, exist_ok=True)


def get_config_path() -> Path:
    """Return the default config file path (~/.config/sonar/config.yaml)."""
    return DEFAULT_CONFIG_PATH


def load_config(path: Optional[str] = None) -> Config:
    """
    Load configuration from a YAML file.
    If path is None, attempts to load from the default config path.
    Falls back to defaults if the file doesn't exist.
    """
    config_path = Path(path) if path else DEFAULT_CONFIG_PATH

    if not config_path.exists():
        return Config.defaults()

    try:
        with open(config_path, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f) or {}
        return Config(
            stt_model=data.get("stt", {}).get("model", Config.stt_model),
            stt_device=data.get("stt", {}).get("device", Config.stt_device),
            llm_provider=data.get("llm", {}).get("provider", Config.llm_provider),
            llm_api_key=data.get("llm", {}).get("api_key", Config.llm_api_key),
            llm_model=data.get("llm", {}).get("model", Config.llm_model),
            search_provider=data.get("search", {}).get("provider", Config.search_provider),
            search_api_key=data.get("search", {}).get("api_key", Config.search_api_key),
            recordings_dir=data.get("paths", {}).get("recordings_dir", Config.recordings_dir),
            output_dir=data.get("paths", {}).get("output_dir", Config.output_dir),
        )
    except (yaml.YAMLError, OSError) as e:
        print(f"Warning: Failed to load config from {config_path}: {e}")
        return Config.defaults()
