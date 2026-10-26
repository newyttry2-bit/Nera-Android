@rem Minimal Gradle wrapper entry point (see gradlew for notes).
@if "%DEBUG%"=="" @echo off
@rem Use gradle from PATH if available; otherwise see README to generate the
@rem official wrapper with `gradle wrapper` in Android Studio.
gradle %*
@if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
