# Third-party notices

The MIT license in this repository applies to project-owned code. Dependency
licenses remain in effect; consult each dependency's package and distribution
when shipping an application.

- Gradle Wrapper scripts and wrapper JAR: Gradle authors, Apache License 2.0.
  The scripts retain their copyright notices. See licenses/Apache-2.0.txt.
- React, Vite, Electron, TypeScript, Tailwind CSS, Radix UI / shadcn-derived
  UI components and Supabase JavaScript client are third-party components.
- AndroidX / Jetpack Compose / Room, Kotlin and OkHttp are third-party
  Android components. Inspect the resolved Gradle dependencies for their notices.
- Lucide icons have their own ISC license.

Dependency versions are pinned by the npm lock files and Android Gradle files.
Release builders are responsible for preserving the notices required by all
redistributed runtime dependencies.
