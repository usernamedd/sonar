"""
Markdown report formatting for Sonar analysis results.
"""
from __future__ import annotations

import os
from datetime import datetime
from pathlib import Path
from typing import Optional

from .models import AnalysisResult


def format_report(result: AnalysisResult) -> str:
    """
    Format an AnalysisResult into a Markdown report string.

    Args:
        result: The AnalysisResult to format.

    Returns:
        A Markdown-formatted string.
    """
    lines = []
    lines.append("# Sonar Analysis Report")
    lines.append("")
    lines.append(f"*生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*")
    lines.append("")

    # Core Question
    lines.append("## Core Question")
    lines.append("")
    lines.append(result.core_question)
    lines.append("")

    # Background
    lines.append("## Background")
    lines.append("")
    lines.append(result.background)
    lines.append("")

    # Solutions
    lines.append("## Solutions")
    lines.append("")
    for i, sol in enumerate(result.solutions, start=1):
        lines.append(f"### {i}. {sol.title}")
        lines.append("")
        lines.append(sol.summary)
        lines.append("")
        lines.append(f"🔗 [{sol.url}]({sol.url})")
        lines.append("")

    return "\n".join(lines)


def save_report(
    result: AnalysisResult,
    output_dir: Optional[str] = None,
) -> str:
    """
    Save a formatted report to a Markdown file with a timestamp filename.

    Args:
        result: The AnalysisResult to save.
        output_dir: Output directory path (default: ~/sonar-reports/).

    Returns:
        Path to the saved report file.
    """
    output_path = Path(output_dir).expanduser() if output_dir else Path.home() / "sonar-reports"
    output_path.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    file_path = output_path / f"sonar-report_{timestamp}.md"

    report_content = format_report(result)

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(report_content)

    file_size = os.path.getsize(file_path)
    print(f"📄 报告已保存: {file_path}")
    print(f"   大小: {file_size / 1024:.1f} KB")

    return str(file_path)
