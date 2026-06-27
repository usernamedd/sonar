"""
Sonar CLI — Recording Conversation Analysis Tool.
"""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Optional

import click

from . import (
    AnalysisResult,
    Config,
    Solution,
    analyze,
    format_report,
    get_config_path,
    load_config,
    record,
    list_devices as list_audio_devices,
    save_report,
    search,
    transcribe,
)


def _print_config_table(config: Config) -> None:
    """Print current configuration in a formatted table."""
    try:
        from rich.console import Console
        from rich.table import Table
        from rich.panel import Panel
        from rich import box

        console = Console()

        # Main config panel
        table = Table(box=box.ROUNDED, title="Sonar Configuration")
        table.add_column("Section", style="cyan", no_wrap=True)
        table.add_column("Key", style="yellow", no_wrap=True)
        table.add_column("Value", style="green")

        table.add_row("STT", "Model", config.stt_model)
        table.add_row("STT", "Device", config.stt_device)
        table.add_row("LLM", "Provider", config.llm_provider)
        table.add_row("LLM", "API Key", config.llm_api_key[:8] + "..." if len(config.llm_api_key) > 8 else config.llm_api_key)
        table.add_row("Search", "Provider", config.search_provider)
        table.add_row("Search", "API Key", config.search_api_key[:8] + "..." if len(config.search_api_key) > 8 else config.search_api_key)
        table.add_row("Paths", "Recordings Dir", config.recordings_dir)
        table.add_row("Paths", "Output Dir", config.output_dir)

        console.print()
        console.print(Panel(table, title="⚙️  Sonar Configuration", border_style="blue"))
        console.print()

        # Config file location
        config_path = get_config_path()
        if config_path.exists():
            console.print(f"  📄 Config file: [bold]{config_path}[/bold]")
        else:
            console.print(f"  📄 Config file: [bold]{config_path}[/bold] (using defaults)")
        console.print()
    except ImportError:
        # Fallback if rich is not available
        print("\n=== Sonar Configuration ===\n")
        print(f"  STT Model:      {config.stt_model}")
        print(f"  STT Device:     {config.stt_device}")
        print(f"  LLM Provider:   {config.llm_provider}")
        print(f"  LLM API Key:    {config.llm_api_key[:8]}...")
        print(f"  Search Provider:{config.search_provider}")
        print(f"  Search API Key: {config.search_api_key[:8]}...")
        print(f"  Recordings Dir: {config.recordings_dir}")
        print(f"  Output Dir:     {config.output_dir}")
        print()


def _print_devices_table() -> None:
    """Print audio devices in a formatted table."""
    try:
        from rich.console import Console
        from rich.table import Table
        from rich.panel import Panel
        from rich import box

        import sounddevice as sd

        console = Console()
        devices = sd.query_devices()
        default_input = sd.default.device[0]
        default_output = sd.default.device[1]

        table = Table(box=box.ROUNDED, title="Available Audio Devices")
        table.add_column("Index", style="cyan", no_wrap=True)
        table.add_column("Name", style="yellow")
        table.add_column("Channels (in/out)", justify="center")
        table.add_column("Sample Rate", justify="center")
        table.add_column("Default", justify="center")

        for i, dev in enumerate(devices):
            name = dev["name"]
            channels = f"{dev['max_input_channels']} / {dev['max_output_channels']}"
            samplerate = f"{dev['default_samplerate']:.0f} Hz"
            is_default = ""
            if i == default_input and i == default_output:
                is_default = "✅ in/out"
            elif i == default_input:
                is_default = "✅ input"
            elif i == default_output:
                is_default = "✅ output"
            table.add_row(str(i), name, channels, samplerate, is_default)

        console.print()
        console.print(Panel(table, border_style="green"))
        console.print()
    except ImportError:
        list_audio_devices()


def _print_analysis_result(result: AnalysisResult, report_path: Optional[str] = None) -> None:
    """Print the analysis result in a nice rich format."""
    try:
        from rich.console import Console
        from rich.panel import Panel
        from rich.markdown import Markdown
        from rich import box

        console = Console()

        console.print()
        console.print(Panel(
            f"[bold]Core Question[/bold]\n\n{result.core_question}",
            title="🎯  Analysis Result",
            border_style="cyan",
            box=box.ROUNDED,
        ))
        console.print()

        console.print(Panel(
            result.background,
            title="📋  Background",
            border_style="blue",
            box=box.ROUNDED,
        ))
        console.print()

        console.print("[bold]💡  Solutions[/bold]")
        console.print()
        for i, sol in enumerate(result.solutions, start=1):
            console.print(f"  [bold cyan]{i}. {sol.title}[/bold cyan]")
            console.print(f"     {sol.summary}")
            console.print(f"     🔗 [link={sol.url}]{sol.url}[/link]")
            console.print()

        if report_path:
            console.print(f"  📄 Report saved: [bold]{report_path}[/bold]")
        console.print()
    except ImportError:
        print(f"\n🎯  Core Question:\n  {result.core_question}")
        print(f"\n📋  Background:\n  {result.background}")
        print("\n💡  Solutions:")
        for i, sol in enumerate(result.solutions, start=1):
            print(f"\n  {i}. {sol.title}")
            print(f"     {sol.summary}")
            print(f"     🔗 {sol.url}")
        print()


