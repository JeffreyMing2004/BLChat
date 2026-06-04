@echo off
echo Starting Bilibili H5 Plugin (Frontend + Backend)...
echo.
echo Starting backend...
start "" cmd /k "cd h5-plugin/server && npm run dev"
echo Starting frontend...
start "" cmd /k "cd h5-plugin/client && npm run dev"
echo.
echo Both processes have been started in separate windows.
echo To stop them, close each window or press Ctrl+C in each.