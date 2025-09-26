// This requires groovy option turned on in Jenkins.

pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/mjshoemake/prism.git', branch: 'main'
            }
        }

        stage('Build API') {
            steps {
                dir('REST') {
                    bat 'mvn clean package'
                }
            }
        }

        stage('Stop Prism API') {
          steps {
            powershell '''
            Get-CimInstance Win32_Process | Where-Object {
              $_.Name -eq 'java.exe' -and $_.CommandLine -like '*prism-api*.jar*'
            } | ForEach-Object {
            Stop-Process -Id $_.ProcessId -Force
            }
            '''
          }
        }

        stage('Deploy and Run API') {
            steps {
                echo 'Kill any running prism-api jar...'
                bat '''
                powershell -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*prism-api*.jar*' } | Select-Object ProcessId, Name, CommandLine"
                '''

                echo 'Copy the jar to target directory...'
                bat '''
                copy /Y REST\\target\\prism-api-1.0.0.jar C:\\WorkApps\\prism-api\\
                '''

                // These do NOT work.... None of the options will leave the process running
                // when the Jenkins job stops. It always stops once the Jenkins job shuts down.
                //start "" java -jar C:\\WorkApps\\prism-api\\prism-api-1.0.0.jar
                //powershell -ExecutionPolicy Bypass -Command "Start-Process -FilePath 'java' -ArgumentList '-jar C:\\WorkApps\\prism-api\\prism-api-1.0.0.jar' -WindowStyle Hidden"
                //powershell -ExecutionPolicy Bypass -Command "Start-Process -FilePath 'java' -ArgumentList '-jar C:\\WorkApps\\prism-api\\prism-api-1.0.0.jar' -NoNewWindow -WindowStyle Hidden -RedirectStandardOutput 'C:\\WorkApps\\prism-api\\server.log' -RedirectStandardError 'C:\\WorkApps\\prism-api\\error.log'"
                //powershell -ExecutionPolicy Bypass -Command "Start-Process -FilePath 'java' -ArgumentList '-jar C:\\WorkApps\\prism-api\\prism-api-1.0.0.jar' -RedirectStandardOutput 'C:\\WorkApps\\prism-api\\server.log' -RedirectStandardError 'C:\\WorkApps\\prism-api\\error.log'"
                //powershell -ExecutionPolicy Bypass -Command "Start-ScheduledTask -TaskName 'StartPrismAPI'"
                //java -jar prism-api-1.0.0.jar > server.log 2> error.log
                //cd C:\\WorkApps\\prism-api
                //  cmd /c start "" java -jar prism-api-1.0.0.jar
                //powershell 'Start-ScheduledTask -TaskName "StartPrismAPI"'
                //bat 'schtasks /run /TN "StartPrismAPI"'
                //bat 'START "" java -jar C:\\WorkApps\\prism-api\\prism-api-1.0.0.jar'
                //powershell '''
                //    Start-Process -FilePath "java.exe" `
                //    -ArgumentList "-jar C:\\WorkApps\\prism-api\\prism-api-1.0.0.jar" `
                //    -WorkingDirectory "C:\\WorkApps\\prism-api" `
                //    -WindowStyle Hidden `
                //    -NoNewWindow:$false
                //'''

                echo 'Start the jar in the background...'
                bat 'cmd /c start "" java -jar C:\\WorkApps\\prism-api\\prism-api-1.0.0.jar'

                echo 'Check to see if the processes are running...'
                bat '''
                powershell -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*prism-api*.jar*' } | Select-Object ProcessId, Name, CommandLine"
                '''
                echo 'Check to see if the processes are running another way...'
                bat 'powershell -ExecutionPolicy Bypass -File C:\\WorkApps\\prism-api\\check-prism-api.ps1'

                echo 'Check to see if the processes are running a third way...'
                bat '''
                powershell -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -match 'C:\\\\WorkApps\\\\prism-api\\\\prism-api-.*\\.jar' } | Select-Object ProcessId, CommandLine"
                '''
            }
        }
        
        stage('Archive Test Results') {
            steps {
                archiveArtifacts artifacts: 'REST/target/surefire-reports/*.xml', allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            junit 'REST/target/surefire-reports/*.xml'
        }
    }
}