def _print_transcript_result(transcript: dict) -> None:
    """Print transcription result with rich formatting."""
    try:
        from rich.console import Console
        from rich.panel import Panel
        from rich.table import Table
        from rich import box
        from rich.text import Text

        console = Console()

        # Summary
        info_table = Table(box=box.SIMPLE, show_header=False)
        info_table.add_column("Key", style="cyan")
        info_table.add_column("Value", style="green")
        info_table.add_row("Language", transcript.get("language", "unknown"))
        info_table.add_row("Text Length", f"{len(transcript['text'])} chars")
        info_table.add_row("Segments", str(len(transcript.get("segments", []))))

        console.print()
        console.print(Panel(info_table, title="📝  Transcription Result", border_style="green"))
        console.print()

        # Full text
        console.print("[bold]📃  Full Text:[/bold]")
        console.print()
        console.print(Panel(transcript["text"], border_style="dim"))
        console.print()

        # Segments with timestamps
        segments = transcript.get("segments", [])
        if segments:
            seg_table = Table(box=box.ROUNDED, title="Segments")
            seg_table.add_column("#", style="cyan", no_wrap=True)
            seg_table.add_column("Start", style="yellow")
            seg_table.add_column("End", style="yellow")
            seg_table.add_column("Text", style="white")

            for i, seg in enumerate(segments[:20], start=1):  # Show first 20
                start_str = f"{seg['start']:.1f}s"
                end_str = f"{seg['end']:.1f}s"
                seg_table.add_row(str(i), start_str, end_str, seg["text"][:80])

            console.print(seg_table)
            if len(segments) > 20:
                console.print(f"  ... and {len(segments) - 20} more segments")
            console.print()
    except ImportError:
        print(f"\n📝  Transcription Result")
        print(f"  Language: {transcript.get('language', 'unknown')}")
        print(f"  Text: {transcript['text'][:200]}...")
        print()


# ---------------------------------------------------------------------------
# CLI Commands
# ---------------------------------------------------------------------------

@click.group(invoke_without_command=False)
@click.pass_context
def cli(ctx: click.Context) -> None:
    """
    Sonar — Recording Conversation Analysis Tool.

    记录和分析对话，提取核心问题并搜索解决方案。
    """
    ctx.ensure_object(dict)


@cli.command()
@click.option("--duration", "-d", type=float, default=None,
              help="Recording duration in seconds (default: record until Enter)")
@click.option("--samplerate", "-sr", type=int, default=16000,
              help="Sample rate in Hz (default: 16000)")
@click.option("--device", type=int, default=None,
              help="Input device index (default: system default)")
def record_cmd(duration: Optional[float], samplerate: int, device: Optional[int]) -> None:
    """🎤 录制音频 — Record audio from the microphone."""
    try:
        filepath = record(duration=duration, samplerate=samplerate, device=device)
        click.echo(f"  File: {filepath}")
    except Exception as e:
        click.echo(f"❌ 录音失败: {e}", err=True)
        sys.exit(1)


@cli.command()
@click.argument("audio_file", type=click.Path(exists=True))
@click.option("--model", "-m", default="base",
              help="Whisper model size: tiny/base/small/medium/large (default: base)")
@click.option("--device", default="cpu",
              help="Device: cpu or cuda (default: cpu)")
@click.option("--language", "-l", default=None,
              help="Language code: zh, en, etc. Auto-detect if not set.")
def transcribe_cmd(audio_file: str, model: str, device: str, language: Optional[str]) -> None:
    """📝 转录音频 — Transcribe an audio file to text."""
    try:
        result = transcribe(
            audio_path=audio_file,
            model_size=model,
            device=device,
            language=language,
        )
        _print_transcript_result(result)
    except Exception as e:
        click.echo(f"❌ 转录失败: {e}", err=True)
        sys.exit(1)


@cli.command()
@click.option("--duration", "-d", type=float, default=None,
              help="Recording duration in seconds (default: record until Enter)")
@click.option("--model", "-m", default="base",
              help="Whisper model size (default: base)")
@click.option("--stt-device", default=None,
              help="STT compute device: cpu or cuda")
@click.option("--language", "-l", default=None,
              help="Language code for transcription")
@click.option("--no-save", is_flag=True,
              help="Don't save report to file, only print")
@click.option("--save-report", is_flag=True, default=True,
              help="Save report to file (default: True)")
