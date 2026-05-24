@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.4"
set "PATH=C:\Program Files\Java\jdk-21.0.4\bin;%PATH%"
set "SERVER_PORT=8080"
set "SPRING_PROFILES_ACTIVE=localqa"
cd /d "D:\Learning\School-Management-System"
call "D:\Learning\School-Management-System\mvnw.cmd" "-Dspring-boot.run.profiles=localqa" "-Dspring-boot.run.useTestClasspath=true" spring-boot:run > "D:\Learning\School-Management-System\logs\localqa-backend.out.log" 2> "D:\Learning\School-Management-System\logs\localqa-backend.err.log"
