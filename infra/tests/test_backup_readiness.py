"""Regresión del arranque temporal por socket de la imagen oficial PostgreSQL."""
import importlib.util
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "backup_readiness_tool", Path(__file__).resolve().parents[1] / "backup/backup_tool.py"
)
tool = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tool)


class PostgresReadinessTest(unittest.TestCase):
    def test_socket_ready_does_not_finish_wait_before_final_tcp_server(self):
        final_server_checks = []

        def readiness(command, **kwargs):
            # Durante initdb el socket ya responde, pero el servidor TCP aún no existe.
            if "-h" not in command:
                return SimpleNamespace(returncode=0)
            self.assertEqual(command[command.index("-h") + 1], "127.0.0.1")
            final_server_checks.append(command)
            return SimpleNamespace(returncode=1 if len(final_server_checks) == 1 else 0)

        with patch.object(tool.subprocess, "run", side_effect=readiness), patch.object(
            tool.time, "sleep"
        ):
            tool.wait_for_postgres("fixture-container")
        self.assertEqual(len(final_server_checks), 2)

    def test_timeout_stops_work_when_final_server_never_starts(self):
        with patch.object(tool.subprocess, "run", return_value=SimpleNamespace(returncode=1)), patch.object(
            tool.time, "sleep"
        ):
            with self.assertRaisesRegex(RuntimeError, "TCP"):
                tool.wait_for_postgres("fixture-container", attempts=2)


if __name__ == "__main__":
    unittest.main()