def analyze_cmd(
    duration: Optional[float],
    model: str,
    stt_device: Optional[str],
    language: Optional[str],
    no_save: bool,
    save_report: bool,
) -> None:
    """
    🚀 完整分析管道 — Record → Transcribe → Analyze → Search → Report.

    Runs the full pipeline: record audio, transcribe, analyze with LLM,
    search for solutions, and output a Markdown report.
    """
    try:
        config = load_config()

        # 1. Record
        click.echo("\n🎤 [bold]Step 1: 录制音频[/bold]")
        audio_path = record(
            duration=duration,
            samplerate=16000,
        )

        # 2. Transcribe
        click.echo("\n📝 [bold]Step 2: 转录音频[/bold]")
        transcript = transcribe(
            audio_path=audio_path,
            model_size=model or config.stt_model,
            device=stt_device or config.stt_device,
            language=language,
        )

        # 3. Analyze
        click.echo("\n🔍 [bold]Step 3: 分析对话[/bold]")
        analysis_result = analyze(
            transcript=transcript["text"],
            provider=config.llm_provider,
            api_key=config.llm_api_key,
            model=config.llm_model,
        )

        # 4. Search
        click.echo("\n🔎 [bold]Step 4: 搜索解决方案[/bold]")
        solutions = search(
            problem=analysis_result.core_question,
            background=analysis_result.background,
            provider=config.search_provider,
            api_key=config.search_api_key,
        )

        # Reconstruct with found solutions
        analysis_result = AnalysisResult(
            core_question=analysis_result.core_question,
            background=analysis_result.background,
            solutions=solutions,
        )

        # 5. Format & Output
        click.echo("\n📄 [bold]Step 5: 生成报告[/bold]")
        report_path = None
        if not no_save and save_report:
            report_path = save_report(analysis_result, output_dir=config.output_dir)

        _print_analysis_result(analysis_result, report_path=report_path)

    except Exception as e:
        click.echo(f"❌ 分析失败: {e}", err=True)
        sys.exit(1)


@cli.command()
@click.option("--text", "-t", default=None,
              help="Transcript text to analyze (if not provided via stdin or file)")
@click.option("--file", "-f", "file_path", type=click.Path(exists=True),
              help="File containing transcript text")
@click.option("--no-save", is_flag=True,
              help="Don't save report to file, only print")
def report_cmd(text: Optional[str], file_path: Optional[str], no_save: bool) -> None:
    """
    📊 分析已有文本 — Analyze an existing transcript text.

    Provide text directly with --text, via a file with --file, or pipe via stdin.
    """
    try:
        config = load_config()

        # Get transcript text from different sources
        if text:
            transcript_text = text
        elif file_path:
            with open(file_path, "r", encoding="utf-8") as f:
                transcript_text = f.read()
        elif not sys.stdin.isatty():
            transcript_text = sys.stdin.read()
        else:
            click.echo("❌ 请提供要分析的文本 (--text, --file, 或通过管道传入)", err=True)
            sys.exit(1)

        if not transcript_text.strip():
            click.echo("❌ 文本内容为空", err=True)
            sys.exit(1)

        click.echo(f"📝 输入文本: {len(transcript_text)} 字符")

        # Analyze
        click.echo("\n🔍 [bold]正在分析对话...[/bold]")
        analysis_result = analyze(
            transcript=transcript_text,
            provider=config.llm_provider,
            api_key=config.llm_api_key,
            model=config.llm_model,
        )

        # Search
        click.echo("\n🔎 [bold]搜索解决方案...[/bold]")
        solutions = search(
            problem=analysis_result.core_question,
            background=analysis_result.background,
            provider=config.search_provider,
            api_key=config.search_api_key,
        )

        analysis_result = AnalysisResult(
            core_question=analysis_result.core_question,
            background=analysis_result.background,
            solutions=solutions,
        )

        # Save & output
        report_path = None
        if not no_save:
            report_path = save_report(analysis_result, output_dir=config.output_dir)

        _print_analysis_result(analysis_result, report_path=report_path)

    except Exception as e:
        click.echo(f"❌ 分析失败: {e}", err=True)
        sys.exit(1)


@cli.command()
def config_cmd() -> None:
    """⚙️  显示配置 — Show current Sonar configuration."""
    config = load_config()
    _print_config_table(config)


@cli.command()
def devices_cmd() -> None:
    """🎛️  列出音频设备 — List available audio input/output devices."""
    _print_devices_table()


# Aliases for convenience
cli.add_command(record_cmd, name="record")
cli.add_command(transcribe_cmd, name="transcribe")
cli.add_command(analyze_cmd, name="analyze")
cli.add_command(report_cmd, name="report")
cli.add_command(config_cmd, name="config")
cli.add_command(devices_cmd, name="devices")
