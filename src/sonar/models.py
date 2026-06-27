"""
Shared data models for Sonar.
"""
from __future__ import annotations

from typing import List, NamedTuple


class Solution(NamedTuple):
    """A solution / search result entry."""
    title: str
    summary: str
    url: str


class AnalysisResult(NamedTuple):
    """Result of LLM-based conversation analysis."""
    core_question: str
    background: str
    solutions: List[Solution]
