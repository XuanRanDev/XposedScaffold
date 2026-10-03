# Project guidance

- Keep this repository host-agnostic. Target-specific features belong under `app`.
- Do not add native hooks, hidden DEX, SO protection, Frida, hot update or telemetry to the scaffold core.
- Every user-facing hook must use `@HookItem`; do not maintain a manual registry.
- Business hooks use `HookContext`/`HookBridge`, not loader implementations.
- DexKit is the preferred solution for obfuscated targets. Cache portable descriptors and verify them when loading.
- Preserve both legacy API 82 and modern API 101–102 entries unless a project explicitly drops one.
- Keep UI metadata declarative. Custom Compose pages are reserved for settings that cannot be represented by standard fields.
- Add tests when changing the processor, lifecycle filtering, bridge adapters or cache keys.
