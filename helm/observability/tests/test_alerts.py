"""Render the actual chart and verify its alert rules with promtool in Docker."""

from pathlib import Path
import shutil
import subprocess
import tempfile

import yaml


chart = Path(__file__).resolve().parents[1]
rendered = subprocess.check_output(
    ["helm", "template", "obs", str(chart), "--namespace", "observability",
     "--set", "secrets.grafanaAdminPassword=validation-only",
     "--show-only", "charts/prometheus/templates/cm.yaml"], text=True
)
config = yaml.safe_load(rendered)
prometheus = yaml.safe_load(config["data"]["prometheus.yml"])
assert "/etc/config/alerts" in prometheus["rule_files"]
version = config["metadata"]["labels"]["app.kubernetes.io/version"]

with tempfile.TemporaryDirectory(prefix="outbox-alert-tests-") as directory:
    work = Path(directory)
    (work / "alerts.yaml").write_text(config["data"]["alerts"])
    shutil.copyfile(chart / "tests/alerts.test.yaml", work / "alerts.test.yaml")
    for command in (["check", "rules", "alerts.yaml"], ["test", "rules", "alerts.test.yaml"]):
        subprocess.run(
            ["docker", "run", "--rm", "--network=none", "--entrypoint=/bin/promtool",
             "--mount", f"type=bind,source={work},target=/tests,readonly",
             "--workdir=/tests", f"prom/prometheus:{version}", *command], check=True
        )
