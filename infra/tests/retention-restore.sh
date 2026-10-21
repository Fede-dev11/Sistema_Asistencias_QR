#!/usr/bin/env bash
set -euo pipefail
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
python3 -m unittest discover -s "$script_dir" -p test_backup_readiness.py
exec python3 "$script_dir/retention_restore.py"
