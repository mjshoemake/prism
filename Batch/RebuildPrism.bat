C:
cd \Work\prism\REST
echo "Running Maven to rebuild Prism..."
call build.bat
echo "Running Maven to rebuild Prism... Done."
cd ../target
echo "Restarting Prism server..."
call run.bat
echo "Restarting Prism server... Done."