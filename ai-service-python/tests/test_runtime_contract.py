from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PROVIDER_MARKERS = (
    "openai",
    "anthropic",
    "phoenix",
    "provider",
    "llm",
    "prompt",
)


def test_dockerfile_exposes_fastapi_runtime() -> None:
    dockerfile = (PROJECT_ROOT / "Dockerfile").read_text()

    assert "python:3.12-slim" in dockerfile
    assert "EXPOSE 8000" in dockerfile
    assert "fastapi" in dockerfile or "uvicorn" in dockerfile


def test_runtime_files_do_not_reference_external_ai_providers() -> None:
    runtime_files = [
        PROJECT_ROOT / "Dockerfile",
        PROJECT_ROOT / "pyproject.toml",
        *(PROJECT_ROOT / "app").glob("*.py"),
    ]

    for runtime_file in runtime_files:
        content = runtime_file.read_text().lower()
        for marker in PROVIDER_MARKERS:
            assert marker not in content, f"{marker} found in {runtime_file}"
